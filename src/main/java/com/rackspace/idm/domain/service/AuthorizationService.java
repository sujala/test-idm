package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import java.util.Collection;
import java.util.List;

public interface AuthorizationService {

    boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess);

    boolean isSelf(BaseUser requester, User requestedUser);

    boolean hasDefaultUserRole(EndUser user);
    boolean hasUserAdminRole(EndUser user);
    boolean hasServiceAdminRole(EndUser user);
    boolean hasIdentityAdminRole(EndUser user);

    boolean hasSameDomain(EndUser caller, EndUser retrievedUser);

    /**
     * Verifies the effective caller can impersonate some other user. It does not determine whether the caller can impersonate the
     * particular user the caller is asking to impersonate.
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the specified caller does not exist (e.g. Racker doesn't exist in eDir)
     * @throws ForbiddenException If the user does not have the appropriate role to impersonate another user.
     */
    void verifyEffectiveCallerCanImpersonate();

    void verifyEffectiveCallerHasTenantAccess(String tenantId);
    void verifyDomain(BaseUser caller, BaseUser retrievedUser);


    /**
     * Convert an identity 'classification' role to a static enum that can be subsequently used for comparison/identification purposes
     *
     * @return
     * @throws java.lang.IllegalArgumentException If the provided role is not an identity classification role
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(ClientRole identityTypeRole);

    /**
     * Return the classification of the user as a static enum that can be subsequently used for comparison/identification purposes.
     * Returns null if the user is a Racker or the specified user does not have a classification role.
     *
     * @param baseUser
     * @return
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(BaseUser baseUser);

    /**
     * Given a collection of tenant roles associated with the user, determine the associated Identity role for that user
     *
     * @param userRoles
     * @return
     */
    IdentityUserTypeEnum getIdentityTypeRoleAsEnum(Collection<TenantRole> userRoles);

    /**
     * Whether the effective caller has the specified identity type (or higher) and/or identity role with the
     * specified name. This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleName
     */
    boolean authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName);

    /**
     * Whether the effective caller has the specified identity type (or higher) and/or identity role with one of the
     * specified names. This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleName
     */
    boolean authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum identityType, String... roleName);

    /**
     * Whether the effective caller has the specified identity type (or higher).
     *
     * @param identityType
     */
    boolean authorizeEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum identityType);

    /**
     * Whether the effective caller has at least one role with the specified role name. This can only be used to check
     * identity RBAC roles.
     *
     * @param roleNames
     */
    boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames);

    /**
     * Whether the effective caller has at least one role with the specified role name. This can only be used to check
     * identity RBAC roles.
     *
     * @param roleNames
     */
    boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(String... roleNames);

    /**
     * Verifies that the effective caller has the specified identity type (or higher) and/or a role with the
     * specified name. If so, the method returns. Otherwise, throws a ForbiddenException.
     *
     * This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleName
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName);

    /**
     * Verifies that the effective caller has the specified identity type (or higher) and/or a role with one of the
     * specified names. If so, the method returns. Otherwise, throws a ForbiddenException.
     *
     * This can only be used to check identity RBAC roles.
     *
     * @param identityType
     * @param roleNames
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum identityType, String... roleNames);

    /**
     * Verifies that the effective caller has the specified identity type (or higher). If so, the method returns;
     * otherwise, throws a ForbiddenException.
     *
     * @param identityType
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum identityType);

    /**
     * Verifies that the effective caller has the specified identity type (or higher) and/or a role with the
     * specified name. If so, the method returns. Otherwise, throws a ForbiddenException.
     *
     * This can only be used to check identity RBAC roles.
     *
     * @param roleNames
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames);

    /**
     * Performs the following validation on the effective caller:
     * <ol>
     *     <li>Exists (else NotFoundException)</li>
     *     <li>Is enabled (else NotAuthorizedException)</li>
     *     <li>Is assigned the specified role as either a tenant or global role (else ForbiddenException)</li>
     * </ol>
     *
     * @param roleName
     * @throws com.rackspace.idm.exception.ForbiddenException
     * @throws com.rackspace.idm.exception.NotFoundException
     * @throws com.rackspace.idm.exception.NotAuthorizedException
     */
    void verifyEffectiveCallerHasRoleByName(String roleName);

    /**
     * Return the list of implicit roles associated with the role with the given name.
     *
     * @param roleName
     * @return
     */
    List<ImmutableClientRole> getImplicitRolesForRole(String roleName);

    /**
     * Checks that a user should be considered disabled based on the user state and the service catalog information
     * that would be returned for the user.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     * - The IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP == true
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictUserAuthentication(ServiceCatalogInfo serviceCatalogInfo);

    /**
     * Checks that a user should be considered SUSPENDED based on the user state
     * and the endpoints for their tokens would be returned.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     * - The IdentityConfig.FEATURE_LIST_ENDPOINTS_FOR_TOKEN_FILTERED_FOR_TERMINATOR_PROP == true
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictTokenEndpoints(ServiceCatalogInfo serviceCatalogInfo);

    /**
     * Checks that a user should be considered SUSPENDED based on the user state.
     *
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     *
     * @param serviceCatalogInfo
     * @return
     */
    boolean restrictEndpointsForTerminator(ServiceCatalogInfo serviceCatalogInfo);

    /**
     * Verifies the caller has at least the user-manage role and has access to modify user.
     *
     * @param userId
     * @throws com.rackspace.idm.exception.ForbiddenException
     */
    void verifyEffectiveCallerHasManagementAccessToUser(String userId);

    /**
     * Check whether or not the caller is authorized to modify a delegation agreement.
     *
     * @param delegationAgreement
     * @throws IllegalArgumentException if the delegationAgreement, or delegationAgreement's principal is null.
     */
    boolean isCallerAuthorizedToManageDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * If the passed in AuthenticationRequest does not contain a domainId, this service will update it with a default
     * domainId based on:
     * 1. If the request specifies a tenantId or tenantName and the tenant exists, the domainid is set to the tenant's domain
     * 2. Else the domainId is set to the user's domain
     *
     * The update will only occur if the repository feature flag <i>feature.enable.authorization.domain.default</i> is enabled.
     * @param user
     * @param authenticationRequest
     * @return
     */
    String updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(BaseUser user, AuthenticationRequest authenticationRequest);

    /**
     * If the repository feature flag <i>feature.enable.authorization.domain.verification</i> is enabled, this method
     * verifies that the specified user is authorized to authenticate to the specified domain. If not, an UnauthorizedException
     * is thrown. If the supplied domainId is null or whitespace only, no verification is performed.
     *
     * @param user
     * @param domainId
     */
    void verifyUserAuthorizedToAuthenticateOnDomain(BaseUser user, String domainId);
}