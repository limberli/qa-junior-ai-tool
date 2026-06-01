package limberli.host.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Dedicated thread pool for agent calls. Sized at 2× agent count so parallel
     * calls to Tester + Analyst always have threads available without contention.
     * Using a fixed pool (not virtual threads) for Java 17 compatibility.
     */
    @Bean(name = "agentCallExecutor")
    public ExecutorService agentCallExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
