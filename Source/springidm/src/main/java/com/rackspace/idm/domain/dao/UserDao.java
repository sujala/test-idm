package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;

public interface UserDao {

    void addUser(User user, String customerUniqueId);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    UserAuthenticationResult authenticateByMossoIdAndAPIKey(int mossoId, String apiKey);

    UserAuthenticationResult authenticateByNastIdAndAPIKey(String nastId, String apiKey);

    void deleteUser(String username);

    Users getAllUsers(int offset, int limit);

    String[] getGroupIdsForUser(String username);

    String getUnusedUserInum(String customerInum);

    User getUserByCustomerIdAndUsername(String customerId, String username);

    User getUserByInum(String inum);

    User getUserByMossoId(int mossoId);

    User getUserByNastId(String nastId);

    User getUserByRPN(String rpn);

    User getUserByUsername(String username);

    Users getUsersByCustomerId(String customerId, int offset, int limit);

    boolean isUsernameUnique(String username);

    void setUsersLockedFlagByCustomerId(String customerId, boolean locked);

    /**
     * @param user User instance with update changes
     * @param hasSelfUpdatedPassword True if the user is changing his/her own password.
     */
    void updateUser(User user, boolean hasSelfUpdatedPassword);
}
