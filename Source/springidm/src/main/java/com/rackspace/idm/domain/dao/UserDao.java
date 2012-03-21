package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.Users;

public interface UserDao {

    void addUser(User user);

    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    UserAuthenticationResult authenticateByMossoIdAndAPIKey(int mossoId,
        String apiKey);

    UserAuthenticationResult authenticateByNastIdAndAPIKey(String nastId,
        String apiKey);

    void deleteRacker(String rackerId);

    void deleteUser(User user);
    
    void deleteUser(String username);

    void removeUsersFromClientGroup(ClientGroup group);

    Users getAllUsers(FilterParam[] filters, int offset, int limit);

    String[] getGroupIdsForUser(String username);

    Racker getRackerByRackerId(String rackerId);

    User getUserByCustomerIdAndUsername(String customerId, String username);

    User getUserById(String id);

    User getUserByMossoId(int mossoId);

    User getUserByNastId(String nastId);

    User getUserByRPN(String rpn);

    User getUserBySecureId(String secureId);

    User getUserByUsername(String username);

    boolean isUsernameUnique(String username);

    /**
     * @param user User instance with update changes
     * @param hasSelfUpdatedPassword True if the user is changing his/her own password.
     */
    void updateUser(User user, boolean hasSelfUpdatedPassword);
    void updateUserById(User user, boolean hasSelfUpdatedPassword);
    
    String getNextUserId();

    void softDeleteUser(User user);

    User getSoftDeletedUserById(String id);

    User getSoftDeletedUserByUsername(String username);

    void unSoftDeleteUser(User user);

    public String addApplicationContainerToUser(User user, String clientId);
}
