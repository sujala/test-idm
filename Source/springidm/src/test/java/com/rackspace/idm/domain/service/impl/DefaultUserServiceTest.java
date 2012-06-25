package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/1/11
 * Time: 11:26 AM
 */
public class DefaultUserServiceTest {

    private DefaultUserService defaultUserService;
    private UserDao userDao;
    private AuthDao authDao;
    private ScopeAccessDao scopeAccessDao;
    private ApplicationService applicationService;
    private Configuration config;
    private TokenService tokenService;
    private PasswordComplexityService passwordComplexityService;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;

    @Before
    public void setUp() throws Exception {
        userDao = mock(UserDao.class);
        authDao = mock(AuthDao.class);
        scopeAccessDao = mock(ScopeAccessDao.class);
        applicationService = mock(ApplicationService.class);
        config = mock(Configuration.class);
        tokenService = mock(TokenService.class);
        passwordComplexityService = mock(PasswordComplexityService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);

        defaultUserService = new DefaultUserService(userDao, authDao, scopeAccessDao, applicationService, config, tokenService, passwordComplexityService);

        defaultUserService.setAuthorizationService(authorizationService);
        defaultUserService.setScopeAccessService(scopeAccessService);
    }

    @Test
    public void userExistsById_inMigration_returnsFalse() throws Exception {
        User user = new User();
        user.setInMigration(true);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsById_inMigrationFalse_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(false);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsById_inMigrationIsNull_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(null);
        when(userDao.getUserById("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsById_userIsNull_returnsFalse() throws Exception {
        when(userDao.getUserById("test")).thenReturn(null);
        boolean exists = defaultUserService.userExistsById("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsByUsername_inMigration_returnsFalse() throws Exception {
        User user = new User();
        user.setInMigration(true);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void userExistsByUsername_inMigrationFalse_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(false);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsByUsername_inMigrationIsNull_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(null);
        when(userDao.getUserByUsername("test")).thenReturn(user);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(true));
    }

    @Test
    public void userExistsByUsername_userIsNull_returnsFalse() throws Exception {
        when(userDao.getUserByUsername("test")).thenReturn(null);
        boolean exists = defaultUserService.userExistsByUsername("test");
        assertThat("exists", exists, equalTo(false));
    }

    @Test
    public void validateMossoId_callsUserDAO_getUserByMossoId() throws Exception {
        defaultUserService.validateMossoId(1);
        verify(userDao).getUsersByMossoId(1);
    }

    @Test(expected = BadRequestException.class)
    public void validateMossoId_withExistingUserWithMossoId_throwsBadRequestException() throws Exception {
        Users users = new Users();
        List<User> userList = new ArrayList<User>();
        User testUser = new User("testUser");
        userList.add(testUser);
        users.setUsers(userList);
        testUser.setMossoId(1);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        defaultUserService.validateMossoId(1);
    }

    @Test
    public void validateMossoId_withExistingUserWithMossoId_throwsBadRequestException_returnsCorrectMessage() throws Exception {
        Users users = new Users();
        List<User> userList = new ArrayList<User>();
        User testUser = new User("testUser");
        userList.add(testUser);
        users.setUsers(userList);
        testUser.setMossoId(1);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        try{
            defaultUserService.validateMossoId(1);
        }catch (Exception e){
            assertThat("exception message", e.getMessage(), Matchers.equalTo("User with Mosso Account ID: 1 already exists."));
        }
    }

    @Test
    public void userExistsById_callsUserDao_getUserById() throws Exception {
        defaultUserService.userExistsById("id");
        verify(userDao).getUserById("id");
    }

    @Test
    public void userExistsByUsername_callsUserDao_getUserByUsername() throws Exception {
        defaultUserService.userExistsByUsername("id");
        verify(userDao).getUserByUsername("id");
    }

    @Test
    public void updateUserById_callsUserDaoUpdateByUd() throws Exception {
        User user = new User();
        user.setUsername("user");
        user.setId("id");
        when(userDao.isUsernameUnique(anyString())).thenReturn(true);
        defaultUserService.updateUserById(user, false );
        verify(userDao).updateUserById(user,false);
    }

    @Test(expected = DuplicateUsernameException.class)
    public void updateUserById_throwsDuplicateUsernameException() throws Exception {
        User user = new User();
        user.setUsername("user");
        user.setId("id");
        when(userDao.isUsernameUnique(anyString())).thenReturn(false);
        defaultUserService.updateUserById(user, false );
    }

    @Test
    public void authenticateWithMossoIdAndApiKey_callsUserDao_authenticateByAPIKey() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        defaultUserService.authenticateWithMossoIdAndApiKey(1, "apiKey");
        verify(userDao).authenticateByAPIKey("username", "apiKey");
    }

    @Test
    public void authenticateWithMossoIdAndApiKey_returns() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        when(userDao.authenticateByAPIKey("username", "apiKey")).thenReturn(new UserAuthenticationResult(user, true));
        UserAuthenticationResult result = defaultUserService.authenticateWithMossoIdAndApiKey(1, "apiKey");
        assertThat("username", result.getUser().getUsername(), equalTo("username"));
    }

