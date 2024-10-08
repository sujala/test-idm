package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.domain.config.IdentityConfigHolder;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.signature.Signature;

import java.util.List;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * A simple bean that extracts the common information required for all Fed Auth requests from a SAML Response and performs a basic level of validation
 * on it.
 */
@Getter
public class FederatedAuthRequest {
    private SamlResponseWrapper wrappedSamlResponse;

    private String brokerIssuer;
    private String originIssuer;
    private Assertion brokerAssertion;
    private Signature brokerSignature;
    private List<Assertion> originAssertions;
    private DateTime responseIssueInstant;
    private DateTime requestedTokenExpiration;

    public FederatedAuthRequest(Response rawSamlResponse) {
        wrappedSamlResponse = new SamlResponseWrapper(rawSamlResponse);

        brokerIssuer = wrappedSamlResponse.getBrokerIssuer();
        brokerSignature = wrappedSamlResponse.getBrokerSignature();
        brokerAssertion = wrappedSamlResponse.getBrokerAssertion();
        responseIssueInstant = wrappedSamlResponse.getResponseIssueInstant();
        requestedTokenExpiration = wrappedSamlResponse.getRequestedTokenExpiration();

        originAssertions = wrappedSamlResponse.getOriginAssertions();
        originIssuer = wrappedSamlResponse.getOriginIssuer();

        validateStructure();
    }

    /**
     * Provides a quick and basic validation that the request meets the general contract requirements for
     * a federated auth request. The contents of the SAML Response are validated against the basic requirements for a
     * response (e.g. required fields exist).
     *
     * This guarantees the required data is present, but not necessarily valid. For example, while it
     * guarantees that a signature is provided, it does not validate that the signatures are actually valid. Similarly,
     * it guarantees that an "issuer" is specified, but does not guarantee that the issuer corresponds to a registered
     * IDP within Identity.
     *
     */
    private void validateStructure() {

        // Response Level Validations
        if (StringUtils.isBlank(brokerIssuer)) {
            throw new BadRequestException("Issuer is a required field", ERROR_CODE_FEDERATION2_MISSING_RESPONSE_ISSUER);
        }
        if (wrappedSamlResponse.getBrokerSignature() == null) {
            throw new BadRequestException("The SAML Response must be signed", ERROR_CODE_FEDERATION2_MISSING_RESPONSE_SIGNATURE);
        }
        if (responseIssueInstant == null) {
            throw new BadRequestException("An IssueInstant must be specified on the SAML Response", ERROR_CODE_FEDERATION_MISSING_ISSUE_INSTANT);
        }

        // Broker Assertion Level Validations
        if (brokerAssertion == null) {
            throw new BadRequestException("A broker assertion is required", ERROR_CODE_FEDERATION2_MISSING_BROKER_ASSERTION);
        }
        if (brokerAssertion.getIssuer() == null) {
            throw new BadRequestException("The broker assertion must specify an issuer", ERROR_CODE_FEDERATION2_MISSING_BROKER_ISSUER);
        }
        if (!brokerIssuer.equalsIgnoreCase(brokerAssertion.getIssuer().getValue())) {
            throw new BadRequestException("The broker assertion must match the response issuer", ERROR_CODE_FEDERATION2_INVALID_BROKER_ASSERTION);
        }

        if (requestedTokenExpiration == null) {
            throw new BadRequestException("Token expiration date is not specified", ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP);
        } else if (requestedTokenExpiration.isBeforeNow())  {
            throw new BadRequestException("Token expiration date must be in future", ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP);
        }

        // Origin Assertion Validation
        if (CollectionUtils.isEmpty(originAssertions)) {
            throw new BadRequestException("An origin assertion is required", ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION);
        }
        if (originIssuer == null) {
            throw new BadRequestException("The origin assertion must specify an issuer", ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ISSUER);
        }
        if (originIssuer.equalsIgnoreCase(brokerIssuer)) {
            throw new BadRequestException("The origin assertion specifies an invalid issuer", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ASSERTION);
        }

        // Iterate through all origin assertions
        for (Assertion originAssertion : originAssertions) {
            // All origin issuers must be same
            if (originAssertion.getIssuer() == null || !originIssuer.equalsIgnoreCase(originAssertion.getIssuer().getValue())) {
                throw new BadRequestException("All origin assertions must have the same issuer", ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER);
            }
            if (originAssertion.getSignature() == null) {
                throw new BadRequestException("All origin assertions must be signed", ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION_SIGNATURE);
            }
            if (IdentityConfigHolder.IDENTITY_CONFIG.getReloadableConfig().shouldV2FederationValidateOriginIssueInstant()) {
                if (originAssertion.getIssueInstant() == null) {
                    throw new BadRequestException("An IssueInstant must be specified on all origin assertions", ERROR_CODE_FEDERATION_MISSING_ISSUE_INSTANT);
                }
            }
        }
    }

    public String getIdpAuthContext() {
        AuthnContextClassRef ref = wrappedSamlResponse.getBrokerAuthContextClassRef();
        return ref != null ? ref.getAuthnContextClassRef() : null;
    }

    public String getSingleValueAttribute(Assertion assertion, String attributeName) {
        List<String> vals = wrappedSamlResponse.getAttributeWithinAssertion(assertion, attributeName);
        if (vals == null || vals.size() != 1) {
            return null;
        }
        return vals.get(0);
    }
}
