package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.SignatureValidationException;
import lombok.Getter;
import lombok.Setter;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * This class is responsible for managing the authenticating for v2 federation
 */
@Component
@Getter
@Setter
public class FederatedAuthHandlerV2 {
    private static final Logger log = LoggerFactory.getLogger(FederatedAuthHandlerV2.class);

    @Autowired
    private FederationUtils federationUtils;

    @Autowired
    private IdentityProviderDao identityProviderDao;

    @Autowired
    private FederatedDomainRequestHandler federatedDomainRequestHandler;

    @Autowired
    private FederatedRackerRequestHandler federatedRackerRequestHandler;

    @Autowired
    private IdentityConfig identityConfig;

    private SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();

    /**
     * Authenticates an Federated Authentication request per Identity v2 specifications
     *
     * @param rawSamlResponse
     * @return
     */
    public SamlAuthResponse authenticate(Response rawSamlResponse, boolean applyRcnRoles) {
        // Perform high level validation common to all fed auth requests
        FederatedAuthRequest federatedAuthRequest = new FederatedAuthRequest(rawSamlResponse);

        // Validate the request hasn't expired (based on issue instant of SAML Response)
        federationUtils.validateForExpiredRequest(federatedAuthRequest);

        // Validate the Broker IDP corresponds to an existing IDP
        IdentityProvider brokerIdp = identityProviderDao.getIdentityProviderByUri(federatedAuthRequest.getBrokerIssuer());
        if (brokerIdp == null) {
            throw new BadRequestException("Invalid issuer", ERROR_CODE_FEDERATION2_INVALID_BROKER_ISSUER);
        }

        // Validate the Signature prior to any more in-depth analysis
        try {
            log.debug(String.format("Attempting to validate a federated broker signature for idp '%s'", brokerIdp.getProviderId()));
            profileValidator.validate(federatedAuthRequest.getBrokerSignature()); //validate signature
            federationUtils.validateSignatureForIdentityProvider(federatedAuthRequest.getBrokerSignature(), brokerIdp);
        } catch (SignatureException|SignatureValidationException t) {
            log.debug("Received fed request with invalid signature", t);
            throw new BadRequestException("Signature could not be validated.", ERROR_CODE_FEDERATION_INVALID_BROKER_SIGNATURE, t);
        }

        // Signature is valid. Verify the signer is a broker
        IdentityProviderFederationTypeEnum brokerFederationType = brokerIdp.getFederationTypeAsEnum();
        if (IdentityProviderFederationTypeEnum.BROKER != brokerFederationType) {
            throw new ForbiddenException("v2 can not process non-brokered IDP requests", ERROR_CODE_FEDERATION2_INVALID_BROKER_ISSUER);
        }

        // Validate the Origin IDP corresponds to an existing IDP
        IdentityProvider originIdp = identityProviderDao.getIdentityProviderByUri(federatedAuthRequest.getOriginIssuer());
        if (originIdp == null) {
            throw new BadRequestException("Invalid issuer", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER);
        }

        // Only DOMAIN IDPs are currently allowed to be disabled
        if (originIdp.getEnabled() != null && !originIdp.getEnabled()
                && IdentityProviderFederationTypeEnum.DOMAIN == originIdp.getFederationTypeAsEnum()) {
            throw new ForbiddenException("Issuer is disabled.", ERROR_CODE_FEDERATION2_DISABLED_ORIGIN_ISSUER);
        }

        // Validate the Signature on each origin assertion prior to any more in-depth analysis
        for (Assertion originAssertion : federatedAuthRequest.getOriginAssertions()) {
            log.debug(String.format("Attempting to validate a federated origin signature for idp '%s'", originIdp.getProviderId()));
            try {
                profileValidator.validate(originAssertion.getSignature());
                federationUtils.validateSignatureForIdentityProvider(originAssertion.getSignature(), originIdp);
            } catch (SignatureException|SignatureValidationException t) {
                log.debug("Received fed request with invalid origin assertion signature", t);
                throw new BadRequestException("Signature could not be validated.", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_SIGNATURE, t);
            }
        }

        // All Origin Signatures are valid. Now verify the origin IDP is a valid type
        SamlAuthResponse samlAuthResponse = null;
        IdentityProviderFederationTypeEnum originFederationType = originIdp.getFederationTypeAsEnum();
        if (IdentityProviderFederationTypeEnum.DOMAIN == originFederationType) {
            FederatedDomainAuthRequest federatedDomainAuthRequest = new FederatedDomainAuthRequest(federatedAuthRequest);
            samlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(federatedDomainAuthRequest, originIdp, applyRcnRoles);
        } else if (IdentityProviderFederationTypeEnum.RACKER == originFederationType) {
            FederatedRackerAuthRequest federatedRackerAuthRequest = new FederatedRackerAuthRequest(federatedAuthRequest);
            samlAuthResponse = federatedRackerRequestHandler.processAuthRequestForProvider(federatedRackerAuthRequest, originIdp);
        } else {
            throw new ForbiddenException("The Origin IDP is not valid", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER);
        }

        Audit.logSuccessfulFederatedAuth(samlAuthResponse.getUser());
        return samlAuthResponse;
   }
}
