package com.rackspace.idm.domain.service;

import java.util.List;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.jaxb.UserCredentials;

public interface UserService {

    void addUser(User user) throws DuplicateException;
    
    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey);

    UserAuthenticationResult authenticateWithMossoIdAndApiKey(int mossoId, String apiKey);

    void deleteRacker(String rackerId);
    
    void deleteUser(String username);

    Users getByCustomerId(String customerId, int offset, int limit);

    String generateApiKey();
    
    Racker getRackerByRackerId(String rackerId);
    
    List<String> getRackerRoles(String rackerId);

    User getUser(String username);
    
    User getUserByRPN(String rpn);

    User getUserByNastId(String natsId);

    User getUserByMossoId(int mossoId);
    
    User getUserBySecureId(String secureId);

    User getUser(String customerId, String username);
    
    Clients getUserServices(User user);
    
    User checkAndGetUser(String customerId, String username);

    boolean isUsernameUnique(String username);
    
    void setUserPassword(String customerId, String username, UserCredentials userCred, ScopeAccess token, 
        boolean isRecovery);

    void updateUser(User user, boolean hasSelfUpdatedPassword);

    Password resetUserPassword(User user);

    void updateUserStatus(User user, String statusStr);

    DateTime getUserPasswordExpirationDate(String userName);
}
