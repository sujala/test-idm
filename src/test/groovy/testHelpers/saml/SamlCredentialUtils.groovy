package testHelpers.saml;

import org.apache.commons.io.IOUtils
import org.bouncycastle.openssl.PEMWriter
import org.bouncycastle.util.encoders.Base64Encoder
import org.bouncycastle.x509.X509V3CertificateGenerator
import org.joda.time.DateTime;
import org.opensaml.xml.security.x509.BasicX509Credential
import org.opensaml.xml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

import javax.security.auth.x500.X500Principal;
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator;
import java.security.PrivateKey
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class SamlCredentialUtils {

	private static final Logger logger = LoggerFactory.getLogger(SamlCredentialUtils.class);

	/**
	 * gets credential used to sign saml assertionts that are produced. this method
	 * assumes the cert and pkcs formatted primary key are on file system. this data
	 * could be stored elsewhere e.g keystore
	 * 
	 * a credential is used to sign saml response, and includes the private key
	 * as well as a cert for the public key
	 * 
	 * @return
	 * @throws Throwable
	 */
	public X509Credential getSigningCredential(String publicKeyLocation, String privateKeyLocation) throws Throwable {
        Resource pubKeyResource = getResource(publicKeyLocation);
        Resource priKeyResource = getResource(privateKeyLocation);

		// create public key (cert) portion of credential
        InputStream inStream = pubKeyResource.getInputStream();
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate publicKey = (X509Certificate)cf.generateCertificate(inStream);
		inStream.close();
		    
		// create private key
        InputStream privateKeyStream = priKeyResource.getInputStream()
		byte[] buf = IOUtils.toByteArray(privateKeyStream);
        privateKeyStream.close();
		PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(buf);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = kf.generatePrivate(kspec);
		
		// create credential and initialize
		BasicX509Credential credential = new BasicX509Credential();
		credential.setEntityCertificate(publicKey);
		credential.setPrivateKey(privateKey);
		
		return credential;
	}

    private Resource getResource(String resourceLocation) {
        FileSystemResource fsResource = new FileSystemResource(resourceLocation);
        if (fsResource.exists()) {
            return fsResource;
        }

        ClassPathResource cpResource = new ClassPathResource(resourceLocation);
        if (cpResource.exists()) {
            return cpResource;
        }

        return null;
    }

    static X509Certificate toX509Certificate(String certAsStr) throws Throwable {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(certAsStr.getBytes()));

        return certificate;
    }

    /**
     * Generate a new Credential that includes a privateKey, publicKey, and an x509 certificate generated from
     * the public key.
     */
    static X509Credential generateX509Credential() throws Exception {
        KeyPair keyPair = generateKeyPair()
        X509Certificate cert = generateCertificate(keyPair)

        return generateX509Credential(cert, keyPair);
    }

    static X509Credential generateX509Credential(X509Certificate cert, KeyPair keyPair) throws Exception {
        BasicX509Credential credential = new BasicX509Credential();
        credential.setEntityCertificate(cert);
        credential.setPublicKey(keyPair.getPublic())
        credential.setPrivateKey(keyPair.getPrivate());

        return credential;
    }

    static X509Certificate generateCertificate(KeyPair keyPair = generateKeyPair(),
                                               validityBeginDate = new DateTime().minusDays(1).toDate(),
                                               validityEndDate = new DateTime().plusYears(2).toDate()) {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=Rackspace Test System");

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName); // use the same
        certGen.setNotBefore(validityBeginDate);
        certGen.setNotAfter(validityEndDate);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
        return cert;
    }

    static KeyPair generateKeyPair() throws Exception {
        // GENERATE THE PUBLIC/PRIVATE RSA KEY PAIR
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024, new SecureRandom());

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair
    }

    static String getCertificateAsPEMString(X509Certificate certificate) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        def encoder = new Base64Encoder()
        encoder.encode(certificate.getEncoded(), 0, certificate.getEncoded().length, out)

        return out.toString()
    }

	
}
