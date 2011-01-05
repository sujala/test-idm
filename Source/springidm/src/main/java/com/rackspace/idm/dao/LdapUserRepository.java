package com.rackspace.idm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.util.InumHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
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

    private static final String ATTR_C = "c";
    private static final String ATTR_DISPLAY_NAME = "displayName";
    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_INAME = "iname";
    private static final String ATTR_INUM = "inum";
    private static final String ATTR_LOCKED = "locked";
    private static final String ATTR_MAIL = "mail";
    private static final String ATTR_MIDDLE_NAME = "middleName";
    private static final String ATTR_MEMBER_OF = "isMemberOf";
    private static final String ATTR_O = "o";
    private static final String ATTR_OBJECT_CLASS = "objectClass";
    private static final String ATTR_LANG = "preferredLanguage";
    private static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "rackspaceCustomerNumber";
    private static final String ATTR_RACKSPACE_PERSON_NUMBER = "rackspacePersonNumber";
    private static final String ATTR_RACKSPACE_API_KEY = "rackspaceApiKey";
    private static final String ATTR_RACKSPACE_REGION = "rackspaceRegion";
    private static final String ATTR_PASSWORD_SECRET_A = "secretAnswer";
    private static final String ATTR_PASSWORD_SECRET_Q = "secretQuestion";
    private static final String ATTR_STATUS = "status";
    private static final String ATTR_SEE_ALSO = "seeAlso";

    private static final String ATTR_SN = "sn";
    private static final String ATTR_TIME_ZONE = "timeZone";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_PASSWORD = "userPassword";
    
    private static final String ATTR_MOSSO_ID = "rsMossoId";
    private static final String ATTR_NAST_ID = "rsNastId";

    private static final String[] ATTR_OBJECT_CLASS_VALUES = {"top",
        "rackspacePerson"};

    private static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";

    private static final String USER_FIND_BY_EMAIL_STRING = "(&(objectClass=rackspacePerson)(mail=%s))";
    private static final String USER_FIND_BY_NAST_ID = "(&(objectClass=rackspacePerson)(rsNastId=%s))";
    private static final String USER_FIND_BY_MOSSO_ID = "(&(objectClass=rackspacePerson)(rsMossoId=%s))";
    private static final String USER_FIND_BY_USERNAME_STRING = "(&(objectClass=rackspacePerson)(uid=%s))";

    private static final String USER_FIND_BY_USERNAME_BASESTRING = "(&(objectClass=rackspacePerson)(uid=%s)";

    private static final String USER_FIND_BY_INUM_STRING = "(&(objectClass=rackspacePerson)(inum=%s))";

    private static final String USER_FIND_BY_CUSTOMER_NUMBER_STRING = "(&(objectClass=rackspacePerson)(rackspaceCustomerNumber=%s)(uid=%s)";

    private static final String USER_FIND_ALL_STRING_NOT_DELETED = "(&(objectClass=rackspacePerson)(softDeleted=FALSE))";
    private static final String USER_FIND_BY_USERNAME_STRING_NOT_DELETED = "(&(objectClass=rackspacePerson)(uid=%s)(softDeleted=FALSE))";
    private static final String USER_FIND_BY_CUSTOMERID_USERNAME_NOT_DELETED = "(&(objectClass=rackspacePerson)(rackspaceCustomerNumber=%s)(uid=%s)(softDeleted=FALSE))";
    private static final String USER_FIND_BY_CUSTOMERID_STRING_NOT_DELETED = "(&(objectClass=rackspacePerson)(rackspaceCustomerNumber=%s)(softDeleted=FALSE))";
    private static final String USER_FIND_BY_CUSTOMERID_AND_LOCK_STRING = "(&(objectClass=rackspacePerson)(rackspaceCustomerNumber=%s)(locked=%s)(softDeleted=FALSE))";

    public LdapUserRepository(LdapConnectionPools connPools, Logger logger) {
        super(connPools, logger);
    }

    public void add(User user, String customerDN) {
        getLogger().debug("Adding user - {}", user);
        if (user == null) {
            getLogger().error("Null instance of User was passed");
            throw new IllegalArgumentException(
                "Null instance of User was passed");
        }

        if (!user.getPasswordObj().isNew()) {
            getLogger().error("Password of User is an existing instance");
            throw new IllegalArgumentException(
                "The password appears to be an existing instance. It must be a new instance!");
        }

        String userDN = GlobalConstants.INUM_PREFIX + user.getInum()
            + ",ou=people," + customerDN;

        user.setUniqueId(userDN);

        Attribute[] attributes = getAddAttributes(user);

        LDAPResult result;
        try {
            result = getAppConnPool().add(userDN, attributes);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error adding user {} - {}", user, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error adding user {} - {}", user.getUsername(),
                result.getResultCode());
            throw new IllegalStateException(String.format(
                "LDAP error encountered when adding user: %s - %s",
                user.getUsername(), result.getResultCode().toString()));
        }

        getLogger().debug("Added user {}", user);
    }

    public boolean authenticate(String userName, String password) {
        getLogger().debug("Authenticating user {}", userName);
        BindResult result = null;
        try {
            result = ((LDAPConnectionPool) getBindConnPool()).bind(
                getUserDnByUsername(userName), password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                getLogger().info(
                    "Invalid login attempt by user {} with password {}.",
                    userName, password);
                return false;
            }
            getLogger().error(
                "Bind operation on username " + userName + " failed.", e);
            throw new IllegalStateException(e);
        }

        getLogger().debug(result.toString());
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    public boolean authenticateByAPIKey(String username, String apiKey) {
        getLogger().debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        User user = null;
        SearchResult searchResult = getUserSearchResult(username);

        if (searchResult.getEntryCount() == 0) {
            getLogger().warn("User {} not found", username);
            return false;
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        Boolean authenticated = user.getApiKey().equals(apiKey);

        getLogger().debug("Authenticated User {} by API Key", username);

        return authenticated;
    }

    public void delete(String username) {
        getLogger().info("Deleting username - {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().delete(getUserDnByUsername(username));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error deleting username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error deleting username {} - {}", username,
                result.getResultCode());
            throw new IllegalStateException(
                String.format(
                    "LDAP error encountered when deleting user: %s - %s"
                        + username, result.getResultCode().toString()));
        }

        getLogger().info("Deleted username - {}", username);
    }

    public List<User> findAll() {
        getLogger().debug("Search all users");
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                USER_FIND_ALL_STRING_NOT_DELETED);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for all users under DN {} - {}",
                BASE_DN, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        List<User> users = new ArrayList<User>();
        for (SearchResultEntry e : searchResult.getSearchEntries()) {
            User user = getUser(e);
            users.add(user);
        }

        getLogger().debug("Found {} users under DN ", users.size(), BASE_DN);

        return users;
    }

    public Users findFirst100ByCustomerIdAndLock(String customerId,
        boolean isLocked) {
        getLogger().debug("Doing search for customerId {}", customerId);

        int limit = 100;
        int offset = 1;

        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(
            new SortKey("uid"));

        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(
            offset, 0, limit - 1, 0, null);

        int contentCount = 0;

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        List<User> users = new ArrayList<User>();
        SearchResult searchResult = null;
        try {

            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_CUSTOMERID_AND_LOCK_STRING,
                    customerId, isLocked));

            request.setControls(new Control[]{sortRequest, vlvRequest});
            searchResult = getAppConnPool().search(request);

            for (Control c : searchResult.getResponseControls()) {
                if (c instanceof VirtualListViewResponseControl) {
                    VirtualListViewResponseControl vlvResponse = (VirtualListViewResponseControl) c;
                    contentCount = vlvResponse.getContentCount();
                }
            }

        } catch (LDAPException ldapEx) {
            getLogger().error("Error searching for Users CustomerId {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                users.add(getUser(entry));
            }
        }

        getLogger().debug("Found users {} for customer {}", users, customerId);

        Users usersObject = new Users();

        usersObject.setLimit(limit);
        usersObject.setOffset(offset);
        usersObject.setTotalRecords(contentCount);
        usersObject.setUsers(users);

        return usersObject;
    }

    public Users findByCustomerId(String customerId, int offset, int limit) {
        getLogger().debug("Doing search for customerId {}", customerId);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            throw new IllegalArgumentException(
                "Null or Empty customerId parameter.");
        }

        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(
            new SortKey("uid"));

        // In the constructor below we're adding one to the offset because the
        // Rackspace
        // API standard calls for a 0 based offset while LDAP uses a 1 based
        // offset.
        VirtualListViewRequestControl vlvRequest = new VirtualListViewRequestControl(
            offset + 1, 0, limit - 1, 0, null);

        int contentCount = 0;

        List<User> users = new ArrayList<User>();
        SearchResult searchResult = null;
        try {

            SearchRequest request = new SearchRequest(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_CUSTOMERID_STRING_NOT_DELETED,
                    customerId));

            request.setControls(new Control[]{sortRequest, vlvRequest});
            searchResult = getAppConnPool().search(request);

            for (Control c : searchResult.getResponseControls()) {
                if (c instanceof VirtualListViewResponseControl) {
                    VirtualListViewResponseControl vlvResponse = (VirtualListViewResponseControl) c;
                    contentCount = vlvResponse.getContentCount();
                }
            }

        } catch (LDAPException ldapEx) {
            getLogger().error("Error searching for Users CustomerId {} - {}",
                customerId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                users.add(getUser(entry));
            }
        }

        getLogger().debug("Found users {} for customer {}", users, customerId);

        Users usersObject = new Users();

        usersObject.setLimit(limit);
        usersObject.setOffset(offset);
        usersObject.setTotalRecords(contentCount);
        usersObject.setUsers(users);

        return usersObject;
    }

    public User findByEmail(String email) {
        getLogger().debug("Doing search for user by email: " + email);
        if (StringUtils.isBlank(email)) {
            getLogger().error("Null or Empty email parameter");
            throw new IllegalArgumentException("Null or Empty email parameter.");
        }

        User user = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_EMAIL_STRING, email));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for user by email {} - {}",
                email, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for user with email {}", email);
            throw new IllegalStateException(
                "More than one entry was found for this email");
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByInum(String inum) {
        // NOTE: This method returns a user regardless of whether the
        // softDeleted
        // flag is set or not because this method is only used internally.
        getLogger().debug("Doing search for inum " + inum);
        if (StringUtils.isBlank(inum)) {
            getLogger().error("Null or Empty inum parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }

        User user = null;

        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_INUM_STRING, inum));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for inum {} - {}", inum, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger()
                .error("More than one entry was found for inum {}", inum);
            throw new IllegalStateException(
                "More than one entry was found for this inum");
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findByUsername(String username) {
        getLogger().debug("Doing search for username " + username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        User user = null;
        SearchResult searchResult = getUserSearchResult(username);

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }
    
    public User findByNastId(String nastId) {
        getLogger().debug("Doing search for nastId " + nastId);
        if (StringUtils.isBlank(nastId)) {
            getLogger().error("Null or Empty nastId parameter");
            throw new IllegalArgumentException(
                "Null or Empty nastId parameter.");
        }

        User user = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_NAST_ID, nastId));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for user by nastId {} - {}",
                nastId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for user with nastId {}", nastId);
            throw new IllegalStateException(
                "More than one entry was found for this nastId");
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }
    
    public User findByMossoId(int mossoId) {
        getLogger().debug("Doing search for nastId " + mossoId);

        User user = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_MOSSO_ID, mossoId));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for user by mossoId {} - {}",
                mossoId, ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error(
                "More than one entry was found for user with mossoId {}", mossoId);
            throw new IllegalStateException(
                "More than one entry was found for this mossoId");
        }

        getLogger().debug("Found User - {}", user);

        return user;
    }

    public User findUser(String customerId, String username) {

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

        User user = null;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(USER_FIND_BY_CUSTOMERID_USERNAME_NOT_DELETED,
                    customerId, username));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        getLogger().debug("Found User for customer - {}, {}", customerId, user);

        return user;
    }

    public User findUser(String customerId, String username,
        Map<String, String> userStatusMap) {

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

        User user = null;
        SearchResult searchResult = null;

        String searchString = buildSearchString(
            USER_FIND_BY_CUSTOMER_NUMBER_STRING, userStatusMap);

        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(searchString, customerId, username));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            user = getUser(e);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        getLogger().debug("Found User for customer - {}, {}", customerId, user);

        return user;
    }

    public String[] getRoleIdsForUser(String username) {
        getLogger().debug("Getting RoleIds for User {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error("Null or Empty username parameter");
            throw new IllegalArgumentException(
                "Null or Empty username parameter.");
        }

        String[] roleIds = null;

        SearchResult searchResult = null;

        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(USER_FIND_BY_USERNAME_STRING_NOT_DELETED,
                    username), new String[]{"isMemberOf"});
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            roleIds = e.getAttributeValues(ATTR_MEMBER_OF);
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        getLogger().debug("Got RoleIds for User {} - {}", username, roleIds);

        return roleIds;
    }

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
            user = findByInum(inum);
        } while (user != null);

        return inum;
    }

    public String getUserDnByUsername(String username) {
        String dn = null;
        SearchResult searchResult = getUserSearchResult(username);
        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            dn = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                username);
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }
        return dn;
    }

    public boolean isUsernameUnique(String username) {
        boolean isUsernameUnique = true;
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(USER_FIND_BY_USERNAME_STRING, username));
            if (searchResult.getEntryCount() > 0) {
                isUsernameUnique = false;
            }

        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return isUsernameUnique;
    }

    public void save(User user) {
        getLogger().info("Updating user {}", user);
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            getLogger().error(
                "User instance is null or its userName has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The User instance either null or its userName has no value.");
        }
        User oldUser = findByUsername(user.getUsername());

        if (oldUser == null) {
            getLogger()
                .error("No record found for user {}", user.getUsername());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given User instance. Has the userName been changed?");
        }

        List<Modification> mods = getModifications(oldUser, user);

        if (user.equals(oldUser) || mods.size() < 1) {
            // No changes!
            return;
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(
                getUserDnByUsername(user.getUsername()), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} - {}",
                user.getUsername(), ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} - {}",
                user.getUsername(), result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating user: %s - %s"
                    + user.getUsername(), result.getResultCode().toString()));
        }

        getLogger().info("Updated user - {}", user);
    }

    public void saveRestoredUser(User user, Map<String, String> userStatusMap) {
        getLogger().info("Updating user {}", user);
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            getLogger().error(
                "User instance is null or its userName has no value");
            throw new IllegalArgumentException(
                "Bad parameter: The User instance either null or its userName has no value.");
        }

        User oldUser = null;
        SearchResult searchResult = null;
        String userDN = null;
        String searchString = buildSearchString(
            USER_FIND_BY_USERNAME_BASESTRING, userStatusMap);
        try {
            searchResult = getAppConnPool().search(BASE_DN, SearchScope.SUB,
                String.format(searchString, user.getUsername()));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}",
                user.getUsername(), ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() == 1) {
            SearchResultEntry e = searchResult.getSearchEntries().get(0);
            oldUser = getUser(e);
            userDN = e.getDN();
        } else if (searchResult.getEntryCount() > 1) {
            getLogger().error("More than one entry was found for username {}",
                user.getUsername());
            throw new IllegalStateException(
                "More than one entry was found for this username");
        }

        if (oldUser == null) {
            getLogger()
                .error("No record found for user {}", user.getUsername());
            throw new IllegalArgumentException(
                "There is no exisiting record for the given User instance. Has the userName been changed?");
        }

        if (user.equals(oldUser)) {
            // No changes!
            return;
        }

        LDAPResult result = null;
        try {
            result = getAppConnPool().modify(userDN,
                getModifications(oldUser, user));
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating user {} - {}",
                user.getUsername(), ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            getLogger().error("Error updating user {} - {}",
                user.getUsername(), result.getResultCode());
            throw new IllegalArgumentException(String.format(
                "LDAP error encountered when updating user: %s - %s"
                    + user.getUsername(), result.getResultCode().toString()));
        }

        getLogger().info("Updated user - {}", user);
    }

    public void setAllUsersLocked(String customerId, boolean isLocked) {
        Users users = this.findFirst100ByCustomerIdAndLock(customerId,
            !isLocked);
        if (users.getUsers() != null && users.getUsers().size() > 0) {
            for (User user : users.getUsers()) {
                user.setIsLocked(isLocked);
                this.save(user);
            }
        }
        if (users.getTotalRecords() > 0) {
            this.setAllUsersLocked(customerId, isLocked);
        }

    }

    private Attribute[] getAddAttributes(User user) {

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(user.getCountry())) {
            atts.add(new Attribute(ATTR_C, user.getCountry()));
        }

        if (!StringUtils.isBlank(user.getDisplayName())) {
            atts.add(new Attribute(ATTR_DISPLAY_NAME, user.getDisplayName()));
        }

        if (!StringUtils.isBlank(user.getFirstname())) {
            atts.add(new Attribute(ATTR_GIVEN_NAME, user.getFirstname()));
        }

        if (!StringUtils.isBlank(user.getIname())) {
            atts.add(new Attribute(ATTR_INAME, user.getIname()));
        }

        if (!StringUtils.isBlank(user.getInum())) {
            atts.add(new Attribute(ATTR_INUM, user.getInum()));
        }

        if (!StringUtils.isBlank(user.getEmail())) {
            atts.add(new Attribute(ATTR_MAIL, user.getEmail()));
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
            atts.add(new Attribute(ATTR_RACKSPACE_API_KEY, user.getApiKey()));
        }

        if (!StringUtils.isBlank(user.getSecretAnswer())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_A, user
                .getSecretAnswer()));
        }

        if (!StringUtils.isBlank(user.getSecretQuestion())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_Q, user
                .getSecretQuestion()));
        }

        if (!StringUtils.isBlank(user.getLastname())) {
            atts.add(new Attribute(ATTR_SN, user.getLastname()));
        }

        if (user.getStatus() != null) {
            atts.add(new Attribute(ATTR_STATUS, user.getStatus().toString()));
        }

        if (!StringUtils.isBlank(user.getSeeAlso())) {
            atts.add(new Attribute(ATTR_SEE_ALSO, user.getSeeAlso()));
        }

        if (user.getTimeZoneObj() != null) {
            atts.add(new Attribute(ATTR_TIME_ZONE, user.getTimeZone()));
        }

        atts.add(new Attribute(ATTR_UID, user.getUsername()));

        if (!StringUtils.isBlank(user.getPasswordObj().getValue())) {
            atts.add(new Attribute(ATTR_PASSWORD, user.getPasswordObj()
                .getValue()));
        }

        if (!StringUtils.isBlank(user.getRegion())) {
            atts.add(new Attribute(ATTR_RACKSPACE_REGION, user.getRegion()));
        }

        if (user.getIsLocked() != null) {
            atts.add(new Attribute(ATTR_LOCKED, String.valueOf(user
                .getIsLocked())));
        }

        if (user.getSoftDeleted() != null) {
            atts.add(new Attribute(GlobalConstants.ATTR_SOFT_DELETED, String
                .valueOf(user.getSoftDeleted())));
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

    private User getUser(SearchResultEntry resultEntry) {
        User user = new User(resultEntry.getAttributeValue(ATTR_UID));

        user.setUniqueId(resultEntry.getDN());

        user.setCountry(resultEntry.getAttributeValue(ATTR_C));
        user.setDisplayName(resultEntry.getAttributeValue(ATTR_DISPLAY_NAME));
        user.setFirstname(resultEntry.getAttributeValue(ATTR_GIVEN_NAME));
        user.setIname(resultEntry.getAttributeValue(ATTR_INAME));
        user.setInum(resultEntry.getAttributeValue(ATTR_INUM));
        user.setEmail(resultEntry.getAttributeValue(ATTR_MAIL));
        user.setMiddlename(resultEntry.getAttributeValue(ATTR_MIDDLE_NAME));
        user.setOrgInum(resultEntry.getAttributeValue(ATTR_O));
        user.setPrefferedLang(resultEntry.getAttributeValue(ATTR_LANG));
        user.setCustomerId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        user.setPersonId(resultEntry
            .getAttributeValue(ATTR_RACKSPACE_PERSON_NUMBER));
        user.setApiKey(resultEntry.getAttributeValue(ATTR_RACKSPACE_API_KEY));
        user.setSecretQuestion(resultEntry
            .getAttributeValue(ATTR_PASSWORD_SECRET_Q));
        user.setSecretAnswer(resultEntry
            .getAttributeValue(ATTR_PASSWORD_SECRET_A));

        String statusStr = resultEntry.getAttributeValue(ATTR_STATUS);
        if (statusStr != null) {
            user.setStatus(Enum.valueOf(UserStatus.class,
                statusStr.toUpperCase()));
        }

        user.setSeeAlso(resultEntry.getAttributeValue(ATTR_SEE_ALSO));
        user.setLastname(resultEntry.getAttributeValue(ATTR_SN));
        user.setTimeZoneObj(DateTimeZone.forID(resultEntry
            .getAttributeValue(ATTR_TIME_ZONE)));
        Password pwd = Password.existingInstance(resultEntry
            .getAttributeValue(ATTR_PASSWORD));
        user.setPasswordObj(pwd);

        user.setRegion(resultEntry.getAttributeValue(ATTR_RACKSPACE_REGION));

        String deleted = resultEntry
            .getAttributeValue(GlobalConstants.ATTR_SOFT_DELETED);
        if (deleted != null) {
            user.setSoftDeleted(resultEntry
                .getAttributeValueAsBoolean(GlobalConstants.ATTR_SOFT_DELETED));
        }

        String locked = resultEntry.getAttributeValue(ATTR_LOCKED);
        if (locked != null) {
            user.setIsLocked(resultEntry
                .getAttributeValueAsBoolean(ATTR_LOCKED));
        }
        
        user.setMossoId(resultEntry.getAttributeValueAsInteger(ATTR_MOSSO_ID));
        user.setNastId(resultEntry.getAttributeValue(ATTR_NAST_ID));

        return user;
    }

    private String buildSearchString(String baseString,
        Map<String, String> userStatusMap) {
        String ldapSearchString = baseString;

        for (String key : userStatusMap.keySet()) {
            String value = userStatusMap.get(key);

            if (key.equals(GlobalConstants.ATTR_SOFT_DELETED)) {
                ldapSearchString += "(" + GlobalConstants.ATTR_SOFT_DELETED
                    + "=" + value + "))";
            }

            if (key.equals(ATTR_LOCKED)) {
                ldapSearchString += "(" + ATTR_LOCKED + "=" + value + "))";
            }
        }
        return ldapSearchString;
    }

    private SearchResult getUserSearchResult(String username) {
        SearchResult searchResult = null;
        try {
            searchResult = getAppConnPool().search(
                BASE_DN,
                SearchScope.SUB,
                String.format(USER_FIND_BY_USERNAME_STRING_NOT_DELETED,
                    username));
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for username {} - {}", username,
                ldapEx);
            throw new IllegalStateException(ldapEx);
        }
        return searchResult;
    }

    List<Modification> getModifications(User uOld, User uNew) {
        List<Modification> mods = new ArrayList<Modification>();

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
                    ATTR_DISPLAY_NAME, uNew.getDisplayName()));
            }
        }

        if (uNew.getFirstname() != null) {
            if (StringUtils.isBlank(uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_GIVEN_NAME));
            } else if (!StringUtils.equals(uOld.getFirstname(),
                uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_GIVEN_NAME, uNew.getFirstname()));
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
                    uNew.getEmail()));
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

        if (uNew.getApiKey() != null) {
            if (StringUtils.isBlank(uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_RACKSPACE_API_KEY));
            } else if (!StringUtils.equals(uOld.getApiKey(), uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_RACKSPACE_API_KEY, uNew.getApiKey()));
            }
        }

        if (uNew.getSecretAnswer() != null) {
            if (StringUtils.isBlank(uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PASSWORD_SECRET_A));
            } else if (!StringUtils.equals(uOld.getSecretAnswer(),
                uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_PASSWORD_SECRET_A, uNew.getSecretAnswer()));
            }
        }

        if (uNew.getSecretQuestion() != null) {
            if (StringUtils.isBlank(uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_PASSWORD_SECRET_Q));
            } else if (!StringUtils.equals(uOld.getSecretQuestion(),
                uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_PASSWORD_SECRET_Q, uNew.getSecretQuestion()));
            }
        }

        if (uNew.getSeeAlso() != null) {
            if (StringUtils.isBlank(uNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_SEE_ALSO));
            } else if (!StringUtils
                .equals(uOld.getSeeAlso(), uNew.getSeeAlso())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_SEE_ALSO, uNew.getSeeAlso()));
            }
        }

        if (uNew.getLastname() != null) {
            if (StringUtils.isBlank(uNew.getLastname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SN));
            } else if (!StringUtils.equals(uOld.getLastname(),
                uNew.getLastname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_SN,
                    uNew.getLastname()));
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

        if (uNew.getPasswordObj().isNew()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD,
                uNew.getPasswordObj().getValue()));
        }

        if (uNew.getIsLocked() != null
            && uNew.getIsLocked() != uOld.getIsLocked()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LOCKED,
                String.valueOf(uNew.getIsLocked())));
        }

        if (uNew.getSoftDeleted() != null
            && uNew.getSoftDeleted() != uOld.getSoftDeleted()) {
            mods.add(new Modification(ModificationType.REPLACE,
                GlobalConstants.ATTR_SOFT_DELETED, String.valueOf(uNew
                    .getSoftDeleted())));
        }
        
        if (uNew.getNastId() != null) {
            if (StringUtils.isBlank(uNew.getNastId())) {
                mods.add(new Modification(ModificationType.DELETE,
                    ATTR_NAST_ID));
            } else if (!uNew.getNastId().equals(uOld.getNastId())) {
                mods.add(new Modification(ModificationType.REPLACE,
                    ATTR_NAST_ID, uNew.getNastId()));
            }
        }
        
        if (uNew.getMossoId() != null
            && uNew.getMossoId() != uOld.getMossoId()) {
            mods.add(new Modification(ModificationType.REPLACE,
                ATTR_MOSSO_ID, String.valueOf(uNew
                    .getSoftDeleted())));
        }

        return mods;
    }
}
