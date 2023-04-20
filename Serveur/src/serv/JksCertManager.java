package serv;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class JksCertManager {
    public static void createCert(String keystoreFilename, String keystorePassword, String alias, String keyPassword) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Load or create the keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFilename)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | FileNotFoundException e){
            e.printStackTrace();
            keyStore.load(null, null);
        }

        // Generate a new key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Generate a self-signed X.509 certificate
        X500Principal owner = new X500Principal("CN=localhost");
        Date from = new Date();
        Date to = new Date(from.getTime() + 365 * 24 * 60 * 60 * 1000L);
        X509Certificate cert = generateCertificate(owner, keyPair, from, to);

        // Store the key pair and certificate in the keystore
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[] { cert });
        try (FileOutputStream fos = new FileOutputStream(keystoreFilename)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }
    }

    private static X509Certificate generateCertificate(X500Principal owner, KeyPair keyPair, Date from, Date to) throws Exception {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(new BigInteger(64, new SecureRandom()));
        certGen.setIssuerDN(owner);
        certGen.setSubjectDN(owner);
        certGen.setNotBefore(from);
        certGen.setNotAfter(to);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        return certGen.generate(keyPair.getPrivate());
    }
}
