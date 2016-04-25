package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.SignatureValidationException;
import org.apache.commons.collections.CollectionUtils;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

@Component
public class SamlSignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(SamlSignatureValidator.class);

    private String INVALID_SIGNATURE_ERROR_MSG = "Signature is invalid";

    public void validateSignature(Signature signature, X509Certificate publicCertificate) {
        try {
            BasicX509Credential credential = new BasicX509Credential();
            credential.setEntityCertificate(publicCertificate);
            SignatureValidator signatureValidator = new SignatureValidator(credential);
            signatureValidator.validate(signature);
        } catch (ValidationException t) {
            throw new SignatureValidationException(INVALID_SIGNATURE_ERROR_MSG, t);
        }
    }

    public void validateSignatureForIdentityProvider(Signature signature, IdentityProvider identityProvider) {
        boolean validated = false;
        SignatureValidationException lastException = null;

        List<X509Certificate> idpCerts;
        try {
            idpCerts = identityProvider.getUserCertificatesAsX509();
        }
        catch (CertificateException ex) {
            throw new IllegalStateException("Error retrieving provider certificates", ex);
        }

        if (CollectionUtils.isEmpty(idpCerts)) {
            //if no certs for identity provider, signature obviously won't validate. Just throw standard error message.
            throw new SignatureValidationException("No certificates for IDP");
        }

        /*
        loop through the 1+ signatures to see if any validate. If none do, return the error thrown by the last attempt
        to verify.
         */
        for (int i=0; i<idpCerts.size() && !validated; i++) {
            X509Certificate x509Certificate = idpCerts.get(i);
            try {
                validateSignature(signature, x509Certificate);
                validated = true;
            } catch (SignatureValidationException ex) {
                logger.debug(String.format("Validation error for signature '%s' w/ cert '%s'", signature, x509Certificate));
                lastException = ex;
            }
        }
        if (!validated) {
            throw lastException;
        }
    }

    private Credential getSignatureValidationCredential(String certificateStr) throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
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
    private X509Certificate getPublicCertificate(final String certificateStr) throws CertificateException {
        InputStream inStream = new ByteArrayInputStream(certificateStr.getBytes());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate publicKey = (X509Certificate)cf.generateCertificate(inStream);
        return publicKey;
    }
}
