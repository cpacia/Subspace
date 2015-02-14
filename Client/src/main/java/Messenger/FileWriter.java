package Messenger;

import com.google.protobuf.ByteString;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    //#####################################
    //
    //	Builders
    //
    //#####################################

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

    private synchronized History.MessageList.Builder getMessageFileBuilder() {
        History.MessageList.Builder savedMessages = History.MessageList.newBuilder();
        try{savedMessages.mergeDelimitedFrom(new FileInputStream(messageFilePath)); }
        catch(Exception e){e.printStackTrace();}
        return savedMessages;
    }

    private synchronized void writeMessageFile(History.MessageList.Builder savedMessages) throws IOException{
        FileOutputStream output = new FileOutputStream(messageFilePath);
        savedMessages.build().writeDelimitedTo(output);
        output.close();
    }

    private synchronized History.ChatConversationList.Builder getChatFileBuilder() {
        History.ChatConversationList.Builder savedChats = History.ChatConversationList.newBuilder();
        savedChats.mergeFrom(getMessageFileBuilder().getChatList());
        return savedChats;
    }

    private synchronized void writeChatsToFile(History.ChatConversationList.Builder savedMessages) throws IOException{
        History.MessageList.Builder b = getMessageFileBuilder();
        History.ChatConversationList chatList = savedMessages.build();
        b.setChatList(chatList);
        try{writeMessageFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    private synchronized History.EmailList.Builder getEmailFileBuilder() {
        History.EmailList.Builder savedEmails = History.EmailList.newBuilder();
        savedEmails.mergeFrom(getMessageFileBuilder().getEmailList());
        return savedEmails;
    }

    private synchronized void writeEmailsToFile(History.EmailList.Builder savedMessages) throws IOException{
        History.MessageList.Builder b = getMessageFileBuilder();
        History.EmailList emailList = savedMessages.build();
        b.setEmailList(emailList);
        try{writeMessageFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    private synchronized History.GroupChatList.Builder getGroupChatFileBuilder() {
        History.GroupChatList.Builder savedGroupChats = History.GroupChatList.newBuilder();
        savedGroupChats.mergeFrom(getMessageFileBuilder().getGroupList());
        return savedGroupChats;
    }

    private synchronized void writeGroupChatsToFile(History.GroupChatList.Builder savedMessages) throws IOException{
        History.MessageList.Builder b = getMessageFileBuilder();
        History.GroupChatList groupChatList = savedMessages.build();
        b.setGroupList(groupChatList);
        try{writeMessageFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    //#####################################
    //
    //	Contacts
    //
    //#####################################

    public void setPreloaded(boolean preloaded){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        b.setPreloaded(preloaded);
        try {writeContactsFile(b);
        } catch (IOException e) {e.printStackTrace();}
    }

    public boolean getPreloaded(){
        Contacts.ContactList.Builder b = getContactsFileBuilder();
        return b.getPreloaded();
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

    public String getNameForContact(String address){
        if (hasOpenname(address)){
            return getFormattedName(getOpenname(address));
        }
        else {
            for (Contacts.Contact c : getContacts()){
                if (c.getAddress().equals(address)){
                    return c.getName();
                }
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

    //#####################################
    //
    //	Address Keys
    //
    //#####################################

    public void addKey(ECKey key, String name, int prefixLength, String address,
                       String uploadHostName, @Nullable String openname, @Nullable String roomKey){
        KeyRing.SavedKeys.Builder builder = getKeyFileBuilder();
        ByteString priv = ByteString.copyFrom(key.getPrivKeyBtyes());
        ByteString pub = ByteString.copyFrom(key.getPubKeyBytes());
        KeyRing.Key.Builder addKey = KeyRing.Key.newBuilder();
        addKey.setName(name).setPrivateKey(priv).setPublicKey(pub).setPrefixLength(prefixLength)
                .setAddress(address).setUploadNode(uploadHostName).setTimeOfLastGET("0");
        if (openname != null){addKey.setOpenname(openname);}
        if (roomKey != null){addKey.setRoomKey(roomKey);}
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
        for (KeyRing.Key k : b.getKeyList()){
            if (!k.getName().substring(0,1).equals("#")) return true;
        }
        return false;
    }

    public List<KeyRing.Key> getSavedKeys(){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        List<KeyRing.Key> keys = new ArrayList<>();
        for (KeyRing.Key k : b.getKeyList()){
            if (!k.getName().substring(0,1).equals("#")){
                keys.add(k);
            }
        }
        return keys;
    }

    public List<KeyRing.Key> getAllKeys(){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        return b.getKeyList();
    }

    public boolean keyExists(String address){
        for (KeyRing.Key k : getSavedKeys()){
            if (k.getAddress().equals(address)){return true;}
        }
        return false;
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

    public KeyRing.Key getKeyFromName(String roomName){
        KeyRing.SavedKeys.Builder b = getKeyFileBuilder();
        List<KeyRing.Key> keys = b.getKeyList();
        for (KeyRing.Key key : keys){
            if (key.getName().equals(roomName)){
                return key;
            }
        }
        return null;
    }

    //#####################################
    //
    //	Chat Messages
    //
    //#####################################

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
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        builder.addConversation(conversation);
        try {writeChatsToFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public void addChatMessage(String conversationID, Message m, boolean isSentFromMe){
        History.ChatMessage chatMessage = History.ChatMessage.newBuilder()
                .setContent(m.getDecryptedMessage())
                .setName(m.getSenderName())
                .setTimestamp(m.getTimeStamp())
                .setSentFromMe(isSentFromMe).build();
        History.ChatConversationList.Builder builder = getChatFileBuilder();
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
        try {writeChatsToFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public boolean conversationExists(String conversationID){
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return true;
            }
        }
        return false;
    }

    public String getNameFromConversation(String conversationID){
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return convo.getTheirName();
            }
        }
        return "";
    }

    public History.ChatConversation getConversation(String conversationID){
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                return convo;
            }
        }
        return null;
    }

    public void deleteConversation(String conversationID){
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        List<History.ChatConversation> conversations = builder.getConversationList();
        for (History.ChatConversation convo: conversations){
            if (convo.getConversationID().equals(conversationID)){
                builder.removeConversation(conversations.indexOf(convo));
                break;
            }
        }
        try {writeChatsToFile(builder);
        } catch (IOException e) {e.printStackTrace();}
    }

    public List<History.ChatConversation> getSavedCoversations(){
        History.ChatConversationList.Builder builder = getChatFileBuilder();
        return builder.getConversationList();
    }

    //#####################################
    //
    //	Email
    //
    //#####################################

    public void addEmail(String toAddress, String fromAddress, String senderName,
                         String body, String subject, long timestamp, Boolean isSentFromMe){
        History.EmailList.Builder b = getEmailFileBuilder();
        History.EmailMessage email = History.EmailMessage.newBuilder()
                .setSenderName(senderName)
                .setToAddress(toAddress)
                .setFromAddress(fromAddress)
                .setBody(body)
                .setSubject(subject)
                .setTimestamp(timestamp)
                .setSentFromMe(isSentFromMe).build();
        b.addEmailMessage(email);
        try {writeEmailsToFile(b);
        } catch (IOException e) {e.printStackTrace();}
    }

    public List<History.EmailMessage> getSavedEmails(){
        History.EmailList.Builder b = getEmailFileBuilder();
        return b.getEmailMessageList();
    }

    public List<History.EmailMessage> getEmailsSentToMe(){
        List<History.EmailMessage> l = new ArrayList<>();
        for (History.EmailMessage m : getSavedEmails()){
            if (!m.getSentFromMe()){l.add(m);}
        }
        return l;
    }

    public List<History.EmailMessage> getEmailSentFromMe(){
        List<History.EmailMessage> l = new ArrayList<>();
        for (History.EmailMessage m : getSavedEmails()){
            if (m.getSentFromMe()){l.add(m);}
        }
        return l;
    }

    public void deleteEmail(History.EmailMessage email){
        History.EmailList.Builder b = getEmailFileBuilder();
        int index = b.getEmailMessageList().indexOf(email);
        b.removeEmailMessage(index);
        try {writeEmailsToFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    //#####################################
    //
    //	Group Chats
    //
    //#####################################

    public void addChatRoom(String roomName, boolean isPrivate){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        History.GroupChat groupChat = History.GroupChat.newBuilder()
                .setRoomName(roomName)
                .setIsPrivate(isPrivate).build();
        b.addChat(groupChat);
        try {writeGroupChatsToFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    public void addChatRoomMessage(String roomName, Message m){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        int index = 0;
        for (History.GroupChat g : b.getChatList()){
            if (g.getRoomName().equals(roomName)){
                index = b.getChatList().indexOf(g);
                break;
            }
        }
        History.RoomMessage message = History.RoomMessage.newBuilder()
                .setContent(m.getDecryptedMessage())
                .setSenderAddress(m.getFromAddress())
                .setSenderName(m.getSenderName())
                .setTimestamp(m.getTimeStamp()).build();
        History.GroupChat.Builder groupChatBuilder = History.GroupChat.newBuilder();
        groupChatBuilder.mergeFrom(b.getChat(index));
        groupChatBuilder.addChatRoomMessages(message);
        b.removeChat(index);
        b.addChat(index, groupChatBuilder);
        try {writeGroupChatsToFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    public List<History.GroupChat> getChatRooms(){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        return b.getChatList();
    }

    public int getNumberOfChatRooms(){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        return b.getChatList().size();
    }

    public void deleteChatRoom(int index){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        b.removeChat(index);
        try {writeGroupChatsToFile(b);}
        catch (IOException e){e.printStackTrace();}
    }

    public List<History.RoomMessage> getChatRoomMessages(String roomName){
        History.GroupChatList.Builder b = getGroupChatFileBuilder();
        for (History.GroupChat g : b.getChatList()){
            if (g.getRoomName().equals(roomName)){
                return g.getChatRoomMessagesList();
            }
        }
        return null;
    }

}