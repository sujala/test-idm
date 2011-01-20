package com.rackspace.idm.services;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.*;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.jaxb.CustomParam;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.util.HashHelper;
import com.rackspace.idm.util.TemplateProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DefaultUserService implements UserService {

    private static final String PASSWORD_RECOVERY_URL = "%s?username=%s&token=%s";
    private static final String NEWLINE = System.getProperty("line.separator");

    private UserDao userDao;
    private AuthDao authDao;
    private CustomerDao customerDao;
    private AccessTokenDao tokenDao;
    private RefreshTokenDao refreshTokenDao;
    private ClientDao clientDao;
    private EmailService emailService;
    private RoleService roleService;
    private TemplateProcessor tproc = new TemplateProcessor();
    private Logger logger;
    private boolean isTrustedServer;

    public DefaultUserService(UserDao userDao, AuthDao rackerDao, CustomerDao customerDao, AccessTokenDao tokenDao,
                              RefreshTokenDao refreshTokenDao, ClientDao clientDao, EmailService emailService,
                              RoleService roleService, boolean isTrusted, Logger logger) {

        this.userDao = userDao;
        this.authDao = rackerDao;
        this.customerDao = customerDao;
        this.tokenDao = tokenDao;
        this.refreshTokenDao = refreshTokenDao;
        this.clientDao = clientDao;
        this.emailService = emailService;
        this.roleService = roleService;

        this.isTrustedServer = isTrusted;
        this.logger = logger;
    }

    public void addUser(User user) throws DuplicateException {
        logger.info("Adding User: {}", user);
        String customerId = user.getCustomerId();

        boolean isUsernameUnique = userDao.isUsernameUnique(user.getUsername());

        if (!isUsernameUnique) {
            logger.warn("Couldn't add user {} because username already taken", user);
            throw new DuplicateException(String.format("Username %s already exists", user.getUsername()));
        }

        Customer customer = customerDao.findByCustomerId(customerId);

        if (customer == null) {
            logger.warn("Couldn't add user {} because customer doesn't exist", user);
            throw new IllegalStateException("Customer doesn't exist");
        }

        String customerDN = customerDao.getCustomerDnByCustomerId(customerId);

        user.setOrgInum(customer.getInum());
        user.setInum(userDao.getUnusedUserInum(customer.getInum()));
        user.setStatus(UserStatus.ACTIVE);

        userDao.add(user, customerDN);
        logger.info("Added User: {}", user);
    }

    public boolean authenticateDeprecated(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("username or password parameter is null or blank.");
        }

        logger.debug("Authenticating User: {}", username);
        boolean authenticated = false;
        if (isTrustedServer) {
            authenticated = authDao.authenticate(username, password);
            logger.debug("Authenticated Racker {} : {}", username, authenticated);
            return authenticated;
        }

        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.debug("User {} not found.", username);
            return false;
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            logger.debug("User {} is not active: ", username);
            return false;
        }

        authenticated = userDao.bindUser(username, password);
        logger.debug("Authenticated User: {} : {}", username, authenticated);
        return authenticated;
    }

    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("username or password parameter is null or blank.");
        }

        logger.debug("Authenticating User: {}", username);
        boolean authenticated;
        if (isTrustedServer) {
            authenticated = authDao.authenticate(username, password);
            logger.debug("Authenticated Racker {} : {}", username, authenticated);
            return new UserAuthenticationResult(new BaseUser(username), authenticated);
        }

        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.debug("User {} not found.", username);
            return new UserAuthenticationResult(null, false);
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            logger.debug("User {} is not active: ", username);
            return new UserAuthenticationResult(user, false);
        }

        authenticated = userDao.bindUser(username, password);
        logger.debug("Authenticated User: {} : {}", username, authenticated);
        return new UserAuthenticationResult(user, authenticated);
    }

    public UserAuthenticationResult authenticateWithApiKey(String username, String apiKey) {
        logger.debug("Authenticating User: {} by API Key", username);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(username, apiKey);
        logger.debug("Authenticated User: {} by API Key - {}", username, authenticated);
        return authenticated;
    }

    public UserAuthenticationResult authenticateWithNastIdAndApiKey(String nastId, String apiKey) {
        logger.debug("Authenticating User with NastId {} and API Key", nastId);
        UserAuthenticationResult authenticated = userDao.authenticateByNastIdAndAPIKey(nastId, apiKey);
        logger.debug("Authenticated User with NastId {} and API Key - {}", nastId, authenticated);
        return authenticated;
    }

    public UserAuthenticationResult authenticateWithMossoIdAndApiKey(int mossoId, String apiKey) {
        logger.debug("Authenticating User with MossoId {} and Api Key", mossoId);
        UserAuthenticationResult authenticated = userDao.authenticateByMossoIdAndAPIKey(mossoId, apiKey);
        logger.debug("Authenticated User with MossoId {} and API Key - {}", mossoId, authenticated);
        return authenticated;
    }

    public void deleteUser(String username) {
        logger.info("Deleting User: {}", username);
        this.userDao.delete(username);
        logger.info("Deleted User: {}", username);
    }

    public Users getByCustomerId(String customerId, int offset, int limit) {
        logger.debug("Getting Users for Cutomer: {}", customerId);

        // FIXME: read the default offset and limit from config file
        // instead of the Constants class.

        if (offset < GlobalConstants.LDAP_PAGING_DEFAULT_OFFSET) {
            offset = GlobalConstants.LDAP_PAGING_DEFAULT_OFFSET;
        }

        if (limit < 1) {
            limit = GlobalConstants.LDAP_PAGING_DEFAULT_LIMIT;
        } else if (limit > GlobalConstants.LDAP_PAGING_MAX_LIMIT) {
            limit = GlobalConstants.LDAP_PAGING_MAX_LIMIT;
        }

        Users users = this.userDao.findByCustomerId(customerId, offset, limit);

        logger.debug("Got Users for Customer: {}", customerId);

        return users;
    }

    public User getUser(String username) {
        logger.debug("Getting User: {}", username);
        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.debug("No user found for user name {}", username);
            return null;
        }

        if (StringUtils.isBlank(user.getInum())) {
            String msg = String.format("User %s is missing iNum.", username);
            logger.debug(msg);
            throw new IllegalStateException(msg);
        }

        user.setRoles(roleService.getRolesForUser(user.getUsername()));
        logger.debug("Got User: {}", user);
        return user;
    }

    public User getUserByNastId(String nastId) {
        logger.debug("Getting User: {}", nastId);
        User user = userDao.findByNastId(nastId);
        if (user != null) {
            user.setRoles(roleService.getRolesForUser(user.getUsername()));
        }
        logger.debug("Got User: {}", user);
        return user;
    }

    public User getUserByMossoId(int mossoId) {
        logger.debug("Getting User: {}", mossoId);
        User user = userDao.findByMossoId(mossoId);
        if (user != null) {
            user.setRoles(roleService.getRolesForUser(user.getUsername()));
        }
        logger.debug("Got User: {}", user);
        return user;
    }

    public User getUser(String customerId, String username) {
        logger.debug("Getting User: {} - {}", customerId, username);
        User user = userDao.findUser(customerId, username);
        if (user == null) {
            return null;
        }
        user.setRoles(roleService.getRolesForUser(username));
        logger.debug("Got User: {}", user);
        return user;
    }

    public User getSoftDeletedUser(String customerId, String username) {

        logger.debug("Getting User: {} - {}", customerId, username);

        Map<String, String> userStatusMap = new HashMap<String, String>();
        userStatusMap.put(GlobalConstants.ATTR_SOFT_DELETED, "TRUE");

        User user = userDao.findUser(customerId, username, userStatusMap);

        logger.debug("Got User: {}", user);

        return user;
    }

    public void restoreSoftDeletedUser(User user) {
        logger.info("Restoring Soft Deleted User: {}", user.getUsername());

        user.setSoftDeleted(false);
        Map<String, String> userStatusMap = new HashMap<String, String>();
        userStatusMap.put(GlobalConstants.ATTR_SOFT_DELETED, "TRUE");
        this.userDao.saveRestoredUser(user, userStatusMap);
        logger.info("Restoring Soft Deleted User done.: {}", user.getUsername());
    }

    public void sendRecoveryEmail(String username, String userEmail, PasswordRecovery recoveryParam,
                                  String tokenString) {
        logger.debug("Sending password recovery email for User: {}", username);

        List<String> recipients = new ArrayList<String>();
        recipients.add(userEmail);

        String link = String.format(PASSWORD_RECOVERY_URL, recoveryParam.getCallbackUrl(), username, tokenString);
        String message = getEmailMessageBody(link, recoveryParam);
        try {
            emailService.sendEmail(recipients, recoveryParam.getFrom(), recoveryParam.getSubject(), message);
        } catch (EmailException e) {
            logger.error("Could not send password recovery email for " + username, e);
            throw new IllegalStateException("Could not send email!", e);
        }

        logger.debug("Sent password recovery email for User: {}", username);

    }

    private String getEmailMessageBody(String recoveryUrl, PasswordRecovery recoveryParam) {
        String message = String.format("Here's your recovery link: %s", recoveryUrl);
        String templateUrl = recoveryParam.getTemplateUrl();
        if (StringUtils.isBlank(templateUrl)) {
            return message;
        }

        // Build parameters lookup
        Map<String, String> params = getCustomParamsMap(recoveryParam);

        // Substitute param values
        try {
            URL url = new URL(templateUrl);
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                String subbedLine = tproc.getSubstituedOutput(line, params);
                if (StringUtils.contains(subbedLine, "{{")) {
                    // The template has an unknown parameter. Bail out.
                    return message;
                }
                sb.append(subbedLine);
                sb.append(NEWLINE);
            }
            return sb.toString();
        } catch (MalformedURLException mue) {
            logger.error("Could not retrieve template from URL " + templateUrl, mue);
            // Just use the default message body
            return message;
        } catch (IOException ie) {
            logger.error("Could not connect to URL " + templateUrl, ie);
            // Just use the default message body
            return message;
        }
    }

    private Map<String, String> getCustomParamsMap(PasswordRecovery recoveryParam) {
        List<CustomParam> customParams = null;
        if (recoveryParam.getCustomParams() == null) {
            customParams = new ArrayList<CustomParam>();
        } else {
            customParams = recoveryParam.getCustomParams().getParams();
        }
        Map<String, String> params = new HashMap<String, String>();
        for (CustomParam param : customParams) {
            if (StringUtils.isBlank(param.getName()) || StringUtils.isBlank(param.getValue())) {
                continue;
            }
            params.put(param.getName(), param.getValue());
        }
        return params;
    }

    public void softDeleteUser(String username) {
        logger.info("Soft Deleting User: {}", username);
        User user = this.userDao.findByUsername(username);
        user.setSoftDeleted(true);
        this.userDao.save(user);

        // revoke user's tokens
        String owner = user.getInum();
        Set<String> allClientInums = new HashSet<String>();
        Set<String> allClientIds = new HashSet<String>();

        for (Client client : clientDao.findAll()) {
            allClientInums.add(client.getInum());
            allClientIds.add(client.getClientId());
        }
        tokenDao.deleteAllTokensForOwner(owner, Collections.unmodifiableSet(allClientInums));

        refreshTokenDao.deleteAllTokensForUser(username, Collections.unmodifiableSet(allClientIds));

        logger.info("Soft Deleted User: {}", username);
    }

    public void updateUser(User user) {
        logger.info("Updating User: {}", user);
        this.userDao.save(user);
        logger.info("Updated User: {}", user);
    }

    public void updateUserStatus(User user, String statusStr) {

        UserStatus status = Enum.valueOf(UserStatus.class, statusStr.toUpperCase());
        user.setStatus(status);
        this.userDao.save(user);

        logger.info("Updated User's status: {}, {}", user, status);

        // revoke user's token if disabling
        if (status.equals(UserStatus.INACTIVE)) {
            String owner = user.getInum();
            Set<String> allClientInums = new HashSet<String>();
            for (Client client : clientDao.findAll()) {
                allClientInums.add(client.getInum());
            }
            tokenDao.deleteAllTokensForOwner(owner, Collections.unmodifiableSet(allClientInums));
        }
    }

    public String generateApiKey() {
        try {
            return HashHelper.getRandomSha1();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("The JVM does not support the algorithm needed.", e);
        }
    }

    public boolean isUsernameUnique(String username) {
        return userDao.isUsernameUnique(username);
    }
}
