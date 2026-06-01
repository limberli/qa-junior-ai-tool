package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A2A message part. Supports both old protocol (type="text") and new SDK 0.3.x (kind="text").
 * Factory method uses kind for outgoing responses (new spec); isText() checks both for parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record A2APart(String type, String kind, String text) {

    public static A2APart text(String text) {
        return new A2APart(null, "text", text);
    }

    public boolean hasTextKind() {
        return "text".equals(kind) || "text".equals(type);
    }
}
