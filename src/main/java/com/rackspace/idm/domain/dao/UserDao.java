package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface UserDao {

    void addUser(User user);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    void deleteUser(User user);

    void deleteUser(String username);

    void removeUsersFromClientGroup(ClientGroup group);

    Iterable<User> getUsers();

    PaginatorContext<User> getUsers(int offset, int limit);

    PaginatorContext<User> getUsersByGroupId(String groupId, int offset, int limit);

    PaginatorContext<User> getUsersByDomain(String domainId, int offset, int limit);

    Iterable<User> getUsersByRCN(String RCN);

    Iterable<User> getUsersByUsername(String username);

    String[] getGroupIdsForUser(String username);

    User getUserByCustomerIdAndUsername(String customerId, String username);

    User getUserById(String id);

    Iterable<User> getUsersByDomain(String domainId);

    User getUserByRPN(String rpn);

    User getUserBySecureId(String secureId);

    User getUserByUsername(String username);

    Iterable<User> getUsersByEmail(String email);

    Iterable<User> getUsers(List<String> idList);

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

    Iterable<User> getUsersByDomainAndEnabledFlag(String domainId, boolean enabled);

    Iterable<User> getUsersByGroupId(String groupId);

    void addGroupToUser(String userId, String groupId);

    void deleteGroupFromUser(String groupId, String userId);

    Iterable<Group> getGroupsForUser(String userId);
}
