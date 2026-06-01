package limberli.host.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent endpoint URLs, overridable via environment variables:
 *   AGENTS_TESTER_URL, AGENTS_ANALYST_URL
 */
@ConfigurationProperties(prefix = "agents")
public record AgentProperties(AgentEndpoint tester, AgentEndpoint analyst) {
    public record AgentEndpoint(String url) {}
}
