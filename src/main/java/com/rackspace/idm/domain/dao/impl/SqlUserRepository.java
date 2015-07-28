package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.dao.UserRepository;
import com.rackspace.idm.domain.sql.entity.SqlUser;
import com.rackspace.idm.domain.sql.mapper.impl.GroupMapper;
import com.rackspace.idm.domain.sql.mapper.impl.UserMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.security.MessageDigest;
import java.util.*;

@SQLComponent
public class SqlUserRepository implements UserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlUserRepository.class);

    public static final String NULL_OR_EMPTY_USERNAME_PARAMETER = "Null or Empty username parameter";
    private static final List<String> AUTH_BY_PASSWORD_LIST = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD);
    private static final List<String> AUTH_BY_API_KEY_LIST = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY);

    private static final String SSHA512 = "{SSHA512}";
    private static final int SHA512_SIZE = 64;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private Configuration config;

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public void addUser(User user) {
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextUserId());
        }
        encryptionService.setUserEncryptionSaltAndVersion(user);
        userRepository.save(userMapper.toSQL(user));
    }

    @Override
    public User getUserById(String id) {
        return userMapper.fromSQL(userRepository.findOne(id));
    }

    @Override
    public void deleteUser(User user) {
        userRepository.delete(user.getId());
    }

    @Override
    public void deleteUser(String username) {
        userRepository.deleteByUsername(username);
    }

    @Override
    public Iterable<User> getUsers() {
        return userMapper.fromSQL(userRepository.findAll());
    }

    @Override
    public Iterable<User> getUsers(List<String> idList) {
        return userMapper.fromSQL(userRepository.findAll(idList));
    }

    @Override
    public Iterable<User> getUsersByUsername(String username) {
        return userMapper.fromSQL(userRepository.findByUsername(username));
    }

    @Override
    public User getUserByUsername(String username) {
        final SqlUser sqlUser = userRepository.findOneByUsername(username);
        return sqlUser == null ? null : userMapper.fromSQL(sqlUser);
    }

    @Override
    public boolean isUsernameUnique(String username) {
        return userRepository.countByUsername(username) == 0;
    }

    @Override
    public UserAuthenticationResult authenticate(String username, String password) {
        LOGGER.debug("Authenticating User {}", username);
        if (StringUtils.isBlank(username)) {
            LOGGER.error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            throw new IllegalArgumentException(NULL_OR_EMPTY_USERNAME_PARAMETER);
        }

        final User user = getUserByUsername(username);
        LOGGER.debug("Found user {}, authenticating...", user);
        return authenticateByPassword(user, password);
    }

    private UserAuthenticationResult authenticateByPassword(User user, String password) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        boolean isAuthenticated = checkPassword(user, password);
        final UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated, AUTH_BY_PASSWORD_LIST);
        LOGGER.debug("Authenticated User by password");

        addAuditLogForAuthentication(user, isAuthenticated);

        return authResult;
    }

    private boolean checkPassword(User user, String password) {
        final String userPassword = user.getPassword();
        if (userPassword.startsWith(SSHA512)) {
            return verifyLegacySHA(password, userPassword);
        } else {
            return verifyCrypt(password, userPassword);
        }
    }

    private boolean verifyLegacySHA(String password, String userPassword) {
        try {
            final String sha = userPassword.substring(SSHA512.length());
            final byte[] bytes = Base64.decodeBase64(sha);

            final byte[] salt = new byte[bytes.length - SHA512_SIZE];
            System.arraycopy(bytes, SHA512_SIZE, salt, 0, salt.length);

            final byte[] hash = new byte[SHA512_SIZE];
            System.arraycopy(bytes, 0, hash, 0, SHA512_SIZE);

            final MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(password.getBytes());
            digest.update(salt);
            final byte[] newHash = digest.digest();

            return Arrays.equals(hash, newHash);
        } catch (Exception e) {
            LOGGER.debug("Cannot verify legacy SHA", e);
            return false;
        }
    }

    private boolean verifyCrypt(String password, String userPassword) {
        try {
            final String[] split = userPassword.split("\\$");
            final String hash = split[split.length - 1];
            final String salt = userPassword.replace(hash, "").replaceFirst("\\$$", "");
            final String newCalc = Crypt.crypt(password, salt);
            return userPassword.equalsIgnoreCase(newCalc);
        } catch (Exception e) {
            LOGGER.debug("Cannot verify crypt", e);
            return false;
        }
    }

    private void addAuditLogForAuthentication(User user, boolean authenticated) {
        final Audit audit = Audit.authUser(user);
        if (authenticated) {
            audit.succeed();
        } else {
            String failureMessage = "User Authentication Failed: %s";

            if (getMaxLoginFailuresExceeded(user)) {
                failureMessage = String.format(failureMessage, "User locked due to max login failures limit exceded");
            } else if (user.isDisabled()) {
                failureMessage = String.format(failureMessage, "User is Disabled");
            } else {
                failureMessage = String.format(failureMessage, "Incorrect Credentials");
            }

            audit.fail(failureMessage);
        }
    }

    private boolean getMaxLoginFailuresExceeded(User user) {
        boolean passwordFailureLocked = false;
        if (user.getPasswordFailureDate() != null) {
            final DateTime passwordFailureDateTime = new DateTime(user.getPasswordFailureDate())
                    .plusMinutes(this.getLdapPasswordFailureLockoutMin());
            passwordFailureLocked = passwordFailureDateTime.isAfterNow();
        }
        return passwordFailureLocked;
    }

    protected int getLdapPasswordFailureLockoutMin() {
        return config.getInt("ldap.password.failure.lockout.min");
    }

    @Override
    public UserAuthenticationResult authenticateByAPIKey(String username, String apiKey) {
        LOGGER.debug("Authenticating User {} by API Key ", username);
        if (StringUtils.isBlank(username)) {
            LOGGER.error(NULL_OR_EMPTY_USERNAME_PARAMETER);
            throw new IllegalArgumentException(NULL_OR_EMPTY_USERNAME_PARAMETER);
        }

        final User user = getUserByUsername(username);
        LOGGER.debug("Found user {}, authenticating...", user);
        return authenticateUserByApiKey(user, apiKey);
    }

    private UserAuthenticationResult authenticateUserByApiKey(User user, String apiKey) {
        if (user == null) {
            return new UserAuthenticationResult(null, false);
        }

        final boolean isAuthenticated = !StringUtils.isBlank(user.getApiKey())  && user.getApiKey().equals(apiKey);

        final UserAuthenticationResult authResult = new UserAuthenticationResult(user, isAuthenticated, AUTH_BY_API_KEY_LIST);
        LOGGER.debug("Authenticated User by API Key - {}", authResult);

        addAuditLogForAuthentication(user, isAuthenticated);

        return authResult;
    }

    @Override
    public Iterable<User> getUsersByDomain(String domainId) {
        return userMapper.fromSQL(userRepository.findByDomainId(domainId));
    }

    @Override
    public PaginatorContext<User> getUsersByDomain(String domainId, int offset, int limit) {
        final PageRequest pageRequest = userMapper.getPageRequest(offset, limit);
        return userMapper.fromSQL(userRepository.findByDomainId(domainId, pageRequest), offset, limit);
    }

    @Override
    public PaginatorContext<User> getUsers(int offset, int limit) {
        final PageRequest pageRequest = userMapper.getPageRequest(offset, limit);
        return userMapper.fromSQL(userRepository.findAll(pageRequest), offset, limit);
    }

    @Override
    public PaginatorContext<User> getEnabledUsers(int offset, int limit) {
        final PageRequest pageRequest = userMapper.getPageRequest(offset, limit);
        return userMapper.fromSQL(userRepository.findByEnabledTrue(pageRequest), offset, limit);
    }

    @Override
    public String getNextUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void updateUser(User user) {
        updateUser(user, true);
    }

    @Override
    public void updateUserAsIs(User user) {
        updateUser(user, false);
    }

    private void updateUser(User user, boolean ignoreNulls) {
        final SqlUser sqlUser = userRepository.findOne(user.getId());
        userMapper.toSQL(user, sqlUser, ignoreNulls);
        userRepository.save(sqlUser);
    }

    @Override
    public Iterable<User> getUsersByDomainAndEnabledFlag(String domainId, boolean enabled) {
        return userMapper.fromSQL(userRepository.findByDomainIdAndEnabled(domainId, enabled));
    }

    @Override
    public Iterable<Group> getGroupsForUser(String userId) {
        try {
            final SqlUser sqlUser = userRepository.findOne(userId);
            return groupMapper.fromSQL(groupRepository.findAll(sqlUser.getRsGroupId()));
        } catch (Exception e) {
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public void addGroupToUser(String userId, String groupId) {
        final SqlUser sqlUser = userRepository.findOne(userId);
        if (sqlUser.getRsGroupId() == null) {
            sqlUser.setRsGroupId(new ArrayList<String>());
        }
        sqlUser.getRsGroupId().add(groupId);
        userRepository.save(sqlUser);
    }

    @Override
    public void deleteGroupFromUser(String groupId, String userId) {
        final SqlUser sqlUser = userRepository.findOne(userId);
        if (sqlUser.getRsGroupId() != null) {
            sqlUser.getRsGroupId().remove(groupId);
            userRepository.save(sqlUser);
        }
    }

    @Override
    public PaginatorContext<User> getEnabledUsersByGroupId(String groupId, int offset, int limit) {
        final PageRequest pageRequest = userMapper.getPageRequest(offset, limit);
        return userMapper.fromSQL(userRepository.findByEnabledAndRsGroupIdIn(true, Collections.singleton(groupId), pageRequest), offset, limit);
    }

    @Override
    public Iterable<User> getEnabledUsersByGroupId(String groupId) {
        return userMapper.fromSQL(userRepository.findByEnabledAndRsGroupIdIn(true, Collections.singleton(groupId)));
    }

    @Override
    public Iterable<User> getDisabledUsersByGroupId(String groupId) {
        return userMapper.fromSQL(userRepository.findByEnabledAndRsGroupIdIn(false, Collections.singleton(groupId)));
    }

    @Override
    public Iterable<User> getUsersByEmail(String email) {
        // FIXME: Optimize this or somehow index the "extra" column
        return userMapper.fromSQL(userRepository.findByExtraContains("\"" + email + "\""));
    }

    @Override
    public void doPostEncode(User user) {
        encryptionService.decryptUser(user);
    }

    @Override
    public void doPreEncode(User user) {
        encryptionService.encryptUser(user);
    }

    /*** ------------------------------------------ ***/

    @Override
    public PaginatorContext<User> getUsersToReEncrypt(int offset, int limit) {
        // NO-OP operation (FIXME: remove this)
        return new PaginatorContext<User>();
    }

    @Override
    public void updateUserEncryption(String userId) {
        // NO-OP operation (FIXME: remove this)
    }

}
