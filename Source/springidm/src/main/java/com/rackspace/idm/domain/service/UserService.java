package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;

import java.util.List;

public interface UserService {

    void addUser(User user) throws DuplicateException;
    
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
    
    User getUserByRPN(String rpn);

    User getUserByNastId(String natsId);

    User getUserByMossoId(int mossoId);

    Users getUsersByMossoId(int mossoId);
    
    User getUserBySecureId(String secureId);

    User getUser(String customerId, String username);

    User getSoftDeletedUser(String id);
    
    Applications getUserApplications(User user);
    
    User loadUser(String customerId, String username);
    
    User loadUser(String userId);

    User getUserByScopeAccess(ScopeAccess scopeAccess)throws Exception;

    boolean isUsernameUnique(String username);
    
    void setUserPassword(String userId, PasswordCredentials userCred, ScopeAccess token);

    void updateUser(User user, boolean hasSelfUpdatedPassword);
    void updateUserById(User user, boolean hasSelfUpdatedPassword);
    Password resetUserPassword(User user);

//    DateTime getUserPasswordExpirationDate(String userName);

    //UserAuthenticationResult authenticateRacker(String username, String password);
    
    void softDeleteUser(User user);
    boolean userExistsById(String userId);
    boolean userExistsByUsername(String username);
}
