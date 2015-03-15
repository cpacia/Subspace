package Messenger;

/**
 * Interface for the listening for messages.
 */
public interface MessageListener {

    //Fires on message received
    void onMessageReceived(Message m);

    //Fires on message sent
    void onMessageSent(Message m);

    //Was going to use this track bandwidth usage but it isn't implemented yet.
    void onDataReceived(int bytes);
}