package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.Validator;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository;
import com.unboundid.ldap.sdk.Filter;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/1/11
 * Time: 11:26 AM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultUserServiceTestOld {
    @InjectMocks
    private DefaultUserService defaultUserService = new DefaultUserService();
    @Mock
    private UserDao userDao;
    @Mock
    private AuthDao authDao;
    @Mock
    private ScopeAccessDao scopeAccessDao;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private Configuration config;
    @Mock
    private PasswordComplexityService passwordComplexityService;
    @Mock
    private ScopeAccessService scopeAccessService;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private TenantService tenantService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private CloudRegionService cloudRegionService;
    @Mock
    private TenantDao tenantDao;
    @Mock
    private LdapPatternRepository patternDao;

    private Validator validator;

    private DefaultUserService spy;

    @Before
    public void setUp() throws Exception {
        validator = new Validator();
        validator.setLdapPatternRepository(patternDao);
        defaultUserService.setValidator(validator);
        spy = spy(defaultUserService);
    }

    @Test
    public void hasSubUsers_callUserDao_getUsersByDomainId() throws Exception {
        when((userDao.getUserById("id"))).thenReturn(new User());
        defaultUserService.hasSubUsers("id");
        verify(userDao).getUsersByDomainId(anyString());
    }

    @Test
    public void hasSubUsers_callUserDao_getUserById() throws Exception {
        defaultUserService.hasSubUsers("id");
        verify(userDao).getUserById("id");
    }

    @Test
    public void hasSubUsers_NoUsersWithGivenDomainId_returnsFalse() throws Exception {
        when(userDao.getUserById("id")).thenReturn(new User());
        when(userDao.getUsersByDomainId(anyString())).thenReturn(null);
        boolean hasUsers = defaultUserService.hasSubUsers("id");
        assertThat("User has subusers", hasUsers, equalTo(false));
    }

    @Test
    public void hasSubUsers_noUsersList_returnsFalse() throws Exception {
        User user = new User();
        Users users = new Users();
        when(userDao.getUserById("id")).thenReturn(user);
        when(userDao.getUsersByDomainId(anyString())).thenReturn(users);
        boolean hasUsers = defaultUserService.hasSubUsers("id");
        assertThat("User has subusers", hasUsers, equalTo(false));
    }

    @Test
    public void hasSubUsers_NoUsersWithGivenDomainId_emptyList_returnsFalse() throws Exception {
        User user = new User();
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        when(userDao.getUserById("id")).thenReturn(user);
        when(userDao.getUsersByDomainId(anyString())).thenReturn(users);
        boolean hasUsers = defaultUserService.hasSubUsers("id");
        assertThat("User has subusers", hasUsers, equalTo(false));
    }

    @Test
    public void hasSubUsers_HasNoSubUsers_returnsFalse() throws Exception {
        User userAdmin = new User();
        userAdmin.setDomainId("domainId");
        when(userDao.getUserById("id")).thenReturn(userAdmin);
        Users users = new Users();
        ArrayList<User> userArrayList = new ArrayList<User>();
        User defaultUser = new User();
        defaultUser.setDomainId("domainId");
        userArrayList.add(defaultUser);
        User defaultUser2 = new User();
        defaultUser2.setDomainId("domainId");
        userArrayList.add(defaultUser2);
        users.setUsers(userArrayList);
        when(userDao.getUsersByDomainId(anyString())).thenReturn(users);
        ArrayList<ScopeAccess> list = new ArrayList<ScopeAccess>();
        when(scopeAccessDao.getScopeAccessListByUserId(anyString())).thenReturn(list);
        boolean hasUsers = defaultUserService.hasSubUsers("id");
        assertThat("User has subusers", hasUsers, equalTo(false));
    }

    @Test
    public void hasSubUsers_userDoesNotExists_returnsFalse() throws Exception {
        when(userDao.getUserById("id")).thenReturn(null);
        boolean hasUsers = defaultUserService.hasSubUsers("id");
        assertThat("User has subusers", hasUsers, equalTo(false));
    }

    @Test
    public void hasSubUsers_invalidUserId_returnsFalse() throws Exception {
        boolean hasUsers = defaultUserService.hasSubUsers("bad");
        assertThat("User has subusers", hasUsers, equalTo(false));
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
    public void checkAndGetUserById_userExists_returnsUser() throws Exception {
        User user = new User();
        doReturn(user).when(spy).getUserById("id");
        assertThat("user",spy.checkAndGetUserById("id"),equalTo(user));
    }

    @Test
    public void checkAndGetUserById_userNull_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getUserById("id");
            spy.checkAndGetUserById("id");
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message",ex.getMessage(),equalTo("User id not found"));
        }
    }

    @Test
    public void updateUserById_callsScopeService_getScopeAccessList() throws Exception {
        User user = new User("test");
        user.setId("foo");
        defaultUserService.updateUserById(user, true);
        verify(scopeAccessService).getScopeAccessListByUserId("foo");
    }

    @Test
    public void updateUserById_callsScopeService_updateScopeAccess() throws Exception {
        User user = new User("test");
        user.setId("foo");
        ArrayList<ScopeAccess> list = new ArrayList<ScopeAccess>();
        list.add(new UserScopeAccess());
        when(scopeAccessService.getScopeAccessListByUserId("foo")).thenReturn(list);
        defaultUserService.updateUserById(user, true);
        verify(scopeAccessService).updateScopeAccess(any(ScopeAccess.class));
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
    public void authenticateWithMossoIdAndApiKey_callsUserDao_authenticateByAPIKey() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsers(anyList())).thenReturn(users);
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
        when(userDao.getUsers(anyList())).thenReturn(users);
        when(userDao.authenticateByAPIKey("username", "apiKey")).thenReturn(new UserAuthenticationResult(user, true));
        UserAuthenticationResult result = defaultUserService.authenticateWithMossoIdAndApiKey(1, "apiKey");
        assertThat("username", result.getUser().getUsername(), equalTo("username"));
    }

    @Test
    public void getUser_callsUserDao_getUserByCustomerIdAndUsername() throws Exception {
        defaultUserService.getUser("customerId", "username");
        verify(userDao).getUserByCustomerIdAndUsername("customerId", "username");
    }

    @Test
    public void getUser_foundUser_returnsUser() throws Exception {
        User user = new User();
        when(userDao.getUserByCustomerIdAndUsername("customerId", "username")).thenReturn(user);
        User result = defaultUserService.getUser("customerId", "username");
        assertThat("user", result, equalTo(user));
    }

    @Test (expected = NotFoundException.class)
    public void loadUser_withIdAndUsername_throwsNotFound() throws Exception {
        defaultUserService.loadUser("userId", "username");
    }

    @Test
    public void loadUser_foundUser_returnsUser() throws Exception {
        User user = new User();
        when(userDao.getUserByCustomerIdAndUsername("userId", "username")).thenReturn(user);
        User result = defaultUserService.loadUser("userId", "username");
        assertThat("user", result, equalTo(user));
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
    public void getUserByAuthToken_scopeAccessIsNull_returnsNull() throws Exception {
        when(scopeAccessService.getScopeAccessByAccessToken("authToken")).thenReturn(null);
        User result = defaultUserService.getUserByAuthToken("authToken");
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
        when(userDao.getUsers(anyList())).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(false);
        User result = defaultUserService.getUserByTenantId("1");
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUsersByMossoId_callsUserDao_getUsersByMossoId() throws Exception {
        defaultUserService.getUsersByTenantId("1");
        List<com.unboundid.ldap.sdk.Filter> filters = new ArrayList<Filter>();
        verify(userDao).getUsers(filters);
    }

    @Test
    public void getUserByNastId_notAuthorizedUserAdmin_returnsNull() throws Exception {
        User user = new User();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userDao.getUsers(anyList())).thenReturn(users);
        when(authorizationService.authorizeCloudUserAdmin(null)).thenReturn(false);
        User result = defaultUserService.getUserByTenantId("nastId");
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void softDeleteUser_callsScopeAccessService_expireAllTokensForUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setId("1");
        defaultUserService.softDeleteUser(user);
        verify(scopeAccessService).expireAllTokensForUserById("1");
    }

    @Test
    public void softDeleteUser_callsUserDao_softDeleteUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        defaultUserService.softDeleteUser(user);
        verify(userDao).softDeleteUser(user);
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
    public void validateUserEmailAddress_notValidEmail_throwsBadRequest() throws Exception {
        Pattern pat = new Pattern();
        pat.setRegex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+");
        pat.setErrMsg("Some Error");
        when(patternDao.getPattern(anyString())).thenReturn(pat);
        User user = new User();
        user.setEmail("badEmail");
        defaultUserService.addUser(user);
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
    public void isMigratedUser_userIsNull_returnsFalse() throws Exception {
        boolean result = defaultUserService.isMigratedUser(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void isMigratedUser_userInMigrationIsNull_returnsFalse() throws Exception {
        User user = new User();
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
        user.setEnabled(true);
        user.setId("id");
        user.setDomainId("domainId");
        Pattern pat = new Pattern();
        pat.setRegex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+");
        pat.setErrMsg("Some Error");
        when(patternDao.getPattern(anyString())).thenReturn(pat);
        when(userDao.isUsernameUnique("username")).thenReturn(true);
        Region region = new Region();
        region.setName("DFW");
        when(cloudRegionService.getDefaultRegion(anyString())).thenReturn(region);
        defaultUserService.addUser(user);
        assertThat("password", user.getPasswordObj().toString().length(), not(0));
    }

    @Test (expected = IllegalArgumentException.class)
    public void setPasswordIfNecessary_passwordObjNotNew_throwsIllegalArgument() throws Exception {
        Pattern pat = new Pattern();
        pat.setRegex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+");
        pat.setErrMsg("Some Error");
        when(patternDao.getPattern(anyString())).thenReturn(pat);
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
        Pattern pat = new Pattern();
        pat.setRegex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+");
        pat.setErrMsg("Some Error");
        when(patternDao.getPattern(anyString())).thenReturn(pat);
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

    @Test (expected = BadRequestException.class)
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

    @Test
    public void addBaseUrlToUser_callsEndpointService_getBaseUrlById() throws Exception {
        User user = new User();
        user.setNastId("nastId");
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("NAST");
        Tenant tenant = new Tenant();
        tenant.setBaseUrlIds(new String[0]);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("nastId")).thenReturn(tenant);
        defaultUserService.addBaseUrlToUser(1, user);
        verify(endpointService).getBaseUrlById(1);
    }

    @Test
    public void addBaseUrlToUser_callsTenantService_getTenant() throws Exception {
        User user = new User();
        user.setMossoId(1);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("MOSSO");
        Tenant tenant = new Tenant();
        tenant.setBaseUrlIds(new String[0]);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        defaultUserService.addBaseUrlToUser(1, user);
        verify(tenantService).getTenant("1");
    }

    @Test (expected = BadRequestException.class)
    public void addBaseUrlToUser_tenantBaseUrlIdMatchesCloudBaseUrlId_throwsBadRequestException() throws Exception {
        User user = new User();
        user.setMossoId(1);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("MOSSO");
        cloudBaseUrl.setBaseUrlId(1);
        Tenant tenant = new Tenant();
        String[] ids = {"1"};
        tenant.setBaseUrlIds(ids);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        defaultUserService.addBaseUrlToUser(1, user);
    }

    @Test
    public void addBaseUrlToUser_callsTenantService_updateTenant() throws Exception {
        User user = new User();
        user.setMossoId(1);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("MOSSO");
        cloudBaseUrl.setBaseUrlId(2);
        Tenant tenant = new Tenant();
        String[] ids = {"1"};
        tenant.setBaseUrlIds(ids);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        defaultUserService.addBaseUrlToUser(1, user);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void removeBaseUrlFromUser_callsEndpointService_getBaseUrlById() throws Exception {
        User user = new User();
        user.setNastId("nastId");
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("NAST");
        Tenant tenant = new Tenant();
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("nastId")).thenReturn(tenant);
        defaultUserService.removeBaseUrlFromUser(1, user);
        verify(endpointService).getBaseUrlById(1);
    }

    @Test
    public void removeBaseUrlFromUser_callsTenantService_updateTenant() throws Exception {
        User user = new User();
        user.setMossoId(1);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("MOSSO");
        Tenant tenant = new Tenant();
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        when(tenantService.getTenant("1")).thenReturn(tenant);
        defaultUserService.removeBaseUrlFromUser(1, user);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void getSubUsers_withoutSubUsers_returnsEmptyList() {
        User user = new User();
        user.setId("1");

        String domainId = "1";

        Users domainUsers = new Users();
        domainUsers.setUsers(new ArrayList<User>());
        domainUsers.getUsers().add(user);

        when(userDao.getUsersByDomainId(anyString())).thenReturn(domainUsers);

        List<User> subUsers = defaultUserService.getSubUsers(user);

        assertThat("ldap offset default", subUsers.size(), equalTo(0));
    }

    @Test
    public void getSubUsers_withSubUsers_returnsSubUsers() {
        User user = new User();
        user.setId("1");

        User sub = new User();
        sub.setId("2");

        String domainId = "1";

        Users domainUsers = new Users();
        domainUsers.setUsers(new ArrayList<User>());
        domainUsers.getUsers().add(user);
        domainUsers.getUsers().add(sub);

        when(userDao.getUsersByDomainId(anyString())).thenReturn(domainUsers);

        List<User> subUsers = defaultUserService.getSubUsers(user);

        assertThat("ldap offset default", subUsers.size(), equalTo(1));
    }

    @Test
    public void getPaginatedUsers_callsUserDao_getPaginatedUsers() {
        FilterParam[] filters = new FilterParam[]{};
        defaultUserService.getAllUsersPaged(filters, 0, 5);
        verify(userDao).getAllUsersPaged(any(FilterParam[].class), anyInt(), anyInt());
    }
}
