package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionResponse;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.validation.Validator;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    void addIdentityAdminV20(User user);

    void addSubUserV20(User user, boolean isCreateUserOneCall);

    void addUserAdminV20(User user, boolean isCreateUserOneCall);

    void addUnverifiedUser(User user);

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    void deleteUser(User user);
    
    void deleteUser(String username);

    Iterable<User> getUsersByUsername(String username);

    List<User> getAllUsers();

    Iterable<User> getUsersWithDomainAndEnabledFlag(String domainId, Boolean enabled);

    Iterable<User> getEnabledUsersByGroupId(String groupId);

    /**
     * Get all disabled users associated with specified groupId
     *
     * @param groupId
     * @return
     */
    Iterable<User> getDisabledUsersByGroupId(String groupId);

    String generateApiKey();
    
    Racker getRackerByRackerId(String rackerId);
    
    List<String> getRackerIamRoles(String rackerId);

    User getUser(String username);

    Iterable<User> getUsersByEmail (String email, User.UserType userType);

    User getUserByAuthToken(String authToken);

    /**
     * Retrieve the user for whom the token was issued. This returns the associated user if one exists, regardless of the state of the
     * token (e.g. expired) or the state of the user (e.g. - disabled).
     *
     * @param token
     * @return
     */
    BaseUser getTokenSubject(String token);

    User getUserById(String id);

    User checkAndGetUserById(String id);
    
    Iterable<User> getUsersByTenantId(String tenantId);

    /**
     * Retrieve the first user-admin found that is associated with the specified tenantId
     *
     * @param tenantId
     * @return
     */
    User getUserByTenantId(String tenantId);

    User loadUser(String userId);

    BaseUser getUserByScopeAccess(ScopeAccess scopeAccess, boolean checkUserDisabled);

    /**
     * Retrieve the user (racker, provisioned, federated) associated with the token
     *
     * @param scopeAccess
     * @return
     */
    BaseUser getUserByScopeAccess(ScopeAccess scopeAccess);

    boolean isUsernameUnique(String username);

    boolean hasSubUsers(String userId);
    
    void updateUser(User user) throws IOException, JAXBException;

    /**
     * Updates a user for multifactor. Assumes the passed in user was only modified for multifactor changes.
     *
     * @param user
     */
    void updateUserForMultiFactor(User user);

    Password resetUserPassword(User user);

    void addBaseUrlToUser(String baseUrlId, User user);

    void removeBaseUrlFromUser(String baseUrlId, User user);

	List<User> getSubUsers(User user);

    void setUserDao(UserDao userDao);

    void setRackerAuthDao(RackerAuthDao rackerDao);

    void setApplicationService(ApplicationService clientService);

    void setConfig(Configuration config);

    void setCloudRegionService(CloudRegionService cloudRegionService);

    PaginatorContext<User> getAllEnabledUsersPaged(int offset, int limit);

    PaginatorContext<User> getAllUsersPagedWithDomain(String domainId, int offset, int limit);

    PaginatorContext<User> getUsersWithRole(String roleId, int offset, int limit);

    PaginatorContext<User> getUsersWithDomainAndRole(String domainId, String roleId, int offset, int limit);

    PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit);

    /**
     * Return the list of enabled users with supplied contactId.
     * @param contactId
     * @throws IllegalArgumentException If supplied contactId is null or empty string
     * @return
     */
    Iterable<User> getEnabledUsersByContactId(String contactId);

    void setValidator(Validator validator);

    User getUserByUsernameForAuthentication(String username);

    User checkAndGetUserByName(String username);

    void setScopeAccessService(ScopeAccessService scopeAccessService);

    Iterable<User> getUsersWithDomain(String domainId);

    void setTenantService(TenantService tenantService);

    void reEncryptUsers();

    void setCryptHelper(CryptHelper cryptHelper);

    void setPropertiesService(PropertiesService propertiesService);

    void addGroupToUser(String groupId, String userId);

    void deleteGroupFromUser(String groupId, String userId);

    Iterable<Group> getGroupsForUser(String userId);

    boolean isUserInGroup(String userId, String groupId);

    /**
     * Checks that a user is considered enabled. This is not a one to one comparison with user enabled flag as it considers
     * other factors such as domain enabled status.
     *
     * @param user
     * @throws com.rackspace.idm.exception.UserDisabledException If user is not enabled or otherwise should be considered not enabled.
     */
    void validateUserIsEnabled(BaseUser user);

    void checkUserDisabled(BaseUser user);

    /**
     * Checks that a user should be considered disabled based on the enabled state of the user's assigned tenants.
     * Returns TRUE if the user should be considered disabled by meeting the following criteria:
     * - The user has AT LEAST ONE tenant
     * - ALL tenants on the user are disabled
     * - The user is a user admin or below level of access
     *
     * @param user
     * @return TRUE if a user should be considered disabled based on the state of their tenants
     */
    boolean userDisabledByTenants(EndUser user);

    /**
     * [CIDMDEV-5312] Remove Federated Users eligible for deletion.
     *
     * @param request
     * @param response
     */
    void expiredFederatedUsersDeletion(FederatedUsersDeletionRequest request, FederatedUsersDeletionResponse response);

    /**
     * The format of the nast tenant id is XY, where X is determined by the configuration property NAST_TENANT_PREFIX_PROP
     * and Y is the domainId.
     *
     * Returns null if the supplied domainId is null.
     *
     * @param domainId
     * @return
     */
    String getNastTenantId(String domainId);

    /**
     * Given a User object, configures the user to be a user admin for their domain. This method does the following:
     * - Verifies that the user will be the only user in the domain
     * - Attaches the user-admin role to the user
     * - Attaches the mosso and nast roles to the user if assignMossoAndNastDefaultRoles == true && user.domainId is numeric
     * @param user
     * @param assignMossoAndNastDefaultRoles
     */
    void configureNewUserAdmin(User user, boolean assignMossoAndNastDefaultRoles);

    /**
     * Add group to user
     *
     * @param group
     * @param baseUser
     */
    void addUserGroupToUser(UserGroup group, User baseUser);

    /**
     * Remove group from user
     *
     * @param group
     * @param baseUser
     */
    void removeUserGroupFromUser(UserGroup group, User baseUser);

    /**
     * Returns the calculated password expiration for a user.
     * Returns null for the following scenarios:
     *  - the user does not have a password change date
     *  - the user's domain does not have a password policy
     *
     * NOTE: Due to the above scenarios, a user's password can still be expired if
     * the user does not have a password change date and the domain has a password
     * policy set (policy password duration > 0). In order to determine if a password
     * is expired, use <code>UserService.getPasswordExpiration(User)</code> instead
     *
     * @param user
     * @throws IllegalArgumentException if the user is null
     * @return
     */
    DateTime getPasswordExpiration(User user);

    /**
     * Returns a boolean indicating if the user's password is expired or not.
     * A password is considered expired if:
     *  - the user has a password change date set that is older than the allowed domain password policy for the user's domain
     *  - the user does not have a password change date set and the domain has a non-zero password policy duration
     *
     * NOTE: If a user is pointing to a domain that does not exist, then the password is NOT considered expired.
     *
     * @param user
     * @throws IllegalArgumentException if the user is null
     * @return
     */
    boolean isPasswordExpired(User user);

    /*
     * Assign the specified roles to the user. The 'allowedRoleAccess` determines the administrator access on roles provided
     * in the list of role assignments.
     *
     * @param user
     * @param roleAssignments
     * @param allowedRoleAccess
     *
     * @throws IllegalArgumentException if user, user.getUniqueId(), roleAssignments or allowedRoleAccess is null
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceRoleAssignmentsOnUser(User user, RoleAssignments roleAssignments, IdentityUserTypeEnum callerUserType);

    /**
     * Retrieve the set of roles assignments on the user.
     *
     * @param user
     * @param paginationParams
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnUser(User user, PaginationParams paginationParams);

    /**
     * Retrieve the user-admin set on domain. If "rsUserAdminDN" is not set, return null.
     *
     * @param domain
     * @throws IllegalArgumentException If supplied domain is null.
     * @return
     */
    User getUserAdminByDomain(Domain domain);

    /**
     * Retrieve the user-admin associated with the specified tenantId. Return null if tenant, tenant's domain, or user
     * are not found.
     *
     * @param tenantId
     * @throws IllegalArgumentException If supplied tenantId is null.
     * @return
     */
    User getUserAdminByTenantId(String tenantId);

    String inferCloudBasedOnDomainType(String domainType);
}
