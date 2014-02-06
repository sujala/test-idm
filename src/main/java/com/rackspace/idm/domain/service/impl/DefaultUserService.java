package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.RackerDao;
import com.rackspace.idm.domain.dao.UserDao;
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

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultUserService implements UserService {
    public static final String GETTING_USER = "Getting User: {}";
    public static final String GOT_USER = "Got User: {}";
    public static final String ENCRYPTION_VERSION_ID = "encryptionVersionId";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    private boolean userUUIDEnabled = false;

    @PostConstruct
    public void initialize() {
        userUUIDEnabled = config.getBoolean("user.uuid.enabled");
    }

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

    @Override
    public void addUser(User user) {
        logger.info("Adding User: {}", user);
        if(!validator.isEmpty(user.getEmail())){
            validator.isEmailValid(user.getEmail());
        }
        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());

        validateUsername(user);
        setPasswordIfNecessary(user);

        if (user.getRegion() == null) {
            Region region = cloudRegionService.getDefaultRegion(config.getString("cloud.region"));
            if (region == null) {
                throw new BadRequestException("default cloud region was not found");
            }

            user.setRegion(region.getName());
        }

        if (user.getEnabled() == null) {
            user.setEnabled(user.getEnabled());
        }

        user.setId(generateUniqueId());

        userDao.addUser(user);
        logger.info("Added User: {}", user);

        //Every user by default has the idm application provisioned for them
        logger.info("Adding User Scope Access for Idm to user {}", user);
        UserScopeAccess usa = scopeAccessService.createInstanceOfUserScopeAccess(user, getIdmClientId(), getRackspaceCustomerId());

        this.scopeAccessService.addUserScopeAccess(user, usa);

        //Every user by default has the cloud auth application provisioned for them
        UserScopeAccess cloudUsa = scopeAccessService.createInstanceOfUserScopeAccess(user, getCloudAuthClientId(), getRackspaceCustomerId());

        this.scopeAccessService.addUserScopeAccess(user, cloudUsa);

        logger.info("Added User Scope Access for Idm to user {}", user);
    }

    private String generateUniqueId() {
        if (userUUIDEnabled) {
            return UUID.randomUUID().toString().replace("-", "");
        } else {
            return this.userDao.getNextUserId();
        }
    }

    //TODO: consider removing this method. Just here so code doesn't break
    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        logger.debug("Authenticating User: {} by Username", username);
        UserAuthenticationResult authenticated = userDao.authenticate(username, password);
        logger.debug("Authenticated User: {} by Username - {}", username,
                authenticated);
        return authenticated;
    }

    @Override
    public UserAuthenticationResult authenticateWithApiKey(String username,
                                                           String apiKey) {
        logger.debug("Authenticating User: {} by API Key", username);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(
                username, apiKey);
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

    @Override
    public void deleteRacker(String rackerId) {
        logger.info("Deleting Racker: {}", rackerId);

        this.rackerDao.deleteRacker(rackerId);

        logger.info("Deleted Racker: {}", rackerId);
    }

    @Override
    public void deleteUser(User user) {
        logger.info("Deleting User: {}", user.getUsername());

        this.userDao.deleteUser(user);

        logger.info("Deleted User: {}", user.getUsername());
    }

    @Override
    public void deleteUser(String username) {
        logger.info("Deleting User: {}", username);

        this.userDao.deleteUser(username);

        logger.info("Deleted User: {}", username);
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
            if (authorizationService.hasUserAdminRole(authorizationService.getAuthorizationContext(user))) {
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
            if(authorizationService.hasDefaultUserRole(authorizationService.getAuthorizationContext(userInList))) {
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
        userDao.updateUser(user, false);
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
    public void updateUser(User user, boolean hasSelfUpdatedPassword) throws IOException, JAXBException {
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
        userDao.updateUser(user, hasSelfUpdatedPassword);

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
        AuthorizationContext context = authorizationService.getAuthorizationContext(user);
        if (authorizationService.hasUserAdminRole(context)) {
            List<User> enabledUserAdmins = domainService.getEnabledDomainAdmins(user.getDomainId());
            if (enabledUserAdmins.size() != 0) {
                return;
            }
            List<User> subUsers = getSubUsers(user);

            for (User subUser : subUsers) {
                if (subUser.getEnabled()) {
                    subUser.setEnabled(false);
                    userDao.updateUser(subUser, false);
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

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }

    private void validateUsername(User user) {
        boolean isUsernameUnique = userDao.isUsernameUnique(user.getUsername());
        if (!isUsernameUnique) {
            logger.warn("Couldn't add/update user {} because username already taken", user);
            throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
        }
    }

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

    private void setPasswordIfNecessary(User user) {
        String password = user.getPassword();

        if (!StringUtils.isEmpty(user.getPassword())) {
            checkPasswordComplexity(password);
        } else {
            // False, since a user wouldn't add himself
            Password newpassword = Password.generateRandom(false, user);
        }

        if (!user.isPasswordIsNew()) {
            logger.error("Password of User is an existing instance");
            throw new IllegalArgumentException(
                    "The password appears to be an existing instance. It must be a new instance!");
        }
    }

    private void checkPasswordComplexity(String password) {
        if (isPasswordRulesEnforced()) {
            PasswordComplexityResult result = passwordComplexityService.checkPassword(password);
            if (!result.isValidPassword()) {
                String errorMsg = String.format("Invalid password %s", password);
                logger.warn(errorMsg);
                throw new PasswordValidationException(errorMsg);
            }
        }
    }

    @Override
    public BaseUser getUserByScopeAccess(ScopeAccess scopeAccess, boolean checkUserDisabled) {
        BaseUser user;
        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
            user = getRackerByRackerId((rackerScopeAccess.getRackerId()));
        } else if (scopeAccess instanceof ImpersonatedScopeAccess) {
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
        } else if (scopeAccess instanceof ClientScopeAccess) {
            return null;
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
}
