package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SignatureValidationException;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import com.rackspace.idm.util.SamlSignatureValidator;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.opensaml.saml2.core.*;
import org.opensaml.xml.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for handling identity operations related
 * to identities that do not exist locally in the directory store
 *
 */
@Component
public class DefaultFederatedIdentityService implements FederatedIdentityService {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

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


    @Override
    public SamlAuthResponse processSamlResponse(Response response) {
        //validate
        SamlResponseDecorator decoratedSamlResponse = new SamlResponseDecorator(response);

        //before anything, validate the issuer and signature
        IdentityProvider provider = getIdentityProviderForResponse(decoratedSamlResponse);
        validateSignatureForProvider(decoratedSamlResponse.checkAndGetSignature(), provider);

        //sig valid. Now validate format of the request. Don't need to use results, just perform the validation
        decoratedSamlResponse.checkAndGetUsername();
        decoratedSamlResponse.checkAndGetSubjectConfirmationNotOnOrAfterDate();
        decoratedSamlResponse.checkAndGetAuthnInstant();
        decoratedSamlResponse.checkAndGetAuthContextClassRef();

        //validate the issueInstant is not older than the configure max age for a saml response
        DateTime issueInstant = decoratedSamlResponse.checkAndGetIssueInstant();
        validateIssueInstant(issueInstant);

        //Basic format is good. Now hand off request to handler for the user source
        IdentityProviderFederationTypeEnum federationType = provider.getFederationTypeAsEnum();
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

    /**
     * System must also log removal in user deletion log and send a feed event indication
     * the deletion of the user
     * @param logoutRequest
     */
    @Override
    public SamlLogoutResponse processLogoutRequest(LogoutRequest logoutRequest) {
        LogoutRequestDecorator decoratedLogoutRequest = new LogoutRequestDecorator(logoutRequest);
        SamlLogoutResponse logoutResponse;

        //before anything, validate the issuer and signature
        IdentityProvider provider;
        try {
            provider = getIdentityProviderForLogoutRequest(decoratedLogoutRequest);
            validateSignatureForProvider(decoratedLogoutRequest.checkAndGetSignature(), provider);

            //sig valid. Now validate format of the request. Don't need to use results, just perform the validation
            decoratedLogoutRequest.checkAndGetUsername();

            //validate the issueInstant is not older than the configure max age
            DateTime issueInstant = decoratedLogoutRequest.checkAndGetIssueInstant();
            validateIssueInstant(issueInstant);

            //Basic format is good. Now hand off request to handler for the user source
            IdentityProviderFederationTypeEnum providerSource = provider.getFederationTypeAsEnum();

            if (IdentityProviderFederationTypeEnum.DOMAIN == providerSource) {
                logoutResponse = provisionedUserSourceFederationHandler.processLogoutRequestForProvider(decoratedLogoutRequest, provider);
            } else if (IdentityProviderFederationTypeEnum.RACKER == providerSource) {
                logoutResponse = rackerSourceFederationHandler.processLogoutRequestForProvider(decoratedLogoutRequest, provider);
            } else {
                UnsupportedOperationException ex = new UnsupportedOperationException(String.format("Provider federation type '%s' not supported", providerSource));
                logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.RESPONDER_URI, ex.getMessage(), ex);
            }
        } catch (BadRequestException e) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.REQUESTER_URI, e.getMessage(), e);
        } catch (Exception e) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.RESPONDER_URI, "Error encountered processing LogoutRequest", e);
        }

        return logoutResponse;
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

    private IdentityProvider getIdentityProviderForLogoutRequest(LogoutRequestDecorator logoutRequestDecorator) {
        IdentityProvider provider = identityProviderDao.getIdentityProviderByUri(logoutRequestDecorator.checkAndGetIssuer());
        if (provider == null) {
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_PROVIDER, "Issuer is unknown"));
        }
        return provider;
    }

    @Override
    public void addIdentityProvider(IdentityProvider identityProvider) {
        identityProvider.setProviderId(getNextId());
        identityProviderDao.addIdentityProvider(identityProvider);
    }

    @Override
    public void updateIdentityProvider(IdentityProvider identityProvider) {
        identityProviderDao.updateIdentityProvider(identityProvider);
    }

    @Override
    public IdentityProvider getIdentityProvider(String id) {
        return identityProviderDao.getIdentityProviderById(id);
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return identityProviderDao.getIdentityProviderByName(name);
    }

    @Override
    public IdentityProvider checkAndGetIdentityProvider(String id) {
        IdentityProvider provider = getIdentityProvider(id);

        if (provider == null) {
            String errMsg = String.format("Identity Provider with id/name: '%s' was not found.", id);
            log.info(errMsg);
            throw new NotFoundException(errMsg);
        }
        return provider;
    }

    @Override
    public IdentityProvider getIdentityProviderByIssuer(String issuer) {
        return identityProviderDao.getIdentityProviderByUri(issuer);
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId) {
        return identityProviderDao.findIdentityProvidersApprovedForDomain(domainId);
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForDomain(String domainId) {
        return identityProviderDao.findIdentityProvidersExplicitlyApprovedForDomain(domainId);
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForAnyDomain() {
        return identityProviderDao.findIdentityProvidersExplicitlyApprovedForAnyDomain();
    }

    @Override
    public List<IdentityProvider> findAllIdentityProviders() {
        return identityProviderDao.findAllIdentityProviders();
    }

    @Override
    public void deleteIdentityProviderById(String id) {
        try {
            identityProviderDao.deleteIdentityProviderById(id);
        } catch (NotFoundException e) {
            throw new NotFoundException(ErrorCodes.ERROR_MESSAGE_IDP_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, e);
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

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
