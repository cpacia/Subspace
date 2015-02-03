package Messenger;

/**
 * Created by chris on 2/3/15.
 */

public class BadHMACException extends Exception {
    public BadHMACException() {
        super();
    }

    public BadHMACException(String message) {
        super(message);
    }
}
