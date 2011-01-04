package com.rackspace.idm.interceptors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.test.stub.StubLogger;

/**
 * @author john.eo FIXME Actually write some tests
 */
public class AuthorizationFilterTest {
    private AuthorizationFilter authorizatinFilter;

    private ClientService clientService;

    private OAuthService oauthService;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    @Before
    public void setUp() {
        oauthService = EasyMock.createMock(OAuthService.class);
        clientService = EasyMock.createMock(ClientService.class);
        logger = new StubLogger();
        authorizationHelper = new IDMAuthorizationHelper(oauthService, null,
            null, clientService, logger);

        authorizatinFilter = new AuthorizationFilter(clientService,
            oauthService, authorizationHelper, logger);
    }

    @Test
    public void shouldAcceptRequestsForLockingCustomers() {

    }

    @Test
    public void shouldAcceptRequestsForInitialUserCreation() {
    }

    @Test
    public void shouldIgnoreAnyOtherRequest() {
    }

    @Test
    public void shouldAddFirstUserWhenAuthorized() {
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAddFirstUserWhenNotAuthorized() {
        throw new ForbiddenException();
    }

    @Test
    public void shouldSetCustomerLockWhenAuthorized() {
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenNotAuthorized() {
        throw new ForbiddenException();
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenClientDoesNotExist() {
        throw new ForbiddenException();
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenClientIdIsNull() {
        throw new ForbiddenException();
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenPermissionListIsIsNull() {
        throw new ForbiddenException();
    }
}
