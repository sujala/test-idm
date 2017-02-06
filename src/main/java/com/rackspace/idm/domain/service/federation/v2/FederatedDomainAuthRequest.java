package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.Response;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * A simple bean for extracting the necessary information from a raw saml request - refines a federatedAuthRequest to be
 * a Federated Domain Auth Request
 */
public class FederatedDomainAuthRequest {

    @Delegate
    private FederatedAuthRequest federatedAuthRequest;

    @Getter
    private String username;

    @Getter
    private String domainId;

    @Getter
    private String email;

    @Getter
    private Set<String> roleNames = Collections.EMPTY_SET;

    @Getter
    private AuthenticatedByMethodEnum authenticatedByForRequest;

    public FederatedDomainAuthRequest(FederatedAuthRequest federatedAuthRequest) {
        this.federatedAuthRequest = federatedAuthRequest;

        username = federatedAuthRequest.getWrappedSamlResponse().getUsername();
        domainId = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_DOMAIN);
        email = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_EMAIL);
        List<String> allRoleNames = federatedAuthRequest.getWrappedSamlResponse().getAttributeWithinAssertion(getBrokerAssertion(), SAMLConstants.ATTR_ROLES);
        if (CollectionUtils.isNotEmpty(allRoleNames)) {
            roleNames = new HashSet<>(allRoleNames);
        }
        authenticatedByForRequest = processAuthenticatedByForRequest();

        validateStructure();
    }

    public FederatedDomainAuthRequest(Response samlResponse) {
        this(new FederatedAuthRequest(samlResponse));
    }

    private void validateStructure() {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Invalid username. One, and only one, username must be provided.", ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }
        if (StringUtils.isBlank(domainId)) {
            throw new BadRequestException("Invalid domain. One, and only one, domain must be provided.", ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }
        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("Invalid email. One, and only one, email address must be provided.", ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }
        if (authenticatedByForRequest == null) {
            throw new BadRequestException("Unsupported authentication context class ref.", ERROR_CODE_FEDERATION2_INVALID_AUTH_CONTEXT);
        }
    }

    private AuthenticatedByMethodEnum processAuthenticatedByForRequest() {
        DomainAuthContextEnum val = DomainAuthContextEnum.fromSAMLAuthnContextClassRef(federatedAuthRequest.getIdpAuthContext());
        return val != null ? val.getIdmAuthBy() : null;
    }

    private enum DomainAuthContextEnum {
        PASSWORD(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.PASSWORD);

        private String samlAuthnContextClassRef;
        private AuthenticatedByMethodEnum idmAuthBy;

        DomainAuthContextEnum(String samlAuthnContextClassRef, AuthenticatedByMethodEnum idmAuthBy) {
            this.samlAuthnContextClassRef = samlAuthnContextClassRef;
            this.idmAuthBy = idmAuthBy;
        }

        public AuthenticatedByMethodEnum getIdmAuthBy() {
            return idmAuthBy;
        }

        /**
         * Convert from the SAML provided value to the enum
         *
         * @param value
         * @return
         */
        public static DomainAuthContextEnum fromSAMLAuthnContextClassRef(String value) {
            for (DomainAuthContextEnum domainAuthContextEnum : values()) {
                if (domainAuthContextEnum.samlAuthnContextClassRef.equalsIgnoreCase(value)) {
                    return domainAuthContextEnum;
                }
            }
            return null;
        }
    }
}
