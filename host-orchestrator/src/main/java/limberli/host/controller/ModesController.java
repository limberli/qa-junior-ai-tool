package limberli.host.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import limberli.host.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Exposes the generation-settings schema to the UI by proxying the tester agent's /modes.
 *
 * Keeps a single source of truth (the tester's qa-prompts.yml) while giving the UI one
 * public entry point (the orchestrator). If the tester is unreachable, returns 503 so the
 * UI can fall back to its built-in default config.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Modes", description = "Generation-settings schema for the UI")
public class ModesController {

    private final RestClient restClient;
    private final AgentProperties agentProperties;

    @GetMapping("/modes")
    @Operation(summary = "Get available generation modes and their settings schema")
    public ResponseEntity<Object> modes() {
        String url = agentProperties.tester().url() + "/modes";
        try {
            Object schema = restClient.get().uri(url).retrieve().body(Object.class);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            log.warn("Could not fetch modes schema from tester at {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
