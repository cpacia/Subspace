package Messenger;

/**And exception to be throw when creating an address if the prefix length isn't valid*/
public class InvalidPrefixLengthException extends Exception {
    public InvalidPrefixLengthException() {
        super();
    }

    public InvalidPrefixLengthException(String message) {
        super(message);
    }
}