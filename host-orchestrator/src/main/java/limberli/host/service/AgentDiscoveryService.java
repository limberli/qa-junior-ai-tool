package limberli.host.service;

import jakarta.annotation.PostConstruct;
import limberli.common.a2a.AgentCardDto;
import limberli.host.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers registered agents at startup by fetching their AgentCards.
 *
 * Agents are pre-configured via application.yml (agents.tester.url, agents.analyst.url).
 * Discovery fetches /.well-known/agent-card.json from each configured URL and caches
 * the AgentCard for introspection (/api/agents endpoint).
 *
 * Failure is non-fatal: a warning is logged and the agent is marked as "unconfigured".
 * OrchestrationService will attempt to call agents regardless — this discovery is
 * purely informational (name, capabilities, skills).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentDiscoveryService {

    private final RestClient restClient;
    private final AgentProperties agentProperties;

    private final Map<String, AgentCardDto> discoveredCards = new ConcurrentHashMap<>();

    @PostConstruct
    public void discoverAll() {
        discover("tester", agentProperties.tester().url());
        discover("analyst", agentProperties.analyst().url());
    }

    private void discover(String alias, String baseUrl) {
        try {
            AgentCardDto card = restClient.get()
                    .uri(baseUrl + "/.well-known/agent-card.json")
                    .retrieve()
                    .body(AgentCardDto.class);

            if (card != null) {
                discoveredCards.put(alias, card);
                log.info("Agent discovered: alias={} name='{}' url={}", alias, card.name(), baseUrl);
            }
        } catch (RestClientException e) {
            // Agent might not be up yet; orchestration still proceeds using configured URLs.
            log.warn("Agent discovery failed (non-fatal): alias={} url={} reason={}", alias, baseUrl, e.getMessage());
        }
    }

    public Map<String, AgentCardDto> getDiscoveredCards() {
        return Map.copyOf(discoveredCards);
    }
}
