package Messenger;

import com.google.protobuf.ByteString;

import javax.annotation.Nullable;
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
    File contactsFile;
    String contactsFilePath;


    public FileWriter() {
        boolean b = (new File(Main.params.getApplicationDataFolder()+"/avatars/")).mkdirs();
        keyFilePath = Main.params.getApplicationDataFolder() + "/keyring.dat";
        keyRingFile = new File(keyFilePath);
        try{if (!keyRingFile.exists()){keyRingFile.createNewFile();}}
        catch (IOException e){e.printStackTrace();}
        messageFilePath = Main.params.getApplicationDataFolder() + "/messages.dat";
        messageFile = new File(messageFilePath);
        try{if (!messageFile.exists()){messageFile.createNewFile();}}
        catch (IOException e){e.printStackTrace();}
        contactsFilePath = Main.params.getApplicationDataFolder() + "/contacts.dat";
        contactsFile = new File(contactsFilePath);
        try{if (!contactsFile.exists()){contactsFile.createNewFile();}}
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

    private synchronized Contacts.ContactList.Builder getContactsFileBuilder() {
        Contacts.ContactList.Builder savedContacts = Contacts.ContactList.newBuilder();
        try{savedContacts.mergeDelimitedFrom(new FileInputStream(contactsFilePath)); }
        catch(Exception e){e.printStackTrace();}
        return savedContacts;
    }

    private synchronized void writeContactsFile(Contacts.ContactList.Builder savedContacts) throws IOException{
        FileOutputStream output = new FileOutputStream(contactsFilePath);
        savedContacts.build().writeDelimitedTo(output);
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

    public List<Contacts.Contact> getContacts(){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        return b.getContactList();
    }

    public boolean isContactFresh(String address){
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                return c.getIsFresh();
            }
        }
        return false;
    }

    public void addContact(String address, String name, @Nullable String openname){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        Contacts.Contact c = null;
        if (openname==null) {
            c = Contacts.Contact.newBuilder()
                    .setAddress(address)
                    .setIsFresh(true)
                    .setName(name).build();
        } else {
            c = Contacts.Contact.newBuilder()
                    .setAddress(address)
                    .setIsFresh(true)
                    .setName(name)
                    .setOpenname(openname).build();
        }
        b.addContact(c);
        try {writeContactsFile(b);
        } catch (IOException e) {e.printStackTrace();}
    }

    public void updateContact(String address, @Nullable String name, @Nullable String openname){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        int index = 0;
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                index = getContacts().indexOf(c);
                break;
            }
        }
        Contacts.Contact.Builder newContact = Contacts.Contact.newBuilder();
        newContact.mergeFrom(b.getContact(index));
        if (name!=null){newContact.setName(name);}
        if (openname!=null){newContact.setOpenname(openname);}
        newContact.setIsFresh(false);
        b.removeContact(index);
        b.addContact(index, newContact);
        try { writeContactsFile(b);}
        catch (IOException e) {e.printStackTrace();}
    }

    public void deleteContact(String address){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        int index = 0;
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                index = getContacts().indexOf(c);
                break;
            }
        }
        b.removeContact(index);
        try { writeContactsFile(b);}
        catch (IOException e) {e.printStackTrace();}
    }

    public boolean contactExists(String address){
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                return true;
            }
        }
        return false;
    }

    public boolean hasOpenname(String address){
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                if (c.hasOpenname()) return true;
            }
        }
        return false;
    }

    public String getOpenname(String address){
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                   return c.getOpenname();
                }
        }
        return null;
    }

    public String getFormattedName(String openname){
        for (Contacts.Contact c : getContacts()){
            if (c.getOpenname().equals(openname)){
                return c.getName();
            }
        }
        return null;
    }

    public boolean hasOpennameChanged(String address, String openname){
        for (Contacts.Contact c : getContacts()){
            if (c.getAddress().equals(address)){
                if (c.getOpenname().equals(openname)) return false;
            }
        }
        return true;
    }

    public void addKey(ECKey key, String name, int prefixLength, String address,
                       String uploadHostName, @Nullable String openname){
        KeyRing.SavedKeys.Builder builder = getKeyFileBuilder();
        ByteString priv = ByteString.copyFrom(key.getPrivKeyBtyes());
        ByteString pub = ByteString.copyFrom(key.getPubKeyBytes());
        KeyRing.Key addKey = null;
        if (openname==null) {
            addKey = KeyRing.Key.newBuilder().setName(name)
                    .setPrivateKey(priv)
                    .setPublicKey(pub)
                    .setPrefixLength(prefixLength)
                    .setAddress(address)
                    .setUploadNode(uploadHostName)
                    .setTimeOfLastGET("0")
                    .build();
        }
        else {
            addKey = KeyRing.Key.newBuilder().setName(name)
                    .setPrivateKey(priv)
                    .setPublicKey(pub)
                    .setPrefixLength(prefixLength)
                    .setAddress(address)
                    .setUploadNode(uploadHostName)
                    .setTimeOfLastGET("0")
                    .setOpenname(openname)
                    .build();
        }
        builder.addKey(addKey);
        try {writeKeyFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public void deleteKey(String address){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        int index = 0;
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getAddress().equals(address)){
                index = keys.indexOf(key);
                break;
            }
        }
        b.removeKey(index);
        try { writeKeyFile(b);}
        catch (IOException e) {e.printStackTrace();}
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