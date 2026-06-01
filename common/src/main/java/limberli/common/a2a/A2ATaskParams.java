package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2ATaskParams(
        String id,
        String sessionId,
        A2AMessage message,
        Map<String, Object> metadata
) {}
