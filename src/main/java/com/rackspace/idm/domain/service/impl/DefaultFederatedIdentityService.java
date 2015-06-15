package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.FederatedIdentityService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.SignatureValidationException;
import com.rackspace.idm.util.SamlSignatureValidator;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Override
    public SamlAuthResponse processSamlResponse(Response response) {
        //validate
        SamlResponseDecorator decoratedSamlResponse = new SamlResponseDecorator(response);

        //before anything, validate the issuer and signature
        IdentityProvider provider = validateIssuer(decoratedSamlResponse);
        validateSignatureForProvider(decoratedSamlResponse.checkAndGetSignature(), provider);

        //sig valid. Now validate format of the request. Don't need to use results, just perform the validation
        decoratedSamlResponse.checkAndGetUsername();
        decoratedSamlResponse.checkAndGetSubjectConfirmationNotOnOrAfterDate();
        decoratedSamlResponse.checkAndGetAuthnInstant();
        decoratedSamlResponse.checkAndGetAuthContextClassRef();

        //Basic format is good. Now hand off request to handler for the user source
        TargetUserSourceEnum providerSource = provider.getTargetUserSourceAsEnum();
        if (TargetUserSourceEnum.PROVISIONED == providerSource) {
            return provisionedUserSourceFederationHandler.processRequestForProvider(decoratedSamlResponse, provider);
        } else if (TargetUserSourceEnum.RACKER == providerSource) {
            return rackerSourceFederationHandler.processRequestForProvider(decoratedSamlResponse, provider);
        }

        throw new UnsupportedOperationException(String.format("Provider user source '%s' not supported", providerSource));
    }

    private IdentityProvider validateIssuer(SamlResponseDecorator samlResponseDecorator) {
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
            throw new BadRequestException(ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SIGNATURE, "Signature is invalid"), t);
        }
    }
}
