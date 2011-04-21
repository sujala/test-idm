package com.rackspace.idm.domain.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.EmailService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.CustomParam;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.jaxb.UserCredentials;
import com.rackspace.idm.util.HashHelper;
import com.rackspace.idm.util.TemplateProcessor;
import com.rackspace.idm.validation.RegexPatterns;

public class DefaultUserService implements UserService {

    private static final String PASSWORD_RECOVERY_URL = "%s?username=%s&token=%s";
    private static final String NEWLINE = System.getProperty("line.separator");
    private static final Pattern emailPattern = Pattern
        .compile(RegexPatterns.EMAIL_ADDRESS);

    private final UserDao userDao;
    private final AuthDao authDao;
    private final CustomerDao customerDao;
    private final EmailService emailService;
    private final ClientService clientService;

    private final TemplateProcessor tproc = new TemplateProcessor();
    private final boolean isTrustedServer;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultUserService(UserDao userDao, AuthDao rackerDao,
        CustomerDao customerDao, EmailService emailService,
        ClientService clientService, boolean isTrusted) {

        this.userDao = userDao;
        this.authDao = rackerDao;
        this.customerDao = customerDao;
        this.emailService = emailService;
        this.clientService = clientService;
        this.isTrustedServer = isTrusted;
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

        if (isTrustedServer) {
            boolean authenticated = authDao.authenticate(username, password);
            logger.debug("Authenticated Racker {} : {}", username,
                authenticated);
            BaseUser user = new BaseUser(username);
            return new UserAuthenticationResult(user, authenticated);
        }

        UserAuthenticationResult result = userDao.authenticate(username,
            password);

        if (result.isAuthenticated()) {
            List<ClientGroup> groups = this.clientService
                .getClientGroupsForUser(result.getUser().getUsername());

            BaseUser baseUser = new BaseUser(result.getUser().getUniqueId(),
                result.getUser().getUsername(), result.getUser()
                    .getCustomerId(), groups);

            result = new UserAuthenticationResult(baseUser,
                result.isAuthenticated());
        }
        logger.debug("Authenticated User: {} : {}", username, result);
        return result;
    }

