package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.validation.Validator;
import org.apache.commons.configuration.Configuration;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    void setUserDefaultsBasedOnCaller(User user, User caller);

    void setUserDefaultsBasedOnCaller(User user, User caller, boolean isCreateUserInOneCall);

    void addUser(User user);

    void addUserV20(User user);

    void addUserV20(User user, boolean isCreateUserInOneCall);

    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    void deleteUser(User user);
    
    void deleteUser(String username);

    Iterable<User> getUsersByRCN(String RCN);

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
    
    List<String> getRackerRoles(String rackerId);

    User getUser(String username);

    Iterable<User> getUsersByEmail(String email);

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

    User getUserByTenantId(String tenantId);

    User getUser(String customerId, String username);

    User getSoftDeletedUser(String id);

    Applications getUserApplications(User user);
    
    User loadUser(String customerId, String username);
    
    User loadUser(String userId);

    BaseUser getUserByScopeAccess(ScopeAccess scopeAccess, boolean checkUserDisabled);

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

    void softDeleteUser(User user) throws IOException, JAXBException;

    void addBaseUrlToUser(String baseUrlId, User user);

    void removeBaseUrlFromUser(String baseUrlId, User user);

	List<User> getSubUsers(User user);

    void setUserDao(UserDao userDao);

    void setAuthDao(AuthDao rackerDao);

    void setApplicationService(ApplicationService clientService);

    void setConfig(Configuration config);

    void setCloudRegionService(CloudRegionService cloudRegionService);

    PaginatorContext<User> getAllEnabledUsersPaged(int offset, int limit);

    PaginatorContext<User> getAllUsersPagedWithDomain(String domainId, int offset, int limit);

    PaginatorContext<User> getUsersWithRole(String roleId, int offset, int limit);

    PaginatorContext<User> getUsersWithDomainAndRole(String domainId, String roleId, int offset, int limit);

    PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit);

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
    void validateUserIsEnabled(User user);

    void checkUserDisabledByScopeAccess(ScopeAccess scopeAccess);
}
