package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.RSAClient;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/1/12
 * Time: 4:07 PM
 */
public class DefaultAuthenticationServiceTest {

    ApplicationDao applicationDao = mock(ApplicationDao.class);
    UserDao userDao = mock(UserDao.class);
    ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);

    private Configuration config = mock(Configuration.class);
    DefaultAuthenticationService defaultAuthenticationService = new DefaultAuthenticationService(null,null,null,scopeAccessService,applicationDao,config,userDao,null,null);
    DefaultAuthenticationService spy;
    RSAClient rsaClient = mock(RSAClient.class);

    @Before
    public void setUp() throws Exception {
        ClientAuthenticationResult value = new ClientAuthenticationResult(new Application(), true);
        when(userDao.getUserByUsername(anyString())).thenReturn(new Racker());
        when(applicationDao.authenticate(anyString(), anyString())).thenReturn(value);
        when(rsaClient.authenticate(anyString(),anyString())).thenReturn(true);
        when(config.getBoolean(anyString(),anyBoolean())).thenReturn(true);
        defaultAuthenticationService.setRsaClient(rsaClient);
        spy = spy(defaultAuthenticationService);
    }

    @Test
    public void getAuthDataWithClientRoles_authDataHasNoUserApplicationOrRacker_returnsAuthDataWithSuccess() throws Exception {
        AuthData authData = new AuthData();
        doReturn(authData).when(spy).getAuthData(null);
        assertThat("auth data", spy.getAuthDataWithClientRoles(null),equalTo(authData));
    }

    @Test
    public void getAuthData_scopeAccessNotInstanceOfHasAccessTokenHasRefreshTokenAndPasswordResetScopeAccess_callsSetClient() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).setClient(eq(scopeAccess), any(AuthData.class));
        spy.getAuthData(scopeAccess);
        verify(spy).setClient(eq(scopeAccess),any(AuthData.class));
    }

    @Test
    public void setClient_scopeAccessInstanceOfDelegatedClientScopeAccess_getsDataFromScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new DelegatedClientScopeAccess();
        ((DelegatedClientScopeAccess) scopeAccess).setUsername("jsmith");
        ((DelegatedClientScopeAccess) scopeAccess).setUserRCN("123");
        AuthData authData = new AuthData();
        defaultAuthenticationService.setClient(scopeAccess,authData);
        assertThat("auth data user",authData.getUser(), notNullValue());
        assertThat("auth data user username", authData.getUser().getUsername(),equalTo("jsmith"));
        assertThat("auth data user customer id", authData.getUser().getCustomerId(),equalTo("123"));
    }

    @Test
    public void getTokens_blankGrantType_throwsBadRequestException() throws Exception {
        try{
            defaultAuthenticationService.getTokens(new Credentials(),new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("exception message",ex.getMessage(),equalTo("grant_type cannot be null"));
        }
    }

    @Test
    public void getTokens_blankClientId_throwsBadRequestException() throws Exception {
        try{
            Credentials trParam = new Credentials();
            trParam.setGrantType("password");
            defaultAuthenticationService.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("exception message",ex.getMessage(),equalTo("client_id cannot be blank"));
        }
    }

    @Test
    public void getTokens_clientIsNotAuthenticated_throwsNotAuthenticatedException() throws Exception {
        try{
            Credentials trParam = new Credentials();
            trParam.setGrantType("password");
            trParam.setClientId("123");
            when(applicationDao.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(),false));
            defaultAuthenticationService.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Bad Client credentials for 123"));
        }
    }

    @Test
    public void getTokens_trParamInstanceOfRackerCredentialsAndUsernameBlank_throwsBadRequestException() throws Exception {
        try{
            Credentials trParam = new RackerCredentials();
            trParam.setGrantType("password");
            trParam.setClientId("123");
            when(applicationDao.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(),true));
            defaultAuthenticationService.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("exception message",ex.getMessage(),equalTo("username cannot be blank"));
        }
    }

    @Test
    public void getTokens_trParamInstanceOfRackerCredentialsAndUserNotAuthenticated_throwsNotAuthenticatedException() throws Exception {
        try{
            Credentials trParam = new RackerCredentials();
            trParam.setGrantType("password");
            trParam.setClientId("123");
            trParam.setUsername("jsmith");
            when(applicationDao.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(), true));
            doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateRacker("jsmith",null,false);
            spy.getTokens(trParam, new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Bad User credentials for jsmith"));
        }
    }

    @Test
    public void getTokens_trParamInstanceOfRSACredentialsAndUserNotAuthenticated_throwsNotAuthenticatedException() throws Exception {
        try{
            Credentials trParam = new RSACredentials();
            trParam.setGrantType("password");
            trParam.setClientId("123");
            when(applicationDao.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(), true));
            doReturn(new UserAuthenticationResult(new User(), false)).when(spy).authenticateRacker(null, null, true);
            spy.getTokens(trParam, new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Bad RSA credentials for null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsPasswordAndUsernameBlank_throwsBadRequestException() throws Exception {
        try{
            Credentials trParam = new Credentials();
            trParam.setGrantType("password");
            trParam.setClientId("123");
            when(applicationDao.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(), true));
            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("exception message",ex.getMessage(),equalTo("username cannot be blank"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenExpiredAndClientIdsNotEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            ScopeAccess scopeAccess =  new UserScopeAccess();
            ((HasRefreshToken) scopeAccess).setRefreshTokenExpired();
            scopeAccess.setClientId("123");
            Application client = new Application();
            client.setClientId("456");
            Credentials trParam = new Credentials();
            trParam.setGrantType("refresh_token");
            trParam.setClientId("123");
            when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Refresh Token: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenExpiredAndClientIdsEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            ScopeAccess scopeAccess =  new UserScopeAccess();
            ((HasRefreshToken) scopeAccess).setRefreshTokenExpired();
            scopeAccess.setClientId("123");
            Application client = new Application();
            client.setClientId("123");
            Credentials trParam = new Credentials();
            trParam.setGrantType("refresh_token");
            trParam.setClientId("123");
            when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Refresh Token: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenNotExpiredAndClientIdsNotEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            ScopeAccess scopeAccess =  new UserScopeAccess();
            ((HasRefreshToken) scopeAccess).setRefreshTokenExp(new DateTime().plusMinutes(5).toDate());
            ((HasRefreshToken) scopeAccess).setRefreshTokenString("token");
            scopeAccess.setClientId("123");
            Application client = new Application();
            client.setClientId("456");
            Credentials trParam = new Credentials();
            trParam.setGrantType("refresh_token");
            trParam.setClientId("123");
            when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Refresh Token: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenNotExpiredAndClientIdsEqualAndScopeAccessInstanceOfUserScopeAccessAndNullUser_throwsUserDisabledException() throws Exception {
        try{
            ScopeAccess scopeAccess =  new UserScopeAccess();
            ((HasRefreshToken) scopeAccess).setRefreshTokenExp(new DateTime().plusMinutes(5).toDate());
            ((HasRefreshToken) scopeAccess).setRefreshTokenString("string");
            scopeAccess.setClientId("123");
            Application client = new Application();
            client.setClientId("123");
            Credentials trParam = new Credentials();
            trParam.setGrantType("refresh_token");
            trParam.setClientId("123");
            when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(userDao.getUserById(null)).thenReturn(null);
            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.UserDisabledException"));
            assertThat("exception message",ex.getMessage(),equalTo("User NULL is disabled"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenNotExpiredAndClientIdsEqualAndScopeAccessInstanceOfUserScopeAccessAndUserDisabled_throwsUserDisabledException() throws Exception {
        try{
            User user = new User();
            user.setEnabled(false);

            ScopeAccess scopeAccess =  new UserScopeAccess();
            ((HasRefreshToken) scopeAccess).setRefreshTokenExp(new DateTime().plusMinutes(5).toDate());
            ((HasRefreshToken) scopeAccess).setRefreshTokenString("string");
            scopeAccess.setClientId("123");

            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("refresh_token");
            trParam.setClientId("123");

            when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(userDao.getUserById(null)).thenReturn(user);

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.UserDisabledException"));
            assertThat("exception message",ex.getMessage(),equalTo("User NULL is disabled"));
        }
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenNotExpiredAndClientIdsEqualAndScopeAccessInstanceOfUserScopeAccessAndUserNotDisabled_returnsCorrectScopeAccess() throws Exception {
        User user = new User();
        user.setEnabled(true);

        ScopeAccess scopeAccess =  new UserScopeAccess();
        ((HasRefreshToken) scopeAccess).setRefreshTokenExp(new DateTime().plusMinutes(5).toDate());
        ((HasRefreshToken) scopeAccess).setRefreshTokenString("string");
        scopeAccess.setClientId("123");

        Application client = new Application();
        client.setClientId("123");

        Credentials trParam = new Credentials();
        trParam.setGrantType("refresh_token");
        trParam.setClientId("123");

        when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
        when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
        when(userDao.getUserById(null)).thenReturn(user);
        doReturn("token").when(spy).generateToken();
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();

        spy.getTokens(trParam,new DateTime());

        assertThat("access token",((HasAccessToken) scopeAccess).getAccessTokenString(),equalTo("token"));
        assertThat("access token expiration", ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime()),equalTo(false));
    }

    @Test
    public void getTokens_grantTypeIsRefreshTokenAndRefreshTokenNotExpiredAndClientIdsEqualAndScopeAccessNotInstanceOfUserScopeAccess_callsScopeAccessServiceMethod() throws Exception {
        User user = new User();
        user.setEnabled(true);

        ScopeAccess scopeAccess =  new RackerScopeAccess();
        ((HasRefreshToken) scopeAccess).setRefreshTokenExp(new DateTime().plusMinutes(5).toDate());
        ((HasRefreshToken) scopeAccess).setRefreshTokenString("string");
        scopeAccess.setClientId("123");

        Application client = new Application();
        client.setClientId("123");

        Credentials trParam = new Credentials();
        trParam.setGrantType("refresh_token");
        trParam.setClientId("123");

        when(scopeAccessService.getScopeAccessByRefreshToken(null)).thenReturn(scopeAccess);
        when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
        when(userDao.getUserById(null)).thenReturn(user);
        doReturn("token").when(spy).generateToken();
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();

        spy.getTokens(trParam,new DateTime());

        verify(scopeAccessService).updateScopeAccess(scopeAccess);
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndNullScopeAccess_throwsNotAuthenticatedException() throws Exception {
        try{
            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("authorization_code");
            trParam.setClientId("123");

            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(null);

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Authorization Code: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndAuthorizedCodeExpiredAndClientIdsNotEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
            delegatedClientScopeAccess.setAccessTokenExpired();
            delegatedClientScopeAccess.setClientId("456");

            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("authorization_code");
            trParam.setClientId("123");

            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(delegatedClientScopeAccess);

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Authorization Code: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndAuthorizedCodeExpiredAndClientIdsEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
            delegatedClientScopeAccess.setAccessTokenExpired();
            delegatedClientScopeAccess.setClientId("123");

            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("authorization_code");
            trParam.setClientId("123");

            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(delegatedClientScopeAccess);

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Authorization Code: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndAuthorizedCodeNotExpiredAndClientIdsNotEqual_throwsNotAuthenticatedException() throws Exception {
        try{
            DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
            delegatedClientScopeAccess.setAuthCodeExp(new DateTime().plusMinutes(5).toDate());
            delegatedClientScopeAccess.setAuthCode("token");
            delegatedClientScopeAccess.setClientId("456");

            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("authorization_code");
            trParam.setClientId("123");

            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
            when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(delegatedClientScopeAccess);

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unauthorized Authorization Code: null"));
        }
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndAuthorizedCodeNotExpiredAndClientIdsEqual_updatesScopeAccess() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAuthCodeExp(new DateTime().plusMinutes(5).toDate());
        delegatedClientScopeAccess.setAuthCode("token");
        delegatedClientScopeAccess.setClientId("123");

        Application client = new Application();
        client.setClientId("123");

        Credentials trParam = new Credentials();
        trParam.setGrantType("authorization_code");
        trParam.setClientId("123");

        when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
        when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(delegatedClientScopeAccess);
        doReturn("generatedToken").when(spy).generateToken();
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();

        spy.getTokens(trParam,new DateTime());
        verify(scopeAccessService).updateScopeAccess(delegatedClientScopeAccess);
    }

    @Test
    public void getTokens_grantTypeIsAuthorizedCodeAndAuthorizedCodeNotExpiredAndClientIdsEqual_returnsCorrectScopeAccess() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAuthCodeExp(new DateTime().plusMinutes(5).toDate());
        delegatedClientScopeAccess.setAuthCode("token");
        delegatedClientScopeAccess.setClientId("123");

        Application client = new Application();
        client.setClientId("123");

        Credentials trParam = new Credentials();
        trParam.setGrantType("authorization_code");
        trParam.setClientId("123");

        when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));
        when(scopeAccessService.getScopeAccessByAuthCode(null)).thenReturn(delegatedClientScopeAccess);
        doReturn("generatedToken").when(spy).generateToken();
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();

        spy.getTokens(trParam,new DateTime());
        assertThat("scope access refresh token string", delegatedClientScopeAccess.getRefreshTokenString(),equalTo("generatedToken"));
        assertThat("scope access access token string",delegatedClientScopeAccess.getAccessTokenString(),equalTo("generatedToken"));
        assertThat("scope access auth code", delegatedClientScopeAccess.getAuthCode(),nullValue());
        assertThat("scope access auth code exp",delegatedClientScopeAccess.getAuthCodeExp(),nullValue());
    }

    @Test
    public void getTokens_grantTypeIsAssertion_throwsNotAuthenticatedException() throws Exception {
        try{
            Application client = new Application();
            client.setClientId("123");

            Credentials trParam = new Credentials();
            trParam.setGrantType("assertion");
            trParam.setClientId("123");

            when(applicationDao.authenticate("123", null)).thenReturn(new ClientAuthenticationResult(client, true));

            spy.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotAuthenticatedException"));
            assertThat("exception message",ex.getMessage(),equalTo("Unsupported GrantType: ASSERTION"));
        }
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_nullUserAndNullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateUserScopeAccessForClientId(null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_gettingUserScopeAccessThrowsNotFoundException_createsScopeAccess() throws Exception {
        when(scopeAccessService.getUserScopeAccessForClientId(null,null)).thenThrow(new NotFoundException());
        when(scopeAccessService.addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class))).thenReturn(new UserScopeAccess());
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("generatedToken").when(spy).generateToken();
        spy.getAndUpdateUserScopeAccessForClientId(new User(), new Application());
        verify(scopeAccessService).addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class));
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_accessExpirationIsNull_setsAccessTokenString() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(null);

        when(scopeAccessService.getUserScopeAccessForClientId(null, null)).thenThrow(new NotFoundException());
        when(scopeAccessService.addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class))).thenReturn(new UserScopeAccess());
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("generatedToken").when(spy).generateToken();
        UserScopeAccess scopeAccess = spy.getAndUpdateUserScopeAccessForClientId(new User(), new Application());
        assertThat("access token string",scopeAccess.getAccessTokenString(),equalTo("generatedToken"));
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_accessExpirationIsNull_setsAccessTokenExpirationDate() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(null);

        when(scopeAccessService.getUserScopeAccessForClientId(null,null)).thenThrow(new NotFoundException());
        when(scopeAccessService.addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class))).thenReturn(new UserScopeAccess());
        doReturn(60).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("generatedToken").when(spy).generateToken();
        UserScopeAccess scopeAccess = spy.getAndUpdateUserScopeAccessForClientId(new User(), new Application());
        assertThat("access expiration date",scopeAccess.getAccessTokenExp(),lessThan(new DateTime().plusMinutes(2).toDate()));
        assertThat("access expiration date",scopeAccess.getAccessTokenExp(),greaterThan(new DateTime().toDate()));
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_refreshExpirationIsNull_setsRefreshTokenString() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setRefreshTokenExp(null);

        when(scopeAccessService.getUserScopeAccessForClientId(null,null)).thenThrow(new NotFoundException());
        when(scopeAccessService.addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class))).thenReturn(new UserScopeAccess());
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("generatedToken").when(spy).generateToken();
        UserScopeAccess scopeAccess = spy.getAndUpdateUserScopeAccessForClientId(new User(), new Application());
        assertThat("refresh token string", scopeAccess.getRefreshTokenString(), equalTo("generatedToken"));
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_refreshExpirationIsNull_setsRefreshTokenExpirationDate() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setRefreshTokenExp(null);

        when(scopeAccessService.getUserScopeAccessForClientId(null,null)).thenThrow(new NotFoundException());
        when(scopeAccessService.addDirectScopeAccess((String) eq(null), any(UserScopeAccess.class))).thenReturn(new UserScopeAccess());
        doReturn(60).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("generatedToken").when(spy).generateToken();
        UserScopeAccess scopeAccess = spy.getAndUpdateUserScopeAccessForClientId(new User(), new Application());
        assertThat("refresh expiration date",scopeAccess.getRefreshTokenExp(),lessThan(new DateTime().plusYears(100).plusSeconds(1).toDate()));
        assertThat("refresh expiration date",scopeAccess.getRefreshTokenExp(),greaterThan(new DateTime().plusYears(100).minusMinutes(1).toDate()));
    }

    @Test
    public void authenticate_withRSACredentials_callsAuthenticateRacker() throws Exception {
        RSACredentials rsaCredentials = new RSACredentials();
        rsaCredentials.setUsername("u");
        rsaCredentials.setPassword("p");
        rsaCredentials.setGrantType("password");
        rsaCredentials.setClientId("id");
        doNothing().when(spy).validateCredentials(rsaCredentials);
        doReturn(new RackerScopeAccess()).when(spy).getAndUpdateRackerScopeAccessForClientId(any(Racker.class), any(Application.class));
        spy.authenticate(rsaCredentials);
        verify(spy).authenticateRacker("u", "p", true);
    }

    @Test
    public void authenticateRacker_withFlagSetToTrue_callsClient() throws Exception {
        spy.authenticateRacker("foo", "bar", true);
        verify(rsaClient).authenticate("foo", "bar");
    }
}
