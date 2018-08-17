package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.SignatureValidationException;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common utilities for Federation
 */
@Component
public class FederationUtils {
    private static final Logger log = LoggerFactory.getLogger(FederationUtils.class);

    @Autowired
    private IdentityConfig identityConfig;

    /**
     * Validates that the request did not occur too far in the past based on configured application settings which
     * control the max "age" of a request. It also allows for separately configured amount of "clock skew" to account for
     * skew between the system clock on the origin server and the IDM server.
     */
    public void validateForExpiredRequest(FederatedAuthRequest federatedAuthRequest) {
        // Validate the specified datetime is not older than the configure max age for a saml response
        Collection<DateTime> issueInstants = new ArrayList<>();
        issueInstants.add(federatedAuthRequest.getResponseIssueInstant());
        if (identityConfig.getReloadableConfig().shouldV2FederationValidateOriginIssueInstant()) {
            issueInstants.addAll(CollectionUtils.collect(federatedAuthRequest.getOriginAssertions(), new Transformer<Assertion, DateTime>() {
                @Override
                public DateTime transform(Assertion assertion) {
                    return assertion.getIssueInstant();
                }
            }));
        }
        CollectionUtils.forAllDo(issueInstants, new Closure<DateTime>() {
            @Override
            public void execute(DateTime issueInstant) {
                validateIssueInstant(issueInstant);
            }
        });
    }

    /**
     * Validates that the request did not occur too far in the past based on configured application settings which
     * control the max "age" of a request. It also allows for separately configured amount of "clock skew" to account for
     * skew between the system clock on the origin server and the IDM server.
     */
    public void validateForExpiredRequest(LogoutRequestDecorator logoutRequest) {
        // Validate the specified datetime is not older than the configure max age for a saml response
        Collection<DateTime> issueInstants = new ArrayList<>();
        issueInstants.add(logoutRequest.checkAndGetIssueInstant());
        CollectionUtils.forAllDo(issueInstants, new Closure<DateTime>() {
            @Override
            public void execute(DateTime issueInstant) {
                validateIssueInstant(issueInstant);
            }
        });
    }

    public void validateSignatureForIdentityProvider(Signature signature, IdentityProvider identityProvider) {
        boolean validated = false;
        SignatureValidationException lastException = null;

        List<X509Certificate> idpCerts;
        try {
            idpCerts = identityProvider.getUserCertificatesAsX509();
        }
        catch (CertificateException ex) {
            throw new IllegalStateException(String.format("Error retrieving provider certificates for idp '%s'", identityProvider.getProviderId()), ex);
        }

        if (CollectionUtils.isEmpty(idpCerts)) {
            //if no certs for identity provider, signature obviously won't validate. Just throw standard error message.
            log.warn("Error validation signature for idp '%s'; no registered certificates!", identityProvider.getProviderId());
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
                log.debug(String.format("Signature did not validate w/ cert '%s'", x509Certificate));
                lastException = ex;
            }
        }
        if (!validated) {
            throw lastException;
        }
    }

    private void validateSignature(Signature signature, X509Certificate publicCertificate) {
        try {
            BasicX509Credential credential = new BasicX509Credential(publicCertificate);
            SignatureValidator.validate(signature, credential);
        } catch (SignatureException t) {
            throw new SignatureValidationException("Signature is invalid", t);
        }
    }

    public void validateIssueInstant(DateTime issueInstant) {
        if (issueInstant == null) {
            throw new BadRequestException("Saml issueInstant is required");
        }

        DateTime now = new DateTime();
        int maxResponseAge = identityConfig.getReloadableConfig().getFederatedResponseMaxAge();
        int maxResponseSkew = identityConfig.getReloadableConfig().getFederatedResponseMaxSkew();
        int timeDelta = Seconds.secondsBetween(issueInstant, now).getSeconds();
        if (issueInstant.isAfter(now.plusSeconds(maxResponseSkew))) {
            throw new BadRequestException("Saml issueInstant cannot be in the future.");
        }
        if (timeDelta > maxResponseAge + maxResponseSkew) {
            throw new BadRequestException("Saml issueInstant cannot be older than " + maxResponseAge + " seconds.");
        }
    }

}
