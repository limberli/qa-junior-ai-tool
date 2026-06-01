package limberli.host.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import limberli.common.a2a.AgentCardDto;
import limberli.common.dto.AnalysisRequest;
import limberli.common.dto.AnalysisResponse;
import limberli.common.dto.ConversationDto;
import limberli.host.repository.Conversation;
import limberli.host.repository.ConversationRepository;
import limberli.host.service.AgentDiscoveryService;
import limberli.host.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Analysis", description = "Document analysis and conversation history")
public class AnalysisController {

    private final OrchestrationService orchestrationService;
    private final ConversationRepository conversationRepository;
    private final AgentDiscoveryService discoveryService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze a requirements document",
            description = "Sends the document to Tester and Analyst agents in parallel via A2A Protocol, " +
                    "aggregates their responses, and persists the conversation.")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        return ResponseEntity.ok(orchestrationService.analyze(request.documentText()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get analysis history (paginated, newest first)")
    public ResponseEntity<Page<ConversationDto>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") @Max(50) int size) {

        Page<ConversationDto> result = conversationRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toDto);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/agents")
    @Operation(summary = "List discovered agents and their capabilities")
    public ResponseEntity<Map<String, AgentCardDto>> agents() {
        return ResponseEntity.ok(discoveryService.getDiscoveredCards());
    }

    private ConversationDto toDto(Conversation c) {
        String preview = c.getDocumentText().length() > 200
                ? c.getDocumentText().substring(0, 200) + "…"
                : c.getDocumentText();
        return new ConversationDto(c.getId(), preview, c.getTesterResponse(), c.getAnalystResponse(), c.getCreatedAt());
    }
}
