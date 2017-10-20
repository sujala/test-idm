package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.Response;

import java.util.*;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * A simple bean for extracting the necessary information from a raw saml request - refines a federatedAuthRequest to be
 * a Federated Domain Auth Request
 */
public class FederatedDomainAuthRequest {

    public static final String INVALID_USER_GROUP_NAME_ERROR_MSG = "Invalid group assignment '%s'. A group name must be provided";

    @Delegate
    private FederatedAuthRequest federatedAuthRequest;

    @Getter
    private String username;

    @Getter
    private String domainId;

    @Getter
    private String email;

    /**
     * A mapping of a role name to the tenant assignments of the role. A null value in the map (or an empty set)
     * for a role name indicates the role is globally assigned. If the set of tenants is empty, it should also be
     * considered a global assignment.
     */
    @Getter
    private Map<String, Set<String>> roleNames = Collections.EMPTY_MAP;

    @Getter
    private Set<String> groupNames = Collections.EMPTY_SET;

    @Getter
    private AuthenticatedByMethodEnum authenticatedByForRequest;

    public FederatedDomainAuthRequest(FederatedAuthRequest federatedAuthRequest) {
        this.federatedAuthRequest = federatedAuthRequest;

        username = federatedAuthRequest.getWrappedSamlResponse().getUsername();
        domainId = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_DOMAIN);
        email = getSingleValueAttribute(getBrokerAssertion(), SAMLConstants.ATTR_EMAIL);
        populateRoleNames();
        populateUserGroupNames();
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

    private void populateRoleNames() {

        List<String> allRoleValues = federatedAuthRequest.getWrappedSamlResponse().getAttributeWithinAssertion(getBrokerAssertion(), SAMLConstants.ATTR_ROLES);
        if (CollectionUtils.isNotEmpty(allRoleValues)) {
            roleNames = new HashMap<>(); // Could store a null key and a null value
            for (String roleAssignmentValue : allRoleValues) {
                FederatedRoleAssignment ra = new FederatedRoleAssignment(roleAssignmentValue);
                if (StringUtils.isBlank(ra.roleName)) {
                    throw new BadRequestException(String.format("Invalid role assignment '%s'. A role name must be provided", roleAssignmentValue), ERROR_CODE_FEDERATION2_INVALID_ROLE_ASSIGNMENT);
                }
                if (!roleNames.containsKey(ra.roleName)) {
                /*
                 If list doesn't contain the role yet, then we haven't seen this rolename yet. Will need to populate
                 the map. Populating the value with null (as opposed to a Set) means the role assignment is global
                 */
                    HashSet<String> tenants = null;
                    if (StringUtils.isNotBlank(ra.tenantName)) {
                        tenants = new HashSet<>();
                        tenants.add(ra.tenantName);
                    }
                    roleNames.put(ra.roleName, tenants);
                } else {
                    /*
                     If the list already contains the role, we either add a tenant, or ignore a tenant assignment
                     if the role is already assigned globally
                     */
                    Set<String> roleTenants = roleNames.get(ra.roleName);
                    if (roleTenants != null) {
                        if (StringUtils.isNotBlank(ra.tenantName)) {
                            // Another tenant assignment, add to list
                            roleTenants.add(ra.tenantName);
                        } else {
                            // Global assignment. Null out list so assigned globally
                            roleNames.put(ra.roleName, null);
                        }
                    }
                }
            }
        }

    }

    private void populateUserGroupNames() {
        List<String> allGroupValues = federatedAuthRequest.getWrappedSamlResponse().getAttributeWithinAssertion(getBrokerAssertion(), SAMLConstants.ATTR_GROUPS);

        if (CollectionUtils.isNotEmpty(allGroupValues)) {
            groupNames = new HashSet<>();
            for (String groupName : allGroupValues) {
                if (StringUtils.isBlank(groupName)) {
                    throw new BadRequestException(String.format(INVALID_USER_GROUP_NAME_ERROR_MSG, groupName), ERROR_CODE_FEDERATION2_INVALID_GROUP_ASSIGNMENT);
                }
                groupNames.add(groupName);
            }
        }

    }

    private enum DomainAuthContextEnum {
        PASSWORD(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.PASSWORD),
        OTHER("", AuthenticatedByMethodEnum.OTHER); // Use "" as value so non-null value

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
            return OTHER;
        }
    }
}
