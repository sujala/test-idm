package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.service.*;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.RSAClient;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.validation.groups.Default;
import java.util.ArrayList;
import java.util.List;

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
@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthenticationServiceTestOld {

    @Mock
    ApplicationService applicationService;
    @Mock
    UserService userService;
    @Mock
    ScopeAccessService scopeAccessService;
    @Mock
    TenantService tenantService;
    @Mock
    InputValidator inputValidator;
    @Mock
    Configuration config;
    @Mock
    RSAClient rsaClient;
    @Mock
    AuthDao authDao;

    @InjectMocks
    DefaultAuthenticationService defaultAuthenticationService = new DefaultAuthenticationService();

    @Before
    public void setUp() throws Exception {
        ClientAuthenticationResult value = new ClientAuthenticationResult(new Application(), true);
        when(userService.getUser(anyString())).thenReturn(new User());
        when(applicationService.authenticate(anyString(), anyString())).thenReturn(value);
        when(rsaClient.authenticate(anyString(),anyString())).thenReturn(true);
        when(config.getBoolean(anyString(),anyBoolean())).thenReturn(true);
        defaultAuthenticationService.setRsaClient(rsaClient);
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
            when(applicationService.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(),false));
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
            when(applicationService.authenticate("123",null)).thenReturn(new ClientAuthenticationResult(new Application(),true));
            defaultAuthenticationService.getTokens(trParam,new DateTime());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("exception message",ex.getMessage(),equalTo("username cannot be blank"));
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
    public void getAndUpdateUserScopeAccessForClientId_nullUserAndClientExists_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateUserScopeAccessForClientId(null,new Application());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void getAndUpdateUserScopeAccessForClientId_UserExistsAndClientNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateUserScopeAccessForClientId(new User(),null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void getAndUpdateClientScopeAccessForClientId_nullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateClientScopeAccessForClientId(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument cannot be null."));
        }
    }

    @Test
    public void getAndUpdateRackerScopeAccessForClientId_nullRackerAndNullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateRackerScopeAccessForClientId(null, null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void getAndUpdateRackerScopeAccessForClientId_nullRackerAndClientExists_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateRackerScopeAccessForClientId(null,new Application());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void getAndUpdateRackerScopeAccessForClientId_rackerExistsAndNullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultAuthenticationService.getAndUpdateRackerScopeAccessForClientId(new Racker(),null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception type",ex.getMessage(),equalTo("Argument(s) cannot be null."));
        }
    }

    @Test
    public void validateRackerHasRackerRole_TenantRoleNameIsRackerAndClientIdMatches_doesNotGetClientRoles() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("Racker");
        tenantRole.setClientId("123");
        List<TenantRole> tenantRolesForScopeAccess = new ArrayList<TenantRole>();
        tenantRolesForScopeAccess.add(tenantRole);
        when(tenantService.getTenantRolesForUser(null)).thenReturn(tenantRolesForScopeAccess);
        when(config.getString("idm.clientId")).thenReturn("123");
        defaultAuthenticationService.validateRackerHasRackerRole(null,null,null);
        verify(applicationService,never()).getClientRolesByClientId(anyString());
    }

    @Test
    public void validateRackerHasRackerRole_TenantRoleNameIsRackerAndClientIdDoesNotMatch_getsClientRolesToUser() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("Racker");
        tenantRole.setClientId("456");

        List<TenantRole> tenantRolesForScopeAccess = new ArrayList<TenantRole>();
        tenantRolesForScopeAccess.add(tenantRole);

        ClientRole clientRole = new ClientRole();
        clientRole.setName("NotARacker");

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(clientRole);

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(tenantRolesForScopeAccess);
        when(config.getString("idm.clientId")).thenReturn("123");
        when(applicationService.getClientRolesByClientId("123")).thenReturn(clientRoles);

        defaultAuthenticationService.validateRackerHasRackerRole(null,null,null);
        verify(applicationService).getClientRolesByClientId("123");
    }

    @Test
    public void validateRackerHasRackerRole_TenantRoleNameIsNotRackerAndClientIdMatches_getsClientRolesToUser() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("NotARacker");
        tenantRole.setClientId("123");

        List<TenantRole> tenantRolesForScopeAccess = new ArrayList<TenantRole>();
        tenantRolesForScopeAccess.add(tenantRole);

        ClientRole clientRole = new ClientRole();
        clientRole.setName("NotARacker");

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(clientRole);

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(tenantRolesForScopeAccess);
        when(config.getString("idm.clientId")).thenReturn("123");
        when(applicationService.getClientRolesByClientId("123")).thenReturn(clientRoles);

        defaultAuthenticationService.validateRackerHasRackerRole(null,null,null);
        verify(applicationService).getClientRolesByClientId("123");
    }

    @Test
    public void validateRackerHasRackerRole_TenantRoleNameIsNotRackerAndClientIdDoesNotMatch_getsClientRolesToUser() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("NotARacker");
        tenantRole.setClientId("456");

        List<TenantRole> tenantRolesForScopeAccess = new ArrayList<TenantRole>();
        tenantRolesForScopeAccess.add(tenantRole);

        ClientRole clientRole = new ClientRole();
        clientRole.setName("NotARacker");

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(clientRole);

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(tenantRolesForScopeAccess);
        when(config.getString("idm.clientId")).thenReturn("123");
        when(applicationService.getClientRolesByClientId("123")).thenReturn(clientRoles);

        defaultAuthenticationService.validateRackerHasRackerRole(null,null,null);
        verify(applicationService).getClientRolesByClientId("123");
    }

    @Test
    public void validateRackerHasRackerRole_TenantRoleNameIsNotRackerAndClientIdDoesNotMatchAndClientRoleNameNotRacker_doesNotAddTenantRoleToUser() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("NotARacker");
        tenantRole.setClientId("456");

        List<TenantRole> tenantRolesForScopeAccess = new ArrayList<TenantRole>();
        tenantRolesForScopeAccess.add(tenantRole);

        ClientRole clientRole = new ClientRole();
        clientRole.setName("NotARacker");

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(clientRole);

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(tenantRolesForScopeAccess);
        when(config.getString("idm.clientId")).thenReturn("123");
        when(applicationService.getClientRolesByClientId("123")).thenReturn(clientRoles);

        defaultAuthenticationService.validateRackerHasRackerRole(null,null,null);
        verify(tenantService,never()).addTenantRoleToUser(any(Racker.class), any(TenantRole.class));
    }

    @Test
    public void validateCredentials_credentialsNull_throwsBadRequestException() throws Exception {
        try{
            defaultAuthenticationService.validateCredentials(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("assertion type", ex.getClass().getName(), equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("assertion message", ex.getMessage(), equalTo("Invalid request: Missing or malformed parameter(s)."));
        }
    }

    @Test
     public void validateCredentials_grantTypeNull_throwsBadRequestException() throws Exception {
        try{
            defaultAuthenticationService.validateCredentials(new Credentials());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("assertion type", ex.getClass().getName(), equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("assertion message", ex.getMessage(),equalTo("Invalid request: Missing or malformed parameter(s)."));
        }
    }

    @Test
    public void validateCredentials_grantTypeIsAuthorizationCode_throwsBadRequestException() throws Exception {
        try{
            Credentials trParam = new Credentials();
            ApiError apiError = new ApiError();
            apiError.setMessage("everything");
            trParam.setGrantType("authorization_code");
            when(inputValidator.validate(trParam,Default.class, AuthorizationCodeCredentialsCheck.class)).thenReturn(apiError);
            defaultAuthenticationService.validateCredentials(trParam);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("assertion type", ex.getClass().getName(), equalTo("com.rackspace.idm.exception.BadRequestException"));
            assertThat("assertion message", ex.getMessage(),equalTo("Bad request parameters: everything"));
        }
    }
}
