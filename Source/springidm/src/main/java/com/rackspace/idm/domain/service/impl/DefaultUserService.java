package com.rackspace.idm.domain.service.impl;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.UserCredentials;
import com.rackspace.idm.util.HashHelper;

public class DefaultUserService implements UserService {

    private final UserDao userDao;
    private final AuthDao authDao;
    private final CustomerDao customerDao;
    private final ScopeAccessDao scopeAccessDao;
    private final ClientService clientService;
    private final Configuration config;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultUserService(UserDao userDao, AuthDao rackerDao,
        CustomerDao customerDao, ScopeAccessDao scopeAccessDao,
        ClientService clientService, Configuration config) {

        this.userDao = userDao;
        this.authDao = rackerDao;
        this.customerDao = customerDao;
        this.scopeAccessDao = scopeAccessDao;
        this.clientService = clientService;
        this.config = config;
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
        String customerId = user.getCustomerId();

        boolean isUsernameUnique = userDao.isUsernameUnique(user.getUsername());

        if (!isUsernameUnique) {
            logger.warn("Couldn't add user {} because username already taken",
                user);
            throw new DuplicateException(String.format(
                "Username %s already exists", user.getUsername()));
        }

        Customer customer = customerDao.getCustomerByCustomerId(customerId);

        if (customer == null) {
            logger.warn("Couldn't add user {} because customer doesn't exist",
                user);
            throw new IllegalStateException("Customer doesn't exist");
        }

        String customerDN = customer.getUniqueId();

        user.setOrgInum(customer.getInum());
        user.setInum(userDao.getUnusedUserInum(customer.getInum()));
        user.setStatus(UserStatus.ACTIVE);

        if (user.getPasswordObj() == null
            || StringUtils.isBlank(user.getPassword())) {
            Password newpassword = Password.generateRandom(false); // False,
                                                                   // since a
                                                                   // user
                                                                   // wouldn't
                                                                   // add
                                                                   // himself.
            user.setPasswordObj(newpassword);
        } else

        if (!user.getPasswordObj().isNew()) {
            logger.error("Password of User is an existing instance");
            throw new IllegalArgumentException(
                "The password appears to be an existing instance. It must be a new instance!");
        }

        userDao.addUser(user, customerDN);
        logger.info("Added User: {}", user);
    }

    @Override
    public UserAuthenticationResult authenticate(String username,
        String password) {
        logger.debug("Authenticating User: {}", username);

        if (isTrustedServer()) {
            boolean authenticated = authDao.authenticate(username, password);
            logger.debug("Authenticated Racker {} : {}", username,
                authenticated);
            Racker racker = this.getRackerByRackerId(username);
            
            if (racker == null) {
                racker = new Racker();
                racker.setRackerId(username);
                this.userDao.addRacker(racker);
            }
            
            BaseUser user = new BaseUser(username);
            user.setUniqueId(racker.getUniqueId());
            return new UserAuthenticationResult(user, authenticated);
        }

        UserAuthenticationResult result = userDao.authenticate(username,
            password);

        logger.debug("Authenticated User: {} : {}", username, result);
        return result;
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
    public void deleteRacker(String rackerId) {
        logger.info("Deleting Racker: {}", rackerId);

        this.userDao.deleteRacker(rackerId);

        logger.info("Deleted Racker: {}", rackerId);
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
    public Users getByCustomerId(String customerId, int offset, int limit) {
        logger.debug("Getting Users for Cutomer: {}", customerId);

        Users users = this.userDao.getUsersByCustomerId(customerId, offset,
            limit);

        logger.debug("Got Users for Customer: {}", customerId);

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
    public boolean isUsernameUnique(String username) {
        return userDao.isUsernameUnique(username);
    }

    @Override
    public void setUserPassword(String customerId, String username,
        UserCredentials userCred, ScopeAccess token, boolean isRecovery) {

        logger.debug("Updating Password for User: {}", username);

        if (!isRecovery) {
            if (userCred.getCurrentPassword() == null
                || StringUtils.isBlank(userCred.getCurrentPassword()
                    .getPassword())) {
                String errMsg = "Value for Current Password cannot be blank";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // authenticate using old password
            UserAuthenticationResult uaResult = this.authenticate(username,
                userCred.getCurrentPassword().getPassword());
            if (!uaResult.isAuthenticated()) {
                String errorMsg = String.format(
                    "Current password does not match for user: %s", username);
                logger.warn(errorMsg);
                throw new NotAuthenticatedException(errorMsg);
            }
        }

        User user = this.checkAndGetUser(customerId, username);

        user.setPasswordObj(Password.newInstance(userCred.getNewPassword()
            .getPassword()));
        boolean isSelfUpdate = (token instanceof UserScopeAccess && ((UserScopeAccess) token)
            .getUsername().equals(username))
            || (token instanceof PasswordResetScopeAccessObject && ((PasswordResetScopeAccessObject) token)
                .getUsername().equals(username));

        this.updateUser(user, isSelfUpdate);
        logger.debug("Updated password for user: {}", user);

    }

    @Override
    public void updateUser(User user, boolean hasSelfUpdatedPassword) {
        logger.info("Updating User: {}", user);
        this.userDao.updateUser(user, hasSelfUpdatedPassword);
        logger.info("Updated User: {}", user);
    }

    @Override
    public void updateUserStatus(User user, String statusStr) {
        UserStatus status = Enum.valueOf(UserStatus.class,
            statusStr.toUpperCase());
        user.setStatus(status);
        this.userDao.updateUser(user, false);

        logger.info("Updated User's status: {}, {}", user, status);
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
    public DateTime getUserPasswordExpirationDate(String userName) {

        DateTime passwordExpirationDate = null;

        User user = getUser(userName);

        if (user == null) {
            logger.debug("No user found, returning null.");
            return null;
        }

        Customer customer = customerDao.getCustomerByCustomerId(user
            .getCustomerId());

        if (customer == null) {
            logger.debug("No customer found, returning null");
            return null;
        }

        Boolean passwordRotationPolicyEnabled = customer
            .getPasswordRotationEnabled();

        if (passwordRotationPolicyEnabled != null
            && passwordRotationPolicyEnabled) {
            int passwordRotationDurationInDays = customer
                .getPasswordRotationDuration();

            DateTime timeOfLastPwdChange = user.getPasswordObj()
                .getLastUpdated();

            passwordExpirationDate = timeOfLastPwdChange
                .plusDays(passwordRotationDurationInDays);
        }
        logger
            .debug("Password expiration date set: {}", passwordExpirationDate);

        return passwordExpirationDate;
    }

    @Override
    public User checkAndGetUser(String customerId, String username) {
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
    public Clients getUserServices(User user) {
        if (user == null || user.getUniqueId() == null) {
            String errmsg = "Null User instance or is lacking uniqueID";
            logger.debug(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        List<ScopeAccess> services = this.scopeAccessDao
            .getScopeAccessesByParent(user.getUniqueId());

        List<Client> clientList = new ArrayList<Client>();

        for (ScopeAccess service : services) {
            if (service instanceof UserScopeAccess) {
                clientList
                    .add(this.clientService.getById(service.getClientId()));
            }
        }

        Clients clients = new Clients();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());
        logger.debug("Found Clients: {}.", clients);

        return clients;
    }

    private boolean isTrustedServer() {
        return config.getBoolean("ldap.server.trusted", false);
    }
}
