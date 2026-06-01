package limberli.common.a2a;

import java.util.Map;

/**
 * A2A JSON-RPC 2.0 request envelope.
 * Spec: https://google.github.io/A2A/specification/
 */
public record A2ARequest(String jsonrpc, String method, Object id, A2ATaskParams params) {

    public static A2ARequest sendTask(String taskId, String userText) {
        return sendTask(taskId, userText, Map.of());
    }

    public static A2ARequest sendTask(String taskId, String userText, Map<String, Object> metadata) {
        return new A2ARequest(
                "2.0",
                "tasks/send",
                taskId,
                new A2ATaskParams(taskId, null, A2AMessage.user(userText), metadata != null ? metadata : Map.of())
        );
    }
}
