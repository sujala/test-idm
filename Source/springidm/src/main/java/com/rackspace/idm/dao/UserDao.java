package com.rackspace.idm.dao;

import java.util.List;
import java.util.Map;

import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.Users;

public interface UserDao {

    boolean authenticate(String userName, String password);

    boolean authenticateByAPIKey(String username, String apiKey);

    void add(User user, String customerDN);

    User findByEmail(String email);

    User findUser(String customerId, String username);

    User findUser(String customerId, String username,
        Map<String, String> userStatusMap);

    User findByUsername(String username);

    User findByNastId(String nastId);

    User findByMossoId(int mossoId);

    User findByInum(String inum);

    List<User> findAll();

    boolean isUsernameUnique(String username);

    void save(User user);

    void setAllUsersLocked(String customerId, boolean locked);

    void delete(String username);

    String getUserDnByUsername(String username);

    String getUnusedUserInum(String customerInum);

    String[] getRoleIdsForUser(String username);

    Users findByCustomerId(String customerId, int offset, int limit);

    Users findFirst100ByCustomerIdAndLock(String customerId, boolean isLocked);

    void saveRestoredUser(User user, Map<String, String> userStatusMap);
}