    @Test
    public void authenticateWithNastIdAndApiKey_callsUserDao_authenticateByAPIKey() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByNastId("nastId")).thenReturn(users);
        defaultUserService.authenticateWithNastIdAndApiKey("nastId", "apiKey");
        verify(userDao).authenticateByAPIKey("username", "apiKey");
    }

    @Test
    public void authenticateWithNastIdAndApiKey_returns() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByNastId("nastId")).thenReturn(users);
        when(userDao.authenticateByAPIKey("username", "apiKey")).thenReturn(new UserAuthenticationResult(user, true));
        UserAuthenticationResult result = defaultUserService.authenticateWithNastIdAndApiKey("nastId", "apiKey");
        assertThat("username", result.getUser().getUsername(), equalTo("username"));
    }

    @Test (expected = NotFoundException.class)
    public void loadUser_userIsNull_throwsNotFound() throws Exception {
        defaultUserService.loadUser("userId");
    }

    @Test
    public void loadUser_userNotNull_returnsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        when(userDao.getUserById("userId")).thenReturn(user);
        User foundUser = defaultUserService.loadUser("userId");
        assertThat("username", foundUser.getUsername(), equalTo("username"));
    }

    @Test
    public void deleteUser_callsUserDao_deleteUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        defaultUserService.deleteUser(user);
        verify(userDao).deleteUser(user);
    }

    @Test
    public void generateApiKey_generatesRandomApiKey() throws Exception {
        String randomKey = defaultUserService.generateApiKey();
        assertThat("random apikey", randomKey.length(), not(0));
    }

    @Test
    public void getAllUsers_callsUserDao_getAllUsers() throws Exception {
        defaultUserService.getAllUsers(null, 1, 1);
        verify(userDao).getAllUsers(null, 1, 1);
    }

    @Test
    public void getAllUsers_withJustFilterParam_callsUserDaoGetAllUsers() throws Exception {
        defaultUserService.getAllUsers(null);
        verify(userDao).getAllUsers(any(FilterParam[].class), anyInt(), anyInt());
    }

    @Test (expected = ForbiddenException.class)
    public void getRackerRoles_notTrustedServer_throwsForbiddenException() throws Exception {
        when(config.getBoolean("ldap.server.trusted", false)).thenReturn(false);
        defaultUserService.getRackerRoles("rackerId");
    }

    @Test
    public void getRackerRoles_callsAuthDao_getRackerRoles() throws Exception {
        when(config.getBoolean("ldap.server.trusted", false)).thenReturn(true);
        defaultUserService.getRackerRoles("rackerId");
        verify(authDao).getRackerRoles("rackerId");
    }

    @Test
    public void getRackerRoles_returnRoles() throws Exception {
        List<String> rackeList = new ArrayList<String>();
        rackeList.add("test");
        when(config.getBoolean("ldap.server.trusted", false)).thenReturn(true);
        when(authDao.getRackerRoles("rackerId")).thenReturn(rackeList);
        List<String> roles = defaultUserService.getRackerRoles("rackerId");
        assertThat("roles", roles.get(0), equalTo("test"));
    }

    @Test
    public void getUserByAuthToken_authTokenIsNull_returnsNull() throws Exception {
        User result = defaultUserService.getUserByAuthToken(null);
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByAuthToken_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        ReadOnlyEntry readOnlyEntry = mock(ReadOnlyEntry.class);
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken("authToken")).thenReturn(scopeAccess);
        when(scopeAccess.getLDAPEntry()).thenReturn(readOnlyEntry);
        defaultUserService.getUserByAuthToken("authToken");
        verify(scopeAccessService).getScopeAccessByAccessToken("authToken");
    }

    @Test
    public void getUserByMossoId_usersSizeIsZero_returnsNull() throws Exception {
        List<User> userList = new ArrayList<User>();
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        User result = defaultUserService.getUserByMossoId(1);
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByMossoId_notAuthorizedUserAdmin_returnsNull() throws Exception {
        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(false);
        User result = defaultUserService.getUserByMossoId(1);
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByMossoId_authorizedUserAdmin_returnAdminUser() throws Exception {
        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(true);
        User result = defaultUserService.getUserByMossoId(1);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUsersByMossoId_callsUserDao_getUsersByMossoId() throws Exception {
        defaultUserService.getUsersByMossoId(1);
        verify(userDao).getUsersByMossoId(1);
    }

    @Test
    public void getUsersByMossoId_returnUsers() throws Exception {
        Users users = new Users();
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        Users result = defaultUserService.getUsersByMossoId(1);
        assertThat("users", result, equalTo(users));
    }

    @Test
    public void getUserByNastId_usersSizeIsZero_returnsNull() throws Exception {
        List<User> userList = new ArrayList<User>();
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByNastId("nastId")).thenReturn(users);
        User result = defaultUserService.getUserByNastId("nastId");
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByNastId_notAuthorizedUserAdmin_returnsNull() throws Exception {
        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByNastId("nastId")).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(false);
        User result = defaultUserService.getUserByNastId("nastId");
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByNastId_authorizedUserAdmin_returnAdminUser() throws Exception {
        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByNastId("nastId")).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(true);
        User result = defaultUserService.getUserByNastId("nastId");
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getSoftDeletedUser_callsUserDao_getSoftDeletedUserById() throws Exception {
        defaultUserService.getSoftDeletedUser("id");
        verify(userDao).getSoftDeletedUserById("id");
    }

    @Test
    public void getSoftDeletedUser_returnsUser() throws Exception {
        User user = new User();
        when(userDao.getSoftDeletedUserById("id")).thenReturn(user);
        User result = defaultUserService.getSoftDeletedUser("id");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserApplications_userIsNull_throwsIllegalArgument() throws Exception {
        defaultUserService.getUserApplications(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getUserApplications_userUniqueIdIsNull_throwsIllegalArgument() throws Exception {
        defaultUserService.getUserApplications(new User());
    }

    @Test
    public void getUserApplications_callsScopeAccessDao_getScopeAccessesByParent() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        defaultUserService.getUserApplications(user);
        verify(scopeAccessDao).getScopeAccessesByParent("uniqueId");
    }

    @Test
    public void getUserApplications_callsApplicationService_getById() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setClientId("clientId");
        List<ScopeAccess> services = new ArrayList<ScopeAccess>();
        services.add(userScopeAccess);
        when(scopeAccessDao.getScopeAccessesByParent("uniqueId")).thenReturn(services);
        defaultUserService.getUserApplications(user);
        verify(applicationService).getById("clientId");
    }

    @Test
    public void getUserApplications_notInstanceOfUserScopeAccess_returnsClient() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setClientId("clientId");
        List<ScopeAccess> services = new ArrayList<ScopeAccess>();
        services.add(rackerScopeAccess);
        when(scopeAccessDao.getScopeAccessesByParent("uniqueId")).thenReturn(services);
        Applications result = defaultUserService.getUserApplications(user);
        assertThat("clients", result.getClients().size(), equalTo(0));
    }

    @Test
    public void isUsernameUnique_callsUserDao_isUsernameUnique() throws Exception {
        defaultUserService.isUsernameUnique("username");
        verify(userDao).isUsernameUnique("username");
    }

    @Test
    public void resetUserPassword_callsUserDao_updateUser() throws Exception {
        User user = new User();
        defaultUserService.resetUserPassword(user);
        verify(userDao).updateUser(user, false);
    }

    @Test
    public void resetUserPassword_returnsNewRandomPassword() throws Exception {
        Password result = defaultUserService.resetUserPassword(new User());
        assertThat("password", result.toString().length(), not(0));
    }

    @Test (expected = BadRequestException.class)
    public void setUserPassword_currentPasswordIsNull_throwsBadRequest() throws Exception {
        PasswordCredentials passwordCredentials = mock(PasswordCredentials.class);
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        when(passwordCredentials.getNewPassword()).thenReturn(password);
        when(passwordCredentials.isVerifyCurrentPassword()).thenReturn(true);
        when(userDao.getUserById("userId")).thenReturn(new User());
        defaultUserService.setUserPassword("userId", passwordCredentials, scopeAccess);
    }

    @Test (expected = NotAuthenticatedException.class)
    public void setUserPassword_notAuthenticated_throwsNotAuthenticated() throws Exception {
        User user = new User();
        user.setUsername("username");
        PasswordCredentials passwordCredentials = mock(PasswordCredentials.class);
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        when(passwordCredentials.getNewPassword()).thenReturn(password);
        when(passwordCredentials.isVerifyCurrentPassword()).thenReturn(true);
        when(userDao.getUserById("userId")).thenReturn(user);
        when(passwordCredentials.getCurrentPassword()).thenReturn(password);
        when(userDao.authenticate("username", "aZ23Afdkadzsd")).thenReturn(new UserAuthenticationResult(user, false));
        defaultUserService.setUserPassword("userId", passwordCredentials, scopeAccess);
    }

    @Test
    public void setUserPassword_userScopeAccess_updateUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        PasswordCredentials passwordCredentials = mock(PasswordCredentials.class);
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        when(passwordCredentials.getNewPassword()).thenReturn(password);
        when(passwordCredentials.isVerifyCurrentPassword()).thenReturn(true);
        when(userDao.getUserById("userId")).thenReturn(user);
        when(passwordCredentials.getCurrentPassword()).thenReturn(password);
        when(userDao.authenticate("username", "aZ23Afdkadzsd")).thenReturn(new UserAuthenticationResult(user, true));
        when(scopeAccess.getUsername()).thenReturn("username");
        defaultUserService.setUserPassword("userId", passwordCredentials, scopeAccess);
        verify(userDao).updateUser(user, true);
    }

    @Test
    public void setUserPassword_passwordResetScopeAccess_updateUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        PasswordCredentials passwordCredentials = mock(PasswordCredentials.class);
        PasswordResetScopeAccess scopeAccess = mock(PasswordResetScopeAccess.class);
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        when(passwordCredentials.getNewPassword()).thenReturn(password);
        when(passwordCredentials.isVerifyCurrentPassword()).thenReturn(true);
        when(userDao.getUserById("userId")).thenReturn(user);
        when(passwordCredentials.getCurrentPassword()).thenReturn(password);
        when(userDao.authenticate("username", "aZ23Afdkadzsd")).thenReturn(new UserAuthenticationResult(user, true));
        when(scopeAccess.getUsername()).thenReturn("username");
        defaultUserService.setUserPassword("userId", passwordCredentials, scopeAccess);
        verify(userDao).updateUser(user, true);
    }

    @Test
    public void setUserPassword_neitherUserScopeOrPasswordResetScopeAccess_selfUpdateFalse() throws Exception {
        User user = new User();
        user.setUsername("username");
        PasswordCredentials passwordCredentials = mock(PasswordCredentials.class);
        ImpersonatedScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        when(passwordCredentials.getNewPassword()).thenReturn(password);
        when(passwordCredentials.isVerifyCurrentPassword()).thenReturn(true);
        when(userDao.getUserById("userId")).thenReturn(user);
        when(passwordCredentials.getCurrentPassword()).thenReturn(password);
        when(userDao.authenticate("username", "aZ23Afdkadzsd")).thenReturn(new UserAuthenticationResult(user, true));
        when(scopeAccess.getUsername()).thenReturn("username");
        defaultUserService.setUserPassword("userId", passwordCredentials, scopeAccess);
        verify(userDao).updateUser(user, false);
    }

    @Test (expected = BadRequestException.class)
    public void validateUserEmailAddress_notValidEmail_throwsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("badEmail");
        defaultUserService.addUser(user);
    }

    @Test
    public void validateMossoId_userSizeIsZero_doesNothing() throws Exception {
        List<User> userList = new ArrayList<User>();
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsersByMossoId(1)).thenReturn(users);
        defaultUserService.validateMossoId(1);
    }

    @Test
    public void userExistsById_goesToLastLine_returnTrue() throws Exception {
        User user = mock(User.class);
        when(user.getInMigration()).thenReturn(false).thenReturn(false).thenReturn(true);
        when(userDao.getUserById("id")).thenReturn(user);
        boolean result = defaultUserService.userExistsById("id");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void userExistsByUsername_goesToLastLine_returnTrue() throws Exception {
        User user = mock(User.class);
        when(user.getInMigration()).thenReturn(false).thenReturn(false).thenReturn(true);
        when(userDao.getUserByUsername("id")).thenReturn(user);
        boolean result = defaultUserService.userExistsByUsername("id");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isMigratedUser_userNotInMigration_returnsTrue() throws Exception {
        User user = new User();
        user.setInMigration(false);
        boolean result = defaultUserService.isMigratedUser(user);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isMigratedUser_userInMigration_returnsFalse() throws Exception {
        User user = new User();
        user.setInMigration(true);
        boolean result = defaultUserService.isMigratedUser(user);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void setPasswordIfNecessary_emptyPassword_setsNewPassword() throws Exception {
        Password password = new Password();
        User user = new User();
        user.setEmail("email@email.com");
        user.setUsername("username");
        user.setPasswordObj(password);
        when(userDao.isUsernameUnique("username")).thenReturn(true);
        defaultUserService.addUser(user);
        assertThat("password", user.getPasswordObj().toString().length(), not(0));
    }

    @Test (expected = IllegalArgumentException.class)
    public void setPasswordIfNecessary_passwordObjNotNew_throwsIllegalArgument() throws Exception {
        Password password = new Password();
        Password oldPassword = password.toExisting();
        oldPassword.setValue("aZ23Afdkadzsd");
        User user = new User();
        user.setEmail("email@email.com");
        user.setUsername("username");
        user.setPasswordObj(oldPassword);
        when(userDao.isUsernameUnique("username")).thenReturn(true);
        defaultUserService.addUser(user);
    }

    @Test (expected = PasswordValidationException.class)
    public void checkPasswordComplexity_notValidPassword_throwsPasswordValidation() throws Exception {
        Password password = new Password();
        password.setValue("aZ23Afdkadzsd");
        User user = new User();
        user.setEmail("email@email.com");
        user.setUsername("username");
        user.setPasswordObj(password);
        PasswordComplexityResult passwordComplexityResult = mock(PasswordComplexityResult.class);
        when(config.getBoolean("password.rules.enforced", true)).thenReturn(true);
        when(userDao.isUsernameUnique("username")).thenReturn(true);
        when(passwordComplexityService.checkPassword("aZ23Afdkadzsd")).thenReturn(passwordComplexityResult);
        when(passwordComplexityResult.isValidPassword()).thenReturn(false);
        defaultUserService.addUser(user);
    }

    @Test
    public void getUserByScopeAccess_rackerScopeAccess_returnsUser() throws Exception {
        User user = new Racker();
        user.setEnabled(true);
        RackerScopeAccess scopeAccess = mock(RackerScopeAccess.class);
        when(scopeAccess.getRackerId()).thenReturn("id");
        when(userDao.getRackerByRackerId("id")).thenReturn((Racker)user);
        User result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUserByScopeAccess_impersonatedScopeAccess_returnsUser() throws Exception {
        User user = new Racker();
        ImpersonatedScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        when(scopeAccess.getRackerId()).thenReturn("id");
        when(userDao.getRackerByRackerId("id")).thenReturn((Racker)user);
        User result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUserByScopeAccess_impersonatedScopeAccessIdIsNull_returnsUser() throws Exception {
        User user = new Racker();
        user.setEnabled(true);
        ImpersonatedScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        when(scopeAccess.getRackerId()).thenReturn(null);
        when(scopeAccess.getUsername()).thenReturn("username");
        when(userDao.getUserByUsername("username")).thenReturn(user);
        User result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", result, equalTo(user));
    }

    @Test
    public void getUserByScopeAccess_userScopeAccess_returnsUser() throws Exception {
        User user = new User();
        user.setEnabled(true);
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.getUsername()).thenReturn("username");
        when(userDao.getUserByUsername("username")).thenReturn(user);
        User result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = Exception.class)
    public void getUserByScopeAccess_invalidScopeAccess_throwsException() throws Exception {
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        defaultUserService.getUserByScopeAccess(scopeAccess);
    }

    @Test (expected = NotFoundException.class)
    public void getUserByScopeAccess_userNotFound_throwsNotFound() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.getUsername()).thenReturn("username");
        when(userDao.getUserByUsername("username")).thenReturn(null);
        defaultUserService.getUserByScopeAccess(scopeAccess);
    }

    @Test (expected = NotFoundException.class)
    public void getUserByScopeAccess_userDisabled_throwsNotFound() throws Exception {
        User user = new User();
        user.setEnabled(false);
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.getUsername()).thenReturn("username");
        when(userDao.getUserByUsername("username")).thenReturn(user);
        defaultUserService.getUserByScopeAccess(scopeAccess);
    }

    @Test
    public void getLdapPagingOffsetDefault_returnsOffsetDefault() throws Exception {
        when(config.getInt("ldap.paging.offset.default")).thenReturn(0);
        int result = defaultUserService.getLdapPagingOffsetDefault();
        assertThat("ldap offset default", result, equalTo(0));
    }

    @Test
    public void getLdapPagingLimitDefault_returnsLimitDefault() throws Exception {
        when(config.getInt("ldap.paging.limit.default")).thenReturn(25);
        int result = defaultUserService.getLdapPagingLimitDefault();
        assertThat("ldap limit default", result, equalTo(25));
    }
}
