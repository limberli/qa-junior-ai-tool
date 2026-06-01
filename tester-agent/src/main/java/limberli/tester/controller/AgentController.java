package limberli.tester.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import limberli.common.a2a.*;
import limberli.tester.service.QaSettings;
import limberli.tester.service.TestCaseGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A2A-compliant agent controller for the Tester Agent.
 *
 * Endpoints:
 *   GET  /.well-known/agent-card.json  — A2A agent discovery
 *   POST /                             — A2A JSON-RPC 2.0 (tasks/send)
 *
 * Architectural decision: implemented directly on Spring MVC per A2A Protocol spec
 * (https://google.github.io/A2A/specification/) rather than inheriting from a2awebagent
 * (vishalmysore/a2awebagent). The a2awebagent framework uses a different LLM abstraction
 * layer (tools4ai) which conflicts with Spring AI's ChatClient. Writing against the protocol
 * spec directly gives full control over Spring AI integration and avoids transitive
 * dependency conflicts. See docs/architecture.md ADR-01 for details.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final TestCaseGenerationService testCaseGenerationService;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8081}")
    private int serverPort;

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCardDto agentCard() {
        return new AgentCardDto(
                "Tester Agent",
                "Generates structured, comprehensive test cases from software requirements documents. " +
                        "Produces positive/negative/boundary test cases with IDs, steps, and expected results.",
                "http://localhost:" + serverPort,
                "1.0.0",
                "0.2.9",
                List.of("text/plain"),
                List.of("text/plain"),
                new AgentCardDto.AgentCapabilities(false, false),
                List.of(new AgentCardDto.AgentSkill(
                        "test-case-generation",
                        "Test Case Generation",
                        "Analyzes requirements and produces structured test cases covering happy paths, " +
                                "error handling, boundary values, and security scenarios.",
                        List.of("testing", "qa", "requirements-analysis", "test-design")
                ))
        );
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<A2AResponse> handleTask(@RequestBody A2ARequest request) {
        log.info("Received A2A task: method={} taskId={}", request.method(), request.id());

        if (!"tasks/send".equals(request.method())) {
            return ResponseEntity.badRequest().body(
                    new A2AResponse("2.0", request.id(), null,
                            new A2AError(-32601, "Method not found: " + request.method()))
            );
        }

        String documentText = extractText(request.params());
        if (documentText == null || documentText.isBlank()) {
            return ResponseEntity.badRequest().body(
                    new A2AResponse("2.0", request.id(), null,
                            new A2AError(-32602, "params.message.parts must contain a text part"))
            );
        }

        QaSettings settings = extractSettings(request.params());
        String testCases = testCaseGenerationService.generateTestCases(documentText, settings);

        A2ATaskResult result = new A2ATaskResult(
                request.params().id(),
                new A2ATaskStatus("completed", Instant.now().toString()),
                List.of(new A2ATaskArtifact("test-cases", 0, List.of(A2APart.text(testCases))))
        );

        return ResponseEntity.ok(new A2AResponse("2.0", request.id(), result, null));
    }

    private String extractText(A2ATaskParams params) {
        if (params == null || params.message() == null || params.message().parts() == null) {
            return null;
        }
        return params.message().parts().stream()
                .filter(p -> p.hasTextKind() && p.text() != null)
                .map(A2APart::text)
                .findFirst()
                .orElse(null);
    }

    /** Reads generation settings from A2A {@code params.metadata.qa}; returns null if absent. */
    private QaSettings extractSettings(A2ATaskParams params) {
        if (params == null || params.metadata() == null) {
            return null;
        }
        Object qa = params.metadata().get("qa");
        if (qa == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(qa, QaSettings.class);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring malformed qa settings in metadata: {}", e.getMessage());
            return null;
        }
    }
}
