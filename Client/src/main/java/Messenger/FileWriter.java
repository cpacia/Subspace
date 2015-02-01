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

    public FileWriter() {
        keyFilePath = Main.params.getApplicationDataFolder() + "/keyring.dat";
        keyRingFile = new File(keyFilePath);
        try{if (!keyRingFile.exists()){keyRingFile.createNewFile();}}
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
                                                    .build();
        builder.addKey(addKey);
        try {writeKeyFile(builder);
        } catch (IOException e) {e.printStackTrace();}
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
}
