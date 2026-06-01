package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2AMessage(String role, List<A2APart> parts, String contextId, String messageId) {

    public static A2AMessage user(String text) {
        return new A2AMessage("user", List.of(A2APart.text(text)), null, null);
    }
}
