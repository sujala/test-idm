package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;

import java.util.List;

public interface UserDao {

    void addUser(User user);

    UserAuthenticationResult authenticate(String userName, String password);

    UserAuthenticationResult authenticateByAPIKey(String username, String apiKey);

    void deleteUser(User user);

    void deleteUser(String username);

    Iterable<User> getUsers();

    PaginatorContext<User> getUsers(int offset, int limit);

    PaginatorContext<User> getEnabledUsers(int offset, int limit);

    PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit);

    PaginatorContext<User> getUsersByDomain(String domainId, int offset, int limit);

    Iterable<User> getUsersByUsername(String username);

    User getUserById(String id);

    Iterable<User> getUsersByDomain(String domainId);

    User getUserByUsername(String username);

    Iterable<User> getUsersByEmail(String email);

    Iterable<User> getUsers(List<String> idList);

    PaginatorContext<User> getUsersToReEncrypt(int offset, int limit);

    boolean isUsernameUnique(String username);

    /**
     * Updates the backend with the provided user object. Null values will NOT result in the attribute being removed, unless the property is annotated with @DeleteNullValues.
     *
     * @param user User instance with update changes
     */
    void updateUser(User user);

    /**
     * Updates the backend with the provided user object. Null values result in the attribute being removed.
     *
     * @param user User instance with update changes
     */
    void updateUserAsIs(User user);

    void updateUserEncryption(String userId);

    String getNextUserId();

    Iterable<User> getUsersByDomainAndEnabledFlag(String domainId, boolean enabled);

    Iterable<User> getEnabledUsersByGroupId(String groupId);

    /**
     * Retrieve disabled users associated with the specified groupid
     *
     * @param groupId
     * @return
     */
    Iterable<User> getDisabledUsersByGroupId(String groupId);

    void addGroupToUser(String userId, String groupId);

    void deleteGroupFromUser(String groupId, String userId);

    Iterable<Group> getGroupsForUser(String userId);

    void doPreEncode(User user);

    void doPostEncode(User user);

    void addGroupToUser(User baseUser, UserGroup group);

    void removeGroupFromUser(User baseUser, UserGroup group);
}
