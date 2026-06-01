package limberli.tester.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component("ollama")
@Profile("!groq")
@Slf4j
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    @Value("${spring.ai.openai.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    public OllamaHealthIndicator() {
        this.restClient = RestClient.create();
    }

    @Override
    public Health health() {
        try {
            restClient.get()
                    .uri(ollamaBaseUrl + "/api/tags")
                    .retrieve()
                    .body(String.class);
            return Health.up().withDetail("url", ollamaBaseUrl).build();
        } catch (RestClientException e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return Health.down().withDetail("url", ollamaBaseUrl).withDetail("error", e.getMessage()).build();
        }
    }
}
