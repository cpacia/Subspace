package Messenger;

import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by chris on 1/30/15.
 */
public class FileWriter {

    File keyRingFile;
    String keyFilePath;
    File messageFile;
    String messageFilePath;


    public FileWriter() {
        keyFilePath = Main.params.getApplicationDataFolder() + "/keyring.dat";
        keyRingFile = new File(keyFilePath);
        try{if (!keyRingFile.exists()){keyRingFile.createNewFile();}}
        catch (IOException e){e.printStackTrace();}
        messageFilePath = Main.params.getApplicationDataFolder() + "/messages.dat";
        messageFile = new File(messageFilePath);
        try{if (!messageFile.exists()){messageFile.createNewFile();}}
        catch (IOException e){e.printStackTrace();}
    }

    private synchronized KeyRing.SavedKeys.Builder getKeyFileBuilder() {
        KeyRing.SavedKeys.Builder savedKeys = KeyRing.SavedKeys.newBuilder();
        try{savedKeys.mergeDelimitedFrom(new FileInputStream(keyFilePath)); }
        catch(Exception e){e.printStackTrace();}
        return savedKeys;
    }

    private synchronized void writeKeyFile(KeyRing.SavedKeys.Builder savedKeys) throws IOException{
        FileOutputStream output = new FileOutputStream(keyFilePath);
        savedKeys.build().writeDelimitedTo(output);
        output.close();
    }

    private synchronized History.ChatConversationList.Builder getMessageFileBuilder() {
        History.ChatConversationList.Builder savedMessages = History.ChatConversationList.newBuilder();
        try{savedMessages.mergeDelimitedFrom(new FileInputStream(messageFilePath)); }
        catch(Exception e){e.printStackTrace();}
        return savedMessages;
    }

    private synchronized void writeMessageFile(History.ChatConversationList.Builder savedMessages) throws IOException{
        FileOutputStream output = new FileOutputStream(messageFilePath);
        savedMessages.build().writeDelimitedTo(output);
        output.close();
    }

    public void addKey(ECKey key, String name, int prefixLength, String address, String uploadHostName){
        KeyRing.SavedKeys.Builder builder = getKeyFileBuilder();
        ByteString priv = ByteString.copyFrom(key.getPrivKeyBtyes());
        ByteString pub = ByteString.copyFrom(key.getPubKeyBytes());
        KeyRing.Key addKey = KeyRing.Key.newBuilder().setName(name)
                                                    .setPrivateKey(priv)
                                                    .setPublicKey(pub)
                                                    .setPrefixLength(prefixLength)
                                                    .setAddress(address)
                                                    .setUploadNode(uploadHostName)
                                                    .setTimeOfLastGET("0")
                                                    .build();
        builder.addKey(addKey);
        try {writeKeyFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public void updateGETtime(String address, String timestamp){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        int index = 0;
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getAddress().equals(address)){
                index = keys.indexOf(key);
                break;
            }
        }
        KeyRing.Key.Builder newKey = KeyRing.Key.newBuilder();
        newKey.mergeFrom(b.getKey(index));
        newKey.setTimeOfLastGET(timestamp);
        b.removeKey(index);
        b.addKey(index, newKey);
        try { writeKeyFile(b);}
        catch (IOException e) {e.printStackTrace();}
    }

    public boolean hasKeys(){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        int count = b.getKeyCount();
        if (count>0){return true;}
        else {return false;}
    }

    public List<KeyRing.Key> getSavedKeys(){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        return b.getKeyList();
    }

    public Address getSavedAddress(String address) throws InvalidPrefixLengthException{
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getAddress().equals(address)){
                ECKey ecKey = ECKey.fromPrivOnly(key.getPrivateKey().toByteArray());
                return new Address(key.getPrefixLength(), ecKey);
            }
        }
        return null;
    }

    public String getNameFromAddress(String address){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getAddress().equals(address)){
               return key.getName();
            }
        }
        return null;
    }

    public KeyRing.Key getKeyFromAddress(String address){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getAddress().equals(address)){
                return key;
            }
        }
        return null;
    }

    public void newChatConversation(String conversationID, Message m, String theirName, String theirAddress,
                                    String myAddress, boolean sentFromMe){
        History.ChatMessage chatMessage = History.ChatMessage.newBuilder()
                .setContent(m.getDecryptedMessage())
                .setName(m.getSenderName())
                .setTimestamp(m.getTimeStamp())
                .setSentFromMe(sentFromMe).build();
        History.ChatConversation conversation = History.ChatConversation.newBuilder()
                .addChatMessage(chatMessage)
                .setConversationID(conversationID)
                .setTheirAddress(theirAddress)
                .setTheirName(theirName)
                .setMyAddress(myAddress).build();
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        builder.addConversation(conversation);
        try {writeMessageFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public void addChatMessage(String conversationID, Message m, boolean isSentFromMe){
        History.ChatMessage chatMessage = History.ChatMessage.newBuilder()
                .setContent(m.getDecryptedMessage())
                .setName(m.getSenderName())
                .setTimestamp(m.getTimeStamp())
                .setSentFromMe(isSentFromMe).build();
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        int index = 0;
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                index = conversations.indexOf(convo);
                break;
            }
        }
        History.ChatConversation.Builder convoBuilder = History.ChatConversation.newBuilder();
        convoBuilder.mergeFrom(builder.getConversation(index));
        convoBuilder.addChatMessage(chatMessage);
        if (!isSentFromMe){
            convoBuilder.setTheirName(m.getSenderName());
            convoBuilder.setTheirAddress(m.getFromAddress());
        }
        builder.removeConversation(index);
        builder.addConversation(index, convoBuilder);
        try {writeMessageFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public boolean conversationExists(String conversationID){
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return true;
            }
        }
        return false;
    }

    public String getNameFromConversation(String conversationID){
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return convo.getTheirName();
            }
        }
        return "";
    }

    public History.ChatConversation getConversation(String conversationID){
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return convo;
            }
        }
        return null;
    }

    public void deleteConversation(String conversationID){
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                builder.removeConversation(conversations.indexOf(convo));
                break;
            }
        }
        try {writeMessageFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public List<History.ChatConversation> getSavedCoversations(){
        History.ChatConversationList.Builder builder = getMessageFileBuilder();
        return builder.getConversationList();
    }

}