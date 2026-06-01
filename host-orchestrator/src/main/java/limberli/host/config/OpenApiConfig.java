package limberli.host.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QA Junior AI Tool — Host Orchestrator")
                        .description("Multi-agent QA analysis platform. Orchestrates Tester and Analyst AI agents " +
                                "via A2A Protocol to generate test cases and risk reports from requirements documents.")
                        .version("1.0.0")
                        .contact(new Contact().name("limberli").email("limberlidj@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev"),
                        new Server().url("http://host-orchestrator:8080").description("Docker Compose")
                ));
    }
}
