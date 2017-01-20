package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.SignatureValidationException;
import org.apache.commons.collections.CollectionUtils;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

@Component
public class SamlSignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(SamlSignatureValidator.class);

    private String INVALID_SIGNATURE_ERROR_MSG = "Signature is invalid";

    public void validateSignature(Signature signature, X509Certificate publicCertificate) {
        try {
            BasicX509Credential credential = new BasicX509Credential(publicCertificate);
            SignatureValidator.validate(signature, credential);
        } catch (SignatureException t) {
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
}
