package com.rackspace.idm.interceptors;

import javax.ws.rs.WebApplicationException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;

/**
 * @author john.eo FIXME Actually write some tests!
 */
public class PasswordChangeFilterTest {
    private AuthHeaderHelper authHeaderHelper;
    private AccessTokenService accessService;
    private PasswordChangeFilter filter;

    @Before
    public void setUp() {
        accessService = EasyMock.createMock(AccessTokenService.class);
        authHeaderHelper = new AuthHeaderHelper();
        filter = new PasswordChangeFilter(accessService, authHeaderHelper,
            new StubLogger());
    }

    @Test
    public void shouldIgnoreUserPasswordSetCall() {
    }

    @Test
    public void shouldCheckAllOtherCalls() {
    }

    @Test(expected = WebApplicationException.class)
    public void shouldNotPermitRestrictedTokenAccessToNonPasswordResetUri() {
        throw new NotAuthorizedException();
    }

    @Test
    public void shouldPermitNormalTokenAccessToNonPasswordResetUri() {
    }
}
