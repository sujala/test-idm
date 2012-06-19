package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 12:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class RackerResourceTest {
    private RackerResource rackerResource;
    private UserService userService;
    private AuthorizationService authorizationService;
    private ScopeAccessService scopeAccessService;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        inputValidator = mock(InputValidator.class);
        rackerResource = new RackerResource(userService, authorizationService, scopeAccessService, inputValidator);
    }

    @Test
    public void deleteRacker_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        rackerResource.deleteRacker("racker", "authHeader");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void deleteRacker_callsAuthorizationService_authorizeIdmSuperAdminOrRackspaceClient() throws Exception {
        rackerResource.deleteRacker("racker", "authHeader");
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void deleteRacker_callsUserService_deleteRacker() throws Exception {
        rackerResource.deleteRacker("racker", "authHeader");
        verify(userService).deleteRacker("racker");
    }

    @Test
    public void deleteRacker_responseOk_returns200() throws Exception {
        Response response = rackerResource.deleteRacker("racker", "authHeader");
        assertThat("respone code", response.getStatus(), equalTo(200));
    }
}
