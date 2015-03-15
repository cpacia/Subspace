package Messenger;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPointEncoder;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.encoders.Hex;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Class for creating an elliptic curve key pair using the secp256k1 curve parameters.
 */
public class ECKey {
    public static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE;
    public static final SecureRandom secureRandom;

    static {
        FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
        CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
                CURVE_PARAMS.getH());
        secureRandom = new SecureRandom();
    }

    private ECPrivateKey privKey;
    private ECPublicKey pubKey;

    /**Create a new random key pair*/
    public ECKey() {
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator g = null;
        try {
            g = KeyPairGenerator.getInstance("EC", "BC");
            g.initialize(ecSpec, new SecureRandom());
        } catch (NoSuchProviderException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        KeyPair KeyPair = g.generateKeyPair();
        this.pubKey = (ECPublicKey) KeyPair.getPublic();
        this.privKey = (ECPrivateKey) KeyPair.getPrivate();
        ((ECPointEncoder) this.pubKey).setPointFormat("COMPRESSED");
    }

    /**Create a key pair by passing in the keys in byte format*/
    public ECKey(byte[] privKeyBytes, byte[] pubKeyBytes) {
        this.pubKey = getPubKeyFromBytes(pubKeyBytes);
        this.privKey = getPrivKeyFromBytes(privKeyBytes);
    }

    /**Create a key pair by passing in the keys in the EC key objects*/
    public ECKey(ECPrivateKey privKey, ECPublicKey pubKey) {
        this.pubKey = pubKey;
        this.privKey = privKey;
    }

    /**Create a new key pair from the private key object*/
    public ECKey(ECPrivateKey privateKey) {
        X9ECParameters ecp = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters domainParams = new ECDomainParameters(ecp.getCurve(),
                ecp.getG(), ecp.getN(), ecp.getH(),
                ecp.getSeed());
        ECPoint Q = domainParams.getG().multiply(new BigInteger(privateKey.getD().toByteArray()));
        this.pubKey = getPubKeyFromBytes(Q.getEncoded(true));
        this.privKey = privateKey;

    }

    /**Create a new key pair from private key bytes*/
    public static ECKey fromPrivOnly(byte[] privKeyBytes) {
        return new ECKey(getPrivKeyFromBytes(privKeyBytes));
    }

    /**Create an ECKey object from the public key bytes. The object will only contain the public key instance*/
    public static ECKey fromPubOnly(byte[] pubKeyBytes) {
        return new ECKey(null, getPubKeyFromBytes(pubKeyBytes));
    }

    /**Create an ECKey object from the public key object. It will only contain the public key instance*/
    public static ECKey fromPubOnly(ECPublicKey pubKey) {
        return new ECKey(null, pubKey);
    }

    /**Boolean checking if the ECKey instance has a matching private key or is it pub only*/
    public boolean hasPrivKey() {
        if (this.privKey != null) {
            return true;
        } else {
            return false;
        }
    }

    /**Returns the private key object*/
    public ECPrivateKey getPrivKey() {
        return this.privKey;
    }

    /**Returns the private key in bytes*/
    public byte[] getPrivKeyBtyes() {
        return this.privKey.getD().toByteArray();
    }

    /**Returns the public key object*/
    public ECPublicKey getPubKey() {
        return this.pubKey;
    }

    /**Returns the public key bytes in compressed format*/
    public byte[] getPubKeyBytes() {
        ECPoint pubPoint = CURVE.getCurve().decodePoint(this.pubKey.getQ().getEncoded(true));
        return pubPoint.getEncoded();
    }

    /**Returns the ECPrivateKey object given the private key bytes*/
    public static ECPrivateKey getPrivKeyFromBytes(byte[] privKeyBytes) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory fact = null;
        try {
            fact = KeyFactory.getInstance("EC", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        ECPrivateKey privateKey = null;
        try {
            privateKey = (ECPrivateKey) fact.generatePrivate(new ECPrivateKeySpec(new BigInteger(privKeyBytes), ecSpec));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    /**
     * Returns the ECPublicKey object given the pubkey bytes.
     * ECPublicKey is encoded in X.509 format, there should be a better way to covert from bytes than this.
     */

    public static ECPublicKey getPubKeyFromBytes(byte[] pubKeyBytes) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        if (pubKeyBytes.length == 33) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(Hex.decode("3036301006072a8648ce3d020106052b8104000a032200"));
                outputStream.write(pubKeyBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pubKeyBytes = outputStream.toByteArray();
        }
        X509EncodedKeySpec ks = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory kf = null;
        try {
            kf = java.security.KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        ECPublicKey publicKey = null;
        try {
            publicKey = (ECPublicKey) kf.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        ((ECPointEncoder) publicKey).setPointFormat("COMPRESSED");
        return publicKey;
    }

}