package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2ATaskArtifact(String name, Integer index, List<A2APart> parts) {}
