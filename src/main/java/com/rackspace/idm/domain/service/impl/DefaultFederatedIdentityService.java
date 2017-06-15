package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.domain.service.federation.v2.FederatedAuthHandlerV2;
import com.rackspace.idm.exception.BadRequestException;
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
 */
@Component
public class DefaultFederatedIdentityService implements FederatedIdentityService {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

    @Autowired
    private IdentityProviderDao identityProviderDao;

    @Autowired
    private SamlSignatureValidator samlSignatureValidator;

    @Autowired FederatedAuthHandlerV1 federatedAuthHandlerV1;

    @Autowired
    FederatedAuthHandlerV2 federatedAuthHandlerV2;

    @Autowired
    private ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler;

    @Autowired
    private RackerSourceFederationHandler rackerSourceFederationHandler;

    @Autowired
    private IdentityConfig identityConfig;

    public static final String ERROR_SERVICE_UNAVAILABLE = "Service Unavailable";

    public static final String IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE = "Identity Provider with id/name: '%s' was not found.";

    @Override
    public SamlAuthResponse processSamlResponse(Response response) throws ServiceUnavailableException {
        return federatedAuthHandlerV1.authenticate(response);
   }

    @Override
    public SamlAuthResponse processV2SamlResponse(Response response) throws ServiceUnavailableException {
        try {
            return federatedAuthHandlerV2.authenticate(response);
        } catch (UnsupportedOperationException e) {
            //TODO This catch is just a temporary block until CID-585/CID-618 are implemented
            throw new ServiceUnavailableException(e.getMessage());
        }
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
                logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.RESPONDER, ex.getMessage(), ex);
            }
        } catch (BadRequestException e) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.REQUESTER, e.getMessage(), e);
        } catch (Exception e) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(logoutRequest.getID(), StatusCode.RESPONDER, "Error encountered processing LogoutRequest", e);
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
    public IdentityProvider getIdentityProviderWithMetadataById(String id) {
        return identityProviderDao.getIdentityProviderWithMetadataById(id);
    }

    @Override
    public IdentityProvider getIdentityProviderApprovedForDomain(String name, String domainId) {
        return identityProviderDao.getIdentityProviderApprovedForDomain(name, domainId);
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomain(String name, String domainId) {
        return identityProviderDao.getIdentityProviderExplicitlyApprovedForDomain(name, domainId);
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForAnyDomain(String name) {
        return identityProviderDao.getIdentityProvidersExplicitlyApprovedForAnyDomain(name);
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForAnyDomainByIssuer(String issuer) {
        return identityProviderDao.getIdentityProvidersExplicitlyApprovedForAnyDomainByIssuer(issuer);
    }

    @Override
    public IdentityProvider checkAndGetIdentityProvider(String id) {
        IdentityProvider provider = getIdentityProvider(id);

        if (provider == null) {
            String errMsg = String.format(IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, id);
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
    public IdentityProvider getIdentityProviderApprovedForDomainByIssuer(String issuer, String domainId) {
        return identityProviderDao.getIdentityProviderApprovedForDomainByIssuer(issuer, domainId);
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomainByIssuer(String issuer, String domainId) {
        return identityProviderDao.getIdentityProviderExplicitlyApprovedForDomainByIssuer(issuer, domainId);
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
