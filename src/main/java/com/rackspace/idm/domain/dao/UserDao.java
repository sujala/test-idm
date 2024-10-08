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

    /**
     * Retrieve user-admin set on domain. Return null if domain's userAdminDN is not set, or the user is not found.
     *
     * @param domain
     * @throws IllegalArgumentException If supplied domain is null.
     *
     * @return
     */
    User getUserAdminByDomain(Domain domain);

    Iterable<User> getUsersByEmail(String email, User.UserType userType);

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

    /**
     * Retrieve enabled users associated with the specified contactId.
     *
     * @param contactId
     * @return
     */
    Iterable<User> getEnabledUsersByContactId(String contactId);

    void addGroupToUser(String userId, String groupId);

    void deleteGroupFromUser(String groupId, String userId);

    Iterable<Group> getGroupsForUser(String userId);

    void doPreEncode(User user);

    void doPostEncode(User user);

    void addUserGroupToUser(UserGroup group, User baseUser);

    void removeUserGroupFromUser(UserGroup group, User baseUser);
}
