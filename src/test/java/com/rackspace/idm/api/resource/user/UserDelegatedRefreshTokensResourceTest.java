package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserDelegatedRefreshTokensResourceTest {
    private UserDelegatedRefreshTokensResource userDelegatedRefreshTokensResource;
    private ScopeAccessService scopeAccessService;
    private UserService userService;
    private AuthorizationService authorizationService;
    private TokenConverter tokenConverter;

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        tokenConverter = mock(TokenConverter.class);
        userDelegatedRefreshTokensResource = new UserDelegatedRefreshTokensResource(scopeAccessService, userService, authorizationService, tokenConverter);

        when(userService.loadUser(anyString())).thenReturn(new User());
    }

    @Test
    public void getTokens_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userDelegatedRefreshTokensResource.getTokens("authHeader", "userId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getTokens_callsUserService_loadUser() throws Exception {
        userDelegatedRefreshTokensResource.getTokens("authHeader", "userId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getTokens_callsScopeAccessService_getDelegatedUserScopeAccessForUsername() throws Exception {
        userDelegatedRefreshTokensResource.getTokens("authHeader", "userId");
        verify(scopeAccessService).getDelegatedUserScopeAccessForUsername(anyString());
    }

    @Test
    public void getTokens_callsTokenConverter_toDelegatedTokensJaxb() throws Exception {
        userDelegatedRefreshTokensResource.getTokens("authHeader", "userId");
        verify(tokenConverter).toDelegatedTokensJaxb(any(ArrayList.class));
    }

    @Test
    public void getTokens_responseOk_returns200() throws Exception {
        Response response = userDelegatedRefreshTokensResource.getTokens("authHeader", "userId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getTokenDetails_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(new DelegatedClientScopeAccess());
        userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getTokenDetails_callsScopeAccessService_getDelegatedScopeAccessByRefreshToken() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(new DelegatedClientScopeAccess());
        userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
        verify(scopeAccessService).getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"));
    }

    @Test (expected = NotFoundException.class)
    public void getTokenDetails_delegatedScopeAccessIsNull_throwsNotFound() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(null);
        userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
    }

    @Test
    public void getTokenDetails_callsScopeAccessService_getPermissionsForParent() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(new DelegatedClientScopeAccess());
        userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
        verify(scopeAccessService).getPermissionsForParent(anyString());
    }

    @Test
    public void getTokenDetails_callsTokenConverter_toDelegatedTokenJaxb() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(new DelegatedClientScopeAccess());
        userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
        verify(tokenConverter).toDelegatedTokenJaxb(any(DelegatedClientScopeAccess.class),any(ArrayList.class));
    }

    @Test
    public void getTokenDetails_responseOk_returns200() throws Exception {
        when(scopeAccessService.getDelegatedScopeAccessByRefreshToken(any(User.class), eq("tokenString"))).thenReturn(new DelegatedClientScopeAccess());
        Response response = userDelegatedRefreshTokensResource.getTokenDetails("authHeader", "userId", "tokenString");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void deleteToken_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        userDelegatedRefreshTokensResource.deleteToken("authHeader", "userId", "tokenString");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void deleteToken_callsScopeAccessService_deleteDelegatedToken() throws Exception {
        userDelegatedRefreshTokensResource.deleteToken("authHeader", "userId", "tokenString");
        verify(scopeAccessService).deleteDelegatedToken(any(User.class), anyString());
    }

    @Test
    public void deleteToken_responseNoContent_returns204() throws Exception {
        Response response = userDelegatedRefreshTokensResource.deleteToken("authHeader", "userId", "tokenString");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

}
