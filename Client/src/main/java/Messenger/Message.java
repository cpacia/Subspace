package Messenger;

import com.google.protobuf.ByteString;
import org.bitcoinj.core.AddressFormatException;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.*;

/**
 * Created by chris on 2/1/15.
 */
public class Message {

    private ECKey sendingKey;
    private String fromAddress;
    private String uploadHost;
    private long timeStamp;
    private Address toAddress;
    private String prefix;
    private byte[] payloadBytes;
    private byte[] encryptedPayload;
    private String postKey;

    public Message(Address toAddress, String message, KeyRing.Key fromKey, Payload.MessageType type){
        System.out.println("1");
        this.sendingKey = ECKey.fromPrivOnly(fromKey.getPrivateKey().toByteArray());
        this.fromAddress = fromKey.getAddress();
        this.uploadHost = fromKey.getUploadNode();
        this.timeStamp = System.currentTimeMillis() / 1000L;
        this.toAddress = toAddress;
        this.prefix = toAddress.getPrefix();
        createPostKey();

        Payload.MessageData data = Payload.MessageData.newBuilder()
                .setSenderAddress(this.fromAddress)
                .setMessageType(type)
                .setUnencryptedMessage(message)
                .setTimeStamp(this.timeStamp)
                .build();
        byte[] serializedMesssageData = data.toByteArray();
        byte[] sharedSecret = null;
        byte[] hMACBytes = null;

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try{
            KeyAgreement keyAgree = KeyAgreement.getInstance("ECDH", "BC");
            keyAgree.init(this.sendingKey.getPrivKey());
            keyAgree.doPhase(toAddress.getECKey().getPubKey(), true);
            MessageDigest hash = MessageDigest.getInstance("SHA256", "BC");
            sharedSecret = hash.digest(keyAgree.generateSecret());
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(sharedSecret, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            hMACBytes = sha256_HMAC.doFinal(serializedMesssageData);
        }
        catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e){e.printStackTrace();}
        ByteString serializedData = ByteString.copyFrom(serializedMesssageData);
        ByteString hmac = ByteString.copyFrom(hMACBytes);
        Payload.SignedPayload payload = Payload.SignedPayload.newBuilder()
                .setSerializedMessageData(serializedData)
                .setHMac(hmac)
                .build();
        this.payloadBytes = payload.toByteArray();
        try {
            Cipher c = Cipher.getInstance("ECIESwithAES-CBC");
            c.init(Cipher.ENCRYPT_MODE, toAddress.getECKey().getPubKey(), new SecureRandom());
            encryptedPayload = c.doFinal(payloadBytes, 0, payloadBytes.length);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e){e.printStackTrace();}
    }

    public void send(){
        if (this.uploadHost.equals("localhost")){sendToLocalHost();}
        else {sendToTor();}
    }

    public void sendToTor(){

    }

    public void sendToLocalHost(){
        System.out.println("2");
        try {
            URL url = new URL("http://localhost:8080/" + postKey);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("POST");
            String urlParameters = Hex.toHexString(this.encryptedPayload);
            DataOutputStream wr = new DataOutputStream(httpCon.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            System.out.println("Response Code : " + httpCon.getResponseCode());
        } catch (IOException e){e.printStackTrace();}

    }

    private void createPostKey(){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[12];
        random.nextBytes(bytes);
        String binary = "";
        for (byte b : bytes){
            String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binary = binary + s;
        }
        String binaryKey = this.prefix + binary.substring(0, binary.length()-this.prefix.length());
        postKey = new BigInteger(binaryKey, 2).toString(16);
    }

}
