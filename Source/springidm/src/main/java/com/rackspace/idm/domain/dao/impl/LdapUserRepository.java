package com.rackspace.idm.domain.dao.impl;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import com.rackspace.idm.exception.StalePasswordException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;

public class LdapUserRepository extends LdapRepository implements UserDao {

    // NOTE: This is pretty fragile way of handling the specific error, so we
    // need to look into more reliable way of detecting this error.
    private static final String STALE_PASSWORD_MESSAGE = "Password match in history";

    public LdapUserRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public void addRacker(Racker racker) {
        getLogger().debug("Adding racker - {}", racker);
        if (racker == null) {
            String errmsg = "Null instance of Racer was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER,
                getRackspaceCustomerId())
            .addEqualAttribute(ATTR_OBJECT_CLASS,
                OBJECTCLASS_RACKSPACEORGANIZATION).build();

        SearchResultEntry rackspace = this.getSingleEntry(BASE_DN,
            SearchScope.ONE, searchFilter, ATTR_NO_ATTRIBUTES);

        String userDN = new LdapDnBuilder(rackspace.getDN())
            .addAttribute(ATTR_RACKER_ID, racker.getRackerId())
            .addAttribute(ATTR_OU, OU_PEOPLE_NAME).build();

        racker.setUniqueId(userDN);

        Audit audit = Audit.log(racker).add();

        Attribute[] attributes = getRackerAddAtrributes(racker);

        addEntry(userDN, attributes, audit);

        audit.succeed();

        getLogger().debug("Added racker {}", racker);
    }

