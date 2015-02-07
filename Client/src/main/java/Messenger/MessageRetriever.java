package Messenger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by chris on 2/1/15.
 */

public class MessageRetriever {

    private List<MessageListener> listeners = new ArrayList<MessageListener>();
    private List<KeyRing.Key> keys;
    private boolean running = false;
    FileWriter writer = new FileWriter();

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
            new Thread(task).start();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(addr.toString() + ": " + response.toString());
            JSONObject resp = null;
            String ts = "0";
            try {
                resp = new JSONObject(response.toString());
                ts = resp.getString("timestamp");
            } catch (JSONException e){e.printStackTrace();}
            writer.updateGETtime(addr.toString(), ts);
            Iterator<?> keys = resp.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!key.equals("timestamp")) {
                    String payloadString = "";
                    try{payloadString = resp.getString(key);}
                    catch (JSONException e){e.printStackTrace();}
                    byte[] cipherText = hexStringToByteArray(payloadString);
                    byte[] privKey = writer.getKeyFromAddress(addr.toString()).getPrivateKey().toByteArray();
                    ECKey decryptKey = ECKey.fromPrivOnly(privKey);
                    Message m = new Message(cipherText, decryptKey.getPrivKey(), addr);
                    if (m.isMessageForMe()) {
                        for (MessageListener l : listeners){l.onMessageReceived(m);}
                        System.out.println("Received a message from " + m.getSenderName() + ": " + m.getDecryptedMessage());
                    }
                }
            }
            try {Thread.sleep(500);}
            catch (InterruptedException e) {e.printStackTrace();}
        }
    }

    private void GETfromTOR(Address addr, String hostname){
        while (running){
            StringBuffer response = null;
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            try {
                URL obj = new URL("http://" + hostname + ":8080/" +
                        addr.getPrefix() + "?timestamp=" +
                        writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(addr.toString() + ": " + response.toString());
            JSONObject resp = null;
            String ts = "0";
            try {
                resp = new JSONObject(response.toString());
                ts = resp.getString("timestamp");
            } catch (JSONException e){e.printStackTrace();}
            writer.updateGETtime(addr.toString(), ts);
            Iterator<?> keys = resp.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!key.equals("timestamp")) {
                    String payloadString = "";
                    try{payloadString = resp.getString(key);}
                    catch (JSONException e){e.printStackTrace();}
                    byte[] cipherText = hexStringToByteArray(payloadString);
                    byte[] privKey = writer.getKeyFromAddress(addr.toString()).getPrivateKey().toByteArray();
                    ECKey decryptKey = ECKey.fromPrivOnly(privKey);
                    Message m = new Message(cipherText, decryptKey.getPrivKey(), addr);
                    if (m.isMessageForMe()) {
                        for (MessageListener l : listeners){l.onMessageReceived(m);}
                        System.out.println("Received a message from " + m.getSenderName() + ": " + m.getDecryptedMessage());
                    }
                }
            }
            try {Thread.sleep(500);}
            catch (InterruptedException e) {e.printStackTrace();}
        }
    }

    public void stop(){
        running = false;
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
