package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2ATaskResult(
        String id,
        A2ATaskStatus status,
        List<A2ATaskArtifact> artifacts
) {
    public String extractText() {
        if (artifacts == null || artifacts.isEmpty()) {
            return "";
        }
        return artifacts.stream()
                .filter(a -> a.parts() != null)
                .flatMap(a -> a.parts().stream())
                .filter(p -> p.hasTextKind() && p.text() != null)
                .map(A2APart::text)
                .collect(Collectors.joining("\n"));
    }
}
