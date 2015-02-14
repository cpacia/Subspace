package Messenger;

/**
 * Created by chris on 2/14/15.
 */
public class BadSignatureException extends Exception{
    public BadSignatureException() {
        super();
    }

    public BadSignatureException(String message) {
        super(message);
    }
}