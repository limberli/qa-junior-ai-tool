package limberli.analyst.controller;

import limberli.common.a2a.*;
import limberli.analyst.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * A2A-compliant agent controller for the Analyst Agent.
 *
 * Architecture follows a2aResumeAgent's separation of concerns
 * (core service logic ↔ A2A protocol handling) without importing its
 * PostgreSQL/Redis stack which is unnecessary for this use case.
 * See docs/architecture.md ADR-01.
 *
 * Endpoints:
 *   GET  /.well-known/agent-card.json  — A2A agent discovery
 *   POST /                             — A2A JSON-RPC 2.0 (tasks/send)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final RiskAnalysisService riskAnalysisService;

    @Value("${server.port:8082}")
    private int serverPort;

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCardDto agentCard() {
        return new AgentCardDto(
                "Analyst Agent",
                "Performs comprehensive requirements analysis: identifies risks, contradictions, " +
                        "ambiguities, and missing specifications in software requirements documents.",
                "http://localhost:" + serverPort,
                "1.0.0",
                "0.2.9",
                List.of("text/plain"),
                List.of("text/plain"),
                new AgentCardDto.AgentCapabilities(false, false),
                List.of(new AgentCardDto.AgentSkill(
                        "risk-analysis",
                        "Requirements Risk Analysis",
                        "Produces structured risk matrices, contradiction reports, ambiguity analysis, " +
                                "and testability assessments for requirements documents.",
                        List.of("risk-analysis", "requirements", "qa", "business-analysis")
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

        String analysis = riskAnalysisService.analyzeRisks(documentText);

        A2ATaskResult result = new A2ATaskResult(
                request.params().id(),
                new A2ATaskStatus("completed", Instant.now().toString()),
                List.of(new A2ATaskArtifact("risk-analysis", 0, List.of(A2APart.text(analysis))))
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
}
