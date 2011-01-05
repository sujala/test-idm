package com.rackspace.idm.services;

import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.jaxb.PasswordRecovery;

public interface UserService {

    void addUser(User user) throws DuplicateException;

    boolean authenticate(String username, String password);

    boolean authenticateWithApiKey(String username, String apiKey);
    
    boolean authenticateWithNastIdAndApiKey(String nastId, String apiKey);
    
    boolean authenticateWithMossoIdAndApiKey(int mossoId, String apiKey);

    void deleteUser(String username);

    Users getByCustomerId(String customerId, int offset, int limit);

    String generateApiKey();

    User getUser(String username);
    
    User getUserByNastId(String natsId);
    
    User getUserByMossoId(int mossoId);
    
    User getUser(String customerId, String username);
    
    User getSoftDeletedUser(String customerId, String username);
    
    boolean isUsernameUnique(String username);

    void sendRecoveryEmail(String username, String userEmail,
        PasswordRecovery recoveryParam, String tokenString);

    void softDeleteUser(String username);

    void updateUser(User user);

    void updateUserStatus(User user, String statusStr);

    void restoreSoftDeletedUser(User user);
}
