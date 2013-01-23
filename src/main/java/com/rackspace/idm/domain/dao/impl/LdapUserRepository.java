package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.StaticUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class LdapUserRepository extends LdapRepository implements UserDao {

    // NOTE: This is pretty fragile way of handling the specific error, so we
    // need to look into more reliable way of detecting this error.
    private static final String STALE_PASSWORD_MESSAGE = "Password match in history";
    public static final String ENCRYPTION_ERROR = "encryption error";
    public static final String NULL_OR_EMPTY_USERNAME_PARAMETER = "Null or Empty username parameter";
    public static final String FOUND_USER = "Found User - {}";
    public static final String FOUND_USERS = "Found Users - {}";

    @Autowired
    Paginator<User> paginator;

    @Override
    public void addRacker(Racker racker) {
        getLogger().info("Adding racker - {}", racker);
        if (racker == null) {
            String errmsg = "Null instance of Racer was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        String userDN = new LdapDnBuilder(RACKERS_BASE_DN).addAttribute(ATTR_RACKER_ID, racker.getRackerId()).build();

        racker.setUniqueId(userDN);

        Audit audit = Audit.log(racker).add();

        Attribute[] attributes = getRackerAddAttributes(racker);

        addEntry(userDN, attributes, audit);

        audit.succeed();

        getLogger().info("Added racker {}", racker);
    }

    @Override
    public void addUser(User user) {
        getLogger().info("Adding user - {}", user);
        if (user == null) {
            String errmsg = "Null instance of User was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        String userDN = new LdapDnBuilder(USERS_BASE_DN).addAttribute(ATTR_ID, user.getId()).build();

        user.setUniqueId(userDN);

        Audit audit = Audit.log(user).add();

        Attribute[] attributes = null;

        try {
            attributes = getAddAttributes(user);
            if (isUsernameUnique(user.getUsername())) { // one more check
                addEntry(userDN, attributes, audit);
            }else{
                throw new DuplicateUsernameException("User with username: '" + user.getUsername() + "' already exists.");
            }
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        }

        // Now that it's in LDAP we'll set the password to the "existing" type
        user.setPasswordObj(user.getPasswordObj().toExisting());

        audit.succeed();

        getLogger().info("Added user {}", user);
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
    public void deleteRacker(String rackerId) {
        getLogger().info("Deleting racker - {}", rackerId);
        if (StringUtils.isBlank(rackerId)) {
            getLogger().error("Null or Empty rackerId parameter");
            throw new IllegalArgumentException("Null or Empty rackerId parameter.");
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
    public void deleteUser(User user) {
        getLogger().info("Deleting user - {}", user.getUsername());

        Audit audit = Audit.log(user.getUsername()).delete();

        deleteEntryAndSubtree(user.getUniqueId(), audit);

        audit.succeed();

        getLogger().info("Deleted username - {}", user.getUsername());
    }

    @Override
    public void deleteUser(String username) {
        getLogger().info("Deleting username - {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            throw new IllegalArgumentException("Null or Empty username parameter.");
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
    public String[] getGroupIdsForUser(String username) {
        getLogger().debug("Getting GroupIds for User {}", username);
        if (StringUtils.isBlank(username)) {
            getLogger().error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            getLogger().info("Invalid username parameter");
            return null;
        }

        String[] groupIds = null;

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        SearchResultEntry entry = this.getSingleEntry(USERS_BASE_DN,
            SearchScope.SUB, searchFilter, ATTR_MEMBER_OF);

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
            getLogger().info("Invalid rackerId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKER_ID, rackerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKER).build();

        Racker racker = null;

        SearchResultEntry entry = this.getSingleEntry(RACKERS_BASE_DN,
            SearchScope.ONE, searchFilter);
        if (entry != null) {
            racker = new Racker();
            racker.setUniqueId(entry.getDN());
            racker.setRackerId(entry.getAttributeValue(ATTR_RACKER_ID));
        }

        getLogger().debug("Got Racker {}", racker);
        return racker;
    }

    @Override
    public User getUserByCustomerIdAndUsername(String customerId, String username) {

        getLogger().debug("LdapUserRepository.findUser() - customerId: {}, username: {} ", customerId, username);

        if (StringUtils.isBlank(customerId)) {
            getLogger().error("Null or Empty customerId parameter");
            getLogger().info("Invalid customerId parameter.");
            return null;
        }
        if (StringUtils.isBlank(username)) {
            getLogger().error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            getLogger().info("Invalid username parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, customerId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug("Found User for customer - {}, {}", customerId, user);

        return user;
    }

    @Override
    public User getUserById(String id) {
        // NOTE: This method returns a user regardless of whether the
        // softDeleted flag is set or not because this method is only
        // used internally.
        getLogger().debug("Doing search for id " + id);
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            getLogger().info("Invalid id parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public Users getUsersByMossoId(int mossoId) {
        getLogger().debug("Doing search for mossoId " + mossoId);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_MOSSO_ID, String.valueOf(mossoId))
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES,getLdapPagingOffsetDefault(),getLdapPagingLimitDefault());

        getLogger().debug(FOUND_USERS, users);

        return users;
    }

    @Override
    public Users getUsersByNastId(String nastId) {
        getLogger().debug("Doing search for nastId " + nastId);
        if (StringUtils.isBlank(nastId)) {
            getLogger().error("Null or Empty nastId parameter");
            getLogger().info("Invalid nastId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAST_ID, nastId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES,getLdapPagingOffsetDefault(),getLdapPagingLimitDefault());

        getLogger().debug(FOUND_USERS, users);

        return users;
    }

    @Override
    public Users getUsersByDomainId(String domainId) {
        getLogger().debug("Doing search for domainId " + domainId);
        if (StringUtils.isBlank(domainId)) {
            getLogger().error("Null or Empty domainId parameter");
            getLogger().info("Invalid domainId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .build();

        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES,getLdapPagingOffsetDefault(),getLdapPagingLimitDefault());

        getLogger().debug(FOUND_USERS, users);

        return users;
    }

    @Override
    public User getUserByRPN(String rpn) {
        getLogger().debug("Doing User search by rpn " + rpn);
        if (StringUtils.isBlank(rpn)) {
            getLogger().error("Null or Empty rpn parameter");
            getLogger().info("Invalid rpn parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_RACKSPACE_PERSON_NUMBER, rpn)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public User getUserBySecureId(String secureId) {
        getLogger().debug("Doing User search by secureId " + secureId);
        if (StringUtils.isBlank(secureId)) {
            getLogger().error("Null or Empty secureId parameter");
            getLogger().info("Invalid secureId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_SECURE_ID, secureId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        // This method returns a user whether or not the user has been
        // soft-deleted
        getLogger().debug("Doing search for username " + username);
        if (StringUtils.isBlank(username)) {
            getLogger().error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            getLogger().info("Invalid username parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleUser(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public Users getUsers(List<Filter> filters) {
        getLogger().debug("Doing search for users");

        if(filters == null){
           return new Users();
        }

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                .addOrAttributes(filters)
                .build();

        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES,getLdapPagingOffsetDefault(),getLdapPagingLimitDefault());

        getLogger().debug(FOUND_USERS, users);

        return users;
    }

    @Override
    public PaginatorContext<User> getAllUsersPaged(FilterParam[] filterParams, int offset, int limit) {
        getLogger().debug("Getting paged users");

        Filter searchFilter = createSearchFilter(filterParams);
        PaginatorContext<User> context = getMultipleUsersPaged(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug(FOUND_USERS, context.getValueList());

        return context;
    }

    @Override
    public Users getAllUsers(FilterParam[] filterParams, int offset, int limit) {
        getLogger().debug("Getting all users");

        Filter searchFilter = createSearchFilter(filterParams);
        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES, offset, limit);

        getLogger().debug(FOUND_USERS, users);

        return users;
    }

    @Override
    public Users getAllUsersNoLimit(FilterParam[] filters) {
        getLogger().debug("Getting all users");

        Filter searchFilter = createSearchFilter(filters);
        Users users = getMultipleUsers(searchFilter, ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USERS, users);

        return users;
    }


    protected Filter createSearchFilter(FilterParam[] filterParams) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON);

        if (filterParams != null) {
            for (FilterParam filter : filterParams) {
                if (filter.getParam() == FilterParamName.RCN) {
                    searchBuilder.addEqualAttribute(ATTR_RACKSPACE_CUSTOMER_NUMBER, filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.USERNAME) {
                    searchBuilder.addEqualAttribute(ATTR_UID, filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.DOMAIN_ID) {
                    searchBuilder.addEqualAttribute(ATTR_DOMAIN_ID, filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.GROUP_ID) {
                    searchBuilder.addEqualAttribute(ATTR_GROUP_ID, filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.IN_MIGRATION) {
                    searchBuilder.addEqualAttribute(ATTR_IN_MIGRATION, "TRUE");
                } else if (filter.getParam() == FilterParamName.MIGRATED) {
                    searchBuilder.addEqualAttribute(ATTR_IN_MIGRATION, "FALSE");
                } else if (filter.getParam() == FilterParamName.ENABLED) {
                    searchBuilder.addEqualAttribute(ATTR_ENABLED, filter.getStrValue().toUpperCase());
                }
            }
        }

        return searchBuilder.build();
    }

    @Override
    public boolean isUsernameUnique(String username) {

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = null;
        try {
            SearchResultEntry entry = this.getSingleEntry(BASE_DN, SearchScope.SUB, searchFilter);

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

        getLogger().debug("Unique user search attempt yielded: {}", user);
        return user == null;
    }

    public void updateUserById(User user, boolean hasSelfUpdatedPassword){
        getLogger().info("Updating user to {}", user);
        throwIfEmptyUsername(user);
        User oldUser = getUserById(user.getId());
        if(!StringUtils.equalsIgnoreCase(oldUser.getUsername(), user.getUsername()) && !isUsernameUnique(user.getUsername())){
            throw new DuplicateUsernameException("User with username: '" + user.getUsername() + "' already exists.");
        }
        updateUser(user, oldUser, hasSelfUpdatedPassword);
    }

    @Override
    public void updateUser(User user, boolean hasSelfUpdatedPassword) {
        getLogger().info("Updating user to {}", user);
        throwIfEmptyUsername(user);
        User oldUser = getUserByUsername(user.getUsername());
        updateUser(user, oldUser, hasSelfUpdatedPassword);
    }

    void updateUser(User newUser, User oldUser, boolean hasSelfUpdatedPassword) {
        getLogger().debug("Found existing user {}", oldUser);
        throwIfEmptyOldUser(oldUser, newUser);

        Audit audit = Audit.log(newUser);

        try {
            List<Modification> mods = getModifications(oldUser, newUser, hasSelfUpdatedPassword);
            audit.modify(mods);

            if (mods.size() < 1) {
                // No changes!
                return;
            }
            getAppInterface().modify(oldUser.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            throwIfStalePassword(ldapEx, audit);
            getLogger().error("Error updating user {} - {}", newUser.getUsername(), ldapEx);
            audit.fail("Error updating user");
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            audit.fail(ENCRYPTION_ERROR);
            throw new IllegalStateException(e);
        }

        // Now that its in LDAP we'll set the password to existing type
        newUser.setPasswordObj(newUser.getPasswordObj().toExisting());
        audit.succeed();
        getLogger().info("Updated user - {}", newUser);
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
            SearchRequest request = new SearchRequest(USERS_BASE_DN,
                SearchScope.SUB, searchFilter);
            searchResult = getAppInterface().search(request);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                userList.add(getUser(entry));
            }
        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found {} Users", userList.size());

        List<Modification> mods = new ArrayList<Modification>();
        mods.add(new Modification(ModificationType.DELETE, ATTR_MEMBER_OF, group.getUniqueId()));

        Audit audit = null;
        for (User user : userList) {
            audit = Audit.log(user).modify(mods);
            try {
                getAppInterface().modify(user.getUniqueId(), mods);
            } catch (LDAPException ldapEx) {
                audit.fail(ldapEx.getMessage());
                throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
            }
            audit.succeed();
        }

        getLogger().info("Removed users from clientGroup {}", group);
    }
    
    void throwIfEmptyOldUser(User oldUser, User user) {
        if (oldUser == null) {
            getLogger().error("No record found for user {}", user.getUsername());
            throw new IllegalArgumentException("There is no exisiting record for the given User instance. Has the userName been changed?");
        }
    }

    void throwIfEmptyUsername(User user) {
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            getLogger().error("User instance is null or its userName has no value");
            throw new BadRequestException("Bad parameter: The User is null or has a blank Username");
        }
    }

    void throwIfStalePassword(LDAPException ldapEx, Audit audit) {
        if (ResultCode.CONSTRAINT_VIOLATION.equals(ldapEx.getResultCode())
            && STALE_PASSWORD_MESSAGE.equals(ldapEx.getMessage())) {
            audit.fail(STALE_PASSWORD_MESSAGE);
            throw new StalePasswordException("Past 10 passwords for the user cannot be re-used.");
        }
    }

    void addAuditLogForAuthentication(User user, boolean authenticated) {

        Audit audit = Audit.authUser(user);
        if (authenticated) {
            audit.succeed();
        } else {
            String failureMessage = "User Authentication Failed: %s";

            if (user.isMaxLoginFailuresExceded()) {
                failureMessage = String.format(failureMessage, "User locked due to max login failures limit exceded");
            } else if (user.isDisabled()) {
                failureMessage = String.format(failureMessage, "User is Disabled");
            } else {
                failureMessage = String.format(failureMessage, "Incorrect Credentials");
            }

            audit.fail(failureMessage);
        }
    }

    UserAuthenticationResult authenticateByPassword(User user, String password) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = bindUser(user, password);
        UserAuthenticationResult authResult = validateUserStatus(user, authenticated);
        getLogger().debug("Authenticated User by password");

        addAuditLogForAuthentication(user, authenticated);

        return authResult;
    }

    UserAuthenticationResult authenticateUserByApiKey(User user, String apiKey) {

        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean authenticated = !StringUtils.isBlank(user.getApiKey())  && user.getApiKey().equals(apiKey);

        UserAuthenticationResult authResult = validateUserStatus(user, authenticated);
        getLogger().debug("Authenticated User by API Key - {}", authResult);

        addAuditLogForAuthentication(user, authenticated);

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

    Attribute[] getAddAttributes(User user)
        throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_USER_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(user.getId())) {
            atts.add(new Attribute(ATTR_ID, user.getId()));
        }

        if (!StringUtils.isBlank(user.getCountry())) {
            atts.add(new Attribute(ATTR_C, user.getCountry()));
        }

        if (!StringUtils.isBlank(user.getDisplayName())) {
            atts.add(new Attribute(ATTR_DISPLAY_NAME, cryptHelper.encrypt(user.getDisplayName())));
        }

        if (!StringUtils.isBlank(user.getFirstname())) {
            atts.add(new Attribute(ATTR_GIVEN_NAME, cryptHelper.encrypt(user.getFirstname())));
        }

        if (!StringUtils.isBlank(user.getEmail())) {
            atts.add(new Attribute(ATTR_MAIL, cryptHelper.encrypt(user.getEmail())));
        }

        if (!StringUtils.isBlank(user.getMiddlename())) {
            atts.add(new Attribute(ATTR_MIDDLE_NAME, user.getMiddlename()));
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

        if (!StringUtils.isBlank(user.getSecretQuestionId())) {
            atts.add(new Attribute(ATTR_PASSWORD_SECRET_Q_ID, cryptHelper.encrypt(user.getSecretQuestionId())));
        }

        if (!StringUtils.isBlank(user.getLastname())) {
            atts.add(new Attribute(ATTR_SN, cryptHelper.encrypt(user.getLastname())));
        }

        if (user.getTimeZoneObj() != null) {
            atts.add(new Attribute(ATTR_TIME_ZONE, user.getTimeZone()));
        }

        atts.add(new Attribute(ATTR_UID, user.getUsername()));

        if (!StringUtils.isBlank(user.getPasswordObj().getValue())) {
            atts.add(new Attribute(ATTR_PASSWORD, user.getPasswordObj().getValue()));
            atts.add(new Attribute(ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(user.getPassword())));
            atts.add(new Attribute(ATTR_PASSWORD_SELF_UPDATED, Boolean.FALSE.toString().toUpperCase()));
            atts.add(new Attribute(ATTR_PASSWORD_UPDATED_TIMESTAMP, StaticUtils.encodeGeneralizedTime(new DateTime().toDate())));
        }

        if (!StringUtils.isBlank(user.getRegion())) {
            atts.add(new Attribute(ATTR_RACKSPACE_REGION, user.getRegion()));
        }

        if (user.isEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, String.valueOf(user.isEnabled()).toUpperCase()));
        }

        if (!StringUtils.isBlank(user.getNastId())) {
            atts.add(new Attribute(ATTR_NAST_ID, user.getNastId()));
        }

        if (user.getMossoId() != null && user.getMossoId().intValue() > 0) {
            atts.add(new Attribute(ATTR_MOSSO_ID, user.getMossoId().toString()));
        }

        if(!StringUtils.isBlank(user.getDomainId())){
            atts.add(new Attribute(ATTR_DOMAIN_ID, user.getDomainId()));
        }

        if(user.getInMigration() != null) {
            atts.add(new Attribute(ATTR_IN_MIGRATION, String.valueOf(user.getInMigration()).toUpperCase()));
            atts.add(new Attribute(ATTR_MIGRATION_DATE, StaticUtils.encodeGeneralizedTime(user.getMigrationDate().toDate())));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes to add", attributes.length);
        return attributes;
    }

    protected PaginatorContext<User> getMultipleUsersPaged(Filter searchFilter, String[] searchAttributes, int offset, int limit) {
        SearchRequest searchRequest = new SearchRequest(USERS_BASE_DN, SearchScope.SUB, searchFilter);
        PaginatorContext<User> paginatorContext = paginator.createSearchRequest(ATTR_ID, searchRequest, offset, limit);

        SearchResult searchResult = this.getMultipleEntries(searchRequest);

        if (searchResult == null) {
            return paginatorContext;
        }

        paginator.createPage(searchResult, paginatorContext);
        List<User> userList = new ArrayList<User>();
        try {
            for (SearchResultEntry entry : paginatorContext.getSearchResultEntryList()) {
                userList.add(getUser(entry));
            }
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        paginatorContext.setValueList(userList);

        return paginatorContext;
    }

    Users getMultipleUsers(Filter searchFilter,
        String[] searchAttributes, int offset, int limit) {

        int offsets = offset < 0 ? this.getLdapPagingOffsetDefault() : offset;
        int limits = limit <= 0 ? this.getLdapPagingLimitDefault() : limit;
        limits = limits > this.getLdapPagingLimitMax() ? this.getLdapPagingLimitMax() : limits;

        int contentCount = 0;

        List<User> userList = new ArrayList<User>();

        try {

            List<SearchResultEntry> entries = this.getMultipleEntries(
                USERS_BASE_DN, SearchScope.SUB, ATTR_ID, searchFilter, searchAttributes);

            contentCount = entries.size();

            if (offsets < contentCount) {

                int toIndex = offsets + limits > contentCount ? contentCount : offsets + limits;
                int fromIndex = offsets;

                List<SearchResultEntry> subList = entries.subList(fromIndex, toIndex);

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

        users.setLimit(limits);
        users.setOffset(offsets);
        users.setTotalRecords(contentCount);
        users.setUsers(userList);
        getLogger().debug("Returning {} Users.", users.getTotalRecords());
        return users;
    }

    Users getMultipleUsers(Filter searchFilter, String[] searchAttributes) {
        List<User> userList = new ArrayList<User>();

        try {
            List<SearchResultEntry> entries = this.getMultipleEntries(
                    USERS_BASE_DN, SearchScope.SUB, ATTR_ID, searchFilter, searchAttributes);
            for (SearchResultEntry entry : entries) {
                userList.add(getUser(entry));
            }
        } catch (GeneralSecurityException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }

        Users users = new Users();
        users.setUsers(userList);

        getLogger().debug("returning {} users", userList.size());
        return users;
    }

    Attribute[] getRackerAddAttributes(Racker racker) {

        List<Attribute> atts = new ArrayList<Attribute>();
        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_RACKER_OBJECT_CLASS_VALUES));
        atts.add(new Attribute(ATTR_RACKER_ID, racker.getRackerId()));
        getLogger().debug("Adding Racker attribute {}", racker.getRackerId());
        return atts.toArray(new Attribute[0]);
    }

    User getSingleUser(Filter searchFilter, String[] searchAttributes) {
        User user = null;
        try {

            SearchResultEntry entry = this.getSingleEntry(USERS_BASE_DN, SearchScope.SUB, searchFilter, searchAttributes);

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

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    User getSingleSoftDeletedUser(Filter searchFilter,
        String[] searchAttributes) {
        User user = null;
        try {

            SearchResultEntry entry = this.getSingleEntry(SOFT_DELETED_USERS_BASE_DN, SearchScope.SUB, searchFilter, searchAttributes);

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

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    User getUser(SearchResultEntry resultEntry)
        throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();
        User user = new User();
        user.setId(resultEntry.getAttributeValue(ATTR_ID));
        user.setUniqueId(resultEntry.getDN());
        user.setUsername(resultEntry.getAttributeValue(ATTR_UID));
        user.setCountry(resultEntry.getAttributeValue(ATTR_C));
        user.setDisplayName(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_DISPLAY_NAME)));
        user.setFirstname(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_GIVEN_NAME)));
        user.setEmail(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_MAIL)));
        user.setMiddlename(resultEntry.getAttributeValue(ATTR_MIDDLE_NAME));
        user.setPreferredLang(resultEntry.getAttributeValue(ATTR_LANG));
        user.setCustomerId(resultEntry.getAttributeValue(ATTR_RACKSPACE_CUSTOMER_NUMBER));
        user.setPersonId(resultEntry.getAttributeValue(ATTR_RACKSPACE_PERSON_NUMBER));
        user.setApiKey(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_RACKSPACE_API_KEY)));
        user.setSecretQuestion(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_PASSWORD_SECRET_Q)));
        user.setSecretAnswer(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_PASSWORD_SECRET_A)));
        user.setSecretQuestionId(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_PASSWORD_SECRET_Q_ID)));
        user.setLastname(cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_SN)));
        user.setTimeZoneObj(DateTimeZone.forID(resultEntry.getAttributeValue(ATTR_TIME_ZONE)));
        user.setDomainId(resultEntry.getAttributeValue(ATTR_DOMAIN_ID));

        user.setInMigration(resultEntry.getAttributeValueAsBoolean(ATTR_IN_MIGRATION));
        if(user.getInMigration() != null) {
            user.setMigrationDate(new DateTime(resultEntry.getAttributeValueAsDate(ATTR_MIGRATION_DATE)));
        }

        String ecryptedPwd = cryptHelper.decrypt(resultEntry.getAttributeValueBytes(ATTR_CLEAR_PASSWORD));
        Date lastUpdates = resultEntry.getAttributeValueAsDate(ATTR_PASSWORD_UPDATED_TIMESTAMP);
        boolean wasSelfUpdated = false;
        if(resultEntry.getAttributeValueAsBoolean(ATTR_PASSWORD_SELF_UPDATED) != null){
            wasSelfUpdated = resultEntry.getAttributeValueAsBoolean(ATTR_PASSWORD_SELF_UPDATED);
        }
        DateTime lasteUpdated = new DateTime(lastUpdates);
        Password pwd = Password.existingInstance(ecryptedPwd, lasteUpdated, wasSelfUpdated);
        user.setPasswordObj(pwd);

        user.setRegion(resultEntry.getAttributeValue(ATTR_RACKSPACE_REGION));

        String softDeletedTimestamp = resultEntry.getAttributeValue(ATTR_SOFT_DELETED_DATE);
        if (softDeletedTimestamp != null) {
            user.setSoftDeletedTimestamp(new DateTime(resultEntry.getAttributeValueAsDate(ATTR_SOFT_DELETED_DATE)));
        }

        user.setEnabled(resultEntry.getAttributeValueAsBoolean(ATTR_ENABLED));

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

        user.setSecureId(resultEntry.getAttributeValue(ATTR_SECURE_ID));

        getLogger().debug("Built user object {}.", user);
        return user;
    }

    UserAuthenticationResult validateUserStatus(User user, boolean isAuthenticated) {
        if (isAuthenticated && user.isDisabled()) {
            getLogger().error(user.getUsername());
            throw new UserDisabledException("User '" + user.getUsername() +"' is disabled.");
        }
        getLogger().debug("User {} authenticated == {}", user.getUsername(), isAuthenticated);
        return new UserAuthenticationResult(user, isAuthenticated);
    }

    List<Modification> getModifications(User uOld, User uNew, boolean isSelfUpdate) throws GeneralSecurityException, InvalidCipherTextException {
        CryptHelper cryptHelper = CryptHelper.getInstance();
        List<Modification> mods = new ArrayList<Modification>();

        checkForPasswordModification(uOld, uNew, isSelfUpdate, cryptHelper, mods);
        checkForCustomerIdModfication(uOld, uNew, mods);
        checkForCountryModification(uOld, uNew, mods);
        checkForDisplayNameModification(uOld, uNew, cryptHelper, mods);
        checkForSecureIdModification(uOld, uNew, mods);
        checkForFirstNameModification(uOld, uNew, cryptHelper, mods);
        checkForEmailModification(uOld, uNew, cryptHelper, mods);
        checkForMiddleNameModification(uOld, uNew, mods);
        checkForApiKeyModification(uOld, uNew, cryptHelper, mods);
        checkForSecretAnswerModification(uOld, uNew, cryptHelper, mods);
        checkForSecretQuestionModification(uOld, uNew, cryptHelper, mods);
        checkForSecretQuestionIdModification(uOld, uNew, cryptHelper, mods);
        checkForLastNameModification(uOld, uNew, cryptHelper, mods);
        checkForRegionModification(uOld, uNew, mods);
        checkForPersonIdModification(uOld, uNew, mods);
        checkForLocaleModification(uOld, uNew, mods);
        checkForTimeZoneModification(uOld, uNew, mods);
        checkForEnabledStatusModification(uOld, uNew, mods);
        checkForNastIdModification(uOld, uNew, mods);
        checkForMossoIdModification(uOld, uNew, mods);
        checkForMigrationStatusModification(uOld, uNew, mods);
        checkForUserNameModification(uOld, uNew, mods);
        checkForDomainModification(uOld, uNew, mods);

        getLogger().debug("Found {} mods.", mods.size());

        return mods;
    }

    void checkForUserNameModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getUsername() != null) {
            if (StringUtils.isBlank(uNew.getUsername())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_UID));
            } else if (!StringUtils.equals(uOld.getUsername(), uNew.getUsername())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_UID, uNew.getUsername()));
            }
        }
    }

    void checkForDomainModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getDomainId() != null) {
            if (!StringUtils.equals(uOld.getDomainId(), uNew.getDomainId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_DOMAIN_ID, uNew.getDomainId()));
            }
        }
    }

    void checkForMossoIdModification(User uOld, User uNew, List<Modification> mods) {
        // To delete the attribute MossoId a negative value for the mossoId
        // is sent in.
        if (uNew.getMossoId() != null) {
            if (uNew.getMossoId() < 0) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MOSSO_ID));
            } else if (!uNew.getMossoId().equals(uOld.getMossoId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MOSSO_ID, String.valueOf(uNew.getMossoId())));
            }
        }
    }

    void checkForNastIdModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getNastId() != null) {
            if (StringUtils.isBlank(uNew.getNastId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_NAST_ID));
            } else if (!uNew.getNastId().equals(uOld.getNastId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_NAST_ID, uNew.getNastId()));
            }
        }
    }

    void checkForEnabledStatusModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.isEnabled() != null && uNew.isEnabled() != uOld.isEnabled()) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(uNew.isEnabled()).toUpperCase()));
        }
    }

    void checkForTimeZoneModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getTimeZoneObj() != null && !uNew.getTimeZone().equals(uOld.getTimeZone())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_TIME_ZONE, uNew.getTimeZone()));
        }
    }

    void checkForLocaleModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getLocale() != null && !uNew.getPreferredLang().equals(uOld.getPreferredLang())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_LANG, uNew.getPreferredLang().toString()));
        }
    }

    void checkForPersonIdModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getPersonId() != null) {
            if (StringUtils.isBlank(uNew.getPersonId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_PERSON_NUMBER));
            } else if (!uNew.getPersonId().equals(uOld.getPersonId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_PERSON_NUMBER, uNew.getPersonId()));
            }
        }
    }

    void checkForRegionModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getRegion() != null) {
            if (StringUtils.isBlank(uNew.getRegion())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_REGION));
            } else if (!uNew.getRegion().equals(uOld.getRegion())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_REGION, uNew.getRegion()));
            }
        }
    }

    void checkForLastNameModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getLastname() != null) {
            if (StringUtils.isBlank(uNew.getLastname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SN));
            } else if (!StringUtils.equals(uOld.getLastname(), uNew.getLastname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_SN, cryptHelper.encrypt(uNew.getLastname())));
            }
        }
    }

    void checkForSecretQuestionModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getSecretQuestion() != null) {
            if (StringUtils.isBlank(uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_PASSWORD_SECRET_Q));
            } else if (!StringUtils.equals(uOld.getSecretQuestion(), uNew.getSecretQuestion())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SECRET_Q, cryptHelper.encrypt(uNew.getSecretQuestion())));
            }
        }
    }

    void checkForSecretQuestionIdModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getSecretQuestionId() != null) {
            if (StringUtils.isBlank(uNew.getSecretQuestionId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_PASSWORD_SECRET_Q_ID));
            } else if (!StringUtils.equals(uOld.getSecretQuestionId(), uNew.getSecretQuestionId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SECRET_Q_ID, cryptHelper.encrypt(uNew.getSecretQuestionId())));
            }
        }
    }

    void checkForSecretAnswerModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getSecretAnswer() != null) {
            if (StringUtils.isBlank(uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_PASSWORD_SECRET_A));
            } else if (!StringUtils.equals(uOld.getSecretAnswer(), uNew.getSecretAnswer())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SECRET_A, cryptHelper.encrypt(uNew.getSecretAnswer())));
            }
        }
    }

    void checkForApiKeyModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getApiKey() != null) {
            if (StringUtils.isBlank(uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_API_KEY));
            } else if (!StringUtils.equals(uOld.getApiKey(), uNew.getApiKey())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_API_KEY, cryptHelper.encrypt(uNew.getApiKey())));
            }
        }
    }

    void checkForMiddleNameModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getMiddlename() != null) {
            if (StringUtils.isBlank(uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MIDDLE_NAME));
            } else if (!StringUtils.equals(uOld.getMiddlename(),
                uNew.getMiddlename())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MIDDLE_NAME, uNew.getMiddlename()));
            }
        }
    }

    void checkForEmailModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getEmail() != null) {
            if (StringUtils.isBlank(uNew.getEmail())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_MAIL));
            } else if (!StringUtils.equals(uOld.getEmail(), uNew.getEmail())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_MAIL, cryptHelper.encrypt(uNew.getEmail())));
            }
        }
    }

    void checkForFirstNameModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getFirstname() != null) {
            if (StringUtils.isBlank(uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_GIVEN_NAME));
            } else if (!StringUtils.equals(uOld.getFirstname(), uNew.getFirstname())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_GIVEN_NAME, cryptHelper.encrypt(uNew.getFirstname())));
            }
        }
    }

    void checkForSecureIdModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getSecureId() != null) {
            if (StringUtils.isBlank(uNew.getSecureId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_SECURE_ID));
            } else if (!StringUtils.equals(uOld.getSecureId(), uNew.getSecureId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_SECURE_ID, uNew.getSecureId()));
            }
        }
    }

    void checkForDisplayNameModification(User uOld, User uNew, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        if (uNew.getDisplayName() != null) {
            if (StringUtils.isBlank(uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_DISPLAY_NAME));
            } else if (!StringUtils.equals(uOld.getDisplayName(),
                uNew.getDisplayName())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_DISPLAY_NAME, cryptHelper.encrypt(uNew.getDisplayName())));
            }
        }
    }

    void checkForCountryModification(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getCountry() != null) {
            if (StringUtils.isBlank(uNew.getCountry())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_C));
            } else if (!StringUtils.equals(uOld.getCountry(), uNew.getCountry())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_C, uNew.getCountry()));
            }
        }
    }

    void checkForCustomerIdModfication(User uOld, User uNew, List<Modification> mods) {
        if (uNew.getCustomerId() != null) {
            if (StringUtils.isBlank(uNew.getCustomerId())) {
                mods.add(new Modification(ModificationType.DELETE, ATTR_RACKSPACE_CUSTOMER_NUMBER));
            } else if (!StringUtils.equals(uOld.getCustomerId(),
                uNew.getCustomerId())) {
                mods.add(new Modification(ModificationType.REPLACE, ATTR_RACKSPACE_CUSTOMER_NUMBER, uNew.getCustomerId()));
            }
        }
    }

    void checkForPasswordModification(User uOld, User uNew, boolean isSelfUpdate, CryptHelper cryptHelper, List<Modification> mods) throws GeneralSecurityException, InvalidCipherTextException {
        DateTime currentTime = new DateTime();
        if (uNew.getPasswordObj().isNew()) {
            if (isSelfUpdate) {
                Password oldPwd = uOld.getPasswordObj();
                int secsSinceLastChange = Seconds.secondsBetween(oldPwd.getLastUpdated(), currentTime).getSeconds();
                if (oldPwd.wasSelfUpdated()  && secsSinceLastChange < DateTimeConstants.SECONDS_PER_DAY) {
                    throw new PasswordSelfUpdateTooSoonException();
                }
            }

            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_SELF_UPDATED, Boolean.toString(isSelfUpdate).toUpperCase()));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD_UPDATED_TIMESTAMP, StaticUtils.encodeGeneralizedTime(currentTime.toDate())));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_PASSWORD, uNew.getPasswordObj().getValue()));
            mods.add(new Modification(ModificationType.REPLACE, ATTR_CLEAR_PASSWORD, cryptHelper.encrypt(uNew.getPasswordObj().getValue())));
        }
    }

    void checkForMigrationStatusModification(User uOld, User uNew, List<Modification> mods){
        if(uNew.getInMigration() != null) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_IN_MIGRATION, String.valueOf(uNew.getInMigration())));
        }
    }

    @Override
    public String getNextUserId() {
        return getNextId(NEXT_USER_ID);
    }

    @Override
    public void softDeleteUser(User user) {
        getLogger().info("SoftDeleting user - {}", user.getUsername());
        try {
            String oldDn = user.getUniqueId();
            String newRdn = ATTR_ID + "=" + user.getId();
            String newDn = new LdapDnBuilder(SOFT_DELETED_USERS_BASE_DN)
                .addAttribute(ATTR_ID, user.getId()).build();
            // Move the User
            getAppInterface().modifyDN(oldDn, newRdn, true, SOFT_DELETED_USERS_BASE_DN);
            user.setUniqueId(newDn);
            // Disabled the User
            getAppInterface().modify(user.getUniqueId(), new Modification(
                    ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(false).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting user", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted user - {}", user.getUsername());
    }

    @Override
    public User getSoftDeletedUserById(String id) {

        getLogger().debug("Doing search for id " + id);
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            getLogger().info("Invalid id parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleSoftDeletedUser(searchFilter,
            ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public User getSoftDeletedUserByUsername(String username) {

        getLogger().debug("Doing search for user " + username);
        if (StringUtils.isBlank(username)) {
            getLogger().error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            getLogger().info("Invalid username parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_UID, username)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
            .build();

        User user = getSingleSoftDeletedUser(searchFilter,
            ATTR_USER_SEARCH_ATTRIBUTES);

        getLogger().debug(FOUND_USER, user);

        return user;
    }

    @Override
    public void unSoftDeleteUser(User user) {
        getLogger().info("unSoftDeleting user - {}", user.getUsername());
        try {
            String oldDn = user.getUniqueId();
            String newRdn = ATTR_ID + "=" + user.getId();
            String newDn = new LdapDnBuilder(USERS_BASE_DN)
            .addAttribute(ATTR_ID, user.getId()).build();
            // Modify the User
            getAppInterface().modifyDN(oldDn, newRdn, false, USERS_BASE_DN);
            user.setUniqueId(newDn);
            // Enabled the User
            getAppInterface().modify(user.getUniqueId(), new Modification(
                    ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(true).toUpperCase()));
        } catch (LDAPException e) {
            getLogger().error("Error unsoft deleting user", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("unSoftDeleted user - {}", user.getUsername());
    }

    @Override
    public User getUserByDn(String userDn) {
        getLogger().info("getting user - {}", userDn);
        try {
            SearchResultEntry entry = getEntryByDn(userDn);
            return getUser(entry);
        } catch (GeneralSecurityException e) {
            getLogger().error("Encryption error", e);
            throw new IllegalStateException(e);
        } catch (InvalidCipherTextException e) {
            getLogger().error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }
}
