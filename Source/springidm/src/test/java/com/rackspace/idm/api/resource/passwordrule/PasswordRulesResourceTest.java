package com.rackspace.idm.api.resource.passwordrule;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.idm.api.converter.PasswordRulesConverter;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordRulesResourceTest {
    private PasswordRulesResource passwordRulesResource;
    private PasswordComplexityService passwordComplexityService;
    private PasswordRulesConverter passwordRulesConverter;
    private AuthorizationService authorizationService;

    @Before
    public void setUp() throws Exception {
        passwordComplexityService = mock(PasswordComplexityService.class);
        passwordRulesConverter = mock(PasswordRulesConverter.class);
        authorizationService = mock(AuthorizationService.class);
        passwordRulesResource = new PasswordRulesResource(passwordComplexityService, passwordRulesConverter, authorizationService);
    }

    @Test
    public void getRules_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        passwordRulesResource.getRules("authHeader");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getRules_callsPasswordComplexity_getRules() throws Exception {
        passwordRulesResource.getRules("authHeader");
        verify(passwordComplexityService).getRules();
    }

    @Test
    public void getRules_callsPasswordRulesConverter_toPasswordRulesJaxb() throws Exception {
        passwordRulesResource.getRules("authHeader");
        verify(passwordRulesConverter).toPaswordRulesJaxb(any(ArrayList.class));
    }

    @Test
    public void getRules_responseOk_returns200() throws Exception {
        Response response = passwordRulesResource.getRules("authHeader");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void checkPassword_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        when(passwordComplexityService.checkPassword("password")).thenReturn(new PasswordComplexityResult());
        passwordRulesResource.checkPassword("authHeader", userPassword);
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test (expected = BadRequestException.class)
    public void checkPassword_userPasswordIsNull_throwsBadRequest() throws Exception {
       passwordRulesResource.checkPassword("authHeader", null);
    }

    @Test (expected = BadRequestException.class)
    public void checkPassword_userPasswordGetPasswordIsNull_throwsBadRequest() throws Exception {
        passwordRulesResource.checkPassword("authHeader", new UserPassword());
    }

    @Test
    public void checkPassword_callsPasswordComplexityService_checkPassword() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        when(passwordComplexityService.checkPassword("password")).thenReturn(new PasswordComplexityResult());
        passwordRulesResource.checkPassword("authHeader", userPassword);
        verify(passwordComplexityService).checkPassword("password");
    }

    @Test
    public void checkPassword_callsPasswordRulesConverter_toPasswordValidationJaxb() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        when(passwordComplexityService.checkPassword("password")).thenReturn(new PasswordComplexityResult());
        passwordRulesResource.checkPassword("authHeader", userPassword);
        verify(passwordRulesConverter).toPasswordValidationJaxb(any(PasswordComplexityResult.class));
    }

    @Test
    public void checkPassword_responseOk_returns200() throws Exception {
        UserPassword userPassword = new UserPassword();
        userPassword.setPassword("password");
        when(passwordComplexityService.checkPassword("password")).thenReturn(new PasswordComplexityResult());
        Response response = passwordRulesResource.checkPassword("authHeader", userPassword);
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
