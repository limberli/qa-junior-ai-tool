package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * AgentCard as per A2A Protocol spec — published at /.well-known/agent-card.json
 * protocolVersion, defaultInputModes, defaultOutputModes are required by SDK 0.3.x+
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCardDto(
        String name,
        String description,
        String url,
        String version,
        String protocolVersion,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        AgentCapabilities capabilities,
        List<AgentSkill> skills
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentCapabilities(boolean streaming, boolean pushNotifications) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentSkill(String id, String name, String description, List<String> tags) {}
}
