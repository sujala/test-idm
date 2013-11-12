package com.rackspace.idm.util;

import com.rackspace.idm.exception.SignatureValidationException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;

@Component
public class SamlSignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(SamlSignatureValidator.class);

    public void validateSignature(Signature signature, String publicCertificate) {
        try {
            Credential credential = getSignatureValidationCredential(publicCertificate);
            SignatureValidator signatureValidator = new SignatureValidator(credential);
            signatureValidator.validate(signature);
        } catch (Throwable t) {
            throw new SignatureValidationException("Signature is invalid", t);
        }
    }

    private Credential getSignatureValidationCredential(String certificateStr) throws Throwable  {
        X509Certificate certificate = getPublicCertificate(certificateStr);

        //pull out the public key part of certificate into keyspec
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());

        //get KeyFactory object that creates key objects, specifying RSA
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        //generate public key to validate signatures
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        //create credential
        BasicX509Credential publicCredential = new BasicX509Credential();
        publicCredential.setPublicKey(publicKey);

        return publicCredential;
    }

    /**
     * gets certificate for specified identity provider. this can be stored anywhere on file,
     * in a database, in a key service, etc
     *
     * @param certificateStr
     * @return X509 certificate
     */
    private X509Certificate getPublicCertificate(final String certificateStr) throws Throwable {
        InputStream inStream = new ByteArrayInputStream(certificateStr.getBytes());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate publicKey = (X509Certificate)cf.generateCertificate(inStream);
        inStream.close();
        return publicKey;
    }
}
