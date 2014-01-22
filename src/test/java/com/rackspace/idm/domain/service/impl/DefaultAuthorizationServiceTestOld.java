package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/21/12
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultAuthorizationServiceTestOld {

    DefaultAuthorizationService defaultAuthorizationService;
    ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
    ApplicationService applicationService = mock(ApplicationService.class);
    Configuration config = mock(Configuration.class);
    TenantService tenantSerivce = mock(TenantService.class);
    UserService userService = mock(UserService.class);
    DefaultAuthorizationService spy;

    @Before
    public void setUp() throws Exception {
        defaultAuthorizationService = new DefaultAuthorizationService();
        defaultAuthorizationService.setConfig(config);
        defaultAuthorizationService.setTenantService(tenantSerivce);
        defaultAuthorizationService.setScopeAccessService(scopeAccessService);
        defaultAuthorizationService.setApplicationService(applicationService);
        defaultAuthorizationService.setUserService(userService);
        spy = spy(defaultAuthorizationService);

        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultAuthorizationService.retrieveAccessControlRoles();
    }

    @Test (expected = ForbiddenException.class)
    public void authorizeIdmSuperAdminOrRackspaceClient_notRackspaceClientAndNotIdmSuperAdmin_throwsForbiddenException() throws Exception {
        doReturn(false).when(spy).authorizeRackspaceClient(null);
        doReturn(false).when(spy).authorizeIdmSuperAdmin(null);
        spy.authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdMatchesAndTargetInstanceOfClientScopeAccessAndClientIdMatches_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("123");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("123");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdMatchesAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("abc");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("ABC");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfUserScopeAccessAndUsernamesMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("123");
        ((UserScopeAccess) targetScopeAccess).setUsername("jsmith");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        requestingScopeAccess.setClientId("123");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("456");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("123");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        ((RackerScopeAccess)requestingScopeAccess).setRackerId("123");
        requestingScopeAccess.setClientId("456");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("4");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("123");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        ((RackerScopeAccess)requestingScopeAccess).setRackerId("123");
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfClientScopeAccessAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("456");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("rclements");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfClientScopeAccessAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfUserScopeAccessAndUsernamesDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("4");
        ((UserScopeAccess) targetScopeAccess).setUsername("rclements");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("4");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("rclements");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void verifyIdmSuperAdminAccess_hasAccess_doesNothing() throws Exception {
        ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        spy.setScopeAccessService(scopeAccessService);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        doReturn(true).when(spy).authorizeIdmSuperAdmin(any(AuthorizationContext.class));
        spy.verifyIdmSuperAdminAccess(null);
    }

    @Test (expected = ForbiddenException.class)
    public void verifyIdmSuperAdminAccess_doesNotHaveAccess_throwsForbiddenException() throws Exception {
        ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        spy.setScopeAccessService(scopeAccessService);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        doReturn(false).when(spy).authorizeIdmSuperAdmin(any(AuthorizationContext.class));
        spy.verifyIdmSuperAdminAccess(null);
    }
}
