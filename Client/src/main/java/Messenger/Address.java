package Messenger;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Utils;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Class for creating a subspace address.
 * The address format is <version: 1><prefix: 1><public key: 33><checksum: 4>
 */
public class Address {

    private ECKey key;
    public static int version = 0;
    private int prefixLength;
    private byte[] prefix = new byte[4];
    private byte[] publicKey = new byte[33];
    private byte[] checksum = new byte[4];
    private byte[] versionedchecksummed;

    /**Creates an address from a new ECKey*/
    public Address(int prefixLength) throws InvalidPrefixLengthException{
        if (prefixLength > 32) throw new InvalidPrefixLengthException();
        ECKey key = new ECKey();
        this.key = key;
        createAddressFromKey(prefixLength, key);
    }

    /**Creates an address using a supplied ECKey*/
    public Address(int prefixLength, ECKey key) throws InvalidPrefixLengthException{
        if (prefixLength > 32) throw new InvalidPrefixLengthException();
        this.key = key;
        createAddressFromKey(prefixLength, key);
    }

    /**Creates an address from its string representation*/
    public Address(String address) throws AddressFormatException{
        parse(address);
    }

    /**Creates the address by concatenating the various byte arrays*/
    private void createAddressFromKey(int prefixLength, ECKey key){
        this.prefixLength = prefixLength;
        byte[] length = new byte[]{(byte) prefixLength};
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(length);
            outputStream.write(key.getPubKeyBytes());
        } catch (IOException e){e.printStackTrace();}
        byte[] pubkeyBytes = key.getPubKeyBytes();
        for (int i=0; i<33; i++){
            this.publicKey[i] = pubkeyBytes[i];
        }
        for (int i=0; i<4; i++){
            this.prefix[i] = publicKey[i+1];
        }
        byte[] addressBytes = outputStream.toByteArray();
        byte[] fullChecksum = Utils.doubleDigest(addressBytes);
        for (int i=0; i<4; i++){this.checksum[i] = fullChecksum[i];}
        byte[] ver = new byte[]{(byte) version};
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream( );
        try {
            outputStream2.write(ver);
            outputStream2.write(addressBytes);
            outputStream2.write(checksum);
        } catch (IOException e){e.printStackTrace();}
        this.versionedchecksummed = outputStream2.toByteArray();
    }

    /**Takes in the string representation of the address and parses it into it's parts*/
    private void parse(String address) throws AddressFormatException{
        this.versionedchecksummed = Base58.decode(address);
        this.version = this.versionedchecksummed[0] & 0xFF;
        this.prefixLength = this.versionedchecksummed[1] & 0xFF;
        for (int i=0; i<4; i++){
            this.prefix[i] = this.versionedchecksummed[i+3];
        }
        for (int i=0; i<33; i++){
            this.publicKey[i] = this.versionedchecksummed[i+2];
        }
        for (int i=0; i<4; i++) {
            this.checksum[i] = this.versionedchecksummed[i + 35];
        }
        this.key = ECKey.fromPubOnly(this.publicKey);
    }

    /**Returns the base 58 encoded address*/
    public String toString(){
        return Base58.encode(versionedchecksummed);
    }

    /**Returns the public key object for the address*/
    public ECPublicKey getPublicKey() {
        return (ECPublicKey) this.key.getPubKey();
    }

    /**Returns the ECKey for the address*/
    public ECKey getECKey(){
        return key;
    }

    /**Returns the prefix length used by this address*/
    public int getPrefixLength(){
        return prefixLength;
    }

    /**Returns the first four bytes of the public key*/
    public byte[] getFullPrefix(){
        return prefix;
    }

    /**Returns the prefix as a binary string*/
    public String getPrefix(){
        if (prefixLength==0){return "";}
        else {
            String binary = "";
            for (byte b : prefix){
                String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                binary = binary + s;
            }
            return binary.substring(0, prefixLength);
        }
    }

    /**
     * Tests that the address is formatted correct and the checksum is valid.
     * Returns True if it's valid.
     */
    public static boolean validateAddress(String addr){
        byte[] versionedchecksummed;
        try{versionedchecksummed = Base58.decode(addr);}
        catch (AddressFormatException e){return false;}
        if (versionedchecksummed.length != 39) return false;
        byte[] lenAndPubKey = new byte[34];
        for (int i=0; i<34; i++){
            lenAndPubKey[i] = versionedchecksummed[i+1];
        }
        byte[] checksum = new byte[4];
        for (int i=0; i<4; i++) {
            checksum[i] = versionedchecksummed[i + 35];
        }
        byte[] fullChecksum = Utils.doubleDigest(lenAndPubKey);
        byte[] check = new byte[4];
        for (int i=0; i<4; i++){check[i] = fullChecksum[i];}
        if (!Arrays.equals(checksum, check))return false;
        return true;
    }

}
