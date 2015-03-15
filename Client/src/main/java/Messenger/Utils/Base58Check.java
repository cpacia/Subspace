package Messenger.Utils;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base58 encoding and decoding with a 4 byte checksum added.
 */
public class Base58Check {

    /**Encode a hex string to base 58 while appending a checksum*/
    public static String encode(String hex){
        byte [] hexBytes = hexStringToByteArray(hex);
        byte [] checksum = new byte[4];
        byte[] fullChecksum = Utils.doubleDigest(hexBytes);
        for (int i=0; i<4; i++){checksum[i] = fullChecksum[i];}
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(hexBytes);
            outputStream.write(checksum);
        } catch (IOException e){e.printStackTrace();}
        return Base58.encode(outputStream.toByteArray());
    }

    /**Decodes a base 58 string and tests its checksum*/
    public static byte[] decode(String base58Check) throws AddressFormatException{
        return Base58.decodeChecked(base58Check);
    }

    /**Utitily method for going from a hex string to byte array*/
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
