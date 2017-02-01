package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    Set<String> roleNames = Collections.EMPTY_SET;

    public FederatedDomainAuthRequest(FederatedAuthRequest federatedAuthRequest) {
        this.federatedAuthRequest = federatedAuthRequest;

        username = federatedAuthRequest.getWrappedSamlResponse().getUsername();
        domainId = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_DOMAIN);
        email = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_EMAIL);
        List<String> allRoleNames = federatedAuthRequest.getWrappedSamlResponse().getAttributeWithinAssertion(getBrokerAssertion(), SAMLConstants.ATTR_ROLES);
        if (CollectionUtils.isNotEmpty(allRoleNames)) {
            roleNames = new HashSet<>(allRoleNames);
        }

        validateStructure();
    }

    private void validateStructure() {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Invalid username. One, and only one, domain must be provided.");
        }
        if (StringUtils.isBlank(domainId)) {
            throw new BadRequestException("Invalid domain. One, and only one, domain must be provided.");
        }
        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("Invalid email. One, and only one, email address must be provided.");
        }
    }

    private String getSingleValueAttribute(Assertion assertion, String attributeName) {
        List<String> vals = federatedAuthRequest.getWrappedSamlResponse().getAttributeWithinAssertion(assertion, attributeName);
        if (vals == null || vals.size() != 1) {
            return null;
        }
        return vals.get(0);
    }
}
