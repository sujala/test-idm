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

    @Test
    public void authorizeCloudServiceAdmin_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeCloudServiceAdmin(null),equalTo(false));
    }

    @Test
    public void authorizeCloudServiceAdmin_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeCloudServiceAdmin(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeCloudServiceAdmin_cloudAdminRoleNotNull_doesNotResetCloudAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudServiceAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudServiceAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudServiceAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudServiceAdminRole(null);
    }

    @Test
    public void authorizeRacker_scopeAccessIsNull_returnsFalse() throws Exception {
        boolean result = defaultAuthorizationService.authorizeRacker(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void authorizeRacker_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(RackerScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeRacker(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeRacker_rackerRoleNotNull_doesNotResetRackerRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setRackerRole(clientRole);
        ScopeAccess scopeAccess = mock(RackerScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeRacker(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getRackerRole(), equalTo(clientRole));
        DefaultAuthorizationService.setRackerRole(null);
    }

    @Test
    public void authorizeCloudIdentityAdmin_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCloudIdentityAdmin(null), equalTo(false));
    }

    @Test
    public void authorizeCloudIdentityAdmin_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeCloudIdentityAdmin(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeCloudIdentityAdmin_cloudIdentityAdminRoleNotNull_doesResetCloudIdentityAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudIdentityAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudIdentityAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudIdentityAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudIdentityAdminRole(null);
    }

    @Test
    public void authorizeIdmSuperAdminOrRackspaceClient_rackspaceClient_grantsAccess() throws Exception {
        doReturn(true).when(spy).authorizeRackspaceClient(null);
        spy.authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void authorizeIdmSuperAdminOrRackspaceClient_notRackspaceClient_checksIdmSuperAdmin() throws Exception {
        doReturn(false).when(spy).authorizeRackspaceClient(null);
        doReturn(true).when(spy).authorizeIdmSuperAdmin(null);
        spy.authorizeIdmSuperAdminOrRackspaceClient(null);
        verify(spy).authorizeIdmSuperAdmin(null);
    }

    @Test (expected = ForbiddenException.class)
    public void authorizeIdmSuperAdminOrRackspaceClient_notRackspaceClientAndNotIdmSuperAdmin_throwsForbiddenException() throws Exception {
        doReturn(false).when(spy).authorizeRackspaceClient(null);
        doReturn(false).when(spy).authorizeIdmSuperAdmin(null);
        spy.authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void authorizeCloudUserAdmin_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUserAdmin(null), equalTo(false));
    }

    @Test
    public void authorizeCloudUserAdmin_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUserAdmin(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeCloudUserAdmin_cloudUserAdminRoleNotNull_doesNotResetCloudUserAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudUserAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUserAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudUserAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudUserAdminRole(null);
    }

    @Test
    public void authorizeCloudUser_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUser(null), equalTo(false));
    }

    @Test
    public void authorizeCloudUser_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUser(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeCloudUser_cloudUserAdminRoleNotNull_doesNotResetCloudUserAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudUserRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudUserRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudUserRole(null);
    }

    @Test
    public void authorizeIdmSuperAdmin_customerIdmAuthorized_returnsTrue() throws Exception {
        doReturn(true).when(spy).authorizeCustomerIdm(null);
        assertThat("boolean",spy.authorizeIdmSuperAdmin(null),equalTo(true));
    }

    @Test
    public void authorizeIdmSuperAdmin_scopeAccessNull_returnsFalse() throws Exception {
        doReturn(false).when(spy).authorizeCustomerIdm(null);
        assertThat("boolean",spy.authorizeIdmSuperAdmin(null),equalTo(false));
    }

    @Test
    public void authorizeIdmSuperAdmin_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean",spy.authorizeIdmSuperAdmin(scopeAccess),equalTo(false));
    }

    @Test
    public void authorizeIdmSuperAdmin_idmSuperAdminRoleExists_doesNotResetIdmSuperAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        DefaultAuthorizationService.setIdmSuperAdminRole(clientRole);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        assertThat("client role",DefaultAuthorizationService.getIdmSuperAdminRole(),equalTo(clientRole));
        DefaultAuthorizationService.setIdmSuperAdminRole(null);
    }

    @Test
    public void authorizeRackspaceClient_scopeAccessNotInstanceOfClientScopeAccess() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeRackspaceClient(null),equalTo(false));
    }

    @Test
    public void authorizeCustomerIdm_scopeAccessNotInstanceOfClientScopeAccess_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCustomerIdm(null), equalTo(false));
    }

    @Test
    public void authorizeCustomerIdm_scopeAccessInstanceOfClientScopeAccessAndIdsMatchAndCustomerIdMatches_returnsTrue() throws Exception {
        ScopeAccess scopeAccess = new ClientScopeAccess();
        scopeAccess.setClientId("123");
        scopeAccess.setClientRCN("456");
        doReturn("123").when(spy).getIdmClientId();
        doReturn("456").when(spy).getRackspaceCustomerId();
        assertThat("boolean",spy.authorizeCustomerIdm(scopeAccess),equalTo(true));
    }

    @Test
    public void authorizeCustomerIdm_scopeAccessInstanceOfClientScopeAccessAndIdsMatchAndCustomerIdDoesNotMatch_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ClientScopeAccess();
        scopeAccess.setClientId("123");
        scopeAccess.setClientRCN("4");
        doReturn("123").when(spy).getIdmClientId();
        doReturn("456").when(spy).getRackspaceCustomerId();
        assertThat("boolean",spy.authorizeCustomerIdm(scopeAccess),equalTo(false));
    }

    @Test
    public void authorizeCustomerIdm_scopeAccessInstanceOfClientScopeAccessAndIdsDoNotMatchAndCustomerIdMatches_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ClientScopeAccess();
        scopeAccess.setClientId("12");
        scopeAccess.setClientRCN("456");
        doReturn("123").when(spy).getIdmClientId();
        doReturn("456").when(spy).getRackspaceCustomerId();
        assertThat("boolean",spy.authorizeCustomerIdm(scopeAccess),equalTo(false));
    }

    @Test
    public void authorizeCustomerIdm_scopeAccessInstanceOfClientScopeAccessAndIdsDoNotMatchAndCustomerIdDoesNotMatch_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ClientScopeAccess();
        scopeAccess.setClientId("1");
        scopeAccess.setClientRCN("4");
        doReturn("123").when(spy).getIdmClientId();
        doReturn("456").when(spy).getRackspaceCustomerId();
        assertThat("boolean",spy.authorizeCustomerIdm(scopeAccess),equalTo(false));
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
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdMatchesAndTargetInstanceOfClientScopeAccessAndClientIdDoesNotMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("abc");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("ABC");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
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
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfClientScopeAccessAndClientIdsMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("abc");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("abc");
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
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfUserScopeAccessAndUsernamesMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("1");
        ((UserScopeAccess) targetScopeAccess).setUsername("jsmith");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        requestingScopeAccess.setClientId("123");
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
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfUserScopeAccessAndUsernamesDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("456");
        ((UserScopeAccess) targetScopeAccess).setUsername("rclements");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(false));
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
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("456");
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
        doReturn(true).when(spy).authorizeIdmSuperAdmin(scopeAccess);
        spy.verifyIdmSuperAdminAccess(null);
    }

    @Test (expected = ForbiddenException.class)
    public void verifyIdmSuperAdminAccess_doesNotHaveAccess_throwsForbiddenException() throws Exception {
        ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        spy.setScopeAccessService(scopeAccessService);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        doReturn(false).when(spy).authorizeIdmSuperAdmin(scopeAccess);
        spy.verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void verifyServiceAdminLevelAccess_withoutAdminLevelAccess_throwsForbiddenException() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
            spy.verifyServiceAdminLevelAccess(scopeAccess);
            assertTrue("should throw exception",false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyRackerOrIdentityAdminAccess_notRackerAndNotCloudIdentityAdmin_throwsForbidden() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doReturn(false).when(spy).authorizeRacker(scopeAccess);
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
            spy.verifyRackerOrIdentityAdminAccess(scopeAccess);
            assertTrue("should throw exception",false);
        } catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyIdentityAdminLevelAccess_notServiceAdminAndNotIdentityAdmin_throwsForbiddenException() throws Exception {
        try{
            UserScopeAccess userScopeAccess = new UserScopeAccess();
            userScopeAccess.setAccessTokenString("admin");
            userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(userScopeAccess);
            doReturn(false).when(spy).authorizeCloudServiceAdmin(userScopeAccess);
            spy.verifyIdentityAdminLevelAccess(userScopeAccess);
            assertTrue("should throw exception",false);
        } catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }

    }

    @Test
    public void verifyUserAdminLevelAccess_notAuthorizedAsServiceAdminOrIdentityAdminOrUserAdmin_throwsForbidden() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
            spy.verifyUserAdminLevelAccess(scopeAccess);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_callerEqualsRetrievedUser_doNothing() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("1");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
        } catch (ForbiddenException ex){
            assertTrue("should not throw exception", false);
        }
    }

    @Test
    public void verifyDomain_callerDomainIdNotMatchRetrievedUserDomainId_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            caller.setDomainId("notSame");
            User retrievedUser = new User();
            retrievedUser.setDomainId("domainId");
            caller.setId("1");
            retrievedUser.setId("2");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception",false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_sameDomainId_success() throws Exception {
        User caller = new User();
        caller.setDomainId("domainId");
        User retrievedUser = new User();
        retrievedUser.setDomainId("domainId");
        caller.setId("1");
        retrievedUser.setId("2");
        spy.verifyDomain(retrievedUser, caller);
    }

    @Test
    public void checkAuthAndHandleFailure_isAuthorized_doesNothing() throws Exception {
        defaultAuthorizationService.checkAuthAndHandleFailure(true,null);
    }

    @Test
    public void checkAuthAndHandleFailure_notAuthorized_throwsForbiddenException() throws Exception {
        try{
            ScopeAccess token = new UserScopeAccess();
            ((HasAccessToken) token).setAccessTokenString("cat");
            defaultAuthorizationService.checkAuthAndHandleFailure(false,token);
            assertTrue("expecting exception",false);
        } catch (ForbiddenException ex){
            String message = ex.getMessage();
            assertThat("message",message,equalTo("Token cat Forbidden from this call"));
        }
    }
}
