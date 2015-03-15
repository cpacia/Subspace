package Messenger;

/**
 * Exception to be thrown if the signature on received messages is invalid.
 */
public class BadSignatureException extends Exception{
    public BadSignatureException() {
        super();
    }

    public BadSignatureException(String message) {
        super(message);
    }
}