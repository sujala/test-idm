package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.util.HashHelper;
import com.rackspace.idm.validation.Validator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultUserService implements UserService {
    public static final String GETTING_USER = "Getting User: {}";
    public static final String GOT_USER = "Got User: {}";
    public static final String ENCRYPTION_VERSION_ID = "encryptionVersionId";
    private static final String DELETE_USER_LOG_NAME = "userDelete";
    private static final String DELETE_USER_FORMAT = "DELETED username={},domainId={},roles={}";
    private static final String MOSSO_TENANT_PREFIX = "MossoCloudFS_";
    private static final String MOSSO_BASE_URL_TYPE = "MOSSO";
    private static final String NAST_BASE_URL_TYPE = "NAST";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Logger deleteUserLogger = LoggerFactory.getLogger(DELETE_USER_LOG_NAME);

    @Autowired
    private PasswordComplexityService passwordComplexityService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private AuthDao authDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RackerDao rackerDao;

    @Autowired
    private FederatedUserDao federatedUserDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CloudRegionService cloudRegionService;

    @Autowired
    private Validator validator;

    @Autowired
    private DomainService domainService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private CryptHelper cryptHelper;

    @Autowired
    private ApplicationRoleDao roleDao;

    @Override
    public void addRacker(Racker racker) {
        logger.info("Adding Racker {}", racker);
        Racker exists = this.rackerDao.getRackerByRackerId(racker.getRackerId());
        if (exists != null) {
            throw new DuplicateException("Racker Already Exists");
        }
        this.rackerDao.addRacker(racker);
        logger.info("Added Racker {}", racker);
    }

//    @Override
//    public void addUser(User user) {
//        logger.info("Adding User: {}", user);
//        if(!validator.isEmpty(user.getEmail())){
//            validator.isEmailValid(user.getEmail());
//        }
//        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
//        user.setSalt(cryptHelper.generateSalt());
//
//        validateUsername(user);
//        setPasswordIfNecessary(user);
//
//        if (user.getRegion() == null) {
//            Region region = cloudRegionService.getDefaultRegion(config.getString("cloud.region"));
//            if (region == null) {
//                throw new BadRequestException("default cloud region was not found");
//            }
//
//            user.setRegion(region.getName());
//        }
//
//        if (user.getEnabled() == null) {
//            user.setEnabled(user.getEnabled());
//        }
//
//        user.setId(generateUniqueId());
//
//        userDao.addUser(user);
//        logger.info("Added User: {}", user);
//
//        //Every user by default has the idm application provisioned for them
//        logger.info("Adding User Scope Access for Idm to user {}", user);
//        UserScopeAccess usa = scopeAccessService.createInstanceOfUserScopeAccess(user, getIdmClientId(), getRackspaceCustomerId());
//
//        this.scopeAccessService.addUserScopeAccess(user, usa);
//
//        //Every user by default has the cloud auth application provisioned for them
//        UserScopeAccess cloudUsa = scopeAccessService.createInstanceOfUserScopeAccess(user, getCloudAuthClientId(), getRackspaceCustomerId());
//
//        this.scopeAccessService.addUserScopeAccess(user, cloudUsa);
//
//        logger.info("Added User Scope Access for Idm to user {}", user);
//    }

    @Override
    public void addUser(User user) {
        logger.info("Adding User: {}", user);

        validator.validateUser(user);

        createDomainIfItDoesNotExist(user.getDomainId());
        createDefaultDomainTenantsIfNecessary(user.getDomainId(), user.getRegion());
        checkMaxNumberOfUsersInDomain(user.getDomainId());

        setPasswordIfNotProvided(user);
        setApiKeyIfNotProvided(user);
        setRegionIfNotProvided(user);

        //hack alert!! code requires the user object to have the nastid attribute set. this attribute
        //should no longer be required as users have roles on a tenant instead. once this happens, remove
        //TODO: figure this out
        user.setNastId(getNastTenantId(user.getDomainId()));
        user.setId(userDao.getNextUserId());
        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());
        user.setEnabled(user.getEnabled() == null ? true : user.getEnabled());

        userDao.addUser(user);

        assignUserRoles(user);
    }

    /**
     * sets default parameters on the user e.g domain id, roles, etc based on
     * characteristics of the calling user.
     *
     * @param user
     * @param caller
     */
    @Override
    public void setUserDefaultsBasedOnCaller(User user, User caller) {
        //
        // Based on the caller making the call, apply the following rules to the user being created.
        // We will revisit these rules later on, so they are more simplified. Don't want to change for now.
        // Also logic to determine the type of caller should be defined, when a user could have multiple conflicting roles.
        // e.g if you have identity:admin and identity:user-admin, you automatically are an identity:admin.
        // This doesn't happen today because there is business logic to ensure these cases don't happen.
        // We want to eventually remove that. A user should be able to be assigned any role.
        //
        if (doesUserHaveRole(caller, getSuperUserAdminRole())) {
            if (StringUtils.isNotBlank(user.getDomainId())) {
                throw new BadRequestException("Identity-admin cannot be created with a domain");
            }

            attachRoleToUser(getIdentityAdminRole(), user);
        }

        if (doesUserHaveRole(caller, getIdentityAdminRole())) {
            if (StringUtils.isBlank(user.getDomainId())) {
                throw new BadRequestException("User-admin cannot be created without a domain");
            }

            attachRoleToUser(getUserAdminRole(), user);

            //original code had this. this is in place to help ensure the user has access to their
            //default tenants. currently the user-admin role is not tenant specific. don't want to
            //change existing behavior. Need to have business discussion to determine if a user
            //has a non tenant specific role, whether they have access to all tenants in domain.
            //if this turns out to be the case, then we need to change validateToken logic.
            attachRoleToUser(getComputeDefaultRole(), user, user.getDomainId());
            attachRoleToUser(getObjectStoreDefaultRole(), user, getNastTenantId(user.getDomainId()));
        }

        if (doesUserHaveRole(caller, getUserAdminRole()) || doesUserHaveRole(caller, getUserManageRole())) {
            String callerDefaultRegion = getCallerRegion(caller);
            List<ClientRole> callerRoles = getAssignableCallerRoles(caller);
            Iterable<Group> callerGroups = getGroupsForUser(caller.getId());

            user.setMossoId(caller.getMossoId());
            user.setNastId(caller.getNastId());
            user.setDomainId(caller.getDomainId());
            user.setRegion(callerDefaultRegion);

            attachRoleToUser(getDefaultRole(), user);
            attachRolesToUser(callerRoles, user);
            attachGroupsToUser(callerGroups, user);
        }
    }

    //TODO: consider removing this method. Just here so code doesn't break
    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        logger.debug("Authenticating User: {} by Username", username);
        UserAuthenticationResult authenticated = userDao.authenticate(username, password);
        validateUserStatus(authenticated);
        logger.debug("Authenticated User: {} by Username - {}", username,
                authenticated);
        return authenticated;
    }

    @Override
    public UserAuthenticationResult authenticateWithApiKey(String username,
                                                           String apiKey) {
        logger.debug("Authenticating User: {} by API Key", username);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(username, apiKey);
        validateUserStatus(authenticated);
        logger.debug("Authenticated User: {} by API Key - {}", username,
                authenticated);
        return authenticated;
    }

    @Override
    public User loadUser(String customerId, String username) {
        User user = this.getUser(customerId, username);
        if (user == null) {
            String errorMsg = String.format("User not found: %s - %s",
                    customerId, username);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return user;
    }

    @Override
    public User loadUser(String userId) {
        User user = this.getUserById(userId);
        if (user == null) {
            String errorMsg = String.format("User not found with id: %s", userId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return user;
    }

//    @Override
//    public void deleteRacker(String rackerId) {
//        logger.info("Deleting Racker: {}", rackerId);
//
//        this.rackerDao.deleteRacker(rackerId);
//
//        logger.info("Deleted Racker: {}", rackerId);
//    }

    @Override
    public void deleteUser(User user) {
        logger.info("Deleting User: {}", user.getUsername());

        List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);
        this.userDao.deleteUser(user);
        deleteUserLogger.warn(DELETE_USER_FORMAT,
                new Object[] {user.getUsername(), user.getDomainId(), roles.toString()});

        logger.info("Deleted User: {}", user.getUsername());
    }

    @Override
    public void deleteUser(String username) {
        User user = this.userDao.getUserByUsername(username);
        this.deleteUser(user);
    }

    @Override
    public String generateApiKey() {
            return HashHelper.getRandomSha1();
    }

    @Override
    public Iterable<User> getUsersByRCN(String RCN) {
        logger.debug("Getting All Users");

        return this.userDao.getUsersByRCN(RCN);
    }

    @Override
    public Iterable<User> getUsersByUsername(String username) {
        logger.debug("Getting All Users");

        return this.userDao.getUsersByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        //TODO Paginiation
        logger.debug("Getting All Users");

        PaginatorContext<User> users = this.userDao.getUsers(getLdapPagingOffsetDefault(), getLdapPagingLimitDefault());

        logger.debug("Got All Users {}");

        return users.getValueList();
    }

    @Override
    public Iterable<User> getUsersWithDomainAndEnabledFlag(String domainId, Boolean enabled) {
        logger.debug("Getting All Users: {} - {}", domainId, enabled);

        return this.userDao.getUsersByDomainAndEnabledFlag(domainId, enabled);
    }

    @Override
    public Iterable<User> getUsersByGroupId(String groupId) {
        logger.debug("Getting All Users: {} - {}", groupId);

        return this.userDao.getUsersByGroupId(groupId);
    }


    @Override
    public Racker getRackerByRackerId(String rackerId) {
        logger.debug("Getting Racker: {}", rackerId);
        Racker racker = rackerDao.getRackerByRackerId(rackerId);
        logger.debug("Got Racker: {}", racker);
        return racker;
    }

    @Override
    public List<String> getRackerRoles(String rackerId) {
        logger.debug("Getting Roles for Racker: {}", rackerId);

        if (!isTrustedServer()) {
            throw new ForbiddenException();
        }

        List<String> roles = authDao.getRackerRoles(rackerId);
        logger.debug("Got Roles for Racker: {}", rackerId);
        return roles;
    }


    @Override
    public User getUser(String username) {
        logger.debug(GETTING_USER, username);
        User user = userDao.getUserByUsername(username);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public Iterable<User> getUsersByEmail(String email) {
        logger.debug(GETTING_USER, email);
        return userDao.getUsersByEmail(email);
    }

    public User getUserByAuthToken(String authToken) {
        if (authToken == null) {
            return null;
        }
        ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
        if(scopeAccessByAccessToken == null) {
            return null;
        }

        String uid = scopeAccessService.getUserIdForParent(scopeAccessByAccessToken);
        return getUser(uid);
    }


    @Override
    public User getUser(String customerId, String username) {
        logger.debug("Getting User: {} - {}", customerId, username);
        User user = userDao.getUserByCustomerIdAndUsername(customerId, username);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public Iterable<User> getUsersByTenantId(String tenantId) {
        logger.debug("Get list of users with tenant", tenantId);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForTenant(tenantId);
        List<String> idList = new ArrayList<String>();
        for(TenantRole t : tenantRoles){
            if(t.getUserId() == null){
                tenantService.addUserIdToTenantRole(t);
            }
            idList.add(t.getUserId());
        }
        return userDao.getUsers(idList);
    }

    @Override
    public User getUserByTenantId(String tenantId) {
        logger.debug("Getting user by tenantId: {}", tenantId);
        Iterable<User> users = getUsersByTenantId(tenantId);

        User result = null;
        for (User user : users) {
            if (result == null) {
                result = user;
            }
            if (authorizationService.hasUserAdminRole(user)) {
                result = user;
                break;
            }
        }

        return result;
    }

    @Override
    public User getSoftDeletedUser(String id) {
        logger.debug(GETTING_USER, id);
        User user = userDao.getSoftDeletedUserById(id);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public int getUserWeight(User user, String applicationId) {
        List<TenantRole> tenantRoles = tenantService.getGlobalRolesForUser(user, applicationId);
        for (TenantRole tenantRole : tenantRoles) {
            ClientRole clientRole = applicationService.getClientRoleById(tenantRole.getRoleRsId());
            if (StringUtils.startsWithIgnoreCase(clientRole.getName(), "identity:")) {
                return clientRole.getRsWeight();
            }
        }
        return getDefaultUserWeight();
    }

    private int getDefaultUserWeight() {
        String clientId = config.getString("cloudAuth.clientId");
        String roleName = config.getString("cloudAuth.userRole");
        ClientRole defaultUserRole = applicationService.getClientRoleByClientIdAndRoleName(clientId, roleName);
        return defaultUserRole.getRsWeight();
    }

    @Override
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public User getUserByUsernameForAuthentication(String username) {
        User user = null;
        try {
            user = checkAndGetUserByName(username);
        } catch (NotFoundException e) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage, e);
        }
        return user;
    }

    @Override
    public User checkAndGetUserByName(String username) {
        User user = userDao.getUserByUsername(username);

        if (user == null) {
            String errMsg = String.format("User '%s' not found.", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public Applications getUserApplications(User user) {
        if (user == null || user.getUniqueId() == null) {
            String errmsg = "Null User instance or is lacking uniqueID";
            logger.debug(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        List<Application> clientList = new ArrayList<Application>();

        for (ScopeAccess service : scopeAccessService.getScopeAccessesForUser(user)) {
            if (service instanceof UserScopeAccess) {
                clientList
                        .add(this.applicationService.getById(service.getClientId()));
            }
        }

        Applications clients = new Applications();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());
        logger.debug("Found Clients: {}.", clients);

        return clients;
    }


    @Override
    public boolean isUsernameUnique(String username) {
        return userDao.isUsernameUnique(username);
    }

    @Override
    public List<User> getSubUsers(User user) {
    	List<User> result = new ArrayList<User>();
    	
        if (user != null) {
            for (User subUser : userDao.getUsersByDomain(user.getDomainId())) {
                if (!subUser.getId().equalsIgnoreCase(user.getId())){
                    result.add(subUser);
                }
            }
        }

        return result;
    }

    @Override
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void setConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public void setPasswordComplexityService(PasswordComplexityService passwordComplexityService) {
        this.passwordComplexityService = passwordComplexityService;
    }

    @Override
    public void setCloudRegionService(CloudRegionService cloudRegionService) {
        this.cloudRegionService = cloudRegionService;
    }

    @Override
    public PaginatorContext<User> getAllUsersPaged(int offset, int limit) {
        logger.debug("Getting All Users paged");

        PaginatorContext<User> users = this.userDao.getUsers(offset, limit);

        logger.debug("Got All Users paged");

        return users;
    }

    @Override
    public PaginatorContext<User> getAllEnabledUsersPaged(int offset, int limit) {
        logger.debug("Getting All Enabled Users paged");

        PaginatorContext<User> users = this.userDao.getEnabledUsers(offset, limit);

        logger.debug("Got All Enabled Users paged");

        return users;
    }

    @Override
    public PaginatorContext<User> getAllUsersPagedWithDomain(String domainId, int offset, int limit) {
        logger.debug("Getting Users in Domain {}", domainId);

        PaginatorContext<User> users = this.userDao.getUsersByDomain(domainId, offset, limit);

        logger.debug("Got Users in Domain");

        return users;
    }

    @Override
    public PaginatorContext<User> getUsersWithRole(String roleId, int offset, int limit) {
        logger.debug("Getting All Users with role {}", roleId);

        List<String> userIds = tenantService.getIdsForUsersWithTenantRole(roleId);

        List<User> users = new ArrayList<User>();
        for (User user : this.userDao.getUsers(userIds)) {
            users.add(user);
        }

        PaginatorContext<User> context = new PaginatorContext<User>();
        context.update(users, offset, limit);

        logger.debug("Got Users with role");

        return context;
    }

    @Override
    public PaginatorContext<User> getUsersWithDomainAndRole(String domainId, String roleId, int offset, int limit) {
        logger.debug("Getting Users in Domain {}", domainId);


        List<User> users = new ArrayList<User>();
        for (User user : this.userDao.getUsersByDomain(domainId)) {
            users.add(user);
        }

        List<User> usersWithRole = filterUsersForRole(users, roleId);

        PaginatorContext<User> context = new PaginatorContext<User>();
        context.update(usersWithRole, offset, limit);

        logger.debug("Got Users in Domain");

        return context;
    }

    @Override
    public PaginatorContext<User> getUsersByGroupId(String groupId, int offset, int limit) {

        logger.debug("Getting Users in Group {}", groupId);

        PaginatorContext<User> context = userDao.getUsersByGroupId(groupId, offset, limit);

        logger.debug("Got All Users paged");

        return context;
    }

    @Override
    public boolean hasSubUsers(String userId) {
        User user = userDao.getUserById(userId);
        if (user == null) {
            return false;
        }
        Iterable<User> users = userDao.getUsersByDomain(user.getDomainId());
        if (!users.iterator().hasNext()) {
            return false;
        }
        for (User userInList : users) {
            if(authorizationService.hasDefaultUserRole(userInList)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Password resetUserPassword(User user) {
        Password newPassword = Password.generateRandom(false, user); // Would the user
        // ever reset his
        // own password?
        userDao.updateUser(user);
        logger.debug("Updated password for user: {}", user);

        return newPassword.toExisting();
    }

    boolean checkForPasswordUpdate(User user) {
        if(user != null && !StringUtils.isEmpty(user.getPassword())) {
            return true;
        }
        return false;
    }

    @Override
    public void updateUser(User user) throws IOException, JAXBException {
        logger.info("Updating User: {}", user);
        if(!validator.isBlank(user.getEmail())){
            validator.isEmailValid(user.getEmail());
        }
        // Expire all User tokens if we are updating the password field
        User currentUser = userDao.getUserById(user.getId());
        boolean userIsBeingDisabled= checkIfUserIsBeingDisabled(currentUser, user);

        user.setLdapEntry(currentUser.getLdapEntry());
        user.setEncryptionVersion(currentUser.getEncryptionVersion());
        user.setSalt(currentUser.getSalt());
        userDao.updateUser(user);

        if(checkForPasswordUpdate(user)){
            scopeAccessService.expireAllTokensForUser(user.getUsername());
        }

        if (userIsBeingDisabled) {
            scopeAccessService.expireAllTokensForUser(user.getUsername());
            disableUserAdminSubUsers(currentUser);
        }

        for (ScopeAccess scopeAccess : scopeAccessService.getScopeAccessListByUserId(user.getId())) {
            ((UserScopeAccess)scopeAccess).setUsername(user.getUsername());
            scopeAccessService.updateScopeAccess(scopeAccess);
        }
        logger.info("Updated User: {}", user);
    }

    private void disableUserAdminSubUsers(User user) throws IOException, JAXBException {
        if (authorizationService.hasUserAdminRole(user)) {
            List<User> enabledUserAdmins = domainService.getEnabledDomainAdmins(user.getDomainId());
            if (enabledUserAdmins.size() != 0) {
                return;
            }
            List<User> subUsers = getSubUsers(user);

            for (User subUser : subUsers) {
                if (subUser.getEnabled()) {
                    subUser.setEnabled(false);
                    userDao.updateUser(subUser);
                    scopeAccessService.expireAllTokensForUser(subUser.getUsername());
                }
            }
        }
    }

    private boolean checkIfUserIsBeingDisabled(User currentUser, User user) {
        if (currentUser != null && user != null && user.getEnabled() != null) {
            boolean currentUserEnabled = currentUser.getEnabled();
            boolean userEnabled = user.getEnabled();

            return !userEnabled && userEnabled != currentUserEnabled;
        }
        return false;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

//    private void validateUsername(User user) {
//        boolean isUsernameUnique = userDao.isUsernameUnique(user.getUsername());
//        if (!isUsernameUnique) {
//            logger.warn("Couldn't add user {} because username already taken", user);
//            throw new DuplicateUsernameException("Username unavailable within Rackspace system. Please try another.");
//        }
//    }

    @Override
    public User getUserById(String id) {
        logger.debug(GETTING_USER, id);
        User user = userDao.getUserById(id);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public User checkAndGetUserById(String id) {
        User user = getUserById(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public void softDeleteUser(User user) throws IOException, JAXBException {
        logger.debug("SoftDeleting User: {}", user);
        scopeAccessService.expireAllTokensForUserById(user.getId());
        userDao.softDeleteUser(user);
        logger.debug("SoftDeleted User: {}", user);
    }

//    private void setPasswordIfNecessary(User user) {
//        String password = user.getPassword();
//
//        if (!StringUtils.isEmpty(user.getPassword())) {
//            checkPasswordComplexity(password);
//        } else {
//            // False, since a user wouldn't add himself
//            Password newpassword = Password.generateRandom(false, user);
//        }
//
//        if (!user.isPasswordIsNew()) {
//            logger.error("Password of User is an existing instance");
//            throw new IllegalArgumentException(
//                    "The password appears to be an existing instance. It must be a new instance!");
//        }
//    }
//
//    private void checkPasswordComplexity(String password) {
//        if (isPasswordRulesEnforced()) {
//            PasswordComplexityResult result = passwordComplexityService.checkPassword(password);
//            if (!result.isValidPassword()) {
//                String errorMsg = String.format("Invalid password %s", password);
//                logger.warn(errorMsg);
//                throw new PasswordValidationException(errorMsg);
//            }
//        }
//    }

    @Override
    public BaseUser getUserByScopeAccess(ScopeAccess scopeAccess, boolean checkUserDisabled) {
        BaseUser user = null;
        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
            user = getRackerByRackerId((rackerScopeAccess.getRackerId()));
        }
        else if (scopeAccess instanceof FederatedToken) {
            //whenever a caller makes a request with a token, the code
            //attempts to get the user for the calling token, so it can
            //apply authorization rules, policies, etc
            //federated users are stored in a different space,
            //TODO: investigate encapsulating this in UserDao
            FederatedToken token = (FederatedToken) scopeAccess;
            user = federatedUserDao.getUserByToken(token);
        }
        else if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            if (impersonatedScopeAccess.getRackerId() != null) {
                Racker impersonatingRacker = getRackerByRackerId(impersonatedScopeAccess.getRackerId());
                impersonatingRacker.setRackerId(impersonatingRacker.getRackerId());
                impersonatingRacker.setUsername(impersonatingRacker.getUsername());
                user = impersonatingRacker;
                ((Racker)user).setEnabled(true);
            } else {
                user = getUser(impersonatedScopeAccess.getUsername());
            }
        } else if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess userScopeAccess = (UserScopeAccess) scopeAccess;
            user = getUser(userScopeAccess.getUsername());
        } else {
            throw new BadRequestException("Invalid getUserByScopeAccess, scopeAccess cannot provide information to get a user");
        }
        if (user == null) {
            throw new NotFoundException("User not found with scopeAccess: " + scopeAccess.toString());
        }
        if (checkUserDisabled && user.isDisabled()) {
            throw new NotFoundException("Token not found.");
        }
        return user;
    }

    @Override
    public BaseUser getUserByScopeAccess(ScopeAccess scopeAccess) {
        return getUserByScopeAccess(scopeAccess, true);
    }

    @Override
    public void addBaseUrlToUser(String baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = tenantService.getTenant(tenantId);

        // Check for existing BaseUrl
        if (tenant.getBaseUrlIds() != null && tenant.getBaseUrlIds().size() != 0) {
            for (String bId : tenant.getBaseUrlIds()) {
                if (bId.equals(String.valueOf(baseUrl.getBaseUrlId()))) {
                    throw new BadRequestException("BaseUrl already exists.");
                }
            }
        }

        tenant.getBaseUrlIds().add(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void removeBaseUrlFromUser(String baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = this.tenantService.getTenant(tenantId);
        tenant.getBaseUrlIds().remove(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void addGroupToUser(String groupId, String userId) {
        userDao.addGroupToUser(userId, groupId);
    }

    @Override
    public void deleteGroupFromUser(String groupId, String userId) {
        userDao.deleteGroupFromUser(groupId, userId);
    }

    @Override
    public Iterable<Group> getGroupsForUser(String userId) {
        return userDao.getGroupsForUser(userId);
    }

    @Override
	public boolean isUserInGroup(String userId, String groupId) {
        for (Group currentGroup : getGroupsForUser(userId)) {
            if (currentGroup.getGroupId().equals(groupId)) {
                return true;
            }
        }
        return false;
	}

    protected List<User> filterUsersForRole(List<User> users, String roleId) {
        List<User> result = new ArrayList<User>();
        for (User user : users) {
            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
            if (user.getRoles() != null) {
                roles.addAll(user.getRoles());
            }

            for (TenantRole tenantRole : roles) {
                if (tenantRole.getRoleRsId().equals(roleId)) {
                    result.add(user);
                    break;
                }
            }
        }
        return result;
    }

    private boolean isPasswordRulesEnforced() {
        return config.getBoolean("password.rules.enforced", true);
    }

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public Iterable<User> getUsersWithDomain(String domainId) {
        return this.userDao.getUsersByDomain(domainId);
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setDomainService(DomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public void reEncryptUsers() {
        int offset = 0;
        int limit = 50;

        PaginatorContext<User> context = userDao.getUsersToReEncrypt(offset, limit);

        while(offset < context.getTotalRecords()) {
            for (User user : context.getValueList()) {
                userDao.updateUserEncryption(user.getId());
            }

            offset += (limit - context.getValueList().size());
            context = userDao.getUsersToReEncrypt(offset, limit);
        }
    }

    @Override
    public void setCryptHelper(CryptHelper cryptHelper) {
        this.cryptHelper = cryptHelper;
    }

    @Override
    public void setPropertiesService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    void validateUserStatus(UserAuthenticationResult authenticated ) {
        User user = (User)authenticated.getUser();
        boolean isAuthenticated = authenticated.isAuthenticated();
        if (user != null && isAuthenticated) {
            if (user.isDisabled()) {
                logger.error(user.getUsername());
                throw new UserDisabledException("User '" + user.getUsername() +"' is disabled.");
            }
            if (user.getDomainId() != null) {
                Domain domain = domainService.getDomain(user.getDomainId());
                if (domain != null && !domain.getEnabled()) {
                    logger.error(user.getUsername());
                    throw new UserDisabledException("User '" + user.getUsername() +"' is disabled.");
                }
            }
            logger.debug("User {} authenticated == {}", user.getUsername(), isAuthenticated);
        }
    }

    private void setPasswordIfNotProvided(User user) {
        if (StringUtils.isBlank(user.getPassword())) {
            Password.generateRandom(false, user);
        }
    }

    private void setApiKeyIfNotProvided(User user) {
        if (StringUtils.isBlank(user.getApiKey()) && shouldGenerateApiKeyUserForCreate()) {
            user.setApiKey(UUID.randomUUID().toString().replaceAll("-", ""));
        }
    }

    private void setRegionIfNotProvided(User user) {
        if (StringUtils.isBlank(user.getRegion())) {
            Region region = cloudRegionService.getDefaultRegion(getCloudRegion());
            if (region == null) {
                throw new IllegalStateException("default cloud region not found for: " + getCloudRegion());
            }

            user.setRegion(region.getName());
        }
    }

    private void createDomainIfItDoesNotExist(String domainId) {
        if (StringUtils.isNotBlank(domainId)) {
            if (domainService.getDomain(domainId) == null) {
                domainService.createNewDomain(domainId);
            }
        }
    }

    /**
     * creates default tenants in thew new domain
     *
     * @param domainId
     * @param defaultRegion - used to determine the appropriate v1 defaults for tenant
     */
    private void createDefaultDomainTenantsIfNecessary(String domainId, String defaultRegion) {
        if (StringUtils.isNotBlank(domainId) && domainService.getDomainAdmins(domainId).size() == 0) {
            //for now the default mosso tenant id will be the domain id
            //for now we will create a nast tenant as well. This can be removed once tenant aliases is in place
            //no longer need to call nast xml rpc service, as cloud files will lazy provision the cloud containers.
            String mossoId = domainId;
            String nastId = getNastTenantId(domainId);

            createTenantForDomain(mossoId, domainId, getV1Defaults(MOSSO_BASE_URL_TYPE, defaultRegion));
            createTenantForDomain(nastId, domainId, getV1Defaults(NAST_BASE_URL_TYPE, defaultRegion));
        }
    }

    private void createTenantForDomain(String tenantId, String domainId, List<String> v1Defaults) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setName(tenantId);
        tenant.setDisplayName(tenantId);
        tenant.setEnabled(true);
        tenant.getV1Defaults().addAll(v1Defaults);
        tenantService.addTenant(tenant);
        domainService.addTenantToDomain(tenantId, domainId);
    }

    private List<String> getV1Defaults(String baseUrlType, String defaultRegion){
        List<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByBaseUrlType(baseUrlType);

        if (defaultRegion == null) {
            defaultRegion = config.getString("v1DefaultRegionDefault");
        }

        // replicating what the original code did. apparently it gets the v1 defaults (baseurl ids)
        // from a property file. rather than adding those values directly, it verifies that those
        // ids exist in the instance of identity running, before adding it
        List<String> v1Defaults = new ArrayList<String> ();
        for (CloudBaseUrl baseUrl : baseUrls) {
            if(doesBaseUrlBelongToRegion(baseUrl)) {
                System.out.println("baseUrl: " + baseUrl);
                System.out.println("baseUrl def: " + baseUrl.getDef());
                System.out.println("baseUrl region: " + baseUrl.getRegion());

                if (baseUrl.getDef() && defaultRegion.equals(baseUrl.getRegion())) {
                    System.out.println("baseurl id: " + baseUrl.getBaseUrlId());
                    v1Defaults.add(baseUrl.getBaseUrlId());
                }
            }
        }

        return v1Defaults;
    }

//    private List<Object> getV1DefaultList(String baseUrlType) {
//
//        if(MOSSO_BASE_URL_TYPE.equals(baseUrlType)) {
//           return config.getList("v1defaultMosso");
//        }
//
//        if(NAST_BASE_URL_TYPE.equals(baseUrlType)) {
//            return config.getList("v1defaultNast");
//        }
//
//        return new ArrayList<Object>();
//    }

//    private boolean doesBaseUrlBelongToRegion(CloudBaseUrl baseUrl){
//        //TODO: figure out why we are checking id number
//        if (baseUrl.getBaseUrlId() != null){
//            if(isUkCloudRegion() &&  Integer.parseInt(baseUrl.getBaseUrlId()) >= 1000){
//                return true;
//            }
//            if(!isUkCloudRegion() && Integer.parseInt(baseUrl.getBaseUrlId()) < 1000){
//                return true;
//            }
//        }
//        return false;
//    }

    private boolean doesBaseUrlBelongToRegion(CloudBaseUrl baseUrl){
        if (baseUrl.getBaseUrlId() != null){
            if(isUkCloudRegion() &&  "LON".equals(baseUrl.getRegion())){
                return true;
            }

            if(!isUkCloudRegion() && !"LON".equals(baseUrl.getRegion())){
                return true;
            }
        }

        return false;
    }

    private boolean isUkCloudRegion() {
        return "UK".equalsIgnoreCase(config.getString("cloud.region"));
    }

    private void assignUserRoles(User user) {
        for (TenantRole role : user.getRoles()) {
            ClientRole roleObj = roleDao.getRoleByName(role.getName());

            TenantRole tenantRole = new TenantRole();
            tenantRole.setRoleRsId(roleObj.getId());
            tenantRole.setClientId(roleObj.getClientId());
            tenantRole.setName(roleObj.getName());
            tenantRole.setUserId(user.getId());
            tenantRole.getTenantIds().addAll(role.getTenantIds());

            tenantService.addTenantRoleToUser(user, tenantRole);
        }
    }

    private void checkMaxNumberOfUsersInDomain(String domainId) {
        Iterable<User> users = getUsersWithDomain(domainId);
        int numUsers = 0;
        int maxNumberOfUsersInDomain = getMaxNumberOfUsersInDomain();

        for (Iterator i = users.iterator(); i.hasNext();) {
            i.next();
            numUsers++;

            if (numUsers >= maxNumberOfUsersInDomain) {
                String errMsg = String.format("User cannot create more than %d users in an account.", maxNumberOfUsersInDomain);
                throw new BadRequestException(errMsg);
            }
        }
    }

    private Boolean doesUserHaveRole(User user, ClientRole role) {
        return tenantService.doesUserContainTenantRole(user, role.getId());
    }

    private String getNastTenantId(String domainId)  {
        return StringUtils.isNotBlank(domainId) ? MOSSO_TENANT_PREFIX + domainId : null;
    }

    private int getMaxNumberOfUsersInDomain() {
        return config.getInt("maxNumberOfUsersInDomain");
    }

    private Boolean shouldGenerateApiKeyUserForCreate(){
        return config.getBoolean("generate.apiKey.userForCreate");
    }

    private String getCloudRegion() {
        return config.getString("cloud.region");
    }

    private ClientRole getSuperUserAdminRole() {
        String roleName = config.getString("cloudAuth.serviceAdminRole");
        return applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), roleName);
    }

    private ClientRole getUserAdminRole() {
        String roleName = config.getString("cloudAuth.userAdminRole");
        return applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), roleName);
    }

    private ClientRole getUserManageRole() {
        String roleName = config.getString("cloudAuth.userManagedRole");
        return applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), roleName);
    }

    private ClientRole getIdentityAdminRole() {
        String roleName = config.getString("cloudAuth.adminRole");
        return applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), roleName);
    }

    private ClientRole getDefaultRole() {
        String roleName = config.getString("cloudAuth.userRole");
        return applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), roleName);
    }

    private ClientRole getComputeDefaultRole() {
        String serviceName = config.getString("serviceName.cloudServers");
        Application application = applicationService.getByName(serviceName);
        String defaultRoleName = application.getOpenStackType().concat(":default");
        return applicationService.getClientRoleByClientIdAndRoleName(application.getClientId(), defaultRoleName);
    }

    private ClientRole getObjectStoreDefaultRole() {
        String serviceName = config.getString("serviceName.cloudFiles");
        Application application = applicationService.getByName(serviceName);
        String defaultRoleName = application.getOpenStackType().concat(":default");
        return applicationService.getClientRoleByClientIdAndRoleName(application.getClientId(), defaultRoleName);
    }

    private void attachRolesToUser(List<ClientRole> roles, User user) {
        for (ClientRole role : roles) {
            attachRoleToUser(role, user);
        }
    }

    private void attachGroupsToUser(Iterable<Group> groups, User user) {
        for (Group group : groups) {
            user.getRsGroupId().add(group.getGroupId());
        }
    }

    private void attachRoleToUser(ClientRole role, User user) {
        attachRoleToUser(role, user, null);
    }

    private void attachRoleToUser(ClientRole role, User user, String tenantId) {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName(role.getName());
        tenantRole.getTenantIds().add(tenantId);
        user.getRoles().add(tenantRole);
    }

    /**
     * Gets all the roles from the caller that can be assignable to users that the caller creates.
     * This is temporary. Roles that a user gets by default should not be based on the caller. It
     * will be based on some sort of domain template.
     */
    private List<ClientRole> getAssignableCallerRoles(User caller) {
        List<ClientRole> clientRoles = new ArrayList<ClientRole> ();

        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(caller);
        for (TenantRole tenantRole : tenantRoles) {
            if (isExcludedAssignableCallerRole(tenantRole) && tenantRole.getPropagate()) {
                ClientRole role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), tenantRole.getName());
                clientRoles.add(role);
            }
        }

        return clientRoles;
    }

    private String getCallerRegion(User caller) {
        // this should never happen. we should never have two admins for a domain. when we do support this,
        // this information should be at the domain level.
        List<User> admins = this.domainService.getDomainAdmins(caller.getDomainId());
        if (admins.size() > 0) {
            throw new IllegalStateException("Domain " + caller.getDomainId() + " has more than one user admin.");
        }

        return admins.get(0).getRegion();
    }

    private boolean isExcludedAssignableCallerRole(TenantRole tenantRole) {
       return !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
               && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
               && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
               && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userRole"))
               && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userManagedRole"));
    }
}
