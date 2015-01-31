package Messenger;


import org.bitcoinj.core.AddressFormatException;

import org.spongycastle.util.encoders.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;

import java.security.spec.InvalidKeySpecException;

/**
 * Created by chris on 1/25/15.
 */
public class Crypto {
    public static void main(String[] args) throws InvalidAlgorithmParameterException,
            NoSuchProviderException, NoSuchAlgorithmException, IllegalBlockSizeException,
            NoSuchPaddingException, BadPaddingException, InvalidKeyException, IOException, SignatureException, InvalidKeySpecException{

        byte[] message = Hex.decode("0102030405060708090a0b0c0d0e0f10111213141516");
        byte[] out1, out2;

        // Generate static key pair
        Address addr = null;
        Address addr1 = null;
        Address.version = 0;
        try {
            addr = new Address(0);
        } catch (InvalidPrefixLengthException e) {
            e.printStackTrace();
        }
        System.out.println(addr.toString());
        System.out.println(Hex.toHexString(addr.getECKey().getPrivKeyBtyes()));
        System.out.println(Hex.toHexString(addr.getECKey().getPubKeyBytes()));
        try {
            addr1 = new Address(addr.toString());
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }


        System.out.println(addr1.toString());




        /*
        Cipher c1 = Cipher.getInstance("ECIESwithAES-CBC");
        Cipher c2 = Cipher.getInstance("ECIESwithAES-CBC");

        // Testing with null parameters and DHAES mode off
        c1.init(Cipher.ENCRYPT_MODE, key.getPubKey(), new SecureRandom());
        c2.init(Cipher.DECRYPT_MODE, key.getPrivKey(), new SecureRandom());
        out1 = c1.doFinal(message, 0, message.length);
        out2 = c2.doFinal(out1, 0, out1.length);

        if (Arrays.equals(message, out2)){System.out.println("works");}
        Signature dsa = Signature.getInstance("SHA256withECDSA");

        dsa.initSign(key.getPrivKey());

        String str = "This is string to sign";
        byte[] strByte = str.getBytes("UTF-8");
        dsa.update(strByte);

        byte[] realSig = dsa.sign();
        System.out.println("Signature: " + new BigInteger(1, realSig).toString(16));

        System.out.println(key.getPubKeyBytes().length);

        byte prefixLength[] = new byte[]{(byte)0x02};
        SecureRandom random = new SecureRandom();
        byte prefix[] = new byte[4];
        random.nextBytes(prefix);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(prefixLength);
        outputStream.write(prefix);
        outputStream.write(key.getPubKeyBytes());
        byte addressBytes[] = outputStream.toByteArray( );
        Base58Check formattedAddress = new Base58Check(0, addressBytes);
        System.out.println(formattedAddress);*/



    }

}
