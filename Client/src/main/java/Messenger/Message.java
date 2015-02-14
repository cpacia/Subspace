package Messenger;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bitcoinj.core.AddressFormatException;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.util.encoders.Hex;

import javax.annotation.Nullable;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.*;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by chris on 2/1/15.
 */
public class Message {

    private String senderName;
    private ECKey sendingKey;
    private String fromAddress;
    private String uploadHost;
    private long timeStamp;
    private Address toAddress;
    private String prefix;
    private byte[] payloadBytes;
    private byte[] encryptedPayload;
    private String postKey;
    private boolean isForMe = false;
    private String message;
    private String subject;
    private Payload.MessageType messageType;

    public Message(Address toAddress, String message, KeyRing.Key fromKey, Payload.MessageType type,
                   @Nullable String subject){
        this.messageType = type;
        if (fromKey.hasOpenname()){this.senderName="+"+fromKey.getOpenname();}
        else{this.senderName = fromKey.getName();}
        this.message = message;
        this.sendingKey = ECKey.fromPrivOnly(fromKey.getPrivateKey().toByteArray());
        this.fromAddress = fromKey.getAddress();
        this.uploadHost = fromKey.getUploadNode();
        this.timeStamp = System.currentTimeMillis() / 1000L;
        this.toAddress = toAddress;
        this.prefix = toAddress.getPrefix();
        this.subject = subject;
        createPostKey();

        Payload.MessageData data = null;
        if (this.messageType == Payload.MessageType.CHAT) {
            data = Payload.MessageData.newBuilder()
                    .setSenderAddress(this.fromAddress)
                    .setMessageType(type)
                    .setUnencryptedMessage(message)
                    .setTimeStamp(this.timeStamp)
                    .setName(this.senderName)
                    .build();
        } else if (this.messageType == Payload.MessageType.EMAIL) {
            data = Payload.MessageData.newBuilder()
                    .setSenderAddress(this.fromAddress)
                    .setMessageType(type)
                    .setUnencryptedMessage(message)
                    .setSubject(subject)
                    .setTimeStamp(this.timeStamp)
                    .setName(this.senderName).build();
        }
        else if (this.messageType == Payload.MessageType.CHATROOM) {
            data = Payload.MessageData.newBuilder()
                    .setSenderAddress(this.fromAddress)
                    .setMessageType(type)
                    .setUnencryptedMessage(message)
                    .setTimeStamp(this.timeStamp)
                    .setName(this.senderName).build();
        }
        byte[] serializedMesssageData = data.toByteArray();
        ByteString serializedData = ByteString.copyFrom(serializedMesssageData);
        ByteString hmacOrSig = null;

        if (this.messageType == Payload.MessageType.CHAT || this.messageType == Payload.MessageType.EMAIL) {
            byte[] hMACBytes = null;
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            try {
                KeyAgreement keyAgree = KeyAgreement.getInstance("ECDH", "BC");
                keyAgree.init(this.sendingKey.getPrivKey());
                keyAgree.doPhase(toAddress.getECKey().getPubKey(), true);
                MessageDigest hash = MessageDigest.getInstance("SHA256", "BC");
                byte[] sharedSecret = hash.digest(keyAgree.generateSecret());
                Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
                SecretKeySpec secret_key = new SecretKeySpec(sharedSecret, "HmacSHA256");
                sha256_HMAC.init(secret_key);
                hMACBytes = sha256_HMAC.doFinal(serializedMesssageData);
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
                e.printStackTrace();
            }
            hmacOrSig = ByteString.copyFrom(hMACBytes);
        } else if (this.messageType == Payload.MessageType.CHATROOM){
            byte[] signature = null;
            try {
                Signature ecdsa = Signature.getInstance("SHA256withECDSA");
                ecdsa.initSign(this.sendingKey.getPrivKey());
                ecdsa.update(serializedMesssageData);
                signature = ecdsa.sign();
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e){e.printStackTrace();}
            hmacOrSig = ByteString.copyFrom(signature);
        }
        Payload.SignedPayload payload = Payload.SignedPayload.newBuilder()
                .setSerializedMessageData(serializedData)
                .setHMacOrSig(hmacOrSig)
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

    public Message(byte[] cipherText, ECPrivateKey decryptKey, Address toAddr){
        this.toAddress = toAddr;
        Payload.SignedPayload payload = null;
        Payload.MessageData data = null;
        byte[] decrypted = null;

        try {
            this.encryptedPayload = cipherText;
            Cipher c = Cipher.getInstance("ECIESwithAES-CBC");
            c.init(Cipher.DECRYPT_MODE, decryptKey, new SecureRandom());
            decrypted = c.doFinal(cipherText, 0, cipherText.length);
            isForMe = true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e){isForMe=false;}

        if (isForMe){
            try {
                payload = Payload.SignedPayload.parseFrom(decrypted);
                data = Payload.MessageData.parseFrom(payload.getSerializedMessageData());
                this.fromAddress = data.getSenderAddress();
                this.timeStamp = data.getTimeStamp();
                this.message = data.getUnencryptedMessage();
                this.senderName = data.getName();
                this.messageType = data.getMessageType();
                if (this.messageType == Payload.MessageType.EMAIL){
                    this.subject = data.getSubject();
                }
            } catch (InvalidProtocolBufferException e){isForMe = false;}
            if (this.messageType == Payload.MessageType.CHAT || this.messageType == Payload.MessageType.EMAIL) {
                byte[] hmac = payload.getHMacOrSig().toByteArray();
                byte[] testHMac = null;
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                try {
                    KeyAgreement keyAgree = KeyAgreement.getInstance("ECDH", "BC");
                    keyAgree.init(decryptKey);
                    keyAgree.doPhase(new Address(data.getSenderAddress()).getECKey().getPubKey(), true);
                    MessageDigest hash = MessageDigest.getInstance("SHA256", "BC");
                    byte[] sharedSecret = hash.digest(keyAgree.generateSecret());
                    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
                    SecretKeySpec secret_key = new SecretKeySpec(sharedSecret, "HmacSHA256");
                    sha256_HMAC.init(secret_key);
                    testHMac = sha256_HMAC.doFinal(payload.getSerializedMessageData().toByteArray());
                    if (!Arrays.equals(hmac, testHMac)) {throw new BadHMACException();}
                } catch (NoSuchAlgorithmException | NoSuchProviderException
                        | InvalidKeyException | AddressFormatException | BadHMACException e) {
                    isForMe = false;
                }
            } else if(this.messageType == Payload.MessageType.CHATROOM){
                try {
                    Signature ecdsa = Signature.getInstance("SHA256withECDSA");
                    ecdsa.initVerify(new Address(data.getSenderAddress()).getECKey().getPubKey());
                    ecdsa.update(payload.getSerializedMessageData().toByteArray());
                    if (!ecdsa.verify(payload.getHMacOrSig().toByteArray())) {
                        throw new BadSignatureException();
                    }
                } catch (NoSuchAlgorithmException | AddressFormatException | SignatureException
                        | BadSignatureException | InvalidKeyException e) {isForMe = false;}

            }
        }


    }

    public void send(){
        Runnable task = () -> {
            if (this.uploadHost.equals("localhost")) {
                sendToLocalHost();
            } else {
                sendToTor();
            }
        };
        new Thread(task).start();
    }

    public void sendToTor(){
        String data = Hex.toHexString(this.encryptedPayload);
        try{TorLib.postToURL(this.uploadHost, 8335, this.postKey, data);}
        catch (IOException e){e.printStackTrace();}
    }

    public void sendToLocalHost(){

        try {
            URL obj = new URL("http://localhost:8080/" + postKey);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            String urlParameters = Hex.toHexString(this.encryptedPayload);
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
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
        if (postKey.length()<24){
            for (int i=0; i<24-postKey.length(); i++){
                postKey = "0" + postKey;
            }
        }
    }

    public boolean isMessageForMe(){
        return isForMe;
    }

    public String getFromAddress(){
        return this.fromAddress;
    }

    public long getTimeStamp(){
        return this.timeStamp;
    }

    public String getDecryptedMessage(){
        return this.message;
    }

    public String getSenderName(){
        return this.senderName;
    }

    public Address getToAddress(){
        return toAddress;
    }

    public String getSubject(){
        return this.subject;
    }

    public Payload.MessageType getMessageType(){
        return this.messageType;
    }

    public void setSenderName(String name){
        this.senderName = name;
    }

}
