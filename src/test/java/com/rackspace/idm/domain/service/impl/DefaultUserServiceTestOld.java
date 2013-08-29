package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
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
    private LdapPatternRepository patternDao;
    @Mock
    private CryptHelper cryptHelper;
    @Mock
    private PropertiesService propertiesService;

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
        when(userDao.getUsersByDomain(anyString())).thenReturn(new ArrayList<User>());
        when((userDao.getUserById("id"))).thenReturn(new User());
        defaultUserService.hasSubUsers("id");
        verify(userDao).getUsersByDomain(anyString());
    }

    @Test
    public void hasSubUsers_callUserDao_getUserById() throws Exception {
        defaultUserService.hasSubUsers("id");
        verify(userDao).getUserById("id");
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

}
