package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.Validator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.*;
import com.unboundid.ldap.sdk.Filter;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.HashHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultUserService implements UserService {
    public static final String GETTING_USER = "Getting User: {}";
    public static final String GOT_USER = "Got User: {}";

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

    @Override
    public void addRacker(Racker racker) {
        logger.info("Adding Racker {}", racker);
        Racker exists = this.userDao.getRackerByRackerId(racker.getRackerId());
        if (exists != null) {
            throw new DuplicateException("Racker Already Exists");
        }
        this.userDao.addRacker(racker);
        logger.info("Added Racker {}", racker);
    }

    @Override
    public void addUser(User user) {
        logger.info("Adding User: {}", user);
        if(!validator.isEmpty(user.getEmail())){
            validator.isEmailValid(user.getEmail());
        }
        validateUsername(user);
        setPasswordIfNecessary(user);

        if (user.getRegion() == null) {
            Region region = cloudRegionService.getDefaultRegion(config.getString("cloud.region"));
            if (region == null) {
                throw new BadRequestException("default cloud region was not found");
            }

            user.setRegion(region.getName());
        }

        if (user.isEnabled() == null) {
            user.setEnabled(user.isEnabled());
        }

        if (user.getId() == null) {
            user.setId(this.userDao.getNextUserId());
        }

        userDao.addUser(user);
        logger.info("Added User: {}", user);

        Date accessTokenExp = new DateTime().toDate();
        //Every user by default has the idm application provisioned for them
        logger.info("Adding User Scope Access for Idm to user {}", user);
        UserScopeAccess usa = new UserScopeAccess();
        usa.setUsername(user.getUsername());
        usa.setUserRsId(user.getId());
        usa.setUserRCN(user.getCustomerId());
        usa.setClientId(getIdmClientId());
        usa.setClientRCN(getRackspaceCustomerId());
        usa.setAccessTokenString(UUID.randomUUID().toString().replace("-", ""));
        usa.setAccessTokenExp(accessTokenExp);

        this.scopeAccessService.addDirectScopeAccess(user.getUniqueId(), usa);

        //Every user by default has the cloud auth application provisioned for them
        UserScopeAccess cloudUsa = new UserScopeAccess();
        cloudUsa.setUsername(user.getUsername());
        cloudUsa.setUserRsId(user.getId());
        cloudUsa.setUserRCN(user.getCustomerId());
        cloudUsa.setClientId(getCloudAuthClientId());
        cloudUsa.setClientRCN(getRackspaceCustomerId());
        cloudUsa.setAccessTokenString(UUID.randomUUID().toString().replace("-", ""));
        cloudUsa.setAccessTokenExp(accessTokenExp);

        this.scopeAccessService.addDirectScopeAccess(user.getUniqueId(), cloudUsa);

        logger.info("Added User Scope Access for Idm to user {}", user);
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
    public UserAuthenticationResult authenticateWithMossoIdAndApiKey(int mossoId, String apiKey) {
        logger.debug("Authenticating User with MossoId {} and Api Key", mossoId);
        User user = getUserByTenantId(String.valueOf(mossoId));
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(user.getUsername(), apiKey);
        logger.debug("Authenticated User with MossoId {} and API Key - {}", mossoId, authenticated);
        return authenticated;
    }


    @Override
    public UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey) {
        logger.debug("Authenticating User with NastId {} and API Key", nastId);

        User user = getUserByTenantId(nastId);

        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(user.getUsername(), apiKey);

        logger.debug("Authenticated User with NastId {} and API Key - {}", nastId, authenticated);
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

        this.userDao.deleteRacker(rackerId);

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
    public Users getAllUsers(FilterParam[] filters, Integer offset, Integer limit) {
        logger.debug("Getting All Users");

        Users users = this.userDao.getAllUsers(filters, offset, limit);

        logger.debug("Got All Users {}", filters);

        return users;
    }

    @Override
    public Users getAllUsers(FilterParam[] filters) {
        //TODO Paginiation
        logger.debug("Getting All Users");

        Users users = this.userDao.getAllUsers(filters, getLdapPagingOffsetDefault(), getLdapPagingLimitDefault());

        logger.debug("Got All Users {}", filters);

        return users;
    }


    @Override
    public Racker getRackerByRackerId(String rackerId) {
        logger.debug("Getting Racker: {}", rackerId);
        Racker racker = userDao.getRackerByRackerId(rackerId);
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

    public User getUserByAuthToken(String authToken) {
        if (authToken == null) {
            return null;
        }
        ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
        if(scopeAccessByAccessToken == null) {
            return null;
        }

        String uid = scopeAccessByAccessToken.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
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
    public Users getUsersByTenantId(String tenantId) {
        logger.debug("Get list of users with tenant", tenantId);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForTenant(tenantId);
        List<Filter> filterList = new ArrayList<Filter>();
        for(TenantRole t : tenantRoles){
            if(t.getUserId() == null){
                tenantService.addUserIdToTenantRole(t);
            }
            filterList.add(Filter.createEqualityFilter("rsId", t.getUserId()));
        }
        Users users = userDao.getUsers(filterList);
        logger.debug("Got list of users with tenant", tenantId);
        return users;
    }

    @Override
    public User getUserByTenantId(String tenantId) {
        logger.debug("Getting user by tenantId: {}", tenantId);
        Users users = getUsersByTenantId(tenantId);

        if(users.getUsers() == null || users.getUsers().size() < 1){
            return null;
        }

        if (users.getUsers().size() == 1) {
            return users.getUsers().get(0);
        }else if(users.getUsers().size() > 1){
            for (User user : users.getUsers()) {
                if (authorizationService.hasUserAdminRole(user)) {
                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public User getUserByRPN(String rpn) {
        logger.debug(GETTING_USER, rpn);
        User user = userDao.getUserByRPN(rpn);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public User getUserBySecureId(String secureId) {
        logger.debug("Getting User by secureId: {}", secureId);
        User user = userDao.getUserBySecureId(secureId);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public User getSoftDeletedUser(String id) {
        logger.debug(GETTING_USER, id);
        User user = userDao.getSoftDeletedUserById(id);
        logger.debug(GOT_USER, user);
        return user;
    }

    @Override
    public User getSoftDeletedUserByUsername(String name) {
        logger.debug(GETTING_USER, name);
        User user = userDao.getSoftDeletedUserByUsername(name);
        logger.debug(GOT_USER, user);
        return user;
    }
//
//    @Override
//    public DateTime getUserPasswordExpirationDate(String userName) {
//
//        DateTime passwordExpirationDate = null;
//
//        User user = getUser(userName);
//
//        if (user == null) {
//            logger.debug("No user found, returning null.");
//            return null;
//        }
//
//        Customer customer = customerDao.getCustomerByCustomerId(user
//            .getCustomerId());
//
//        if (customer == null) {
//            logger.debug("No customer found, returning null");
//            return null;
//        }
//
//        Boolean passwordRotationPolicyEnabled = customer
//            .getPasswordRotationEnabled();
//
//        if (passwordRotationPolicyEnabled != null
//            && passwordRotationPolicyEnabled) {
//            int passwordRotationDurationInDays = customer
//                .getPasswordRotationDuration();
//
//            DateTime timeOfLastPwdChange = user.getPasswordObj()
//                .getLastUpdated();
//
//            passwordExpirationDate = timeOfLastPwdChange
//                .plusDays(passwordRotationDurationInDays);
//        }
//        logger
//            .debug("Password expiration date set: {}", passwordExpirationDate);
//
//        return passwordExpirationDate;
//    }

    @Override
    public int getUserWeight(User user, String applicationId) {
        List<TenantRole> tenantRoles = tenantService.getGlobalRolesForUser(user, applicationId);
        for (TenantRole tenantRole : tenantRoles) {
            ClientRole clientRole = applicationService.getClientRoleById(tenantRole.getRoleRsId());
            if (StringUtils.startsWithIgnoreCase(clientRole.getName(), "identity:")) {
                return clientRole.getRsWeight();
            }
        }
        return config.getInt("cloudAuth.defaultUser.rsWeight");
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
        List<ScopeAccess> services = scopeAccessService.getScopeAccessesForParent(user.getUniqueId());

        List<Application> clientList = new ArrayList<Application>();

        for (ScopeAccess service : services) {
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
            Users users = userDao.getUsersByDomainId(user.getDomainId());

            if(users != null) {
            	for (User subUser : users.getUsers()) {
            		if (!subUser.getId().equalsIgnoreCase(user.getId())){
            			result.add(subUser);
            		}
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
    public boolean hasSubUsers(String userId) {
        User user = userDao.getUserById(userId);
        if (user == null) {
            return false;
        }
        Users users = userDao.getUsersByDomainId(user.getDomainId());
        if (users == null || users.getUsers() == null || users.getUsers().size() == 0) {
            return false;
        }
        for (User userInList : users.getUsers()) {
            if(authorizationService.hasDefaultUserRole(userInList)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Password resetUserPassword(User user) {
        Password newPassword = Password.generateRandom(false); // Would the user
        // ever reset his
        // own password?
        user.setPasswordObj(newPassword);
        userDao.updateUser(user, false);
        logger.debug("Updated password for user: {}", user);

        return newPassword.toExisting();
    }

    boolean checkForPasswordUpdate(User currentUser, User user) {
        if(user != null && !StringUtils.isEmpty(user.getPassword())) {
            if(currentUser != null && !StringUtils.isEmpty(currentUser.getPassword()) && !currentUser.getPassword().equals(user.getPassword())){
                return true;
            }
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
        if(checkForPasswordUpdate(currentUser, user) || userIsBeingDisabled)  {
            scopeAccessService.expireAllTokensForUser(user.getUsername());
        }

        userDao.updateUser(user, hasSelfUpdatedPassword);
        if (userIsBeingDisabled) {
            disableUserAdminSubUsers(user);
        }

        List<ScopeAccess> scopeAccessList = scopeAccessService.getScopeAccessListByUserId(user.getId());
        for (ScopeAccess scopeAccess : scopeAccessList) {
            ((UserScopeAccess)scopeAccess).setUsername(user.getUsername());
            scopeAccessService.updateScopeAccess(scopeAccess);
        }
        logger.info("Updated User: {}", user);
    }

    private void disableUserAdminSubUsers(User user) throws IOException, JAXBException {
        if (authorizationService.hasUserAdminRole(user)) {
            List<User> enabledUserAdmins = domainService.getDomainAdmins(user.getDomainId(), true);
            if (enabledUserAdmins.size() != 0) {
                return;
            }
            List<User> subUsers = getSubUsers(user);

            for (User subUser : subUsers) {
                if (subUser.isEnabled()) {
                    subUser.setEnabled(false);
                    userDao.updateUser(subUser, false);
                    scopeAccessService.expireAllTokensForUser(subUser.getUsername());
                }
            }
        }
    }

    private boolean checkIfUserIsBeingDisabled(User currentUser, User user) {
        if (currentUser != null && user != null && user.isEnabled() != null) {
            boolean currentUserEnabled = currentUser.isEnabled();
            boolean userEnabled = user.isEnabled();

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

    @Override
    public boolean userExistsById(String userId) {
        com.rackspace.idm.domain.entity.User userById = userDao.getUserById(userId);
        if (userById == null) {
            return false;
        }
        if (userById.getInMigration() == null) {
            return true;
        }
        return !userById.getInMigration();

    }

    @Override
    public boolean userExistsByUsername(String username) {
        com.rackspace.idm.domain.entity.User userByUsername = userDao.getUserByUsername(username);
        if (userByUsername == null) {
            return false;
        }
        if (userByUsername.getInMigration() == null) {
            return true;
        }
       return !userByUsername.getInMigration();
    }

    @Override
    public boolean isMigratedUser(User user) {
        if (user == null) {
            return false;
        } else if (user.getInMigration() == null) {
            return false;
        } else {
            return !user.getInMigration();
        }
    }

    private void setPasswordIfNecessary(User user) {
        Password password = user.getPasswordObj();

        if (!user.hasEmptyPassword()) {
            checkPasswordComplexity(password);
        } else {
            // False, since a user wouldn't add himself
            Password newpassword = Password.generateRandom(false);
            user.setPasswordObj(newpassword);
        }

        if (!user.getPasswordObj().isNew()) {
            logger.error("Password of User is an existing instance");
            throw new IllegalArgumentException(
                    "The password appears to be an existing instance. It must be a new instance!");
        }
    }

    private void checkPasswordComplexity(Password password) {
        if (isPasswordRulesEnforced()) {
            PasswordComplexityResult result = passwordComplexityService.checkPassword(password.getValue());
            if (!result.isValidPassword()) {
                String errorMsg = String.format("Invalid password %s", password);
                logger.warn(errorMsg);
                throw new PasswordValidationException(errorMsg);
            }
        }
    }

    @Override
    public User getUserByScopeAccess(ScopeAccess scopeAccess) {
        User user = null;
        if (scopeAccess instanceof RackerScopeAccess) {
            RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) scopeAccess;
            user = getRackerByRackerId((rackerScopeAccess.getRackerId()));
        } else if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;
            if (impersonatedScopeAccess.getRackerId() != null) {
                Racker impersonatingRacker = getRackerByRackerId(impersonatedScopeAccess.getRackerId());
                impersonatingRacker.setId(impersonatingRacker.getRackerId());
                impersonatingRacker.setUsername(impersonatingRacker.getUsername());
                user = impersonatingRacker;
                user.setEnabled(true);
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
        if (user.isDisabled()) {
            throw new NotFoundException("Token not found.");
        }
        return user;
    }

    @Override
    public void addBaseUrlToUser(Integer baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = tenantService.getTenant(tenantId);

        // Check for existing BaseUrl
        if (tenant.getBaseUrlIds() != null && tenant.getBaseUrlIds().length != 0) {
            for (String bId : tenant.getBaseUrlIds()) {
                if (bId.equals(String.valueOf(baseUrl.getBaseUrlId()))) {
                    throw new BadRequestException("BaseUrl already exists.");
                }
            }
        }

        tenant.addBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void removeBaseUrlFromUser(Integer baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST")) {
            tenantId = user.getNastId();
        } else {
            tenantId = String.valueOf(user.getMossoId());
        }

        Tenant tenant = this.tenantService.getTenant(tenantId);
        tenant.removeBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public List<Tenant> getUserTenants(String userId) {
        List<Tenant> tenantList = new ArrayList<Tenant>();

        return tenantList;
    }

    @Override
    public PaginatorContext<User> getAllUsersPaged(FilterParam[] filters, int offset, int limit) {
        logger.debug("Getting Users Paged");

        PaginatorContext<User> context = this.userDao.getAllUsersPaged(filters, offset, limit);

        logger.debug("Got Users {}", filters);

        return context;
    }

    @Override
    public PaginatorContext<User> getUsersWithRole(FilterParam[] filters, String roleId, int offset, int limit) {
        logger.debug("Getting Users with Role {}", roleId);

        PaginatorContext<User> userContext = new PaginatorContext<User>();

        if (filters.length == 1) {
            PaginatorContext<String> context = this.tenantService.getIdsForUsersWithTenantRole(roleId, offset, limit);

            ArrayList<User> userList = new ArrayList<User>();
            for (String userId : context.getValueList()) {
                User user = getUserById(userId);
                if (user != null) {
                    userList.add(user);
                }
            }

            setUserContext(userContext, context.getLimit(), context.getOffset(),
                            context.getTotalRecords(), userList);
        } else {
            Users users = this.getAllUsersNoLimit(filters);
            List<User> usersWithRoleList = new ArrayList<User>();
            filterUsersForRole(users, usersWithRoleList, roleId);

            List<User> subList = getSubList(usersWithRoleList, offset, limit);

            setUserContext(userContext, limit, offset, usersWithRoleList.size(), subList);
        }

        logger.debug("Got Users {}", filters);

        return userContext;
    }

    protected Users getAllUsersNoLimit(FilterParam[] filters) {
        logger.debug("Getting all users with {}", filters);

        Users users = this.userDao.getAllUsersNoLimit(filters);

        logger.debug("Got users {}", users);

        return users;
    }

    protected void filterUsersForRole(Users users, List<User> usersWithRole, String roleId) {
        if (users != null) {
            for (User user : users.getUsers()) {
                List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
                if (user.getRoles() != null) {
                    roles.addAll(user.getRoles());
                }

                for (TenantRole tenantRole : roles) {
                    if (tenantRole.getRoleRsId().equals(roleId)) {
                        usersWithRole.add(user);
                        break;
                    }
                }
            }
        }
    }

    protected List<User> getSubList(List<User> userList, int offset, int limit) {
        if (offset > userList.size()) {
            throw new BadRequestException(String.format("Offset greater than total number of records (%s)", userList.size()));
        }

        if (userList.size() > limit) {
            if (userList.size() > offset + limit) {
                return userList.subList(offset, offset + limit);
            } else {
                return userList.subList(offset, userList.size());
            }
        } else {
            return userList.subList(offset, userList.size());
        }
    }

    protected void setUserContext(PaginatorContext<User> userContext, int limit, int offset, int totalRecords, List<User> userList) {
        userContext.setLimit(limit);
        userContext.setOffset(offset);
        userContext.setTotalRecords(totalRecords);
        userContext.setValueList(userList);
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
    public List<User> getUsersInDomain(String domainId, boolean enabled) {
        return userDao.getUsersByDomain(domainId, enabled);
    }

    @Override
    public List<User> getUsersInDomain(String domainId) {
        return userDao.getUsersByDomain(domainId);
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
}
