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

    void addUser(User user);

    void addUserV20(User user);

    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    void deleteUser(User user);
    
    void deleteUser(String username);

    Iterable<User> getUsersByRCN(String RCN);

    Iterable<User> getUsersByUsername(String username);

    List<User> getAllUsers();

    Iterable<User> getUsersWithDomainAndEnabledFlag(String domainId, Boolean enabled);

    Iterable<User> getUsersByGroupId(String groupId);

    String generateApiKey();
    
    Racker getRackerByRackerId(String rackerId);
    
    List<String> getRackerRoles(String rackerId);

    User getUser(String username);

    Iterable<User> getUsersByEmail(String email);

    User getUserByAuthToken(String authToken);
    
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

    PaginatorContext<User> getUsersByGroupId(String groupId, int offset, int limit);

    int getUserWeight(User user, String applicationId);

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

    void checkUserDisabledByScopeAccess(ScopeAccess scopeAccess);
}
