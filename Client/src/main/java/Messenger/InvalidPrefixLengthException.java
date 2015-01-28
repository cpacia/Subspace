package Messenger;

public class InvalidPrefixLengthException extends Exception {
    public InvalidPrefixLengthException() {
        super();
    }

    public InvalidPrefixLengthException(String message) {
        super(message);
    }
}