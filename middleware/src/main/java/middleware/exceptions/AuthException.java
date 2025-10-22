package middleware.exceptions;

public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
