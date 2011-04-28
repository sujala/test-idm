package com.rackspace.idm.domain.service;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.jaxb.PasswordRecovery;
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

    User getUser(String username);
    
    User getUserByRPN(String rpn);

    User getUserByNastId(String natsId);

    User getUserByMossoId(int mossoId);

    User getUser(String customerId, String username);
    
    User checkAndGetUser(String customerId, String username);

    boolean isUsernameUnique(String username);

    void sendRecoveryEmail(String username, String userEmail, PasswordRecovery recoveryParam,
        String tokenString);
    
    void setUserPassword(String customerId, String username, UserCredentials userCred, ScopeAccessObject token, 
        boolean isRecovery);

    void updateUser(User user, boolean hasSelfUpdatedPassword);

    Password resetUserPassword(User user);

    void updateUserStatus(User user, String statusStr);

    DateTime getUserPasswordExpirationDate(String userName);
    
    void grantPermission(String userName, Permission p);
    
    void revokePermission(Permission p);
    
    Permission getGrantedPermission(String userName, String permissionId);
}
