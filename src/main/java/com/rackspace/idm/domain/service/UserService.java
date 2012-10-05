package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

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

    User getUserByNastId(String nastId);

    User getUserByMossoId(int mossoId);

    Users getUsersByMossoId(int mossoId);
    
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
    
    void setUserPassword(String userId, PasswordCredentials userCred, ScopeAccess token);

    void updateUser(User user, boolean hasSelfUpdatedPassword);
    void updateUserById(User user, boolean hasSelfUpdatedPassword);
    Password resetUserPassword(User user);

//    DateTime getUserPasswordExpirationDate(String userName);

    //UserAuthenticationResult authenticateRacker(String username, String password);
    
    void softDeleteUser(User user);
    boolean userExistsById(String userId);
    boolean userExistsByUsername(String username);
    boolean isMigratedUser(User user);

    void addBaseUrlToUser(Integer baseUrlId, User user);

    void removeBaseUrlFromUser(Integer baseUrlId, User user);

	List<User> getSubUsers(User user);
}
