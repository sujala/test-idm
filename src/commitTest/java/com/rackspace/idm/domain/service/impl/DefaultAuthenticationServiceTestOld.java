package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.service.*;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.dao.RackerAuthDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.util.RSAClient;
import com.rackspace.idm.validation.AuthorizationCodeCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import javax.validation.groups.Default;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
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
    RackerAuthDao authDao;

    @InjectMocks
    DefaultRackerAuthenticationService defaultAuthenticationService = new DefaultRackerAuthenticationService();

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
