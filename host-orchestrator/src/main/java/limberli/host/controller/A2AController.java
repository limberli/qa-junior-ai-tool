package limberli.host.controller;

import io.swagger.v3.oas.annotations.Hidden;
import limberli.common.a2a.*;
import limberli.common.dto.AnalysisResponse;
import limberli.host.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Exposes the Host Orchestrator as an A2A-compliant agent.
 *
 * AgentCard:  GET  /.well-known/agent-card.json
 * Tasks:      POST /   (JSON-RPC 2.0)
 *   - method "message/send"  — A2A SDK 0.3.x+ (a2a-ui)
 *   - method "tasks/send"    — legacy internal protocol (tester/analyst agents)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Hidden
public class A2AController {

    private final OrchestrationService orchestrationService;

    @Value("${agent.public-url:http://localhost:8080}")
    private String agentPublicUrl;

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCardDto agentCard() {
        return new AgentCardDto(
                "QA Analysis Orchestrator",
                "Orchestrates Tester and Analyst AI agents to provide comprehensive QA analysis " +
                        "of software requirements: test cases, risk analysis, and contradiction detection.",
                agentPublicUrl,
                "1.0.0",
                "0.2.9",
                List.of("text/plain"),
                List.of("text/plain"),
                new AgentCardDto.AgentCapabilities(false, false),
                List.of(new AgentCardDto.AgentSkill(
                        "requirements-analysis",
                        "Requirements Analysis",
                        "End-to-end QA analysis: generates structured test cases and identifies risks, " +
                                "contradictions, and ambiguities in requirements documents.",
                        List.of("qa", "requirements", "test-cases", "risk-analysis")
                ))
        );
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleTask(@RequestBody A2ARequest request) {
        log.info("A2A request received: method={} id={}", request.method(), request.id());

        return switch (request.method()) {
            case "message/send" -> handleMessageSend(request);
            case "tasks/send"   -> handleTasksSend(request);
            default -> ResponseEntity.badRequest().body(
                    new A2AResponse("2.0", request.id(), null,
                            new A2AError(-32601, "Method not found: " + request.method()))
            );
        };
    }

    /** A2A SDK 0.3.x+ protocol used by a2a-ui. Returns a Task object. */
    private ResponseEntity<?> handleMessageSend(A2ARequest request) {
        String documentText = extractText(request.params());
        if (documentText == null || documentText.isBlank()) {
            return ResponseEntity.badRequest().body(
                    new A2AResponse("2.0", request.id(), null,
                            new A2AError(-32602, "message.parts must contain at least one text part"))
            );
        }

        String contextId = request.params() != null && request.params().message() != null
                && request.params().message().contextId() != null
                ? request.params().message().contextId()
                : UUID.randomUUID().toString();

        AnalysisResponse analysis = orchestrationService.analyze(documentText, request.params().metadata());

        String aggregatedText = "## Test Cases\n\n" + analysis.testerResponse() +
                "\n\n---\n\n## Risk Analysis\n\n" + analysis.analystResponse();

        A2ATask task = new A2ATask(
                analysis.conversationId().toString(),
                contextId,
                "task",
                new A2ATaskStatus("completed", Instant.now().toString()),
                List.of(new A2ANewArtifact(
                        UUID.randomUUID().toString(),
                        "analysis",
                        List.of(A2APart.text(aggregatedText))
                ))
        );

        record TaskResponse(String jsonrpc, Object id, A2ATask result) {}
        return ResponseEntity.ok(new TaskResponse("2.0", request.id(), task));
    }

    /** Legacy tasks/send protocol used by internal tester and analyst agents. */
    private ResponseEntity<A2AResponse> handleTasksSend(A2ARequest request) {
        String documentText = extractText(request.params());
        if (documentText == null || documentText.isBlank()) {
            return ResponseEntity.badRequest().body(
                    new A2AResponse("2.0", request.id(), null,
                            new A2AError(-32602, "message.parts must contain at least one text part"))
            );
        }

        AnalysisResponse analysis = orchestrationService.analyze(documentText);

        String aggregatedText = "## Test Cases\n\n" + analysis.testerResponse() +
                "\n\n---\n\n## Risk Analysis\n\n" + analysis.analystResponse();

        A2ATaskResult result = new A2ATaskResult(
                request.params().id(),
                new A2ATaskStatus("completed", Instant.now().toString()),
                List.of(new A2ATaskArtifact("analysis", 0, List.of(A2APart.text(aggregatedText))))
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
