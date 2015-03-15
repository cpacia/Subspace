package Messenger;

import Messenger.Utils.openname.OpennameUtils;
import org.apache.http.HttpException;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLSocket;
import javax.xml.ws.http.HTTPException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * For each address we own, this class will make a long polling GET from a server,
 * test retrieved messages to see if they belong to us and notify the appropriate listeners.
 */

public class MessageRetriever {

    //Listeners that we registered by other classes
    private List<MessageListener> listeners = new ArrayList<MessageListener>();

    //Keys loaded from disk
    private List<KeyRing.Key> keys;

    //Boolean which tests whether the MessageRetreiver is running
    private boolean running = false;

    //Our writer for reading and writing to disk
    FileWriter writer = new FileWriter();

    //List of active threads
    private List<Thread> threads = new ArrayList<>();

    //List of open HttpURLConnections (for local host only)
    private List<HttpURLConnection> connections = new ArrayList<>();

    //Addresses that we are no longer interested in watching
    private List<String> deletedAddresses = new ArrayList<>();

    //Boolean for whether we have the error dialog open (we don't want to load another one if it's already open).
    private boolean errorDialogOpen = false;

    /**Create the MessageRetriever by passing in a list of address keys*/
    public MessageRetriever(List<KeyRing.Key> keys){
        this.keys = keys;
    }

    /**Start the MessageRetriever*/
    public void start(){
        running = true;
        //For each key we're interested in, start a loop making GET requests from the server.
        for (KeyRing.Key key : keys){
            Runnable task = () -> {
                Address addr = null;
                try{
                    addr = new Address(key.getPrefixLength(),
                            ECKey.fromPrivOnly(key.getPrivateKey().toByteArray()));
                } catch (InvalidPrefixLengthException e){e.printStackTrace();}
                if (key.getUploadNode().equals("localhost")){GETfromLocalhost(addr);}
                else {GETfromTOR(addr, key.getUploadNode());}

            };
            Thread t = new Thread(task);
            t.start();
            threads.add(t);
        }
    }

