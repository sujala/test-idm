package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.validation.Validator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
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
    private RackerAuthDao authDao;
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
    @Mock
    private DomainService domainService;
    @Mock
    private IdentityUserService identityUserService;

    private Validator validator;

    IdentityConfig identityConfig;
    IdentityConfig.ReloadableConfig reloadableConfig;
    IdentityConfig.StaticConfig staticConfig;

    @Before
    public void setUp() throws Exception {
        validator = new Validator();
        //validator.setLdapPatternRepository(patternDao);
        defaultUserService.setValidator(validator);

        identityConfig = mock(IdentityConfig.class);
        reloadableConfig = mock(IdentityConfig.ReloadableConfig.class);
        staticConfig = mock(IdentityConfig.StaticConfig.class);
        when(identityConfig.getReloadableConfig()).thenReturn(reloadableConfig);
        when(identityConfig.getStaticConfig()).thenReturn(staticConfig);

        defaultUserService.setIdentityConfig(identityConfig);
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
    public void generateApiKey_generatesRandomApiKey() throws Exception {
        String randomKey = defaultUserService.generateApiKey();
        assertThat("random apikey", randomKey.length(), not(0));
    }

    @Test (expected = ForbiddenException.class)
    public void getRackerRoles_notallowed_throwsForbiddenException() throws Exception {
        when(staticConfig.isRackerAuthAllowed()).thenReturn(false);
        defaultUserService.getRackerEDirRoles("rackerId");
    }

    @Test
    public void getRackerRoles_callsAuthDao_getRackerRoles() throws Exception {
        when(staticConfig.isRackerAuthAllowed()).thenReturn(true);
        defaultUserService.getRackerEDirRoles("rackerId");
        verify(authDao).getRackerRoles("rackerId");
    }

    @Test
    public void getRackerRoles_returnRoles() throws Exception {
        List<String> rackeList = new ArrayList<String>();
        rackeList.add("test");
        when(staticConfig.isRackerAuthAllowed()).thenReturn(true);
        when(authDao.getRackerRoles("rackerId")).thenReturn(rackeList);
        List<String> roles = defaultUserService.getRackerEDirRoles("rackerId");
        assertThat("roles", roles.get(0), equalTo("test"));
    }

    @Test
    public void getUserByAuthToken_authTokenIsNull_returnsNull() throws Exception {
        User result = defaultUserService.getUserByAuthToken(null);
        assertThat("user", result, equalTo(null));
    }

    @Test
    public void getUserByAuthToken_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken("authToken")).thenReturn(scopeAccess);
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
    public void isUsernameUnique_callsUserDao_isUsernameUnique() throws Exception {
        defaultUserService.isUsernameUnique("username");
        verify(userDao).isUsernameUnique("username");
    }

    @Test
    public void resetUserPassword_callsUserDao_updateUser() throws Exception {
        User user = new User();
        defaultUserService.resetUserPassword(user);
        verify(userDao).updateUser(user);
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
        defaultUserService.addUserv11(user);
    }

    @Test
    public void getUserByScopeAccess_impersonatedScopeAccessIdIsNull_returnsUser() throws Exception {
        User user = new User();
        String username = "testUser";
        user.setUsername(username);
        user.setId("blah");
        user.setEnabled(true);
        ImpersonatedScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        when(scopeAccess.getRackerId()).thenReturn(null);
        when(scopeAccess.getUserRsId()).thenReturn(user.getId());
        when(identityUserService.getEndUserById(user.getId())).thenReturn(user);
        BaseUser result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", (User)result, equalTo(user));
    }

    @Test
    public void getUserByScopeAccess_userScopeAccess_returnsUser() throws Exception {
        User user = new User();
        user.setEnabled(true);
        String userId = "userId";
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.getUserRsId()).thenReturn(userId);
        when(identityUserService.getEndUserById(userId)).thenReturn(user);
        when(domainService.getDomain(anyString())).thenReturn(null);
        BaseUser result = defaultUserService.getUserByScopeAccess(scopeAccess);
        assertThat("user", (User)result, equalTo(user));
    }

    @Test (expected = BadRequestException.class)
    public void getUserByScopeAccess_invalidScopeAccess_throwsException() throws Exception {
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        defaultUserService.getUserByScopeAccess(scopeAccess);
    }

    @Test (expected = NotFoundException.class)
    public void getUserByScopeAccess_userNotFound_throwsNotFound() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        String userId = "userRsId";
        when(scopeAccess.getUserRsId()).thenReturn(userId);
        when(identityUserService.getEndUserById(userId)).thenReturn(null);
        defaultUserService.getUserByScopeAccess(scopeAccess);
    }

    @Test (expected = NotFoundException.class)
    public void getUserByScopeAccess_userDisabled_throwsNotFound() throws Exception {
        User user = new User();
        user.setEnabled(false);
        String userId = "userRsId";
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.getUserRsId()).thenReturn(userId);
        when(identityUserService.getEndUserById(userId)).thenReturn(null);
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
