package com.rackspace.idm.domain.dao.impl;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.PasswordValidationException;
import com.rackspace.idm.exception.StalePasswordException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;

public class LdapUserRepository extends LdapRepository implements UserDao {
    private static final String[] ATTR_SEARCH_ATTRIBUTES = {"*", ATTR_CREATED_DATE, ATTR_UPDATED_DATE,
        ATTR_PWD_ACCOUNT_LOCKOUT_TIME};

    // NOTE: This is pretty fragile way of handling the specific error, so we
    // need to look into more reliable way of detecting this error.
    private static final String STALE_PASSWORD_MESSAGE = "The provided new password was found in the password history for the user";

    public LdapUserRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
    }

    public void add(User user, String customerUniqueId) {
        getLogger().debug("Adding user - {}", user);
        if (user == null) {
            getLogger().error("Null instance of User was passed");
            throw new IllegalArgumentException("Null instance of User was passed");
        }

        String userDN = new LdapDnBuilder(customerUniqueId).addAttriubte(ATTR_INUM, user.getInum())
            .addAttriubte(ATTR_OU, OU_PEOPLE_NAME).build();

        user.setUniqueId(userDN);

        LDAPResult result;
        Audit audit = Audit.log(user).add();
        try {
            Attribute[] attributes = getAddAttributes(user);
            result = getAppConnPool().add(userDN, attributes);
        } catch (LDAPException ldapEx) {
            String errorString = String.format("Error adding user %s - %s", user, ldapEx);
            audit.fail(errorString);
            getLogger().error(errorString);
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            String errorString = String.format("Error adding user %s - %s", user.getUsername(),
                result.getResultCode());
            audit.fail(errorString);
            getLogger().error(errorString);
            throw new IllegalStateException(String.format("LDAP error encountered when adding user: %s - %s",
                user.getUsername(), result.getResultCode().toString()));
        }

        // Now that it's in LDAP we'll set the password to the "existing" type
        user.setPasswordObj(user.getPasswordObj().toExisting());

        audit.succeed();

        getLogger().debug("Added user {}", user);
    }

    public UserAuthenticationResult authenticate(String username, String password) {
        getLogger().debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        User user = findByUsername(username);
        return authenticateByPassword(user, password);
    }

    public UserAuthenticationResult authenticateByAPIKey(String username, String apiKey) {
        getLogger().debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        User user = findByUsername(username);

        return authenticateUserByApiKey(user, apiKey);
    }

    public UserAuthenticationResult authenticateByMossoIdAndAPIKey(int mossoId, String apiKey) {
        getLogger().info("Authenticating User with MossoId {}", mossoId);

        User user = findByMossoId(mossoId);

        return authenticateUserByApiKey(user, apiKey);
    }

    public UserAuthenticationResult authenticateByNastIdAndAPIKey(String nastId, String apiKey) {
        getLogger().debug("Authenticating User with NastId {}", nastId);
        if (StringUtils.isBlank(nastId)) {
            getLogger().error("Null or Empty NastId parameter");
            throw new IllegalArgumentException("Null or Empty NastId parameter.");
        }

        User user = findByNastId(nastId);

        return authenticateUserByApiKey(user, apiKey);
    }

    public void delete(String username) {
        getLogger().info("Deleting username - {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        Audit audit = Audit.log(username).delete();

        User user = this.findByUsername(username);
        if (user == null) {
            String errorMsg = String.format("User %s not found", username);
            audit.fail(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(user.getUniqueId());
        } catch (LDAPException ldapEx) {
            String errorString = String.format("Error deleting username %s - %s", username, ldapEx);
            audit.fail(errorString);
            getLogger().error(errorString);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            String errorString = String.format("Error deleting username %s - %s", username,
                result.getResultCode());
            audit.fail(errorString);
            getLogger().error(errorString);
            throw new IllegalStateException(String.format(
                "LDAP error encountered when deleting user: %s - %s" + username, result.getResultCode()
                    .toString()));
        }

        audit.succeed();
        getLogger().info("Deleted username - {}", username);
    }

    public Users findAll(int offset, int limit) {
        getLogger().debug("Search all users");

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .addEqualAttribute(ATTR_SOFT_DELETED, "FALSE").build();
        Users users = getMultipleUsers(searchFilter, ATTR_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;

    }

    public Users findByCustomerId(String customerId, int offset, int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException("Null or Empty customerId parameter.");
        }

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        Users users = getMultipleUsers(searchFilter, ATTR_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;
    }

    public User findByInum(String inum) {
        // NOTE: This method returns a user regardless of whether the
        // softDeleted flag is set or not because this method is only
        // used internally.
        getLogger().debug("Doing search for inum " + inum);
        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty inum parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_INUM, inum)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByMossoId(int mossoId) {
        getLogger().debug("Doing search for nastId " + mossoId);

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_MOSSO_ID, String.valueOf(mossoId))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByNastId(String nastId) {
        getLogger().debug("Doing search for nastId " + nastId);
        if (StringUtils.isBlank(nastId)) {
            getLogger().error("Null or Empty nastId parameter");
            throw new IllegalArgumentException("Null or Empty nastId parameter.");
        }

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_NAST_ID, nastId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByRPN(String rpn) {
        getLogger().debug("Doing search for rpn " + rpn);
        if (StringUtils.isBlank(rpn)) {
            getLogger().error("Null or Empty rpn parameter");
            throw new IllegalArgumentException("Null or Empty rpn parameter.");
        }

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_RACKSPACE_PERSON_NUMBER, rpn)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByUsername(String username) {
        // This method returns a user whether or not the user has been
        // soft-deleted
        getLogger().debug("Doing search for username " + username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findUser(String customerId, String username) {

        getLogger().debug("LdapUserRepository.findUser() - customerId: {}, username: {} ", customerId,
            username);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException("Null or Empty customerId parameter.");
        }
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User for customer - {}, {}", customerId, user);

        return user;
    }

    public String[] getGroupIdsForUser(String username) {
        getLogger().debug("Getting GroupIds for User {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException("Null or Empty username parameter.");
        }

        String[] groupIds = null;

        SearchResult searchResult = null;

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB, searchFilter,
                new String[]{ATTR_MEMBER_OF});
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            groupIds = e.getAttributeValues(ATTR_MEMBER_OF);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}", username);
            throw new IllegalStateException("More than one entry was found for this username");
        }

        getLogger().debug("Got GroupIds for User {} - {}", username, groupIds);

        return groupIds;
    }

    public String getUnusedUserInum(String customerInum) {
        // TODO: We might may this call to the XDI server in the future.
        if (StringUtils.isBlank(customerInum)) {
            getLogger().error("Null or empty customerInum value passesed in.");
            throw new IllegalArgumentException("Null or empty customerInum value passesed in.");
        }

        User user = null;
        String inum = "";
        do {
            inum = customerInum + InumHelper.getRandomInum(1);
            user = findByInum(inum);
        } while (user != null);

        return inum;
    }

    public boolean isUsernameUnique(String username) {

        String searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        User user = getSingleUser(searchFilter, ATTR_SEARCH_ATTRIBUTES);

        return user == null;
    }

    /* (non-Javadoc)
     * @see com.rackspace.idm.domain.dao.UserDao#save(com.rackspace.idm.domain.entity.User, boolean)
     */
    @Override
    public void save(User user, boolean hasSelfUpdatedPassword) {
        getLogger().info("Updating user {}", user);
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            getLogger().error("User instance is null or its userName has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The User instance either null or its userName has no value.");
        }
        User oldUser = findByUsername(user.getUsername());

        if (oldUser == null) {
            getLogger().error("No record found for user {}", user.getUsername());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given User instance. Has the userName been changed?");
        }

        Audit audit = Audit.log(user);

        LDAPResult result = null;
        try {
            List<Modification> mods = getModifications(oldUser, user, hasSelfUpdatedPassword);
            audit.modify(mods);

            if (mods.size() < 1) {
                // No changes!
                return;
            }
            result = getAppConnPool().modify(oldUser.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {

            if (ResultCode.UNWILLING_TO_PERFORM.equals(ldapEx.getResultCode())
                && STALE_PASSWORD_MESSAGE.equals(ldapEx.getMessage())) {
                audit.fail(STALE_PASSWORD_MESSAGE);
                throw new StalePasswordException("Past 10 passwords for the user cannot be re-used.");
            }

            getLogger().error("Error updating user {} - {}", user.getUsername(), ldapEx);
            audit.fail("Error updating user");
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail("encryption error");
            throw new IllegalStateException(e);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            String errorString = String.format("Error updating user %s - %s", user.getUsername(),
                result.getResultCode());
            audit.fail(errorString);
            getLogger().error(errorString);
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating user: %s - %s" + user.getUsername(), result
                    .getResultCode().toString()));
        }

        // Now that its in LDAP we'll set the password to existing type
        user.setPasswordObj(user.getPasswordObj().toExisting());

        audit.succeed();
        getLogger().info("Updated user - {}", user);
    }

    @Override
    public void setAllUsersLocked(String customerId, boolean isLocked) {
        Users users = this.findFirst100ByCustomerIdAndLock(customerId, !isLocked);
        if (users.getUsers() != null && users.getUsers().size() > 0) {
            for (User user : users.getUsers()) {
                user.setLocked(isLocked);
                this.save(user, false);
            }
        }
        if (users.getTotalRecords() > 0) {
            this.setAllUsersLocked(customerId, isLocked);
        }
    }

    private UserAuthenticationResult authenticateByPassword(User user, String password) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = bindUser(user, password);
        UserAuthenticationResult authResult = validateUserStatus(user, authenticated);
        getLogger().debug("Authenticated User by password");

        addAuditLogForAuthentication(user, authenticated);

        return authResult;
    }

    private UserAuthenticationResult authenticateUserByApiKey(User user, String apiKey) {

        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = !StringUtils.isBlank(user.getApiKey()) && user.getApiKey().equals(apiKey);

        UserAuthenticationResult authResult = validateUserStatus(user, authenticated);
        getLogger().debug("Authenticated User by API Key - {}", authResult);

        addAuditLogForAuthentication(user, authenticated);

        return authResult;
    }

    private boolean bindUser(User user, String password) {
        getLogger().debug("Authenticating user {}", user.getUsername());

        if (user == null || user.getUniqueId() == null) {
            throw new IllegalStateException("User cannot be null and must have a unique Id");
        }

        BindResult result;
        try {
            result = getBindConnPool().bind(user.getUniqueId(), password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                getLogger().info("Invalid login attempt by user {} with password {}.", user.getUsername(),
                    password);
                return false;
            }
            getLogger().error("Bind operation on username " + user.getUsername() + " failed.", e);
            throw new IllegalStateException(e);
        }

        getLogger().debug(result.toString());
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    private Users findFirst100ByCustomerIdAndLock(String customerId, boolean isLocked) {
        getLogger().debug("Doing search for customerId {}", customerId);

        int limit = 100;
        int offset = 0;

        String searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_LOCKED, String.valueOf(isLocked))
            .addEqualAttribute(ATTR_SOFT_DELETED, String.valueOf(false))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();

        Users users = getMultipleUsers(searchFilter, ATTR_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug("Found Users - {}", users);

        return users;
    }

    private Attribute[] getAddAttributes(User user) throws GeneralSecurityException {
        CryptHelper cryptHelper = CryptHelper.getinstance();

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_USER_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(user.getCountry())) {
            atts.add(new Attribute(ATTR_C, user.getCountry()));
        }

        if (!StringUtils.isBlank(user.getDisplayName())) {
            atts.add(new Attribute(ATTR_DISPLAY_NAME, cryptHelper.encrypt(user.getDisplayName())));
        }

        if (!StringUtils.isBlank(user.getFirstname())) {
            atts.add(new Attribute(ATTR_GIVEN_NAME, cryptHelper.encrypt(user.getFirstname())));
        }

        if (!StringUtils.isBlank(user.getIname())) {
            atts.add(new Attribute(ATTR_INAME, user.getIname()));
        }

        if (!StringUtils.isBlank(user.getInum())) {
            atts.add(new Attribute(ATTR_INUM, user.getInum()));
        }

        if (!StringUtils.isBlank(user.getEmail())) {
            atts.add(new Attribute(ATTR_MAIL, cryptHelper.encrypt(user.getEmail())));
        }

        if (!StringUtils.isBlank(user.getMiddlename())) {
            atts.add(new Attribute(ATTR_MIDDLE_NAME, user.getMiddlename()));
        }

        if (!StringUtils.isBlank(user.getOrgInum())) {
            atts.add(new Attribute(ATTR_O, user.getOrgInum()));
        }

        if (user.getLocale() != null) {
            atts.add(new Attribute(ATTR_LANG, user.getPreferredLang().toString()));
        }

        if (!StringUtils.isBlank(user.getCustomerId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, user.getCustomerId()));
        }

        if (!StringUtils.isBlank(user.getPersonId())) {
            atts.add(new Attribute(ATTR_RACKSPACE_PERSON_NUMBER, user.getPersonId()));
        }

        if (!StringUtils.isBlank(user.getApiKey())) {
            atts.add(new Attribute(ATTR_RACKSPACE_API_KEY, cryptHelper.encrypt(user.getApiKey())));
        }

        if (!StringUtils.isBlank(user.getSecretAnswer())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_A, cryptHelper.encrypt(user.getSecretAnswer())));
        }

        if (!StringUtils.isBlank(user.getSecretQuestion())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_Q, cryptHelper.encrypt(user.getSecretQuestion())));
        }

        if (!StringUtils.isBlank(user.getLastname())) {
            atts.add(new Attribute(ATTR_SN, cryptHelper.encrypt(user.getLastname())));
        }

        if (user.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, user.getStatus().toString()));
        }

        if (user.getTimeZoneObj() != null) {
            atts.add(new Attribute(ATTR_TIME_ZONE, user.getTimeZone()));
        }

        atts.add(new Attribute(ATTR_UID, user.getUsername()));

        if (!StringUtils.isBlank(user.getPasswordObj().getValue())) {
            atts.add(new Attribute(ATTR_PASSWORD, user.getPasswordObj().getValue()));
            atts.add(new Attribute(ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(user.getPassword())));
        }

        if (!StringUtils.isBlank(user.getRegion())) {
            atts.add(new Attribute(ATTR_RACKSPACE_REGION, user.getRegion()));
        }

        if (user.isLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(user.isLocked())));
        }

        if (user.isSoftDeleted() != null) {
            atts.add(new Attribute(ATTR_SOFT_DELETED, String.valueOf(user.isSoftDeleted())));
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

    private Users getMultipleUsers(String searchFilter, String[] searchAttributes, int offset, int limit) {

        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(new SortKey(ATTR_UID));

        offset = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        limit = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limit = limit > this.getLdapPagingLimitMax() ? this.getLdapPagingLimitMax() : limit;

        // In the constructor below we're adding one to the offset because the
        // Rackspace API standard calls for a 0 based offset while LDAP uses a
        // 1 based offset.
        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(offset + 1, 0,
            limit - 1, 0, null);

        int contentCount = 0;

        List<User> userList = new ArrayList<User>();
        SearchResult searchResult = null;
        try {

            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB, searchFilter,
                searchAttributes);

            request.setControls(new Control[]{sortRequest, vlvRequest});
            searchResult = getAppConnPool().search(request);

            for (Control c : searchResult.getResponseControls()) {
                if (c instanceof VirtualListViewResponseControl) {
                    VirtualListViewResponseControl vlvResponse = (VirtualListViewResponseControl) c;
                    contentCount = vlvResponse.getContentCount();
                }
            }
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                userList.add(getUser(entry));
            }

        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
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

    private User getSingleUser(String searchFilter, String[] searchAttributes) {
        User user = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB, searchFilter, searchAttributes);

            if (searchResult.getEntryCount() == 1) {
                SearchResultEntry e = searchResult.getSearchEntries().get(0);
                user = getUser(e);
            } else if (searchResult.getEntryCount() > 1) {
                String errMsg = String.format("More than one entry was found for user search - %s",
                    searchFilter);
                getLogger().error(errMsg);
                throw new IllegalStateException(errMsg);
            }
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error("Encryption error", e);
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }

    private User getUser(SearchResultEntry resultEntry) throws GeneralSecurityException {
        CryptHelper cryptHelper = CryptHelper.getinstance();
        User user = new User();
        user.setUniqueId(resultEntry.getDN());
        user.setUsername(resultEntry.getAttributeValue(ATTR_UID));
        user.setCountry(resultEntry.getAttributeValue(ATTR_C));
        user.setDisplayName(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_DISPLAY_NAME)));
        user.setFirstname(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_GIVEN_NAME)));
        user.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        user.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        user.setEmail(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_MAIL)));
        user.setMiddlename(resultEntry.getAttributeValue(ATTR_MIDDLE_NAME));
        user.setOrgInum(resultEntry.getAttributeValue(ATTR_O));
        user.setPrefferedLang(resultEntry.getAttributeValue(ATTR_LANG));
        user.setCustomerId(resultEntry.getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        user.setPersonId(resultEntry.getAttributeValue(ATTR_RACKSPACE_PERSON_NUMBER));
        user.setApiKey(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_RACKSPACE_API_KEY)));
        user.setSecretQuestion(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_PASSWORD_SECRET_Q)));
        user.setSecretAnswer(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_PASSWORD_SECRET_A)));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            user.setStatus(Enum.valueOf(UserStatus.class, statusStr.toUpperCase()));
        }

        user.setLastname(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_SN)));
        user.setTimeZoneObj(DateTimeZone.forID(resultEntry.getAttributeValue(ATTR_TIME_ZONE)));

        String ecryptedPwd = cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_CLEAR_PASSWORD));
        Date lastUpdates = resultEntry.getAttributeValueAsDate(ATTR_PASSWORD_UPDATED_TIMESTAMP);
        Boolean wasSelfUpdated = resultEntry.getAttributeValueAsBoolean(ATTR_PASSWORD_SELF_UPDATED);
        Password pwd = Password.existingInstance(ecryptedPwd, new DateTime(lastUpdates), wasSelfUpdated);
        user.setPasswordObj(pwd);

        user.setRegion(resultEntry.getAttributeValue(ATTR_RACKSPACE_REGION));

        String deleted = resultEntry.getAttributeValue(ATTR_SOFT_DELETED);
        if (deleted != null) {
            user.setSoftDeleted(resultEntry.getAttributeValueAsBoolean(ATTR_SOFT_DELETED));
        }

        String softDeletedTimestamp = resultEntry.getAttributeValue(ATTR_SOFT_DELETED_DATE);
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

        Date passwordFailureDate = resultEntry.getAttributeValueAsDate(ATTR_PWD_ACCOUNT_LOCKOUT_TIME);

        boolean passwordFailureLocked = false;
        if (passwordFailureDate != null) {
            DateTime passwordFailureDateTime = new DateTime(passwordFailureDate).plusMinutes(this
                .getLdapPasswordFailureLockoutMin());
            passwordFailureLocked = passwordFailureDateTime.isAfterNow();
        }
        user.setMaxLoginFailuresExceded(passwordFailureLocked);

        return user;
    }

    private UserAuthenticationResult validateUserStatus(User user, boolean isAuthenticated) {
        if (isAuthenticated && user.isDisabled()) {
            String errMsg = String.format("User %s is disabled.", user.getUsername());
            getLogger().error(errMsg);
            throw new UserDisabledException(errMsg);
        }

        return new UserAuthenticationResult(user, isAuthenticated);
    }

    List<Modification> getModifications(User uOld, User uNew, boolean isSelfUpdate)
        throws GeneralSecurityException {
        CryptHelper cryptHelper = CryptHelper.getinstance();
        List<Modification> mods = new ArrayList<Modification>();

        if (uNew.getPasswordObj().isNew()) {
            if (isSelfUpdate) {
                Password oldPwd = uOld.getPasswordObj();
                int secsSinceLastChange = Seconds.secondsBetween(oldPwd.getLastUpdated(), new DateTime())
                    .getSeconds();
                if (oldPwd.wasSelfUpdated() && secsSinceLastChange < DateTimeConstants.SECONDS_PER_DAY) {
                    throw new PasswordValidationException(
                        "Users cannot change own password more than once in a 24-hour period.");
                }
            }

            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD, uNew.getPasswordObj()
                .getValue()));

            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(uNew
                .getPasswordObj().getValue())));
        }

        if (uNew.getCountry() != null) {
            if (StringUtils.isBlank(uNew.getCountry())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_C));
            } else if (!StringUtils.equals(uOld.getCountry(), uNew.getCountry())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_C, uNew.getCountry()));
            }
        }

        if (uNew.getDisplayName() != null) {
            if (StringUtils.isBlank(uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_DISPLAY_NAME));
            } else if (!StringUtils.equals(uOld.getDisplayName(), uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_DISPLAY_NAME, cryptHelper
                    .encrypt(uNew.getDisplayName())));
            }
        }

        if (uNew.getFirstname() != null) {
            if (StringUtils.isBlank(uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_GIVEN_NAME));
            } else if (!StringUtils.equals(uOld.getFirstname(), uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_GIVEN_NAME, cryptHelper.encrypt(uNew
                    .getFirstname())));
            }
        }

        if (uNew.getIname() != null) {
            if (StringUtils.isBlank(uNew.getIname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_INAME));
            } else if (!StringUtils.equals(uOld.getIname(), uNew.getIname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_INAME, uNew.getIname()));
            }
        }

        if (uNew.getEmail() != null) {
            if (StringUtils.isBlank(uNew.getEmail())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MAIL));
            } else if (!StringUtils.equals(uOld.getEmail(), uNew.getEmail())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MAIL, cryptHelper.encrypt(uNew
                    .getEmail())));
            }
        }

        if (uNew.getMiddlename() != null) {
            if (StringUtils.isBlank(uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MIDDLE_NAME));
            } else if (!StringUtils.equals(uOld.getMiddlename(), uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MIDDLE_NAME, uNew.getMiddlename()));
            }
        }

        if (uNew.getApiKey() != null && !StringUtils.isEmpty(uNew.getApiKey())) {
            if (StringUtils.isBlank(uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_API_KEY));
            } else if (!StringUtils.equals(uOld.getApiKey(), uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_API_KEY, cryptHelper
                    .encrypt(uNew.getApiKey())));
            }
        }

        if (uNew.getSecretAnswer() != null) {
            if (StringUtils.isBlank(uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_PASSWORD_SECRET_A));
            } else if (!StringUtils.equals(uOld.getSecretAnswer(), uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SECRET_A, cryptHelper
                    .encrypt(uNew.getSecretAnswer())));
            }
        }

        if (uNew.getSecretQuestion() != null) {
            if (StringUtils.isBlank(uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_PASSWORD_SECRET_Q));
            } else if (!StringUtils.equals(uOld.getSecretQuestion(), uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SECRET_Q, cryptHelper
                    .encrypt(uNew.getSecretQuestion())));
            }
        }

        if (uNew.getLastname() != null) {
            if (StringUtils.isBlank(uNew.getLastname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SN));
            } else if (!StringUtils.equals(uOld.getLastname(), uNew.getLastname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_SN, cryptHelper.encrypt(uNew
                    .getLastname())));
            }
        }

        if (uNew.getRegion() != null) {
            if (StringUtils.isBlank(uNew.getRegion())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_REGION));
            } else if (!uNew.getRegion().equals(uOld.getRegion())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_REGION, uNew.getRegion()));
            }
        }

        if (uNew.getPersonId() != null) {
            if (StringUtils.isBlank(uNew.getPersonId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_PERSON_NUMBER));
            } else if (!uNew.getPersonId().equals(uOld.getPersonId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_PERSON_NUMBER, uNew
                    .getPersonId()));
            }
        }

        if (uNew.getLocale() != null && !uNew.getPreferredLang().equals(uOld.getPreferredLang())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LANG, uNew.getPreferredLang().toString()));
        }

        if (uNew.getStatus() != null && !uOld.getStatus().equals(uNew.getStatus())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_STATUS, uNew.getStatus().toString()));
        }

        if (uNew.getTimeZoneObj() != null && !uNew.getTimeZone().equals(uOld.getTimeZone())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_TIME_ZONE, uNew.getTimeZone()));
        }

        if (uNew.isLocked() != null && uNew.isLocked() != uOld.isLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED, String.valueOf(uNew.isLocked())));
        }

        if (uNew.isSoftDeleted() != null && uNew.isSoftDeleted() != uOld.isSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_SOFT_DELETED, String.valueOf(uNew
                .isSoftDeleted())));

            if (uNew.isSoftDeleted()) {
                mods.add(new Modification(ModificationType.ADD, ATTR_SOFT_DELETED_DATE, String
                    .valueOf(new DateTime())));
            } else {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SOFT_DELETED_DATE));
            }
        }

        if (uNew.getNastId() != null) {
            if (StringUtils.isBlank(uNew.getNastId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_NAST_ID));
            } else if (!uNew.getNastId().equals(uOld.getNastId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_NAST_ID, uNew.getNastId()));
            }
        }

        // To delete the attribute MossoId a negative value for the mossoId
        // is sent in.
        if (uNew.getMossoId() != null) {
            if (uNew.getMossoId() < 0) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MOSSO_ID));
            } else if (!uNew.getMossoId().equals(uOld.getMossoId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MOSSO_ID, String.valueOf(uNew
                    .getMossoId())));
            }
        }

        return mods;
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
                failureMessage = String.format(failureMessage, "User is Disabled");
            } else {
                failureMessage = String.format(failureMessage, "Incorrect Credentials");
            }

            audit.fail(failureMessage);
        }
    }
}
