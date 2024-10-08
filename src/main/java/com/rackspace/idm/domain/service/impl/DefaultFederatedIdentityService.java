package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.domain.service.IdpPolicyFormatEnum;
import com.rackspace.idm.domain.service.federation.v2.FederatedAuthHandlerV2;
import com.rackspace.idm.domain.service.federation.v2.FederationUtils;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SignatureValidationException;
import com.rackspace.idm.exception.UnrecoverableIdmException;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import com.rackspace.idm.util.SamlSignatureValidator;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.lang.BooleanUtils;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.naming.ServiceUnavailableException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.rackspace.idm.ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER;
import static com.rackspace.idm.ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_ORIGIN_SIGNATURE;

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
    FederatedAuthHandlerV2 federatedAuthHandlerV2;

    @Autowired
    private ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler;

    @Autowired
    private RackerSourceFederationHandler rackerSourceFederationHandler;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private Validator20 validator20;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    DomainService domainService;

    @Autowired
    private FederationUtils federationUtils;

    private SAMLSignatureProfileValidator samlSignatureProfileValidator = new SAMLSignatureProfileValidator();

    public static final String IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE = "Identity Provider with id/name: '%s' was not found.";
    public static final String FEDERATION_IDP_CREATION_NOT_AVAILABLE_MISSING_DEFAULT_POLICY_MESSAGE = "IDP creation is currently unavailable due to missing default for IDP policy.";
    public static final String FEDERATION_IDP_DEFAULT_POLICY_INVALID_LOGGING_ERROR_MESSAGE = "Unable to load and parse the default IDP policy.";
    public static final String FEDERATION_IDP_DEFAULT_POLICY_INVALID_ERROR_MESSAGE = "The default IDP policy is not properly configured.";

    @Override
    public SamlAuthResponse processV2SamlResponse(Response response, boolean applyRcnRoles) throws ServiceUnavailableException {
        try {
            return federatedAuthHandlerV2.authenticate(response, applyRcnRoles);
        } catch (UnsupportedOperationException e) {
            //TODO This catch is just a temporary block until CID-585/CID-618 are implemented
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    @Override
    public void verifyLogoutRequest(LogoutRequest logoutRequest) {
        LogoutRequestDecorator decoratedLogoutRequest = new LogoutRequestDecorator(logoutRequest);

        // Validate the request hasn't expired (based on issue instant of SAML Request)
        federationUtils.validateForExpiredRequest(decoratedLogoutRequest);

        // Validate the IDP corresponds to an existing IDP
        IdentityProvider issuingIdp = identityProviderDao.getIdentityProviderByUri(decoratedLogoutRequest.checkAndGetIssuer());

        // When issueingIdp doesn't have a value for enabled flag is considered "true"
        if (issuingIdp == null || BooleanUtils.isFalse(issuingIdp.getEnabled())) {
            throw new BadRequestException("Invalid issuer", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER);
        }

        // Validate the Signature prior to any more in-depth analysis
        try {
            log.debug(String.format("Attempting to validate a federated logout request signature for idp '%s'", issuingIdp.getProviderId()));
            // Validate signature construct
            samlSignatureProfileValidator.validate(decoratedLogoutRequest.checkAndGetSignature());

            // Validate signature signed by IDP
            federationUtils.validateSignatureForIdentityProvider(decoratedLogoutRequest.checkAndGetSignature(), issuingIdp);
        } catch (SignatureException | SignatureValidationException t) {
            log.debug("Received fed request with invalid signature", t);
            throw new BadRequestException("Signature could not be validated.", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_SIGNATURE, t);
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
            federationUtils.validateForExpiredRequest(decoratedLogoutRequest);

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
    public IdentityProvider getIdentityProviderByEmailDomain(String emailDomain) {
        Assert.notNull(emailDomain);

        return identityProviderDao.getIdentityProviderByEmailDomain(emailDomain);
    }

    @Override
    public IdentityProvider getIdentityProviderWithMetadataById(String id) {
        return identityProviderDao.getIdentityProviderWithMetadataById(id);
    }

    @Override
    public IdentityProvider checkAndGetIdentityProviderWithMetadataById(String id) {
        IdentityProvider idp = identityProviderDao.getIdentityProviderWithMetadataById(id);
        checkIdpIsNotNull(id, idp);
        return idp;
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
        checkIdpIsNotNull(id, provider);
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
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForDomains(Collection<String> domainIds) {
        return identityProviderDao.findIdentityProvidersExplicitlyApprovedForDomains(domainIds);
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

    @Override
    public IdentityProperty checkAndGetDefaultMappingPolicyProperty() throws ServiceUnavailableException {
        IdentityProperty defaultPolicyProperty = identityConfig.getRepositoryConfig().getIdentityProviderDefaultPolicy();
        if (defaultPolicyProperty == null) {
            throw new ServiceUnavailableException(FEDERATION_IDP_CREATION_NOT_AVAILABLE_MISSING_DEFAULT_POLICY_MESSAGE);
        }
        // Validate default mapping policy
        try {
            validator20.validateIdpPolicy(new String(defaultPolicyProperty.getValue(), StandardCharsets.UTF_8),
                    IdpPolicyFormatEnum.valueOf(defaultPolicyProperty.getValueType().toUpperCase()));
        } catch (Exception e) {
            log.error(FEDERATION_IDP_DEFAULT_POLICY_INVALID_LOGGING_ERROR_MESSAGE, e);
            throw new UnrecoverableIdmException(FEDERATION_IDP_DEFAULT_POLICY_INVALID_ERROR_MESSAGE);
        }
        return defaultPolicyProperty;
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

    private void checkIdpIsNotNull(String idpId, IdentityProvider idp) {
        if (idp == null) {
            String errMsg = String.format(IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, idpId);
            log.info(errMsg);
            throw new NotFoundException(errMsg);
        }
    }

}