    /**Runs a loop making repeated long polling GET requests to a server running on localhost*/
    private void GETfromLocalhost(Address addr){
        while (running){
            StringBuffer response = null;
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            //Make GET request
            try {
                //The GET format is url/ + prefix + ?timestamp=
                //The timestamp is the time of the last GET request. We load it from file.
                URL obj = new URL("http://localhost:8335/" +
                        addr.getPrefix() + "?timestamp=" +
                        writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                connections.add(con);
                con.setRequestMethod("GET");
                int responseCode = con.getResponseCode();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                connections.remove(con);
            } catch (IOException e) {
                //Display an error dialog if something goes wrong
                if (!errorDialogOpen) {
                    errorDialogOpen = true;
                    Action response2 = Dialogs.create()
                            .owner(Main.getStage())
                            .title("Error")
                            .style(DialogStyle.CROSS_PLATFORM_DARK)
                            .masthead("Failed to connect to localhost")
                            .message("Make sure the server is running on port 8335.")
                            .actions(Dialog.Actions.OK)
                            .showError();
                    if (response2 == Dialog.Actions.OK) {
                        errorDialogOpen = false;
                        break;
                    }
                }
            }
            System.out.println(addr.toString() + ": " + response.toString());
            JSONObject resp = null;
            try {
                resp = new JSONObject(response.toString());
            } catch (JSONException e){e.printStackTrace();}
            //If we no longer to need messages from this address then break from the loop
            if (deletedAddresses.contains(addr.toString())){
                deletedAddresses.remove(addr.toString());
                break;
            }
            //Test to see if the message belongs to us
            testMessages(addr, resp);
        }
    }

    /**Runs a loop making repeated long polling GET requests over Tor*/
    private void GETfromTOR(Address addr, String hostname){
        while (running){
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            JSONObject resp = null;
            //Make the GET request using the TorLib class
            try{
                //The GET format is url/ + prefix + ?timestamp=
                //The timestamp is the time of the last GET request. We load it from file.
                resp = TorLib.getJSON(hostname, 8335, addr.getPrefix() + "?timestamp=" +
                        writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            } catch (JSONException | IOException | HttpException e){
                //Show an error dialog if something goes wrong
                if (!errorDialogOpen) {
                    errorDialogOpen = true;
                    Action response = Dialogs.create()
                            .owner(Main.getStage())
                            .title("Error")
                            .style(DialogStyle.CROSS_PLATFORM_DARK)
                            .masthead("Failed to connect to " + hostname)
                            .message("The server might not be available. Try again later.")
                            .actions(Dialog.Actions.OK)
                            .showError();
                    if (response == Dialog.Actions.OK) {
                        errorDialogOpen = false;
                        break;
                    }
                }
            }
            //If we are no longer interested in this address then break from the loop
            if (deletedAddresses.contains(addr.toString())){
                deletedAddresses.remove(addr.toString());
                break;
            }
            //Test the messages to see if they are ours
            testMessages(addr, resp);
        }
    }

    /**Test the message to see if it belongs to us*/
    private void testMessages(Address addr, JSONObject resp){
        //The server response contains the timestamp of the latest message.
        //We get it and save it to disk so next time we don't download messages we already have.
        String ts = "0";
        try {
            ts = resp.getString("timestamp");
        } catch (JSONException e){e.printStackTrace();}
        writer.updateGETtime(addr.toString(), ts);
        //Iterate over the messages in the response
        Iterator<?> keys = resp.keys();
        List<Message> messageList = new ArrayList<>();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            //We can skip over the timestamp as we are only interested in the messages
            if (!key.equals("timestamp")) {
                //Parse the payload and test to see if it's for us
                String payloadString = "";
                try {payloadString = resp.getString(key);}
                catch (JSONException e) { e.printStackTrace();}
                byte[] cipherText = hexStringToByteArray(payloadString);
                byte[] privKey = writer.getKeyFromAddress(addr.toString()).getPrivateKey().toByteArray();
                ECKey decryptKey = ECKey.fromPrivOnly(privKey);
                Message m = new Message(cipherText, decryptKey.getPrivKey(), addr);
                //If the message is for us, add it to a list so we can deal with it after the iterator finishes
                if (m.isMessageForMe()){messageList.add(m);}
            }
        }
        //Sort the messages by timestamp in case they weren't ordered when they came from the server.
        Map<Long,Message> sortedMap = new TreeMap<Long, Message>();
        for (Message message : messageList){
            sortedMap.put(message.getTimeStamp(), message);
        }
        //For each message get the openname (if one exists) and create a new contact for the sender (if one doesn't already exist)
        for (Map.Entry<Long, Message> entry : sortedMap.entrySet()) {
            Message m = entry.getValue();
            //Check for openname
            String openname = null;
            if (m.getSenderName().substring(0,1).equals("+")){
                openname = m.getSenderName().substring(1);
                //Contact doesn't exist? Contact doesn't have an openname? Contact's openname has changed?
                if (!writer.contactExists(m.getFromAddress()) ||
                        !writer.hasOpenname(m.getFromAddress()) ||
                        writer.hasOpennameChanged(m.getFromAddress(), m.getSenderName().substring(1))){
                    //Download the openname profile. We need to block while we do this or else the profile wont show
                    //when the user sees the message.
                    String name = OpennameUtils.blockingOpennameDownload(m.getSenderName().substring(1),
                            Main.params.getApplicationDataFolder().toString());
                    //If the openname downloaded properly then we set the formatted name on message so it shows to the user
                    if (name!=null){m.setSenderName(name);}
                }
            }
            //If the contact doesn't exist, create one and save it to our contacts file
            if (!writer.contactExists(m.getFromAddress()) && !writer.keyExists(m.getFromAddress())
                    && !writer.keyExists(m.getToAddress().toString())) {
                //If it has an openname make sure we save it when we create the contact.
                if (openname!=null) {
                    writer.addContact(m.getFromAddress(), m.getSenderName(), openname);
                }
                else {
                    writer.addContact(m.getFromAddress(), m.getSenderName(), null);
                }
            }
            //If the contact does exit and it has an openname, set it on the message so the user will see it.
            else if (writer.hasOpenname(m.getFromAddress())) {
                m.setSenderName(writer.getFormattedName(m.getSenderName().substring(1)));
            }
            //Notify our listeners that we received a message.
            for (MessageListener l : listeners){l.onMessageReceived(m);}
            System.out.println("Received a message from " + m.getSenderName() + ": " + m.getDecryptedMessage());
        }
        try {Thread.sleep(500);}
        catch (InterruptedException e) {e.printStackTrace();}
    }

    /**Stops the MessageListener by shutting down threads, sockets, and URL connections*/
    public void stop(){
        running = false;
        for (Thread t : threads){
            t.stop();
        }
        for (SSLSocket s : TorLib.openSockets){
            try{s.close();}
            catch (IOException e){e.printStackTrace();}
        }
        for (HttpURLConnection con : connections){
            con.disconnect();
        }
    }

    /**A utility method for converting a hex string to a byte array*/
    public static byte[] hexStringToByteArray(String s) {
        byte[] data = null;
        try {
            int len = s.length();
            data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
        } catch (IndexOutOfBoundsException e){data = new byte[]{0x00, 0x00};}
        return data;
    }

    /**Add a new message listener to be notified when a message is received*/
    public void addListener(MessageListener l){
        listeners.add(l);
    }

    /**
     * Method to watch a new key.
     * Starts a new loop making GET requests.
     */
    public void addWatchKey(KeyRing.Key key){
        Runnable task = () -> {
            Address addr = null;
            try{
                addr = new Address(key.getPrefixLength(),
                        ECKey.fromPrivOnly(key.getPrivateKey().toByteArray()));
            } catch (InvalidPrefixLengthException e){e.printStackTrace();}
            if (key.getUploadNode().equals("localhost")){GETfromLocalhost(addr);}
            else {GETfromTOR(addr, key.getUploadNode());}

        };
        new Thread(task).start();
    }

    /**Add a new address to the deleted address list. This will close the loop after if finishes.*/
    public void closeDeletedAddressThread(String address){
        this.deletedAddresses.add(address);
    }

}
