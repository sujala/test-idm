package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@LDAPComponent
public class LdapUserRepository extends LdapGenericRepository<User> implements UserDao {

    public static final String NULL_OR_EMPTY_USERNAME_PARAMETER = "Null or Empty username parameter";
    public static final String INVALID_GROUP_DN = "Group dn could not be parsed";
    private static final List<String> AUTH_BY_PASSWORD_LIST = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD);
    private static final List<String> AUTH_BY_API_KEY_LIST = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY);

    @Autowired
    private CryptHelper cryptHelper;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private Configuration config;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public String getBaseDn(){
        return USERS_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKSPACEPERSON;
    }

    public String[] getSearchAttributes(){
        return ATTR_USER_SEARCH_ATTRIBUTES_NO_PWD_HIS;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void doPreEncode(User user) {
        if(user.getRsGroupId() != null && user.getRsGroupId().isEmpty()){
            user.setRsGroupId(null);
        }
        encryptionService.encryptUser(user);

        if (!StringUtils.isEmpty(user.getPassword())) {
            // We're setting a new password so update password change date
            user.setPasswordLastUpdated(new Date());

            /*
            Store the password in the history for the user if enabled. This is done
            regardless of whether the individual user has a password policy in effect.
             */
            if (identityConfig.getReloadableConfig().maintainPasswordHistory()) {
                // Add the password to the history list and limit to 10
                String hashedPwd = cryptHelper.createLegacySHA(user.getPassword());
                List<String> history = user.getPasswordHistory();
                if (history == null) {
                    history = new ArrayList();
                    user.setPasswordHistory(history);
                }
                history.add(hashedPwd);

                int maxhistory = identityConfig.getReloadableConfig().getPasswordHistoryMax() + 1;
                if (history.size() > maxhistory) {
                    // Create a whole new list, keeping the last maxhistory entries
                    history = new ArrayList(history.subList(history.size() - maxhistory, history.size()));
                    user.setPasswordHistory(history);
                }
            }
        }
    }

    @Override
    public void doPostEncode(User user) {
        encryptionService.decryptUser(user);
    }

    @Override
    public void addGroupToUser(User baseUser, UserGroup group) {
        DN groupDN = getGroupDn(group);
        if (groupDN != null && !baseUser.getUserGroupDNs().contains(groupDN)) {
            baseUser.getUserGroupDNs().add(groupDN);
            updateUser(baseUser);
        }
    }

    @Override
    public void removeGroupFromUser(User baseUser, UserGroup group) {
        DN groupDN = getGroupDn(group);
        if (groupDN != null && baseUser.getUserGroupDNs().contains(groupDN)) {
            baseUser.getUserGroupDNs().remove(groupDN);
            updateUser(baseUser);
        }
    }

    private DN getGroupDn(UserGroup group) {
        DN groupDN = null;
        try {
            groupDN = new DN(group.getUniqueId());
        } catch (LDAPException e) {
            String errmsg = INVALID_GROUP_DN;
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        return groupDN;
    }

    @Override
    public String getNextId() {
        return getUuid();
    }

    @Override
    public void addUser(User user) {
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextUserId());
        }
        encryptionService.setUserEncryptionSaltAndVersion(user);
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
    public User getUserById(String id) {
        return getObject(searchFilterGetUserById(id));
    }

    @Override
    public Iterable<User> getUsersByDomain(String domainId) {
        return getObjects(searchFilterGetUserByDomainId(domainId));
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
    public PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit) {
        return getObjectsPaged(searchFilterGetEnabledUsersByGroupId(groupId), offset, limit);
    }

    @Override
    public PaginatorContext<User> getUsersByDomain(String domainId, int offset, int limit) {
        return getObjectsPaged(searchFilterGetUserByDomainId(domainId), offset, limit);
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
    public void updateUser(User user){
        updateObject(user);
    }

    @Override
    public void updateUserAsIs(User user){
        updateObjectAsIs(user);
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
        UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated, AUTH_BY_PASSWORD_LIST);
        getLogger().debug("Authenticated User by password");

        addAuditLogForAuthentication(user, isAuthenticated);

        return authResult;
    }

    UserAuthenticationResult authenticateUserByApiKey(User user, String apiKey) {

        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean isAuthenticated = !StringUtils.isBlank(user.getApiKey())  && user.getApiKey().equals(apiKey);

        UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated, AUTH_BY_API_KEY_LIST);
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
                getLogger().info("Invalid login attempt by user {} with password ****.", user.getUsername());
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
        return getUuid();
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
    public Iterable<User> getEnabledUsersByGroupId(String groupId) {
        return getObjects(searchFilterGetEnabledUsersByGroupId(groupId));
    }

    @Override
    public Iterable<User> getDisabledUsersByGroupId(String groupId) {
        return getObjects(searchFilterGetDisabledUsersByGroupId(groupId));
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

    private Filter searchFilterGetEnabledUsersByGroupId(String groupId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_GROUP_ID, groupId)
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();
    }

    private Filter searchFilterGetDisabledUsersByGroupId(String groupId) {
        return Filter.createANDFilter(
                Filter.createORFilter(
                        Filter.createNOTFilter(Filter.createPresenceFilter(ATTR_ENABLED)),
                        Filter.createEqualityFilter(ATTR_ENABLED, Boolean.FALSE.toString())
                ),
                Filter.createEqualityFilter(ATTR_GROUP_ID, groupId),
                Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
        );
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
