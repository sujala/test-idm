package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordCredentials;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.DuplicateException;

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

    Users getAllUsers(FilterParam[] filters, int offset, int limit);
    
    String generateApiKey();
    
    Racker getRackerByRackerId(String rackerId);
    
    List<String> getRackerRoles(String rackerId);

    User getUser(String username);
    
    User getUserById(String id);
    
    User getUserByRPN(String rpn);

    User getUserByNastId(String natsId);

    User getUserByMossoId(int mossoId);
    
    User getUserBySecureId(String secureId);

    User getUser(String customerId, String username);
    
    Applications getUserApplications(User user);
    
    User loadUser(String customerId, String username);
    
    User loadUser(String userId);

    boolean isUsernameUnique(String username);
    
    void setUserPassword(String userId, PasswordCredentials userCred, ScopeAccess token);

    void updateUser(User user, boolean hasSelfUpdatedPassword);

    Password resetUserPassword(User user);

    void updateUserStatus(User user, String statusStr);

//    DateTime getUserPasswordExpirationDate(String userName);

    //UserAuthenticationResult authenticateRacker(String username, String password);
}
