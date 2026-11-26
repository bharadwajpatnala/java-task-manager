package app.exception;

public class BadGatewayException extends ApiException{
    public BadGatewayException(String message) {
        super(502, "BAD_GATEWAY", message);
    }
}
