package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.HashHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultUserService implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+\\.[\\w]+");
    private final AuthDao authDao;
    private final ApplicationService clientService;
    private final TokenService oauthService;
    private final Configuration config;

    private final PasswordComplexityService passwordComplexityService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScopeAccessDao scopeAccessDao;

    private final UserDao userDao;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AuthorizationService authorizationService;

    public DefaultUserService(UserDao userDao, AuthDao rackerDao,
                              ScopeAccessDao scopeAccessDao,
                              ApplicationService clientService, Configuration config, TokenService oauthService,
                              PasswordComplexityService passwordComplexityService) {

        this.userDao = userDao;
        this.authDao = rackerDao;
        this.scopeAccessDao = scopeAccessDao;
        this.clientService = clientService;
        this.config = config;
        this.oauthService = oauthService;
        this.passwordComplexityService = passwordComplexityService;
    }

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
    public void addUser(User user) throws DuplicateException {
        logger.info("Adding User: {}", user);

        validateUserEmailAddress(user);
        validateUsername(user);
//        if (user.getMossoId() != null) {
//            validateMossoId(user.getMossoId());
//        }
        setPasswordIfNecessary(user);

        if (user.isEnabled() == null)
            user.setEnabled(user.isEnabled());

        if (user.getId() == null)
            user.setId(this.userDao.getNextUserId());

        if (user.getDomainId() == null)
            user.setDomainId(user.getId());


        userDao.addUser(user);
        logger.info("Added User: {}", user);

        //Every user by default has the idm application provisioned for them
        logger.info("Adding User Scope Access for Idm to user {}", user);
        UserScopeAccess usa = new UserScopeAccess();
        usa.setUsername(user.getUsername());
        usa.setUserRsId(user.getId());
        usa.setUserRCN(user.getCustomerId());
        usa.setClientId(getIdmClientId());
        usa.setClientRCN(getRackspaceCustomerId());

        this.scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), usa);

        //Every user by default has the cloud auth application provisioned for them
        UserScopeAccess cloudUsa = new UserScopeAccess();
        cloudUsa.setUsername(user.getUsername());
        cloudUsa.setUserRsId(user.getId());
        cloudUsa.setUserRCN(user.getCustomerId());
        cloudUsa.setClientId(getCloudAuthClientId());
        cloudUsa.setClientRCN(getRackspaceCustomerId());

        this.scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), cloudUsa);

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
        User user = getUserByMossoId(mossoId);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(user.getUsername(), apiKey);
        logger.debug("Authenticated User with MossoId {} and API Key - {}", mossoId, authenticated);
        return authenticated;
    }


    @Override
    public UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey) {
        logger.debug("Authenticating User with NastId {} and API Key", nastId);

        User user = getUserByNastId(nastId);

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

        List<ClientGroup> groupsOfWhichUserIsMember = this.clientService
                .getClientGroupsForUser(username);

        for (ClientGroup g : groupsOfWhichUserIsMember) {
            this.clientService.removeUserFromClientGroup(username, g);
        }

        this.userDao.deleteUser(username);

        logger.info("Deleted User: {}", username);
    }


    @Override
    public String generateApiKey() {
        try {
            return HashHelper.getRandomSha1();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(
                    "The JVM does not support the algorithm needed.", e);
        }
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
        logger.debug("Getting User: {}", username);
        User user = userDao.getUserByUsername(username);
        logger.debug("Got User: {}", user);
        return user;
    }

    public User getUserByAuthToken(String authToken) {
        if (authToken == null) {
            return null;
        }
        ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
        String uid = scopeAccessByAccessToken.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
        return getUser(uid);
    }


    @Override
    public User getUser(String customerId, String username) {
        logger.debug("Getting User: {} - {}", customerId, username);
        User user = userDao.getUserByCustomerIdAndUsername(customerId, username);
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUserByMossoId(int mossoId) { // Returns the first User-Admin it finds with matching mossoId
        logger.debug("Getting User: {}", mossoId);
        Users users = userDao.getUsersByMossoId(mossoId);

        if (users.getUsers().size() == 1) {
            return users.getUsers().get(0);
        } else if (users.getUsers().size() > 1) {
            for (User user : users.getUsers()) {
                UserScopeAccess sa = scopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), getCloudAuthClientId());
                if (authorizationService.authorizeCloudUserAdmin(sa))
                    return user;
            }
        }
        return null;
    }

    @Override
    public Users getUsersByMossoId(int mossoId) {
        logger.debug("Getting User: {}", mossoId);
        Users users = userDao.getUsersByMossoId(mossoId);
        logger.debug("Got User: {}", users);
        return users;
    }

    @Override
    public User getUserByNastId(String nastId) { // Returns the first User-Admin it finds with matching nastId
        logger.debug("Getting User: {}", nastId);
        Users users = userDao.getUsersByNastId(nastId);

        if (users.getUsers().size() == 1) {
            return users.getUsers().get(0);
        } else if (users.getUsers().size() > 1) {
            for (User user : users.getUsers()) {
                UserScopeAccess sa = scopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), getCloudAuthClientId());
                if (authorizationService.authorizeCloudUserAdmin(sa))
                    return user;
            }
        }
        return null;
    }

    @Override
    public User getUserByRPN(String rpn) {
        logger.debug("Getting User: {}", rpn);
        User user = userDao.getUserByRPN(rpn);
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUserBySecureId(String secureId) {
        logger.debug("Getting User by secureId: {}", secureId);
        User user = userDao.getUserBySecureId(secureId);
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getSoftDeletedUser(String id) {
        logger.debug("Getting User: {}", id);
        User user = userDao.getSoftDeletedUserById(id);
        logger.debug("Got User: {}", user);
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
    public Applications getUserApplications(User user) {
        if (user == null || user.getUniqueId() == null) {
            String errmsg = "Null User instance or is lacking uniqueID";
            logger.debug(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        List<ScopeAccess> services = this.scopeAccessDao
                .getScopeAccessesByParent(user.getUniqueId());

        List<Application> clientList = new ArrayList<Application>();

        for (ScopeAccess service : services) {
            if (service instanceof UserScopeAccess) {
                clientList
                        .add(this.clientService.getById(service.getClientId()));
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
            List<ScopeAccess> scopeAccessList = scopeAccessDao.getScopeAccessListByUserId(userInList.getId());
            for (ScopeAccess scopeAccess : scopeAccessList) {
                boolean isDefaultUser = authorizationService.hasDefaultUserRole(scopeAccess);
                if (isDefaultUser) {
                    return true;
                }
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


    @Override
    public void setUserPassword(String userId, PasswordCredentials userCred, ScopeAccess token) {
        logger.debug("Updating Password for User: {}", userId);

        User user = loadUser(userId);
        checkPasswordComplexity(userCred.getNewPassword());

        if (userCred.isVerifyCurrentPassword()) {
            if (userCred.getCurrentPassword() == null ||
                    StringUtils.isBlank(userCred.getCurrentPassword().getValue())) {
                String errMsg = "Value for Current Password cannot be blank";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // authenticate using old password
            UserAuthenticationResult uaResult = this.authenticate(user.getUsername(), userCred.getCurrentPassword().getValue());
            if (!uaResult.isAuthenticated()) {
                String errorMsg = String.format("Current password does not match for user: %s", userId);
                logger.warn(errorMsg);
                throw new NotAuthenticatedException(errorMsg);
            }
        }

        user.setPasswordObj(Password.newInstance(userCred.getNewPassword().getValue()));

        boolean isSelfUpdate = (token instanceof UserScopeAccess && ((UserScopeAccess) token).getUsername().equals(user.getUsername()))
                || (token instanceof PasswordResetScopeAccess && ((PasswordResetScopeAccess) token).getUsername().equals(user.getUsername()));

        this.updateUser(user, isSelfUpdate);
        logger.debug("Updated password for user: {}", user);
    }

    @Override
    public void updateUser(User user, boolean hasSelfUpdatedPassword) {
        logger.info("Updating User: {}", user);
        validateUserEmailAddress(user);

        //TODO: We might restrict this to certain roles, so we might need a param passed in this method as well
//        if (!user.isEnabled()) {
//        	 oauthService.revokeAllTokensForUser(user.getUniqueId());
//        }

        userDao.updateUser(user, hasSelfUpdatedPassword);
        logger.info("Updated User: {}", user);
    }

    public void updateUserById(User user, boolean hasSelfUpdatedPassword) {
        logger.info("Updating User: {}", user);
        validateUserEmailAddress(user);
        userDao.updateUserById(user, hasSelfUpdatedPassword);
        List<ScopeAccess> scopeAccessList = scopeAccessService.getScopeAccessListByUserId(user.getId());
        for (ScopeAccess scopeAccess : scopeAccessList) {
            ((UserScopeAccess)scopeAccess).setUsername(user.getUsername());
            scopeAccessService.updateScopeAccess(scopeAccess);
        }
        logger.info("Updated User: {}", user);
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

    private void validateUserEmailAddress(User user) {
        if (StringUtils.isBlank(user.getEmail())) {
            return;
        }

        Matcher m = EMAIL_PATTERN.matcher(user.getEmail());

        if (!m.find()) {
            String errMsg = String.format("%s is not a valid email", user.getEmail());
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

    }

    private void validateUsername(User user) {
        boolean isUsernameUnique = userDao.isUsernameUnique(user.getUsername());
        if (!isUsernameUnique) {
            logger.warn("Couldn't add/update user {} because username already taken", user);
            throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
        }
    }

    void validateMossoId(int mossoId) {
        Users usersByMossoId = userDao.getUsersByMossoId(mossoId);
        if (usersByMossoId != null) {
            if (usersByMossoId.getUsers().size() > 0) {
                throw new BadRequestException("User with Mosso Account ID: " + mossoId + " already exists.");
            }
        }
    }

    @Override
    public User getUserById(String id) {
        logger.debug("Getting User: {}", id);
        User user = userDao.getUserById(id);
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public void softDeleteUser(User user) {
        logger.debug("SoftDeleting User: {}", user);
        scopeAccessService.expireAllTokensForUser(user.getUsername());
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
        if (userById.getInMigration()) {
            return false;
        }
        if (userById.getInMigration() == false) {
            return true;
        }
        return true;
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
        if (userByUsername.getInMigration()) {
            return false;
        }
        if (userByUsername.getInMigration() == false) {
            return true;
        }
        return true;
    }

    @Override
    public boolean isMigratedUser(User user) {
        if (user == null)
            return false;
        else if (user.getInMigration() == null)
            return false;
        else if (!user.getInMigration())
            return true;
        else
            return false;
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
    public User getUserByScopeAccess(ScopeAccess scopeAccess) throws Exception {
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
            throw new Exception("Invalid getUserByScopeAccess, scopeAccess cannot provide information to get a user");
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
        if (baseUrl.getOpenstackType().equals("NAST"))
            tenantId = user.getNastId();
        else
            tenantId = String.valueOf(user.getMossoId());

        Tenant tenant = tenantService.getTenant(tenantId);

        // Check for existing BaseUrl
        for (String bId : tenant.getBaseUrlIds()) {
            if (bId.equals(String.valueOf(baseUrl.getBaseUrlId())))
                throw new BadRequestException("BaseUrl already exists.");
        }

        tenant.addBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
    }

    @Override
    public void removeBaseUrlFromUser(Integer baseUrlId, User user) {
        CloudBaseUrl baseUrl = endpointService.getBaseUrlById(baseUrlId);
        String tenantId;
        if (baseUrl.getOpenstackType().equals("NAST"))
            tenantId = user.getNastId();
        else
            tenantId = String.valueOf(user.getMossoId());

        Tenant tenant = this.tenantService.getTenant(tenantId);
        tenant.removeBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
        this.tenantService.updateTenant(tenant);
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

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
}
