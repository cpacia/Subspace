package Messenger;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPointEncoder;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
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
 * Created by chris on 1/26/15.
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

    public ECKey(byte[] privKeyBytes, byte[] pubKeyBytes) {
        this.pubKey = getPubKeyFromBytes(pubKeyBytes);
        this.privKey = getPrivKeyFromBytes(privKeyBytes);
    }

    public ECKey(ECPrivateKey privKey, ECPublicKey pubKey) {
        this.pubKey = pubKey;
        this.privKey = privKey;
    }

    public ECKey(ECPrivateKey privateKey) {
        X9ECParameters ecp = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters domainParams = new ECDomainParameters(ecp.getCurve(),
                ecp.getG(), ecp.getN(), ecp.getH(),
                ecp.getSeed());
        ECPoint Q = domainParams.getG().multiply(new BigInteger(privateKey.getD().toByteArray()));
        this.pubKey = getPubKeyFromBytes(Q.getEncoded(true));
        this.privKey = privateKey;

    }

    public static ECKey fromPrivOnly(ECPrivateKey privateKey) {
        return new ECKey(privateKey);
    }

    public static ECKey fromPrivOnly(byte[] privKeyBytes) {
        return new ECKey(getPrivKeyFromBytes(privKeyBytes));
    }

    public static ECKey fromPubOnly(byte[] pubKeyBytes) {
        return new ECKey(null, getPubKeyFromBytes(pubKeyBytes));
    }

    public static ECKey fromPubOnly(ECPublicKey pubKey) {
        return new ECKey(null, pubKey);
    }

    public boolean hasPrivKey() {
        if (this.privKey != null) {
            return true;
        } else {
            return false;
        }
    }

    public ECPrivateKey getPrivKey() {
        return this.privKey;
    }

    public byte[] getPrivKeyBtyes() {
        return this.privKey.getD().toByteArray();
    }

    public ECPublicKey getPubKey() {
        return this.pubKey;
    }

    public byte[] getPubKeyBytes() {
        ECPoint pubPoint = CURVE.getCurve().decodePoint(this.pubKey.getQ().getEncoded(true));
        return pubPoint.getEncoded();
    }

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

    //There's got to be a better way to do this
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