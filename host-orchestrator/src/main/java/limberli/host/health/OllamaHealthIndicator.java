package limberli.host.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component("ollama")
@RequiredArgsConstructor
@Slf4j
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Override
    public Health health() {
        try {
            String response = restClient.get()
                    .uri(ollamaBaseUrl + "/api/tags")
                    .retrieve()
                    .body(String.class);

            return Health.up()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("status", "reachable")
                    .build();
        } catch (RestClientException e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
