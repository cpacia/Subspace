package Messenger;

/**
 * Created by chris on 2/3/15.
 */
public interface MessageListener {
    void onMessageReceived(Message m);
    void onMessageSent(Message m);
    void onDataReceived(int bytes);
}