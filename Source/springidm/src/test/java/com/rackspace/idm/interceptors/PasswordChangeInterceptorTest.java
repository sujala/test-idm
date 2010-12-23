package com.rackspace.idm.interceptors;

import java.net.URISyntaxException;

import javax.ws.rs.core.HttpHeaders;

import org.easymock.EasyMock;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;

public class PasswordChangeInterceptorTest {
    private AuthHeaderHelper authHeaderHelper;
    private AccessTokenService accessService;
    private PasswordChangeInterceptor interceptor;

    @Before
    public void setUp() {
        accessService = EasyMock.createMock(AccessTokenService.class);
        authHeaderHelper = new AuthHeaderHelper();
        interceptor = new PasswordChangeInterceptor(accessService,
            authHeaderHelper, new StubLogger());
    }

    @Test
    public void shouldIgnoreUserPasswordSetCall() {
        InterceptedCall call = InterceptedCall.SET_USER_PASSWORD;
        boolean accept = interceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertFalse(accept);
    }

    @Test
    public void shouldCheckAllOtherCalls() {
        InterceptedCall call = InterceptedCall.ADD_FIRST_USER;
        boolean accept = interceptor.accept(call.getControllerClass(), call
            .getInterceptedMethod());
        Assert.assertTrue(accept);
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotPermitRestrictedTokenAccessToNonPasswordResetUri()
        throws URISyntaxException {
        EasyMock
            .expect(accessService.getTokenByTokenString("restricted"))
            .andReturn(
                new AccessToken("foo", null, null, null, IDM_SCOPE.SET_PASSWORD));
        EasyMock.replay(accessService);
        MockHttpRequest request = MockHttpRequest
            .get(
                "/idm/customers/RCN-010-101-010/users/johneo/somerestrictedresource")
            .header(HttpHeaders.AUTHORIZATION, "OAuth restricted");
        interceptor.preProcess(request, null);
    }

    @Test
    public void shouldPermitNormalTokenAccessToNonPasswordResetUri()
        throws URISyntaxException {
        EasyMock
            .expect(accessService.getTokenByTokenString("restricted"))
            .andReturn(new AccessToken("foo", null, null, null, IDM_SCOPE.FULL));
        EasyMock.replay(accessService);

        MockHttpRequest request = MockHttpRequest
            .get(
                "/idm/customers/RCN-010-101-010/users/johneo/somerestrictedresource")
            .header(HttpHeaders.AUTHORIZATION, "OAuth restricted");
        ServerResponse response = interceptor.preProcess(request, null);
        Assert.assertNull(response);
    }
}
