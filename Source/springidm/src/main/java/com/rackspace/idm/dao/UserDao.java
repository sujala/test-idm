package com.rackspace.idm.dao;

import java.util.Map;

import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserAuthenticationResult;
import com.rackspace.idm.entities.Users;

public interface UserDao {

    void add(User user, String customerDN);

    boolean authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    UserAuthenticationResult authenticateByMossoIdAndAPIKey(int mossoId, String apiKey);

    UserAuthenticationResult authenticateByNastIdAndAPIKey(String nastId, String apiKey);

    void delete(String username);

    Users findAll(int offset, int limit);

    Users findByCustomerId(String customerId, int offset, int limit);

    User findByEmail(String email);

    User findByInum(String inum);

    User findByMossoId(int mossoId);

    User findByNastId(String nastId);

    User findByUsername(String username);

    User findUser(String customerId, String username);

    User findUser(String customerId, String username,
        Map<String, String> userStatusMap);

    String[] getRoleIdsForUser(String username);

    String getUnusedUserInum(String customerInum);

    String getUserDnByUsername(String username);

    boolean isUsernameUnique(String username);

    void save(User user);

    void saveRestoredUser(User user, Map<String, String> userStatusMap);

    void setAllUsersLocked(String customerId, boolean locked);
}
