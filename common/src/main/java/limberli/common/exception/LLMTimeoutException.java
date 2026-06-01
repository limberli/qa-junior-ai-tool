package limberli.common.exception;

public class LLMTimeoutException extends RuntimeException {
    public LLMTimeoutException(String message) {
        super(message);
    }

    public LLMTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
