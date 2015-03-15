package Messenger;

/**
 * Exception to be thrown if the Hmac check on received messages fails.
 */

public class BadHMACException extends Exception {
    public BadHMACException() {
        super();
    }

    public BadHMACException(String message) {
        super(message);
    }
}
