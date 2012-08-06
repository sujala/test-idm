package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 12:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserRecoveryTokenResourceTest {
    private UserRecoveryTokenResource userRecoveryTokenResource;
    private ScopeAccessService scopeAccessService;
    private TokenConverter tokenConverter;
    private AuthorizationService authorizationService;
    private InputValidator inputValidator;
    private UserService userService;

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        tokenConverter = mock(TokenConverter.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);
        userRecoveryTokenResource = new UserRecoveryTokenResource(scopeAccessService, userService, tokenConverter, authorizationService, inputValidator);
    }

    @Test
    public void getPasswordResetToken_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null)).thenReturn(new PasswordResetScopeAccess());
        userRecoveryTokenResource.getPasswordResetToken("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getPasswordResetToken_callsUserService_loadUser() throws Exception {
        when(scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null)).thenReturn(new PasswordResetScopeAccess());
        userRecoveryTokenResource.getPasswordResetToken("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getPasswordResetToken_callsScopeAccessService_getOrCreatePasswordResetScopeAccessForUser() throws Exception {
        when(scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null)).thenReturn(new PasswordResetScopeAccess());
        userRecoveryTokenResource.getPasswordResetToken("authHeader", "userId");
        verify(scopeAccessService).getOrCreatePasswordResetScopeAccessForUser(null);
    }

    @Test
    public void getPasswordResetToken_callsTokenConverter_toTokenJaxb() throws Exception {
        when(scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null)).thenReturn(new PasswordResetScopeAccess());
        userRecoveryTokenResource.getPasswordResetToken("authHeader", "userId");
        verify(tokenConverter).toTokenJaxb(anyString(), any(Date.class));
    }

    @Test
    public void getPasswordResetToken_responseOk_returns200() throws Exception {
        when(scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null)).thenReturn(new PasswordResetScopeAccess());
        Response response = userRecoveryTokenResource.getPasswordResetToken("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
