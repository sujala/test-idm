package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.resource.cloud.Validator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import org.apache.commons.configuration.Configuration;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    void addUser(User user);
    
    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey);

    UserAuthenticationResult authenticateWithMossoIdAndApiKey(int mossoId, String apiKey);

    void deleteRacker(String rackerId);
    
    void deleteUser(User user);
    
    void deleteUser(String username);

    Users getAllUsers(FilterParam[] filters, Integer offset, Integer limit);

    Users getAllUsers(FilterParam[] filters);

    String generateApiKey();
    
    Racker getRackerByRackerId(String rackerId);
    
    List<String> getRackerRoles(String rackerId);

    User getUser(String username);

    User getUserByAuthToken(String authToken);
    
    User getUserById(String id);

    User checkAndGetUserById(String id);
    
    User getUserByRPN(String rpn);

    Users getUsersByTenantId(String tenantId);

    User getUserByTenantId(String tenantId);

    User getUserBySecureId(String secureId);

    User getUser(String customerId, String username);

    User getSoftDeletedUser(String id);

    User getSoftDeletedUserByUsername(String id);
    
    Applications getUserApplications(User user);
    
    User loadUser(String customerId, String username);
    
    User loadUser(String userId);

    User getUserByScopeAccess(ScopeAccess scopeAccess);

    boolean isUsernameUnique(String username);

    boolean hasSubUsers(String userId);
    
    void updateUser(User user, boolean hasSelfUpdatedPassword) throws IOException, JAXBException;

    Password resetUserPassword(User user);

//    DateTime getUserPasswordExpirationDate(String userName);

//    UserAuthenticationResult authenticateRacker(String username, String password);

    void softDeleteUser(User user) throws IOException, JAXBException;
    boolean userExistsById(String userId);
    boolean userExistsByUsername(String username);
    boolean isMigratedUser(User user);

    void addBaseUrlToUser(Integer baseUrlId, User user);

    void removeBaseUrlFromUser(Integer baseUrlId, User user);

    List<Tenant> getUserTenants(String userId);

	List<User> getSubUsers(User user);


    void setUserDao(UserDao userDao);

    void setAuthDao(AuthDao rackerDao);

    void setScopeAccesss(ScopeAccessDao scopeAccessObjectDao);

    void setApplicationService(ApplicationService clientService);

    void setConfig(Configuration config);

    void setPasswordComplexityService(PasswordComplexityService passwordComplexityService);

    void setCloudRegionService(CloudRegionService cloudRegionService);
    
    PaginatorContext<User> getAllUsersPaged(FilterParam[] filters, int offset, int limit);

    PaginatorContext<User> getUsersWithRole(FilterParam[] filters, String roleId, int offset, int limit);

    int getUserWeight(User user, String applicationId);

    void setValidator(Validator validator);

    User getUserByUsernameForAuthentication(String username);

    User checkAndGetUserByName(String username);

    void setTenantDao(TenantDao tenantDao);

    void setScopeAccessService(ScopeAccessService scopeAccessService);
}
