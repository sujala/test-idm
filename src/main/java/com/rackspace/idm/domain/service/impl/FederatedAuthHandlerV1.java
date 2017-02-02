package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SignatureValidationException;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import com.rackspace.idm.util.SamlSignatureValidator;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.ServiceUnavailableException;
import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for handling identity operations related
 * to identities that do not exist locally in the directory store
 *
 * @deprecated The v1 Fed API is being deprecated in favor of 2.0.
 */
@Component
@Deprecated
public class FederatedAuthHandlerV1 {
    private static final Logger log = LoggerFactory.getLogger(FederatedAuthHandlerV1.class);

    @Autowired
    private IdentityProviderDao identityProviderDao;

    @Autowired
    private SamlSignatureValidator samlSignatureValidator;

    @Autowired
    private ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler;

    @Autowired
    private RackerSourceFederationHandler rackerSourceFederationHandler;

    @Autowired
    private IdentityConfig identityConfig;

    public static final String ERROR_SERVICE_UNAVAILABLE = "Service Unavailable";

    public SamlAuthResponse authenticate(Response response) throws ServiceUnavailableException {
        //validate
        SamlResponseDecorator decoratedSamlResponse = new SamlResponseDecorator(response);

        IdentityProvider provider = getIdentityProviderForResponse(decoratedSamlResponse);
        IdentityProviderFederationTypeEnum federationType = provider.getFederationTypeAsEnum();

        //before anything, validate the issuer and signature
        validateSignatureForProvider(decoratedSamlResponse.checkAndGetSignature(), provider);

        if (IdentityProviderFederationTypeEnum.BROKER == federationType) {
            throw new ForbiddenException("v1 Authentication is not supported for this IDP", ErrorCodes.ERROR_CODE_FEDERATION_INVALID_PROVIDER);
        }

        //sig valid. Now validate format of the request. Don't need to use results, just perform the validation
        decoratedSamlResponse.checkAndGetUsername();
        decoratedSamlResponse.checkAndGetSubjectConfirmationNotOnOrAfterDate();
        decoratedSamlResponse.checkAndGetAuthnInstant();
        decoratedSamlResponse.checkAndGetAuthContextClassRef();

        //validate the issueInstant is not older than the configure max age for a saml response
        DateTime issueInstant = decoratedSamlResponse.checkAndGetIssueInstant();
        validateIssueInstant(issueInstant);

        //Basic format is good. Now hand off request to handler for the user source
        SamlAuthResponse authResponse = null;

        if (IdentityProviderFederationTypeEnum.DOMAIN == federationType) {
            authResponse = provisionedUserSourceFederationHandler.processRequestForProvider(decoratedSamlResponse, provider);
        } else if (IdentityProviderFederationTypeEnum.RACKER == federationType) {
            authResponse = rackerSourceFederationHandler.processRequestForProvider(decoratedSamlResponse, provider);
        } else {
            throw new UnsupportedOperationException(String.format("Provider user source '%s' not supported", federationType));
        }

        Audit.logSuccessfulFederatedAuth(authResponse.getUser());
        return authResponse;
   }

    private void validateIssueInstant(DateTime issueInstant) {
        //validate the issueInstant is not older than the configure max age for a saml response
        DateTime now = new DateTime();
        int maxResponseAge = identityConfig.getReloadableConfig().getFederatedResponseMaxAge();
        int maxResponseSkew = identityConfig.getReloadableConfig().getFederatedResponseMaxSkew();
        int timeDelta = Seconds.secondsBetween(issueInstant, now).getSeconds();
        if (issueInstant.isAfter(now.plusSeconds(maxResponseSkew))) {
            throw new BadRequestException("Saml response issueInstant cannot be in the future.");
        }
        if (timeDelta > maxResponseAge + maxResponseSkew) {
            throw new BadRequestException("Saml responses cannot be older than " + maxResponseAge + " seconds.");
        }
    }

    private IdentityProvider getIdentityProviderForResponse(SamlResponseDecorator samlResponseDecorator) {
        IdentityProvider provider = identityProviderDao.getIdentityProviderByUri(samlResponseDecorator.checkAndGetIssuer());
        if (provider == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_PROVIDER, "Issuer is unknown"));
        }
        return provider;
    }

    private void validateSignatureForProvider(Signature signature, IdentityProvider provider) {
        try {
            samlSignatureValidator.validateSignatureForIdentityProvider(signature, provider);
        } catch (SignatureValidationException t) {
            log.debug("Received fed request with invalid signature", t);
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SIGNATURE, "Signature is invalid"), t);
        }
    }
}
