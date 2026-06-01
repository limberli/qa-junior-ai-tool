package limberli.common.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record A2AResponse(String jsonrpc, Object id, A2ATaskResult result, A2AError error) {

    public boolean isSuccess() {
        return error == null && result != null;
    }
}
