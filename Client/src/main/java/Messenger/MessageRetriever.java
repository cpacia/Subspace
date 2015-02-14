package Messenger;

import Messenger.Utils.openname.OpennameUtils;
import org.apache.http.HttpException;
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
 * Created by chris on 2/1/15.
 */

public class MessageRetriever {

    private List<MessageListener> listeners = new ArrayList<MessageListener>();
    private List<KeyRing.Key> keys;
    private boolean running = false;
    FileWriter writer = new FileWriter();
    private List<Thread> threads = new ArrayList<>();
    private List<HttpURLConnection> connections = new ArrayList<>();

    public MessageRetriever(List<KeyRing.Key> keys){
        this.keys = keys;
    }

    public void start(){
        running = true;
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

    private void GETfromLocalhost(Address addr){
        while (running){
            StringBuffer response = null;
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            try {
                URL obj = new URL("http://localhost:8080/" +
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
                e.printStackTrace();
            }
            System.out.println(addr.toString() + ": " + response.toString());
            JSONObject resp = null;
            try {
                resp = new JSONObject(response.toString());
            } catch (JSONException e){e.printStackTrace();}
            testMessages(addr, resp);
        }
    }

    private void GETfromTOR(Address addr, String hostname){
        while (running){
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            JSONObject resp = null;
            try{
                resp = TorLib.getJSON(hostname, 8335, addr.getPrefix() + "?timestamp=" +
                        writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            } catch (JSONException | IOException | HttpException e){e.printStackTrace();}
            testMessages(addr, resp);
        }
    }

    private void testMessages(Address addr, JSONObject resp){
        String ts = "0";
        try {
            ts = resp.getString("timestamp");
        } catch (JSONException e){e.printStackTrace();}
        writer.updateGETtime(addr.toString(), ts);
        Iterator<?> keys = resp.keys();
        List<Message> messageList = new ArrayList<>();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (!key.equals("timestamp")) {
                String payloadString = "";
                try {payloadString = resp.getString(key);}
                catch (JSONException e) { e.printStackTrace();}
                byte[] cipherText = hexStringToByteArray(payloadString);
                byte[] privKey = writer.getKeyFromAddress(addr.toString()).getPrivateKey().toByteArray();
                ECKey decryptKey = ECKey.fromPrivOnly(privKey);
                Message m = new Message(cipherText, decryptKey.getPrivKey(), addr);
                if (m.isMessageForMe()){messageList.add(m);}
            }
        }
        Map<Long,Message> sortedMap = new TreeMap<Long, Message>();
        for (Message message : messageList){
            sortedMap.put(message.getTimeStamp(), message);
        }
        for (Map.Entry<Long, Message> entry : sortedMap.entrySet()) {
            Message m = entry.getValue();
            String openname = null;
            if (m.getSenderName().substring(0,1).equals("+")){
                openname = m.getSenderName().substring(1);
                if (!writer.contactExists(m.getFromAddress()) ||
                        !writer.hasOpenname(m.getFromAddress()) ||
                        writer.hasOpennameChanged(m.getFromAddress(), m.getSenderName().substring(1))){
                    String name = OpennameUtils.blockingOpennameDownload(m.getSenderName().substring(1),
                            Main.params.getApplicationDataFolder().toString());
                    if (name!=null){m.setSenderName(name);}
                }
            }
            if (!writer.contactExists(m.getFromAddress())) {
                if (openname!=null) {
                    writer.addContact(m.getFromAddress(), m.getSenderName(), openname);
                }
                else {
                    writer.addContact(m.getFromAddress(), m.getSenderName(), null);
                }
            }
            else if (writer.hasOpenname(m.getFromAddress())) {
                m.setSenderName(writer.getFormattedName(m.getSenderName().substring(1)));
            }
            for (MessageListener l : listeners){l.onMessageReceived(m);}
            System.out.println("Received a message from " + m.getSenderName() + ": " + m.getDecryptedMessage());
        }
        try {Thread.sleep(500);}
        catch (InterruptedException e) {e.printStackTrace();}
    }

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

    public void addListener(MessageListener l){
        listeners.add(l);
    }

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

}
