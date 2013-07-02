package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.Filter;

import java.util.List;

public interface UserDao {

    void addUser(User user);

    void addRacker(Racker racker);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    void deleteRacker(String rackerId);

    void deleteUser(User user);
    
    void deleteUser(String username);

    void removeUsersFromClientGroup(ClientGroup group);

    List<User> getAllUsers(FilterParam[] filters, int offset, int limit);

    String[] getGroupIdsForUser(String username);

    Racker getRackerByRackerId(String rackerId);

    User getUserByCustomerIdAndUsername(String customerId, String username);

    User getUserById(String id);

    //List<User> getUsersByMossoId(int mossoId);

    //List<User> getUsersByNastId(String nastId);

    List<User> getUsersByDomainId(String domainId);

    User getUserByRPN(String rpn);

    User getUserBySecureId(String secureId);

    User getUserByUsername(String username);

    List<User> getUsersByEmail(String email);

    List<User> getUsers(List<Filter> filters);

    PaginatorContext<User> getUsersToReEncrypt(int offset, int limit);

    boolean isUsernameUnique(String username);

    /**
     * @param user User instance with update changes
     * @param hasSelfUpdatedPassword True if the user is changing his/her own password.
     */
    void updateUser(User user, boolean hasSelfUpdatedPassword);

    void updateUserEncryption(String userId);

    String getNextUserId();

    void softDeleteUser(User user);

    User getSoftDeletedUserById(String id);

    User getSoftDeletedUserByUsername(String username);

    void unSoftDeleteUser(User user);

    PaginatorContext<User> getAllUsersPaged(FilterParam[] filterParams, int offset, int limit);

    List<User> getAllUsersNoLimit(FilterParam[] filters);

    User getUserByDn(String userDn);

    List<User> getUsersByDomain(String domainId);

    List<User> getUsersByDomain(String domainId, boolean enabled);
}
