package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LdapUserRepository extends LdapGenericRepository<User> implements UserDao {

    public static final String NULL_OR_EMPTY_USERNAME_PARAMETER = "Null or Empty username parameter";
    public static final String FOUND_USERS = "Found Users - {}";

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    EncryptionService encryptionService;

    @Autowired
    Configuration config;

    @Autowired
    GroupDao groupDao;

    @Override
    public String getBaseDn(){
        return USERS_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKSPACEPERSON;
    }

    @Override
    public String getSoftDeletedBaseDn() {
        return SOFT_DELETED_USERS_BASE_DN;
    }

    public String[] getSearchAttributes(){
        return ATTR_USER_SEARCH_ATTRIBUTES;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void doPreEncode(User user) {
        encryptionService.encryptUser(user);
    }

    @Override
    public void doPostEncode(User user) {
        encryptionService.decryptUser(user);
    }

    @Override
    public void addUser(User user) {
        addObject(user);
    }

    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        getLogger().debug("Authenticating User {}", username);
        if (StringUtils.isBlank(username)) {
            String errmsg = NULL_OR_EMPTY_USERNAME_PARAMETER;
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        User user = getUserByUsername(username);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateByPassword(user, password);
    }

    @Override
    public UserAuthenticationResult authenticateByAPIKey(String username, String apiKey) {
        getLogger().debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            String errmsg = NULL_OR_EMPTY_USERNAME_PARAMETER;
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        User user = getUserByUsername(username);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateUserByApiKey(user, apiKey);
    }

    @Override
    public void deleteUser(User user) {
        deleteObject(user);
    }

    @Override
    public void deleteUser(String username) {
        deleteObject(searchFilterGetUserByUsername(username));
    }

    @Override
    public String[] getGroupIdsForUser(String username) {
        User user = getObject(searchFilterGetUserByUsername(username));
        return user.getRsGroupId().toArray(new String[0]);
    }

    @Override
    public User getUserByCustomerIdAndUsername(String customerId, String username) {
        return getObject(searchFilterGetUserByCustomerIdAndUsername(customerId, username));
    }

    @Override
    public User getUserById(String id) {
        return getObject(searchFilterGetUserById(id));
    }

    @Override
    public Iterable<User> getUsersByDomain(String domainId) {
        return getObjects(searchFilterGetUserByDomainId(domainId));
    }

    @Override
    public User getUserByRPN(String rpn) {
        return getObject(searchFilterGetUserByRPN(rpn));
    }

    @Override
    public User getUserBySecureId(String secureId) {
        return getObject(searchFilterGetUserBySecureId(secureId));
    }

    @Override
    public User getUserByUsername(String username) {
        return getObject(searchFilterGetUserByUsername(username));
    }

    @Override
    public Iterable<User> getUsersByEmail(String email) {
        return getObjects(searchFilterGetUserByEmail(email));
    }

    @Override
    public Iterable<User> getUsers(List<String> idList) {
        return getObjects(searchFilterGetUserById(idList));
    }

    @Override
    public PaginatorContext<User> getUsersToReEncrypt(int offset, int limit) {
        String encryptionVersionId = encryptionService.getEncryptionVersionId();
        return getObjectsPaged(searchFilterGetUsersToReEncrypt(encryptionVersionId), offset, limit);
    }

    @Override
    public PaginatorContext<User> getUsers(int offset, int limit) {
        return getObjectsPaged(searchFilterGetUser(), offset, limit);
    }

    @Override
    public PaginatorContext<User> getEnabledUsers(int offset, int limit) {
        return getObjectsPaged(searchFilterGetEnabledUser(), offset, limit);
    }


    @Override
    public PaginatorContext<User> getUsersByGroupId(String groupId, int offset, int limit) {
        return getObjectsPaged(searchFiltergetUserByGroupId(groupId), offset, limit);
    }

    @Override
    public PaginatorContext<User> getUsersByDomain(String domainId, int offset, int limit) {
        return getObjectsPaged(searchFilterGetUserByDomainId(domainId), offset, limit);
    }

    @Override
    public Iterable<User> getUsersByRCN(String RCN) {
        return getObjects(searchFilterGetUserByRCN(RCN));
    }

    @Override
    public Iterable<User> getUsersByUsername(String username) {
        return getObjects(searchFilterGetUserByUsername(username));
    }

    @Override
    public boolean isUsernameUnique(String username) {
        return !getObjects(searchFilterGetUserByUsername(username)).iterator().hasNext();
    }

    @Override
    public void updateUser(User user, boolean hasSelfUpdatedPassword){
        updateObject(user);
    }

    @Override
    public void updateUserEncryption(String userId) {
        User user = getUserById(userId);

        getLogger().info("Updating user encryption to {}", user.getUsername());

        String encryptionVersionId = encryptionService.getEncryptionVersionId();
        user.setEncryptionVersion(encryptionVersionId);

        String userSalt = cryptHelper.generateSalt();
        user.setSalt(userSalt);

        encryptionService.encryptUser(user);
        updateObject(user);

        getLogger().info("Updated user encryption to {}", user.getUsername());
    }

    @Override
    public void removeUsersFromClientGroup(ClientGroup group) {
        getLogger().debug("Doing search for users that belong to group {}", group);

        for (User user : getObjects(searchFilterGetUserByGroupDn(group))) {
            user.setRsGroupDN(null);
            updateObject(user);
        }

        getLogger().info("Removed users from clientGroup {}", group);
    }

    void addAuditLogForAuthentication(User user, boolean authenticated) {

        Audit audit = Audit.authUser(user);
        if (authenticated) {
            audit.succeed();
        } else {
            String failureMessage = "User Authentication Failed: %s";

            if (getMaxLoginFailuresExceeded(user)) {
                failureMessage = String.format(failureMessage, "User locked due to max login failures limit exceded");
            } else if (user.isDisabled()) {
                failureMessage = String.format(failureMessage, "User is Disabled");
            } else {
                failureMessage = String.format(failureMessage, "Incorrect Credentials");
            }

            audit.fail(failureMessage);
        }
    }

    private boolean getMaxLoginFailuresExceeded(User user) {
        boolean passwordFailureLocked = false;
        if (user.getPasswordFailureDate() != null) {
            DateTime passwordFailureDateTime = new DateTime(user.getPasswordFailureDate())
                    .plusMinutes(this.getLdapPasswordFailureLockoutMin());
            passwordFailureLocked = passwordFailureDateTime.isAfterNow();
        }
        return passwordFailureLocked;
    }

    UserAuthenticationResult authenticateByPassword(User user, String password) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean isAuthenticated = bindUser(user, password);
        UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated);
        getLogger().debug("Authenticated User by password");

        addAuditLogForAuthentication(user, isAuthenticated);

        return authResult;
    }

    UserAuthenticationResult authenticateUserByApiKey(User user, String apiKey) {

        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean isAuthenticated = !StringUtils.isBlank(user.getApiKey())  && user.getApiKey().equals(apiKey);

        UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated);
        getLogger().debug("Authenticated User by API Key - {}", authResult);

        addAuditLogForAuthentication(user, isAuthenticated);

        return authResult;
    }

    boolean bindUser(User user, String password) {
        if (user == null || user.getUniqueId() == null) {
            throw new IllegalStateException("User cannot be null and must have a unique Id");
        }

        getLogger().debug("Authenticating user {}", user.getUsername());

        BindResult result;
        try {
            result = getBindConnPool().bind(user.getUniqueId(), password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                getLogger().info("Invalid login attempt by user {} with password {}.",user.getUsername(), password);
                return false;
            }
            getLogger().error("Bind operation on username " + user.getUsername() + " failed.", e);
            throw new IllegalStateException(e.getMessage(), e);
        }

        getLogger().debug(result.toString());
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    @Override
    public String getNextUserId() {
        return getNextId(NEXT_USER_ID);
    }

    @Override
    public void softDeleteUser(User user) {
        softDeleteObject(user);
    }

    @Override
    public User getSoftDeletedUserById(String id) {
        return getObject(searchFilterGetUserById(id), getSoftDeletedBaseDn());
    }

    @Override
    public User getSoftDeletedUserByUsername(String username) {
        return getObject(searchFilterGetUserByUsername(username), getSoftDeletedBaseDn());
    }

    @Override
    public void unSoftDeleteUser(User user) {
        unSoftDeleteObject(user);
    }

    @Override
    public Iterable<User> getUsers() {
        return getObjects(searchFilterGetUser());
    }

    @Override
    public Iterable<User> getUsersByDomainAndEnabledFlag(String domainId, boolean enabled) {
        return getObjects(searchFilterGetUserByDomainIdAndEnabled(domainId, enabled));
    }

    @Override
    public Iterable<User> getUsersByGroupId(String groupId) {
        return getObjects(searchFiltergetUserByGroupId(groupId));
    }

    @Override
    public void addGroupToUser(String userId, String groupId) {
        getLogger().debug("Adding group {} to user {}", groupId, userId);

        User user = getObject(searchFilterGetUserById(userId));
        user.getRsGroupId().add(groupId);
        updateObject(user);

        getLogger().debug("Adding groupId {} to user {}", groupId, userId);
    }

    @Override
    public void deleteGroupFromUser(String groupId, String userId) {
        getLogger().debug("Removing group {} from user {}", groupId, userId);

        User user = getObject(searchFilterGetUserById(userId));
        user.getRsGroupId().remove(groupId);
        updateObject(user);

        getLogger().debug("Removing groupId {} from user {}", groupId, userId);
    }

    @Override
    public Iterable<Group> getGroupsForUser(String userId) {
        getLogger().debug("Inside getGroupsForUser {}", userId);

        List<Group> groups = new ArrayList<Group>();

        User user = getObject(searchFilterGetUserById(userId));

        if(user != null){
            for (String groupId : user.getRsGroupId()) {
                groups.add(groupDao.getGroupById(groupId));
            }
        }
        return groups;
    }

    private Filter searchFilterGetUser() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetEnabledUser() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
                .build();
    }

    private Filter searchFilterGetUserByUsername(String username) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByCustomerIdAndUsername(String customerId, String username) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByDomainId(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByRPN(String rpn) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKSPACE_PERSON_NUMBER, rpn)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserBySecureId(String secureId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_SECURE_ID, secureId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByEmail(String email) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_MAIL, email)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByDomainIdAndEnabled(String domainId, boolean enabled) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(enabled).toUpperCase())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserByGroupDn(ClientGroup group) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_MEMBER_OF, group.getUniqueId())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUserById(List<String> idList) {
        List<Filter> idFilter = new ArrayList<Filter>();
        for(String id : idList){
            idFilter.add(Filter.createEqualityFilter("rsId", id));
        }

        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .addOrAttributes(idFilter)
                .build();
    }

    private Filter searchFilterGetUserByRCN(String rcn) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, rcn)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFiltergetUserByGroupId(String groupId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_GROUP_ID, groupId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetUsersToReEncrypt(String encryptionVersionId) {
        return Filter.createANDFilter(
                Filter.createORFilter(
                        Filter.createNOTFilter(Filter.createPresenceFilter(ATTR_ENCRYPTION_VERSION_ID)),
                        Filter.createNOTFilter(Filter.createEqualityFilter(ATTR_ENCRYPTION_VERSION_ID, encryptionVersionId))
                ),
                Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
        );
    }

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }
}
