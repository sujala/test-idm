package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.powermock.api.mockito.PowerMockito.mock;

public class AuthenticationServiceTests {

    UserService mockUserService;
    ApplicationService mockClientService;
    AuthorizationService mockAuthorizationService;
    AuthHeaderHelper authHeaderHelper;
    InputValidator inputValidator;
    ScopeAccessService mockScopeAccessService;
    TenantService mockTenantService;
    DefaultAuthenticationService authenticationService;
    DefaultAuthenticationService authSpy;
    AuthDao mockAuthDao;
    List<TenantRole> tenantRoles;

    String customerId = "RACKSPACE";
    String clientId = "DELETE_My_ClientId";
    String clientSecret = "DELETE_My_Client_Secret";
    String username = "someuser";
    Password userpass = Password.newInstance("secret");
    String firstname = "John";
    String lastname = "Smith";
    String useremail = "someuser@example.com";
    String tokenVal = "asdf1234";
    String requestorToken = "requestortoken5678";
    String refreshTokenVal = "somerefreshtoken1234";
    int expireInSeconds = 3600;
    String rackerId = "rackerId";
    String uniqueId = "uniqueId";


    @Before
    public void setUp() {
        mockUserService = EasyMock.createMock(UserService.class);
        mockClientService = EasyMock.createMock(ApplicationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        mockAuthorizationService = EasyMock.createNiceMock(AuthorizationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        authHeaderHelper = new AuthHeaderHelper();
        inputValidator = EasyMock.createMock(InputValidator.class);
        mockTenantService = mock(TenantService.class);
        mockAuthDao = EasyMock.createMock(AuthDao.class);

        tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.add(new TenantRole());

        final Configuration appConfig = new PropertiesConfiguration();
        appConfig.addProperty("token.expirationSeconds", expireInSeconds);
        appConfig.addProperty("rackspace.customerId", "RACKSPACE");
        appConfig.addProperty("ldap.server.trusted", "true");
        appConfig.addProperty("idm.clientId", "TESTING");

        authenticationService = new DefaultAuthenticationService();
        authenticationService.setAuthDao(mockAuthDao);
        authenticationService.setTenantService(mockTenantService);
        authenticationService.setScopeAccessService(mockScopeAccessService);
        authenticationService.setApplicationService(mockClientService);
        authenticationService.setConfig(appConfig);
        authenticationService.setUserService(mockUserService);
        authenticationService.setInputValidator(inputValidator);

        authSpy = PowerMockito.spy(authenticationService);
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToUser() {
        //setup
        final ScopeAccess scopeAccess = getFakeUserScopeAccess();

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        PowerMockito.when(mockTenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoles);

        EasyMock.replay(mockScopeAccessService);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getUser());
        Assert.assertEquals(tenantRoles, authData.getUser().getRoles());
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToApplication() {
        //setup
        final ScopeAccess scopeAccess = getFakeClientScopeAccess();

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        PowerMockito.when(mockTenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoles);

        EasyMock.replay(mockScopeAccessService);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getApplication());
        Assert.assertEquals(tenantRoles, authData.getApplication().getRoles());
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToRacker() {
        //setup
        final ScopeAccess scopeAccess = getFakeRackerScopeAcces();
        final List<String> rackerRoles = Arrays.asList("role1", "role2");

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        EasyMock.expect(mockAuthDao.getRackerRoles(rackerId)).andReturn(rackerRoles);
        EasyMock.expect(mockAuthDao.getRackerRoles(rackerId)).andReturn(rackerRoles);

        EasyMock.replay(mockScopeAccessService, mockAuthDao);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getRacker());
        Assert.assertEquals(rackerRoles, authData.getRacker().getRackerRoles());
    }

    private UserScopeAccess getFakeUserScopeAccess() {
        final UserScopeAccess usa = new UserScopeAccess();
        usa.setAccessTokenString(tokenVal);
        usa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        usa.setRefreshTokenString(refreshTokenVal);
        usa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        usa.setUsername(username);
        usa.setUserRCN(customerId);
        usa.setClientId(clientId);
        usa.setClientRCN(customerId);
        usa.setUserRsId(username);
        usa.setLdapEntry(new ReadOnlyEntry("accessToken=12345,cn=TOKENS,o=org", new Attribute("name", "value")));
        return usa;
    }

    private ClientScopeAccess getFakeClientScopeAccess() {
        final ClientScopeAccess csa = new ClientScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        return csa;
    }

    private RackerScopeAccess getFakeRackerScopeAcces() {
        final RackerScopeAccess csa = new RackerScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setRefreshTokenString(refreshTokenVal);
        csa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setRackerId(rackerId);
        return csa;
    }

    private PasswordResetScopeAccess getFakePasswordResetScopeAccess() {
        final PasswordResetScopeAccess csa = new PasswordResetScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setUsername(username);
        csa.setUserRCN(customerId);
        return csa;
    }

    private Racker getFakeRacker() {
        Racker racker = new Racker();
        racker.setUniqueId(uniqueId);
        racker.setUsername(username);
        racker.setEnabled(true);
        return racker;
    }

    private Application getTestClient() {
        final Application client = new Application(clientId, ClientSecret
                .newInstance(clientSecret), "DELETE_My_Name", customerId
        );
        return client;
    }

    private AuthCredentials getTestAuthCredentials() {
        final AuthCredentials authCredentials = new AuthCredentials();
        authCredentials.setClientId(clientId);
        authCredentials.setClientSecret(clientSecret);
        authCredentials.setExpirationInSec(expireInSeconds);
        authCredentials.setGrantType("NONE");
        authCredentials.setUsername(username);
        authCredentials.setPassword(userpass.getValue());
        authCredentials.setRefreshToken(refreshTokenVal);

        return authCredentials;
    }

    private RackerCredentials getTestRackerCredentials() {
        final RackerCredentials rackerAuthCredentials = new RackerCredentials();
        rackerAuthCredentials.setClientId(clientId);
        rackerAuthCredentials.setClientSecret(clientSecret);
        rackerAuthCredentials.setExpirationInSec(expireInSeconds);
        rackerAuthCredentials.setGrantType("NONE");
        rackerAuthCredentials.setUsername(username);
        rackerAuthCredentials.setPassword(userpass.getValue());
        rackerAuthCredentials.setRefreshToken(refreshTokenVal);

        return rackerAuthCredentials;
    }
}
