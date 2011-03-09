package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.jaxb.PasswordRecovery;

public interface UserService {

    void addUser(User user) throws DuplicateException;

    UserAuthenticationResult authenticate(String userId, String password);

    UserAuthenticationResult authenticateWithApiKey(String username, String apiKey);

    UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey);

    UserAuthenticationResult authenticateWithMossoIdAndApiKey(int mossoId, String apiKey);

    void deleteUser(String username);

    Users getByCustomerId(String customerId, int offset, int limit);

    String generateApiKey();

    User getUser(String username);
    
    User getUserByRPN(String rpn);

    User getUserByNastId(String natsId);

    User getUserByMossoId(int mossoId);

    User getUser(String customerId, String username);

    boolean isUsernameUnique(String username);

    void sendRecoveryEmail(String username, String userEmail, PasswordRecovery recoveryParam, String tokenString);

    void updateUser(User user);

    void updateUserStatus(User user, String statusStr);
}
