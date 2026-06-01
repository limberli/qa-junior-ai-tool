package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2ATaskStatus(String state, String timestamp) {}