    @Override
    public void addUser(User user, String customerUniqueId) {
        getLogger().debug("Adding user - {}", user);
        if (user == null) {
            String errmsg = "Null instance of User was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        String userDN = new LdapDnBuilder(customerUniqueId)
            .addAttribute(ATTR_INUM, user.getInum())
            .addAttribute(ATTR_OU, OU_PEOPLE_NAME).build();

        user.setUniqueId(userDN);

        Audit audit = Audit.log(user).add();

        Attribute[] attributes = null;

        try {
            attributes = getAddAttributes(user);
            addEntry(userDN, attributes, audit);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        }

        // Now that it's in LDAP we'll set the password to the "existing" type
        user.setPasswordObj(user.getPasswordObj().toExisting());

        audit.succeed();

        getLogger().debug("Added user {}", user);
    }

    @Override
    public UserAuthenticationResult authenticate(String username,
        String password) {
        getLogger().debug("Authenticating User {}", username);
        if (StringUtils.isBlank(username)) {
            String errmsg = "Null or Empty username parameter";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        User user = getUserByUsername(username);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateByPassword(user, password);
    }

    @Override
    public UserAuthenticationResult authenticateByAPIKey(String username,
        String apiKey) {
        getLogger().debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            String errmsg = "Null or Empty username parameter";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        User user = getUserByUsername(username);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateUserByApiKey(user, apiKey);
    }

    @Override
    public UserAuthenticationResult authenticateByMossoIdAndAPIKey(int mossoId,
        String apiKey) {
        getLogger().info("Authenticating User with MossoId {}", mossoId);

        User user = getUserByMossoId(mossoId);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateUserByApiKey(user, apiKey);
    }

    @Override
    public UserAuthenticationResult authenticateByNastIdAndAPIKey(
        String nastId, String apiKey) {
        getLogger().debug("Authenticating User with NastId {}", nastId);
        if (StringUtils.isBlank(nastId)) {
            String errmsg = "Null or Empty NastId parameter";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        User user = getUserByNastId(nastId);
        getLogger().debug("Found user {}, authenticating...", user);
        return authenticateUserByApiKey(user, apiKey);
    }

    @Override
    public void deleteRacker(String rackerId) {
        getLogger().info("Deleting racker - {}", rackerId);
        if (StringUtils.isBlank(rackerId)) {
            getLogger().error("Null or Empty rackerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty rackerId parameter.");
        }

        Audit audit = Audit.log(rackerId).delete();

        Racker racker = this.getRackerByRackerId(rackerId);
        if (racker == null) {
            String errorMsg = String.format("Racker %s not found", rackerId);
            audit.fail(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        getLogger().debug("Found Racker {}, deleting...", racker);

        this.deleteEntryAndSubtree(racker.getUniqueId(), audit);
        audit.succeed();
        getLogger().info("Deleted racker - {}", rackerId);
    }

    @Override
    public void deleteUser(String username) {
        getLogger().info("Deleting username - {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        Audit audit = Audit.log(username).delete();

        User user = this.getUserByUsername(username);
        if (user == null) {
            String errorMsg = String.format("User %s not found", username);
            audit.fail(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        getLogger().debug("Found user {}, deleting... {}", user);

        this.deleteEntryAndSubtree(user.getUniqueId(), audit);

        audit.succeed();
        getLogger().info("Deleted username - {}", username);
    }

    @Override
    public Users getAllUsers(int offset, int limit) {
        getLogger().debug("Search all users");

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();
        Users users = getMultipleUsers(searchFilter,
            ATTR_USER_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;

    }

    @Override
    public String[] getGroupIdsForUser(String username) {
        getLogger().debug("Getting GroupIds for User {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        String[] groupIds = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter, ATTR_MEMBER_OF);

        if (entry != null) {
            groupIds = entry.getAttributeValues(ATTR_MEMBER_OF);
        }

        getLogger().debug("Got GroupIds for User {} - {}", username, groupIds);

        return groupIds;
    }

    @Override
    public Racker getRackerByRackerId(String rackerId) {
        getLogger().debug("Getting Racker {}", rackerId);
        if (StringUtils.isBlank(rackerId)) {
            getLogger().error("Null or Empty rackerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty rackerId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKER_ID, rackerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKER).build();

        Racker racker = null;

        SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB,
            searchFilter);
        if (entry != null) {
            racker = new Racker();
            racker.setUniqueId(entry.getDN());
            racker.setRackerId(entry.getAttributeValue(ATTR_RACKER_ID));
        }

        getLogger().debug("Got Racker {}", racker);
        return racker;
    }

    @Override
    public String getUnusedUserInum(String customerInum) {
        // TODO: We might may this call to the XDI server in the future.
        if (StringUtils.isBlank(customerInum)) {
            getLogger().error("Null or empty customerInum value passesed in.");
            throw new IllegalArgumentException(
                "Null or empty customerInum value passesed in.");
        }

        User user = null;
        String inum = "";
        do {
            inum = customerInum + InumHelper.getRandomInum(1);
            user = getUserByInum(inum);
        } while (user != null);

        getLogger().debug("Generated a new inum {}", inum);

        return inum;
    }

    @Override
    public User getUserByCustomerIdAndUsername(String customerId,
        String username) {

        getLogger().debug(
            "LdapUserRepository.findUser() - customerId: {}, username: {} ",
            customerId, username);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User for customer - {}, {}", customerId, user);

        return user;
    }

    @Override
    public User getUserByInum(String inum) {
        // NOTE: This method returns a user regardless of whether the
        // softDeleted flag is set or not because this method is only
        // used internally.
        getLogger().debug("Doing search for inum " + inum);
        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty inum parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_INUM, inum)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    @Override
    public User getUserByMossoId(int mossoId) {
        getLogger().debug("Doing search for nastId " + mossoId);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_MOSSO_ID, String.valueOf(mossoId))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    @Override
    public User getUserByNastId(String nastId) {
        getLogger().debug("Doing search for nastId " + nastId);
        if (StringUtils.isBlank(nastId)) {
            getLogger().error("Null or Empty nastId parameter");
            throw new IllegalArgumentException(
                "Null or Empty nastId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAST_ID, nastId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    @Override
    public User getUserByRPN(String rpn) {
        getLogger().debug("Doing search for rpn " + rpn);
        if (StringUtils.isBlank(rpn)) {
            getLogger().error("Null or Empty rpn parameter");
            throw new IllegalArgumentException("Null or Empty rpn parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_PERSON_NUMBER, rpn)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        // This method returns a user whether or not the user has been
        // soft-deleted
        getLogger().debug("Doing search for username " + username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    @Override
    public Users getUsersByCustomerId(String customerId, int offset, int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        Users users = getMultipleUsers(searchFilter,
            ATTR_USER_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;
    }

    @Override
    public boolean isUsernameUnique(String username) {

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        return user == null;
    }

    @Override
    public void setUsersLockedFlagByCustomerId(String customerId,
        boolean isLocked) {
        Users users = this.findFirst100ByCustomerIdAndLock(customerId,
            !isLocked);
        if (users.getUsers() != null && users.getUsers().size() > 0) {
            for (User user : users.getUsers()) {
                user.setLocked(isLocked);
                this.updateUser(user, false);
            }
        }
        if (users.getTotalRecords() > 0) {
            this.setUsersLockedFlagByCustomerId(customerId, isLocked);
        }
    }

    @Override
    public void updateUser(User user, boolean hasSelfUpdatedPassword) {
        getLogger().info("Updating user {}", user);

        throwIfEmptyUsername(user);

        User oldUser = getUserByUsername(user.getUsername());

        throwIfEmptyOldUser(oldUser, user);

        Audit audit = Audit.log(user);

        try {
            List<Modification> mods = getModifications(oldUser, user,
                hasSelfUpdatedPassword);
            audit.modify(mods);

            if (mods.size() < 1) {
                // No changes!
                return;
            }
            getAppConnPool().modify(oldUser.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            throwIfStalePassword(ldapEx, audit);

            getLogger().error("Error updating user {} - {}",
                user.getUsername(), ldapEx);
            audit.fail("Error updating user");
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        }

        // Now that its in LDAP we'll set the password to existing type
        user.setPasswordObj(user.getPasswordObj().toExisting());

        audit.succeed();
        getLogger().info("Updated user - {}", user);
    }

    @Override
    public void removeUsersFromClientGroup(ClientGroup group) {

        getLogger().debug("Doing search for users that belong to group {}",
            group);

        if (group == null) {
            String errMsg = "Null group passed in";
            getLogger().error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_MEMBER_OF, group.getUniqueId())
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        List<User> userList = new ArrayList<User>();
        SearchResult searchResult = null;
        try {
            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                searchFilter);
            searchResult = getAppConnPool().search(request);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                userList.add(getUser(entry));
            }
        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found Users - {}", userList);

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.DELETE, ATTR_MEMBER_OF,
            group.getUniqueId()));

        Audit audit = null;
        for (User user : userList) {
            audit = Audit.log(user).modify(mods);
            try {
                getAppConnPool().modify(user.getUniqueId(), mods);
            } catch (LDAPException ldapEx) {
                audit.fail(ldapEx.getMessage());
                throw new IllegalStateException(ldapEx);
            }
            audit.succeed();
        }

        getLogger().info("Removed users from clientGroup {}", group);
    }

    private void throwIfEmptyOldUser(User oldUser, User user)
        throws IllegalArgumentException {
        if (oldUser == null) {
            getLogger()
                .error("No record found for user {}", user.getUsername());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given User instance. Has the userName been changed?");
        }
    }

    private void throwIfEmptyUsername(User user)
        throws IllegalArgumentException {
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            getLogger().error(
                "User instance is null or its userName has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The User instance either null or its userName has no value.");
        }
    }

    private void throwIfStalePassword(LDAPException ldapEx, Audit audit)
        throws StalePasswordException {
        if (ResultCode.CONSTRAINT_VIOLATION.equals(ldapEx.getResultCode())
            && STALE_PASSWORD_MESSAGE.equals(ldapEx.getMessage())) {
            audit.fail(STALE_PASSWORD_MESSAGE);
            throw new StalePasswordException(
                "Past 10 passwords for the user cannot be re-used.");
        }
    }

    private void addAuditLogForAuthentication(User user, boolean authenticated) {

        Audit audit = Audit.authUser(user);
        if (authenticated) {
            audit.succeed();
        } else {
            String failureMessage = "User Authentication Failed: %s";

            if (user.isMaxLoginFailuresExceded()) {
                failureMessage = String.format(failureMessage,
                    "User locked due to max login failures limit exceded");
            } else if (user.isDisabled()) {
                failureMessage = String.format(failureMessage,
                    "User is Disabled");
            } else {
                failureMessage = String.format(failureMessage,
                    "Incorrect Credentials");
            }

            audit.fail(failureMessage);
        }
    }

    private UserAuthenticationResult authenticateByPassword(User user,
        String password) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = bindUser(user, password);
        UserAuthenticationResult authResult = validateUserStatus(user,
            authenticated);
        getLogger().debug("Authenticated User by password");

        addAuditLogForAuthentication(user, authenticated);

        return authResult;
    }

    private UserAuthenticationResult authenticateUserByApiKey(User user,
        String apiKey) {

        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = !StringUtils.isBlank(user.getApiKey())
            && user.getApiKey().equals(apiKey);

        UserAuthenticationResult authResult = validateUserStatus(user,
            authenticated);
        getLogger().debug("Authenticated User by API Key - {}", authResult);

        addAuditLogForAuthentication(user, authenticated);

        return authResult;
    }

    private boolean bindUser(User user, String password) {
        getLogger().debug("Authenticating user {}", user.getUsername());

        if (user == null || user.getUniqueId() == null) {
            throw new IllegalStateException(
                "User cannot be null and must have a unique Id");
        }

        BindResult result;
        try {
            result = getBindConnPool().bind(user.getUniqueId(), password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                getLogger().info(
                    "Invalid login attempt by user {} with password {}.",
                    user.getUsername(), password);
                return false;
            }
            getLogger()
                .error(
                    "Bind operation on username " + user.getUsername()
                        + " failed.", e);
            throw new IllegalStateException(e);
        }

        getLogger().debug(result.toString());
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    private Users findFirst100ByCustomerIdAndLock(String customerId,
        boolean isLocked) {
        getLogger().debug("Doing search for customerId {}", customerId);

        int limit = 100;
        int offset = 0;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_LOCKED, String.valueOf(isLocked))
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        Users users = getMultipleUsers(searchFilter,
            ATTR_USER_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;
    }

    private Attribute[] getAddAttributes(User user)
        throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_USER_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(user.getCountry())) {
            atts.add(new Attribute(ATTR_C, user.getCountry()));
        }

        if (!StringUtils.isBlank(user.getDisplayName())) {
            atts.add(new Attribute(ATTR_DISPLAY_NAME, cryptHelper.encrypt(user
                .getDisplayName())));
        }

        if (!StringUtils.isBlank(user.getFirstname())) {
            atts.add(new Attribute(ATTR_GIVEN_NAME, cryptHelper.encrypt(user
                .getFirstname())));
        }

        if (!StringUtils.isBlank(user.getIname())) {
            atts.add(new Attribute(ATTR_INAME, user.getIname()));
        }

        if (!StringUtils.isBlank(user.getInum())) {
            atts.add(new Attribute(ATTR_INUM, user.getInum()));
        }

        if (!StringUtils.isBlank(user.getEmail())) {
            atts.add(new Attribute(ATTR_MAIL, cryptHelper.encrypt(user
                .getEmail())));
        }

        if (!StringUtils.isBlank(user.getMiddlename())) {
            atts.add(new Attribute(ATTR_MIDDLE_NAME, user.getMiddlename()));
        }

        if (!StringUtils.isBlank(user.getOrgInum())) {
            atts.add(new Attribute(ATTR_O, user.getOrgInum()));
        }

        if (user.getLocale() != null) {
            atts.add(new Attribute(ATTR_LANG, user.getPreferredLang()
                .toString()));
        }

        if (!StringUtils.isBlank(user.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, user
                .getCustomerId()));
        }

        if (!StringUtils.isBlank(user.getPersonId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_PERSON_NUMBER, user
                .getPersonId()));
        }

        if (!StringUtils.isBlank(user.getApiKey())) {
            atts.add(new Attribute(ATTR_RACKSPACE_API_KEY, cryptHelper
                .encrypt(user.getApiKey())));
        }

        if (!StringUtils.isBlank(user.getSecretAnswer())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_A, cryptHelper
                .encrypt(user.getSecretAnswer())));
        }

        if (!StringUtils.isBlank(user.getSecretQuestion())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_Q, cryptHelper
                .encrypt(user.getSecretQuestion())));
        }

        if (!StringUtils.isBlank(user.getLastname())) {
            atts.add(new Attribute(ATTR_SN, cryptHelper.encrypt(user
                .getLastname())));
        }

        if (user.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, user.getStatus().toString()));
        }

        if (user.getTimeZoneObj() != null) {
            atts.add(new Attribute(ATTR_TIME_ZONE, user.getTimeZone()));
        }

        atts.add(new Attribute(ATTR_UID, user.getUsername()));

        if (!StringUtils.isBlank(user.getPasswordObj().getValue())) {
            atts.add(new Attribute(ATTR_PASSWORD, user.getPasswordObj()
                .getValue()));
            atts.add(new Attribute(ATTR_CLEAR_PASSWORD, cryptHelper
                .encrypt(user.getPassword())));
            atts.add(new Attribute(ATTR_PASSWORD_SELF_UPDATED, Boolean.FALSE
                .toString()));
            atts.add(new Attribute(ATTR_PASSWORD_UPDATED_TIMESTAMP, StaticUtils
                .encodeGeneralizedTime(new DateTime().toDate())));
        }

        if (!StringUtils.isBlank(user.getRegion())) {
            atts.add(new Attribute(ATTR_RACKSPACE_REGION, user.getRegion()));
        }

        if (user.isLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(user.isLocked())));
        }

        if (user.isSoftDeleted() != null) {
            atts.add(new Attribute(ATTR_SOFT_DELETED, String.valueOf(user
                .isSoftDeleted())));
        }

        if (!StringUtils.isBlank(user.getNastId())) {
            atts.add(new Attribute(ATTR_NAST_ID, user.getNastId()));
        }

        if (user.getMossoId() != null && user.getMossoId().intValue() > 0) {
            atts.add(new Attribute(ATTR_MOSSO_ID, user.getMossoId().toString()));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);

        return attributes;
    }

    private Users getMultipleUsers(Filter searchFilter,
        String[] searchAttributes, int offset, int limit) {

        offset = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        limit = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limit = limit > this.getLdapPagingLimitMax() ? this
            .getLdapPagingLimitMax() : limit;

        int contentCount = 0;

        List<User> userList = new ArrayList<User>();

        try {

            List<SearchResultEntry> entries = this.getMultipleEntries(BASE_DN,
                SearchScope.SUB, searchFilter, ATTR_UID, searchAttributes);

            contentCount = entries.size();

            if (offset < contentCount) {

                int toIndex = offset + limit > contentCount ? contentCount
                    : offset + limit;
                int fromIndex = offset;

                List<SearchResultEntry> subList = entries.subList(fromIndex,
                    toIndex);

                for (SearchResultEntry entry : subList) {
                    userList.add(getUser(entry));
                }
            }

        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found users {}", userList);

        Users users = new Users();

        users.setLimit(limit);
        users.setOffset(offset);
        users.setTotalRecords(contentCount);
        users.setUsers(userList);

        return users;
    }

    private Attribute[] getRackerAddAtrributes(Racker racker) {

        List<Attribute> atts = new ArrayList<Attribute>();
        atts.add(new Attribute(ATTR_OBJECT_CLASS,
            ATTR_RACKER_OBJECT_CLASS_VALUES));
        atts.add(new Attribute(ATTR_RACKER_ID, racker.getRackerId()));
        return atts.toArray(new Attribute[0]);
    }

    private User getSingleUser(Filter searchFilter, String[] searchAttributes) {
        User user = null;
        try {

            SearchResultEntry entry = this.getSingleEntry(BASE_DN,
                SearchScope.SUB, searchFilter, searchAttributes);

            if (entry != null) {
                user = getUser(entry);
            }

        } catch (GeneralSecurityException e) {
            getLogger().error("Encryption error", e);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }

    private User getUser(SearchResultEntry resultEntry)
        throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();
        User user = new User();
        user.setUniqueId(resultEntry.getDN());
        user.setUsername(resultEntry.getAttributeValue(ATTR_UID));
        user.setCountry(resultEntry.getAttributeValue(ATTR_C));
        user.setDisplayName(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_DISPLAY_NAME)));
        user.setFirstname(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_GIVEN_NAME)));
        user.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        user.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        user.setEmail(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_MAIL)));
        user.setMiddlename(resultEntry.getAttributeValue(ATTR_MIDDLE_NAME));
        user.setOrgInum(resultEntry.getAttributeValue(ATTR_O));
        user.setPreferredLang(resultEntry.getAttributeValue(ATTR_LANG));
        user.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        user.setPersonId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_PERSON_NUMBER));
        user.setApiKey(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_RACKSPACE_API_KEY)));
        user.setSecretQuestion(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_PASSWORD_SECRET_Q)));
        user.setSecretAnswer(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_PASSWORD_SECRET_A)));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            user.setStatus(Enum.valueOf(UserStatus.class,
                statusStr.toUpperCase()));
        }

        user.setLastname(cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_SN)));
        user.setTimeZoneObj(DateTimeZone.forID(resultEntry
            .getAttributeValue(ATTR_TIME_ZONE)));

        String ecryptedPwd = cryptHelper.decrypt(resultEntry
            .getAttributeValueBytes(ATTR_CLEAR_PASSWORD));
        Date lastUpdates = resultEntry
            .getAttributeValueAsDate(ATTR_PASSWORD_UPDATED_TIMESTAMP);
        boolean wasSelfUpdated = resultEntry
            .getAttributeValueAsBoolean(ATTR_PASSWORD_SELF_UPDATED);
        DateTime lasteUpdated = new DateTime(lastUpdates);
        Password pwd = Password.existingInstance(ecryptedPwd, lasteUpdated,
            wasSelfUpdated);
        user.setPasswordObj(pwd);

        user.setRegion(resultEntry.getAttributeValue(ATTR_RACKSPACE_REGION));

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            user.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String softDeletedTimestamp = resultEntry
            .getAttributeValue(ATTR_SOFT_DELETED_DATE);
        if (softDeletedTimestamp != null) {
            user.setSoftDeletedTimestamp(new DateTime(resultEntry
                .getAttributeValueAsDate(ATTR_SOFT_DELETED_DATE)));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            user.setLocked(resultEntry.getAttributeValueAsBoolean(ATTR_LOCKED));
        }

        user.setMossoId(resultEntry.getAttributeValueAsInteger(ATTR_MOSSO_ID));
        user.setNastId(resultEntry.getAttributeValue(ATTR_NAST_ID));

        Date created = resultEntry.getAttributeValueAsDate(ATTR_CREATED_DATE);
        if (created != null) {
            DateTime createdDate = new DateTime(created);
            user.setCreated(createdDate);
        }

        Date updated = resultEntry.getAttributeValueAsDate(ATTR_UPDATED_DATE);
        if (updated != null) {
            DateTime updatedDate = new DateTime(updated);
            user.setUpdated(updatedDate);
        }

        Date passwordFailureDate = resultEntry
            .getAttributeValueAsDate(ATTR_PWD_ACCOUNT_LOCKOUT_TIME);

        boolean passwordFailureLocked = false;
        if (passwordFailureDate != null) {
            DateTime passwordFailureDateTime = new DateTime(passwordFailureDate)
                .plusMinutes(this.getLdapPasswordFailureLockoutMin());
            passwordFailureLocked = passwordFailureDateTime.isAfterNow();
        }
        user.setMaxLoginFailuresExceded(passwordFailureLocked);

        return user;
    }

    private UserAuthenticationResult validateUserStatus(User user,
        boolean isAuthenticated) {
        if (isAuthenticated && user.isDisabled()) {
            String errMsg = String.format("User %s is disabled.",
                user.getUsername());
            getLogger().error(errMsg);
            throw new UserDisabledException(errMsg);
        }

        return new UserAuthenticationResult(user, isAuthenticated);
    }

    List<Modification> getModifications(User uOld, User uNew,
        boolean isSelfUpdate) throws GeneralSecurityException,
        InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();
        List<Modification> mods = new ArrayList<Modification>();

        DateTime currentTime = new DateTime();
        if (uNew.getPasswordObj().isNew()) {
            if (isSelfUpdate) {
                Password oldPwd = uOld.getPasswordObj();
                int secsSinceLastChange = Seconds.secondsBetween(
                    oldPwd.getLastUpdated(), currentTime).getSeconds();
                if (oldPwd.wasSelfUpdated()
                    && secsSinceLastChange < DateTimeConstants.SECONDS_PER_DAY) {
                    throw new PasswordSelfUpdateTooSoonException();
                }
            }

            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_SELF_UPDATED, Boolean.toString(isSelfUpdate)));
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_PASSWORD_UPDATED_TIMESTAMP, StaticUtils
                    .encodeGeneralizedTime(currentTime.toDate())));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD,
                uNew.getPasswordObj().getValue()));
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(uNew.getPasswordObj()
                    .getValue())));
        }

        if (uNew.getCountry() != null) {
            if (StringUtils.isBlank(uNew.getCountry())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_C));
            } else if (!StringUtils
                .equals(uOld.getCountry(), uNew.getCountry())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_C,
                    uNew.getCountry()));
            }
        }

        if (uNew.getDisplayName() != null) {
            if (StringUtils.isBlank(uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_DISPLAY_NAME));
            } else if (!StringUtils.equals(uOld.getDisplayName(),
                uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_DISPLAY_NAME, cryptHelper.encrypt(uNew
                        .getDisplayName())));
            }
        }

        if (uNew.getFirstname() != null) {
            if (StringUtils.isBlank(uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_GIVEN_NAME));
            } else if (!StringUtils.equals(uOld.getFirstname(),
                uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_GIVEN_NAME, cryptHelper.encrypt(uNew.getFirstname())));
            }
        }

        if (uNew.getIname() != null) {
            if (StringUtils.isBlank(uNew.getIname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_INAME));
            } else if (!StringUtils.equals(uOld.getIname(), uNew.getIname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_INAME,
                    uNew.getIname()));
            }
        }

        if (uNew.getEmail() != null) {
            if (StringUtils.isBlank(uNew.getEmail())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MAIL));
            } else if (!StringUtils.equals(uOld.getEmail(), uNew.getEmail())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MAIL,
                    cryptHelper.encrypt(uNew.getEmail())));
            }
        }

        if (uNew.getMiddlename() != null) {
            if (StringUtils.isBlank(uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_MIDDLE_NAME));
            } else if (!StringUtils.equals(uOld.getMiddlename(),
                uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_MIDDLE_NAME, uNew.getMiddlename()));
            }
        }

        if (uNew.getApiKey() != null && !StringUtils.isEmpty(uNew.getApiKey())) {
            if (StringUtils.isBlank(uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_RACKSPACE_API_KEY));
            } else if (!StringUtils.equals(uOld.getApiKey(), uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_RACKSPACE_API_KEY, cryptHelper.encrypt(uNew
                        .getApiKey())));
            }
        }

        if (uNew.getSecretAnswer() != null) {
            if (StringUtils.isBlank(uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PASSWORD_SECRET_A));
            } else if (!StringUtils.equals(uOld.getSecretAnswer(),
                uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_PASSWORD_SECRET_A, cryptHelper.encrypt(uNew
                        .getSecretAnswer())));
            }
        }

        if (uNew.getSecretQuestion() != null) {
            if (StringUtils.isBlank(uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PASSWORD_SECRET_Q));
            } else if (!StringUtils.equals(uOld.getSecretQuestion(),
                uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_PASSWORD_SECRET_Q, cryptHelper.encrypt(uNew
                        .getSecretQuestion())));
            }
        }

        if (uNew.getLastname() != null) {
            if (StringUtils.isBlank(uNew.getLastname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SN));
            } else if (!StringUtils.equals(uOld.getLastname(),
                uNew.getLastname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_SN,
                    cryptHelper.encrypt(uNew.getLastname())));
            }
        }

        if (uNew.getRegion() != null) {
            if (StringUtils.isBlank(uNew.getRegion())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_RACKSPACE_REGION));
            } else if (!uNew.getRegion().equals(uOld.getRegion())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_RACKSPACE_REGION, uNew.getRegion()));
            }
        }

        if (uNew.getPersonId() != null) {
            if (StringUtils.isBlank(uNew.getPersonId())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_RACKSPACE_PERSON_NUMBER));
            } else if (!uNew.getPersonId().equals(uOld.getPersonId())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_RACKSPACE_PERSON_NUMBER, uNew.getPersonId()));
            }
        }

        if (uNew.getLocale() != null
            && !uNew.getPreferredLang().equals(uOld.getPreferredLang())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LANG, uNew
                .getPreferredLang().toString()));
        }

        if (uNew.getStatus() != null
            && !uOld.getStatus().equals(uNew.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS,
                uNew.getStatus().toString()));
        }

        if (uNew.getTimeZoneObj() != null
            && !uNew.getTimeZone().equals(uOld.getTimeZone())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_TIME_ZONE,
                uNew.getTimeZone()));
        }

        if (uNew.isLocked() != null && uNew.isLocked() != uOld.isLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(uNew.isLocked())));
        }

        if (uNew.isSoftDeleted() != null
            && uNew.isSoftDeleted() != uOld.isSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_SOFT_DELETED, String.valueOf(uNew.isSoftDeleted())));

            if (uNew.isSoftDeleted()) {
                mods.add(new Modification(ModificationType.ADD,
                    ATTR_SOFT_DELETED_DATE, String.valueOf(currentTime)));
            } else {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_SOFT_DELETED_DATE));
            }
        }

        if (uNew.getNastId() != null) {
            if (StringUtils.isBlank(uNew.getNastId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_NAST_ID));
            } else if (!uNew.getNastId().equals(uOld.getNastId())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_NAST_ID, uNew.getNastId()));
            }
        }

        // To delete the attribute MossoId a negative value for the mossoId
        // is sent in.
        if (uNew.getMossoId() != null) {
            if (uNew.getMossoId() < 0) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_MOSSO_ID));
            } else if (!uNew.getMossoId().equals(uOld.getMossoId())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_MOSSO_ID, String.valueOf(uNew.getMossoId())));
            }
        }

        return mods;
    }
}
