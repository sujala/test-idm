package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.HashHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   // private final CustomerDao customerDao;
    private final PasswordComplexityService passwordComplexityService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final ScopeAccessDao scopeAccessDao;
    
    private final UserDao userDao;

    public DefaultUserService(UserDao userDao, AuthDao rackerDao,
    	ScopeAccessDao scopeAccessDao,
        ApplicationService clientService, Configuration config, TokenService oauthService,
        PasswordComplexityService passwordComplexityService) {

        this.userDao = userDao;
        this.authDao = rackerDao;
        //this.customerDao = customerDao;
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
            throw new DuplicateException("Racker Already Exsits");
        }
        this.userDao.addRacker(racker);
        logger.info("Added Racker {}", racker);
    }

    @Override
    public void addUser(User user) throws DuplicateException {
        logger.info("Adding User: {}", user);
        
        validateUserEmailAddress(user);
        validateUsername(user);
        if(user.getMossoId() != null)
            validateMossoId(user.getMossoId());
        setPasswordIfNecessary(user);
        
        user.setEnabled(true);
        user.setId(this.userDao.getNextUserId());
        
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
    public UserAuthenticationResult authenticateWithMossoIdAndApiKey(
        int mossoId, String apiKey) {
        logger
            .debug("Authenticating User with MossoId {} and Api Key", mossoId);
        UserAuthenticationResult authenticated = userDao
            .authenticateByMossoIdAndAPIKey(mossoId, apiKey);
        logger.debug("Authenticated User with MossoId {} and API Key - {}",
            mossoId, authenticated);
        return authenticated;
    }

    
    @Override
    public UserAuthenticationResult authenticateWithNastIdAndApiKey(
        String nastId, String apiKey) {
        logger.debug("Authenticating User with NastId {} and API Key", nastId);
        UserAuthenticationResult authenticated = userDao
            .authenticateByNastIdAndAPIKey(nastId, apiKey);
        logger.debug("Authenticated User with NastId {} and API Key - {}",
            nastId, authenticated);
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
    public Users getAllUsers(FilterParam[] filters, int offset, int limit) {
        logger.debug("Getting All Users");

        Users users = this.userDao.getAllUsers(filters, offset, limit);

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

    
    @Override
    public User getUser(String customerId, String username) {
        logger.debug("Getting User: {} - {}", customerId, username);
        User user = userDao
            .getUserByCustomerIdAndUsername(customerId, username);
        logger.debug("Got User: {}", user);
        return user;
    }

    
    @Override
    public User getUserByMossoId(int mossoId) {
        logger.debug("Getting User: {}", mossoId);
        User user = userDao.getUserByMossoId(mossoId);
        logger.debug("Got User: {}", user);
        return user;
    }

    
    @Override
    public User getUserByNastId(String nastId) {
        logger.debug("Getting User: {}", nastId);
        User user = userDao.getUserByNastId(nastId);
        logger.debug("Got User: {}", user);
        return user;
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
        
        this.userDao.updateUser(user, hasSelfUpdatedPassword);
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
    		logger.warn("Couldn't add user {} because username already taken", user);
    		throw new DuplicateUsernameException(String.format("Username %s already exists", user.getUsername()));
	 	}
    }

    void validateMossoId(int mossoId){
        User userByMossoId = userDao.getUserByMossoId(mossoId);
        if(userByMossoId!=null){
            throw new BadRequestException("User with Mosso Account ID: "+ mossoId+ " already exists.");
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
        userDao.softDeleteUser(user);
        logger.debug("SoftDeleted User: {}", user);
    }

    @Override
    public boolean userExistsById(String userId) {
        com.rackspace.idm.domain.entity.User userById = userDao.getUserByUsername(userId);
        if (userById == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean userExistsByUsername(String username) {
        com.rackspace.idm.domain.entity.User userByUsername = userDao.getUserByUsername(username);
        if (userByUsername == null) {
            return false;
        }
        return true;
    }

    private void setPasswordIfNecessary(User user) {
    	Password password = user.getPasswordObj();
    	
        if (!user.hasEmptyPassword()) {
        	checkPasswordComplexity(password);
        }
        else {
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
    
    private boolean isPasswordRulesEnforced() {
        return config.getBoolean("password.rules.enforced", true);
    }
}
