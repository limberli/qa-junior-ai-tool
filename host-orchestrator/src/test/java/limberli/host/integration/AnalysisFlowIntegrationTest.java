package limberli.host.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import limberli.common.dto.AnalysisRequest;
import limberli.common.dto.AnalysisResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the full POST /api/analyze flow.
 *
 * Two WireMock servers stand in for the Tester and Analyst agents.
 * The test verifies JSON-RPC request/response handling, parallel orchestration,
 * and H2 persistence — without any real Ollama or agent processes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalysisFlowIntegrationTest {

    static WireMockServer testerMock;
    static WireMockServer analystMock;

    @BeforeAll
    static void startMocks() {
        testerMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        analystMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        testerMock.start();
        analystMock.start();
    }

    @AfterAll
    static void stopMocks() {
        testerMock.stop();
        analystMock.stop();
    }

    @DynamicPropertySource
    static void overrideAgentUrls(DynamicPropertyRegistry registry) {
        registry.add("agents.tester.url", () -> "http://localhost:" + testerMock.port());
        registry.add("agents.analyst.url", () -> "http://localhost:" + analystMock.port());
        // Use dev profile (H2 in-memory) for tests
        registry.add("spring.profiles.active", () -> "dev");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void stubAgents() {
        // AgentCard stubs (for AgentDiscoveryService @PostConstruct)
        testerMock.stubFor(get("/.well-known/agent-card.json")
                .willReturn(okJson(testerAgentCardJson())));
        analystMock.stubFor(get("/.well-known/agent-card.json")
                .willReturn(okJson(analystAgentCardJson())));

        // A2A tasks/send stubs
        testerMock.stubFor(post("/")
                .willReturn(okJson(testerTaskResponseJson())));
        analystMock.stubFor(post("/")
                .willReturn(okJson(analystTaskResponseJson())));
    }

    @Test
    void givenValidDocument_whenAnalyze_thenReturnsAggregatedResponse() {
        AnalysisRequest request = new AnalysisRequest("As a user I want to log in with email and password.");

        ResponseEntity<AnalysisResponse> response = restTemplate.postForEntity(
                "/api/analyze", request, AnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalysisResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.conversationId()).isNotNull();
        assertThat(body.testerResponse()).contains("TC-001");
        assertThat(body.analystResponse()).contains("RISK");
        assertThat(body.analyzedAt()).isNotNull();
    }

    @Test
    void givenBlankDocument_whenAnalyze_thenReturnsBadRequest() {
        AnalysisRequest request = new AnalysisRequest("   ");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/analyze", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenPriorAnalysis_whenGetHistory_thenReturnsPaginatedResults() {
        // Perform an analysis to create a record
        restTemplate.postForEntity("/api/analyze",
                new AnalysisRequest("History test document"), AnalysisResponse.class);

        ResponseEntity<String> history = restTemplate.getForEntity(
                "/api/history?page=0&size=5", String.class);

        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).contains("content");
    }

    // ===== JSON fixtures =====

    private static String testerAgentCardJson() {
        return """
                {
                  "name": "Tester Agent",
                  "description": "Generates test cases",
                  "url": "http://localhost",
                  "version": "1.0.0",
                  "capabilities": {"streaming": false, "pushNotifications": false},
                  "skills": []
                }
                """;
    }

    private static String analystAgentCardJson() {
        return """
                {
                  "name": "Analyst Agent",
                  "description": "Risk analysis",
                  "url": "http://localhost",
                  "version": "1.0.0",
                  "capabilities": {"streaming": false, "pushNotifications": false},
                  "skills": []
                }
                """;
    }

    private static String testerTaskResponseJson() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": "task-1",
                  "result": {
                    "id": "task-1",
                    "status": {"state": "completed", "timestamp": "2024-01-01T00:00:00Z"},
                    "artifacts": [
                      {
                        "name": "test-cases",
                        "index": 0,
                        "parts": [{"type": "text", "text": "TC-001: Verify login with valid credentials"}]
                      }
                    ]
                  }
                }
                """;
    }

    private static String analystTaskResponseJson() {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": "task-2",
                  "result": {
                    "id": "task-2",
                    "status": {"state": "completed", "timestamp": "2024-01-01T00:00:00Z"},
                    "artifacts": [
                      {
                        "name": "risk-analysis",
                        "index": 0,
                        "parts": [{"type": "text", "text": "RISK-001: No password complexity rules specified"}]
                      }
                    ]
                  }
                }
                """;
    }
}
