package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;

public interface UserDao {

    void addUser(User user);

    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    void deleteRacker(String rackerId);

    void deleteUser(User user);
    
    void deleteUser(String username);

    void removeUsersFromClientGroup(ClientGroup group);

    Users getAllUsers(FilterParam[] filters, int offset, int limit);

    String[] getGroupIdsForUser(String username);

    Racker getRackerByRackerId(String rackerId);

    User getUserByCustomerIdAndUsername(String customerId, String username);

    User getUserById(String id);

    Users getUsersByMossoId(int mossoId);

    Users getUsersByNastId(String nastId);

    Users getUsersByDomainId(String domainId);

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

    PaginatorContext<User> getPaginatedUsers(FilterParam[] filterParams, int offset, int limit);
}
