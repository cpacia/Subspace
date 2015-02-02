package Messenger;

import org.bitcoinj.core.AddressFormatException;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;

/**
 * Created by chris on 2/1/15.
 */
public class MessageRetriever {

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
                else {GETfromTOR();}

            };
            new Thread(task).start();
        }
    }

    private void GETfromLocalhost(Address addr){
        System.out.println(addr.getPrefix());
            StringBuffer response = null;
            System.out.println("Getting messages after " + writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
            try {
                URL obj = new URL("http://localhost:8080/" +
                        addr.getPrefix() + "?timestamp=" +
                        writer.getKeyFromAddress(addr.toString()).getTimeOfLastGET());
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // optional default is GET
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
            try {
                //print result

                JSONObject resp = new JSONObject(response.toString());
                Iterator<?> keys = resp.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String payloadString = resp.getString(key);
                    byte[] encrypted = hexStringToByteArray(payloadString);
                    Cipher c = Cipher.getInstance("ECIESwithAES-CBC");

                    // Testing with null parameters and DHAES mode off
                    byte[] privKey = writer.getKeyFromAddress(addr.toString()).getPrivateKey().toByteArray();
                    c.init(Cipher.DECRYPT_MODE, ECKey.fromPrivOnly(privKey).getPrivKey(), new SecureRandom());
                    byte[] decrypted = c.doFinal(encrypted, 0, encrypted.length);
                    Payload.SignedPayload payload = Payload.SignedPayload.parseFrom(decrypted);
                    Payload.MessageData data = Payload.MessageData.parseFrom(payload.getSerializedMessageData());
                    System.out.println(addr.toString() + ": " + data.getUnencryptedMessage());
                }

            } catch (IOException | JSONException | IllegalBlockSizeException
                    | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
                    | NoSuchPaddingException e) {
                System.out.println(addr.toString() + ": Message not intended for us");
            }
            writer.updateGETtime(addr.toString());
            System.out.println("finished");

    }

    private void GETfromTOR(){

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

}