    @Override
    public UserAuthenticationResult authenticateWithApiKey(String username,
        String apiKey) {
        logger.debug("Authenticating User: {} by API Key", username);
        UserAuthenticationResult authenticated = userDao.authenticateByAPIKey(
            username, apiKey);
        if (authenticated.isAuthenticated()) {
            List<ClientGroup> groups = this.clientService
                .getClientGroupsForUser(authenticated.getUser().getUsername());
            BaseUser baseUser = new BaseUser(authenticated.getUser()
                .getUsername(), authenticated.getUser().getCustomerId(), groups);

            authenticated = new UserAuthenticationResult(baseUser,
                authenticated.isAuthenticated());
        }
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
        if (authenticated.isAuthenticated()) {
            List<ClientGroup> groups = this.clientService
                .getClientGroupsForUser(authenticated.getUser().getUsername());
            BaseUser baseUser = new BaseUser(authenticated.getUser()
                .getUsername(), authenticated.getUser().getCustomerId(), groups);

            authenticated = new UserAuthenticationResult(baseUser,
                authenticated.isAuthenticated());
        }
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
        if (authenticated.isAuthenticated()) {
            List<ClientGroup> groups = this.clientService
                .getClientGroupsForUser(authenticated.getUser().getUsername());
            BaseUser baseUser = new BaseUser(authenticated.getUser()
                .getUsername(), authenticated.getUser().getCustomerId(), groups);

            authenticated = new UserAuthenticationResult(baseUser,
                authenticated.isAuthenticated());
        }
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

        if (user == null) {
            logger.warn("No user found for user name {}", username);
            return null;
        }

        if (StringUtils.isBlank(user.getInum())) {
            String msg = String.format("User %s is missing iNum.", username);
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }

        user.setGroups(clientService.getClientGroupsForUser(user.getUsername()));
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUser(String customerId, String username) {
        logger.debug("Getting User: {} - {}", customerId, username);
        User user = userDao
            .getUserByCustomerIdAndUsername(customerId, username);
        if (user == null) {
            return null;
        }
        user.setGroups(clientService.getClientGroupsForUser(user.getUsername()));
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUserByMossoId(int mossoId) {
        logger.debug("Getting User: {}", mossoId);
        User user = userDao.getUserByMossoId(mossoId);
        if (user != null) {
            user.setGroups(clientService.getClientGroupsForUser(user
                .getUsername()));
        }
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUserByNastId(String nastId) {
        logger.debug("Getting User: {}", nastId);
        User user = userDao.getUserByNastId(nastId);
        if (user != null) {
            user.setGroups(clientService.getClientGroupsForUser(user
                .getUsername()));
        }
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public User getUserByRPN(String rpn) {
        logger.debug("Getting User: {}", rpn);
        User user = userDao.getUserByRPN(rpn);

        if (user != null) {
            user.setGroups(clientService.getClientGroupsForUser(user
                .getUsername()));
        }
        logger.debug("Got User: {}", user);
        return user;
    }

    @Override
    public boolean isUsernameUnique(String username) {
        return userDao.isUsernameUnique(username);
    }

    @Override
    public void setUserPassword(String customerId, String username,
        UserCredentials userCred, AccessToken token, boolean isRecovery) {

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
        boolean isSelfUpdate = token.getOwner().equals(username);

        this.updateUser(user, isSelfUpdate);
        logger.debug("Updated password for user: {}", user);

    }

    @Override
    public void sendRecoveryEmail(String username, String userEmail,
        PasswordRecovery recoveryParam, String tokenString) {
        logger.debug("Sending password recovery email for User: {}", username);

        // validate from address
        String fromEmail = recoveryParam.getFrom();
        Matcher m = emailPattern.matcher(fromEmail);
        boolean matchFound = m.matches();
        if (!matchFound) {
            String errorMsg = "Invalid from address";
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // validate reply-to address
        String replyToEmail = recoveryParam.getReplyTo();
        if (replyToEmail == null) {
            replyToEmail = fromEmail;
            recoveryParam.setReplyTo(replyToEmail);
        }

        Matcher replyToMatcher = emailPattern.matcher(replyToEmail);
        matchFound = replyToMatcher.matches();
        if (!matchFound) {
            String errorMsg = "Invalid reply-to address";
            logger.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        List<String> recipients = new ArrayList<String>();
        recipients.add(userEmail);

        String link = String.format(PASSWORD_RECOVERY_URL,
            recoveryParam.getCallbackUrl(), username, tokenString);
        String message = getEmailMessageBody(link, recoveryParam);
        try {
            emailService.sendEmail(recipients, recoveryParam.getFrom(),
                recoveryParam.getSubject(), message);
        } catch (EmailException e) {
            logger.error("Could not send password recovery email for "
                + username, e);
            throw new IllegalStateException("Could not send email!", e);
        }

        logger.debug("Sent password recovery email for User: {}", username);

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
            return null;
        }

        Customer customer = customerDao.getCustomerByCustomerId(user
            .getCustomerId());

        if (customer == null) {
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

    private Map<String, String> getCustomParamsMap(
        PasswordRecovery recoveryParam) {
        List<CustomParam> customParams = null;
        if (recoveryParam.getCustomParams() == null) {
            customParams = new ArrayList<CustomParam>();
        } else {
            customParams = recoveryParam.getCustomParams().getParams();
        }
        Map<String, String> params = new HashMap<String, String>();
        for (CustomParam param : customParams) {
            if (StringUtils.isBlank(param.getName())
                || StringUtils.isBlank(param.getValue())) {
                continue;
            }
            params.put(param.getName(), param.getValue());
        }
        return params;
    }

    private String getEmailMessageBody(String recoveryUrl,
        PasswordRecovery recoveryParam) {
        String message = String.format("Here's your recovery link: %s",
            recoveryUrl);
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
            logger.error("Could not retrieve template from URL " + templateUrl,
                mue);
            // Just use the default message body
            return message;
        } catch (IOException ie) {
            logger.error("Could not connect to URL " + templateUrl, ie);
            // Just use the default message body
            return message;
        }
    }
}
