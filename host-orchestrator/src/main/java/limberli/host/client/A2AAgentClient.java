package limberli.host.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import limberli.common.a2a.A2ARequest;
import limberli.common.a2a.A2AResponse;
import limberli.common.exception.AgentUnavailableException;
import limberli.common.exception.LLMTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * A2A Protocol client implementing the JSON-RPC 2.0 "tasks/send" method.
 *
 * Architecture note: the official io.github.a2asdk SDK wraps exactly these
 * same HTTP calls. Using RestClient directly keeps the dependency footprint
 * minimal and gives precise control over serialisation and error handling.
 * Swapping in the SDK is a one-line change per call site.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class A2AAgentClient {

    private final RestClient restClient;

    @Qualifier("agentCallExecutor")
    private final ExecutorService agentCallExecutor;

    /**
     * Sends a task to an A2A-compliant agent and returns the text response.
     *
     * Resilience chain (outer → inner):
     *   CircuitBreaker → TimeLimiter (30 s) → Retry (3×, exponential back-off)
     *
     * TimeLimiter requires CompletableFuture return type. The underlying thread
     * (from agentCallExecutor) continues running past the timeout — this is an
     * accepted trade-off on Java 17 where virtual threads are not available.
     * The thread pool is capped so the worst case is 4 blocked threads, not unbounded.
     */
    @CircuitBreaker(name = "agentCall", fallbackMethod = "fallbackSendTask")
    @TimeLimiter(name = "agentCall")
    @Retry(name = "agentCall")
    public CompletableFuture<String> sendTask(String agentUrl, String documentText, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> doSendTask(agentUrl, documentText, metadata), agentCallExecutor);
    }

    private String doSendTask(String agentUrl, String documentText, Map<String, Object> metadata) {
        String taskId = UUID.randomUUID().toString();
        log.debug("A2A tasks/send → url={} taskId={} docLength={}", agentUrl, taskId, documentText.length());

        A2ARequest request = A2ARequest.sendTask(taskId, documentText, metadata);

        try {
            A2AResponse response = restClient.post()
                    .uri(agentUrl + "/")
                    .body(request)
                    .retrieve()
                    .body(A2AResponse.class);

            if (response == null || !response.isSuccess()) {
                String errorMsg = response != null && response.error() != null
                        ? response.error().message() : "null response";
                throw new LLMTimeoutException("Agent returned error: " + errorMsg);
            }

            String text = response.result().extractText();
            log.debug("A2A response received ← taskId={} responseLength={}", taskId, text.length());
            return text;

        } catch (ResourceAccessException e) {
            // Connection refused, timeout at network layer — agent is down
            throw new AgentUnavailableException("Agent unreachable at " + agentUrl, e);
        } catch (RestClientException e) {
            throw new AgentUnavailableException("Agent HTTP error at " + agentUrl + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused") // called by Resilience4j via reflection
    private CompletableFuture<String> fallbackSendTask(String agentUrl, String documentText, Map<String, Object> metadata, Exception e) {
        log.warn("Agent fallback triggered: url={} cause={}", agentUrl, e.getMessage());
        return CompletableFuture.failedFuture(
                new AgentUnavailableException("Agent at " + agentUrl + " is unavailable after retries: " + e.getMessage(), e)
        );
    }
}
