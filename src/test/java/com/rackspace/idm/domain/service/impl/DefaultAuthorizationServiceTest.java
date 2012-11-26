package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/21/12
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultAuthorizationServiceTest {

    DefaultAuthorizationService defaultAuthorizationService;
    ScopeAccessDao scopeAccessDao = mock(ScopeAccessDao.class);
    ApplicationDao clientDao = mock(ApplicationDao.class);
    TenantDao tenantDao = mock(TenantDao.class);
    Configuration config = mock(Configuration.class);
    TenantService tenantSerivce = mock(TenantService.class);
    DefaultAuthorizationService spy;

    @Before
    public void setUp() throws Exception {
        defaultAuthorizationService = new DefaultAuthorizationService();
        defaultAuthorizationService.setScopeAccessDao(scopeAccessDao);
        defaultAuthorizationService.setApplicationDao(clientDao);
        defaultAuthorizationService.setTenantDao(tenantDao);
        defaultAuthorizationService.setConfig(config);
        defaultAuthorizationService.setTenantService(tenantSerivce);
        spy = spy(defaultAuthorizationService);

    }

    @Test (expected = IllegalArgumentException.class)
    public void authorize_nullToken_throwsIllegalArgumentException() throws Exception {
        defaultAuthorizationService.authorize(null,null,null);
    }

    @Test
    public void authorize_clientHasAuthorizedRoles_grantsAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.getScopeAccessByAccessToken("token")).thenReturn(scopeAccess);
        when(spy.doesClientHaveAuthorizedRoles(scopeAccess, null)).thenReturn(true);
        spy.authorize("token", null, null);
    }

    @Test
    public void authorize_clientDoesNotHaveAuthorizedRolesAndIsEntityBeingAccessed_grantsAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.getScopeAccessByAccessToken("token")).thenReturn(scopeAccess);
        when(spy.doesClientHaveAuthorizedRoles(scopeAccess, null)).thenReturn(false);
        when(spy.isClientTheEntityBeingAccessed(scopeAccess, null)).thenReturn(true);
        spy.authorize("token",null,null);
    }

    @Test (expected = ForbiddenException.class)
    public void authorize_clientDoesNotHaveAuthorizedRolesAndIsNotEntityBeingAccessed_throwsForbiddenException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.getScopeAccessByAccessToken("token")).thenReturn(scopeAccess);
        when(spy.doesClientHaveAuthorizedRoles(scopeAccess, null)).thenReturn(false);
        when(spy.isClientTheEntityBeingAccessed(scopeAccess, null)).thenReturn(false);
        spy.authorize("token", null, null);
    }

    @Test
    public void doesClientHaveAuthorizedRoles_scopeAccessHasTenantRole_returnsTrue() throws Exception {
        List<String> allAuthorizedRoles = new ArrayList<String>();
        allAuthorizedRoles.add("1");
        ClientRole clientRole = new ClientRole();
        when(spy.createRoleList(null)).thenReturn(allAuthorizedRoles);
        when(clientDao.getClientRoleById("1")).thenReturn(clientRole);
        when(tenantDao.doesScopeAccessHaveTenantRole(null, clientRole)).thenReturn(true);
        assertThat("has authorized status", spy.doesClientHaveAuthorizedRoles(null, null), equalTo(true));
    }

    @Test
    public void doesClientHaveAuthorizedRoles_scopeAccessDoesNotHaveTenantRole_returnsFalse() throws Exception {
        List<String> allAuthorizedRoles = new ArrayList<String>();
        allAuthorizedRoles.add("1");
        ClientRole clientRole = new ClientRole();
        when(spy.createRoleList(null)).thenReturn(allAuthorizedRoles);
        when(clientDao.getClientRoleById("1")).thenReturn(clientRole);
        when(tenantDao.doesScopeAccessHaveTenantRole(null, clientRole)).thenReturn(false);
        assertThat("has authorized status", spy.doesClientHaveAuthorizedRoles(null, null), equalTo(false));
    }

    @Test
    public void doesClientHaveAuthorizedRoles_roleListIsEmpty_returnsFalse() throws Exception {
        List<String> allAuthorizedRoles = new ArrayList<String>();
        when(spy.createRoleList(null)).thenReturn(allAuthorizedRoles);
        assertThat("has authorized status", spy.doesClientHaveAuthorizedRoles(null, null), equalTo(false));
    }

    @Test
    public void createRoleList_twoRoles_returnsCorrectList() throws Exception {
       List<String> allAuthorizedRoles = defaultAuthorizationService.createRoleList("programmers","awesome");
       assertThat("number of roles", allAuthorizedRoles.size(), equalTo(3));
       assertThat("first role",allAuthorizedRoles.get(0),equalTo(ClientRole.SUPER_ADMIN_ROLE));
       assertThat("second role",allAuthorizedRoles.get(1),equalTo("programmers"));
       assertThat("third role",allAuthorizedRoles.get(2),equalTo("awesome"));
    }

    @Test
    public void createRoleList_nullParameter_returnsListWithOnlySuperAdminRole() throws Exception {
        List<String> allAuthorizedRoles = defaultAuthorizationService.createRoleList(null);
        assertThat("number of roles",allAuthorizedRoles.size(),equalTo(1));
        assertThat("first role",allAuthorizedRoles.get(0),equalTo(ClientRole.SUPER_ADMIN_ROLE));
    }

    @Test
    public void isClientTheEntityBeingAccessed_returnsFalse() throws Exception {
        assertThat("boolean",defaultAuthorizationService.isClientTheEntityBeingAccessed(null, null),equalTo(false));
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
    public void authorizeCloudServiceAdmin_cloudAdminNull_callsClientDao() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("roleName");
        spy.authorizeCloudServiceAdmin(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName("clientId", "roleName");
    }

    @Test
    public void authorizeCloudServiceAdmin_cloudAdminRoleNotNull_doesNotResetCloudAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudServiceAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudAdminRole(null);
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
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
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
    public void authorizeCloudIdentityAdmin_cloudAdminNull_callsClientDao() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(config.getString("cloudAuth.adminRole")).thenReturn("roleName");
        spy.authorizeCloudIdentityAdmin(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName("clientId", "roleName");
    }

    @Test
    public void authorizeCloudIdentityAdmin_cloudIdentityAdminRoleNotNull_doesResetCloudIdentityAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudIdentityAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
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
    public void authorizeCloudUserAdmin_cloudAdminNull_callsClientDao() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("roleName");
        spy.authorizeCloudUserAdmin(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName("clientId", "roleName");
    }

    @Test
    public void authorizeCloudUserAdmin_cloudUserAdminRoleNotNull_doesNotResetCloudUserAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudUserAdminRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUserAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudUserAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudUserAdminRole(null);
    }

    @Test
    public void authorizeCloudUser_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUser(null), equalTo(false));
    }

    @Test
    public void hasDefaultUserRole_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.hasDefaultUserRole(null), equalTo(false));
    }

    @Test
    public void authorizeCloudUser_tokenExpired_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        assertThat("boolean", defaultAuthorizationService.authorizeCloudUser(scopeAccess), equalTo(false));
    }

    @Test
    public void authorizeCloudUser_cloudUserAdminRoleNull_setsCloudUserAdminRole() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        DefaultAuthorizationService.setCloudUserRole(null);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void hasDefaultUserRole_cloudUserAdminRoleNull_setsCloudUserAdminRole() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        DefaultAuthorizationService.setCloudUserRole(null);
        defaultAuthorizationService.hasDefaultUserRole(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void authorizeCloudUser_cloudUserAdminRoleNotNull_doesNotResetCloudUserAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCloudUserRole(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCloudUserRole(), equalTo(clientRole));
        DefaultAuthorizationService.setCloudUserRole(null);
    }

    @Test
    public void authorizeCloudUser_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        DefaultAuthorizationService.setCloudUserRole(null);
        verify(tenantDao).doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class));
    }

    @Test
    public void hasDefaultUserROle_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        DefaultAuthorizationService.setCloudUserRole(new ClientRole());
        defaultAuthorizationService.hasDefaultUserRole(scopeAccess);
        verify(tenantDao).doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class));
    }

    @Test
    public void hasUserAdminRole_scopeAccessIsNull_returnsFalse() throws Exception {
        boolean result = defaultAuthorizationService.hasUserAdminRole(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void hasUserAdminRole_cloudUserAdminRoleIsNull_setsRole() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        defaultAuthorizationService.hasUserAdminRole(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName(null, null);
    }

    @Test
    public void hasUserAdminRole_callsTenantDao_doesScopeAccessHaveTenantRole() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ClientRole cloud_user_admin_role = new ClientRole();
        DefaultAuthorizationService.setCloudUserAdminRole(cloud_user_admin_role);
        defaultAuthorizationService.hasUserAdminRole(scopeAccess);
        verify(tenantDao).doesScopeAccessHaveTenantRole(scopeAccess, cloud_user_admin_role);
    }

    @Test
    public void authorizeCloudUser_returnsBoolean() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(tenantDao.doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class))).thenReturn(true);
        boolean authorized = defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        DefaultAuthorizationService.setCloudUserRole(null);
        assertThat("boolean", authorized, equalTo(true));
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
    public void authorizeIdmSuperAdmin_idmSuperAdminRoleNull_setsIdmSuperAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(clientRole);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getIdmSuperAdminRole(), equalTo(clientRole));
        DefaultAuthorizationService.setIdmSuperAdminRole(null);
    }

    @Test
    public void authorizeIdmSuperAdmin_idmSuperAdminRoleExists_doesNotResetIdmSuperAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        DefaultAuthorizationService.setIdmSuperAdminRole(clientRole);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        assertThat("client role",DefaultAuthorizationService.getIdmSuperAdminRole(),equalTo(clientRole));
        DefaultAuthorizationService.setIdmSuperAdminRole(null);
    }

    @Test
    public void authorizeIdmSuperAdmin_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        DefaultAuthorizationService.setIdmSuperAdminRole(null);
        verify(tenantDao).doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class));
    }

    @Test
    public void authorizeIdmSuperAdmin_returnsBoolean() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(tenantDao.doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class))).thenReturn(true);
        boolean authorized = spy.authorizeIdmSuperAdmin(scopeAccess);
        DefaultAuthorizationService.setIdmSuperAdminRole(null);
        assertThat("boolean", authorized, equalTo(true));
    }

    @Test
    public void authorizeRackspaceClient_scopeAccessNotInstanceOfClientScopeAccess() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeRackspaceClient(null),equalTo(false));
    }

    @Test
    public void authorizeUser_scopeAccessNotInstanceOfUserScopeAccessOrDelegatedClientScopeAccess_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeUser(null, null, null), equalTo(false));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfUserScopeAccessUsernamesEqualUserRcnEquals_returnsTrue() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername("jsmith");
        userScopeAccess.setUserRCN("id");
        assertThat("boolean", defaultAuthorizationService.authorizeUser(userScopeAccess, "ID", "jsmith"), equalTo(true));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfUserScopeAccessUsernamesNotEqualUserRcnEquals_returnsFalse() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername("jsmith");
        userScopeAccess.setUserRCN("id");
        assertThat("boolean",defaultAuthorizationService.authorizeUser(userScopeAccess,"ID","psmith"),equalTo(false));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfDelegateClientAccessUsernamesEqualUserRcnsEqual_returnsTrue() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("id");
        assertThat("boolean",defaultAuthorizationService.authorizeUser(delegatedClientScopeAccess,"ID","jsmith"),equalTo(true));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfDelegateClientAccessUsernamesEqualUserRcnsNotEqual_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("id");
        assertThat("boolean",defaultAuthorizationService.authorizeUser(delegatedClientScopeAccess,"foo","jsmith"),equalTo(false));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfDelegateClientAccessUsernamesNotEqualUserRcnsEqual_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("id");
        assertThat("boolean",defaultAuthorizationService.authorizeUser(delegatedClientScopeAccess,"ID","psmith"),equalTo(false));
    }

    @Test
    public void authorizeUser_scopeAccessInstanceOfDelegateClientAccessUsernamesNotEqualUserRcnsNotEqual_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("id");
        assertThat("boolean",defaultAuthorizationService.authorizeUser(delegatedClientScopeAccess,"foo","psmith"),equalTo(false));
    }

    @Test
    public void authorizeCustomerUser_scopeAccessNotInstanceOfUserScopeAccessOrDelegatedClientScopeAccess_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCustomerUser(null, null), equalTo(false));
    }

    @Test
    public void authorizeCustomerUser_scopeAccessInstanceOfDelegatedClientScopeAccessAndUsernamesEqual_returnsTrue() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUserRCN("rcn");
        assertThat("boolean",defaultAuthorizationService.authorizeCustomerUser(delegatedClientScopeAccess, "rcn"),equalTo(true));

    }

    @Test
    public void authorizeAdmin_scopeAccessNotInstanceOfUserScopeAccessOrDelegatedClientScopeAccess_returnsFalse() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(null,null),equalTo(false));
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserInGroupAndIdMatches_returnsTrue() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIdmAdminGroupDn("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(true);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"rcn"),equalTo(true));
        DefaultAuthorizationService.setIdmAdminGroupDn(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserNotInGroupAndIdMatches_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIdmAdminGroupDn("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(false);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"rcn"),equalTo(false));
        DefaultAuthorizationService.setIdmAdminGroupDn(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserInGroupAndIdDoesNotMatch_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIdmAdminGroupDn("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(true);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"scn"),equalTo(false));
        DefaultAuthorizationService.setIdmAdminGroupDn(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserNotInGroupAndIdDoesNotMatch_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIdmAdminGroupDn("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(false);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"scn"),equalTo(false));
        DefaultAuthorizationService.setIdmAdminGroupDn(null);
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
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyServiceAdminLevelAccess_withAdminLevelAccess_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        spy.verifyServiceAdminLevelAccess(scopeAccess);
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
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyRackerOrIdentityAdminAccess_isRackerAndNotCloudIdentityAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeRacker(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        spy.verifyRackerOrIdentityAdminAccess(scopeAccess);
    }

    @Test
    public void verifyRackerOrIdentityAdminAccess_callsAuthorizationService_authorizeCloudIdentityAdmin() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeRacker(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        spy.verifyRackerOrIdentityAdminAccess(scopeAccess);
        verify(spy).authorizeCloudIdentityAdmin(scopeAccess);
    }

    @Test
    public void verifyRackerOrIdentityAdminAccess_isIdentityAdminAndRacker_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeRacker(scopeAccess);
        spy.verifyRackerOrIdentityAdminAccess(scopeAccess);
    }

    @Test
    public void verifyRackerOrIdentityAdminAccess_isIdentityAdmin_callsAuthorizeCloudIdentityAdmin() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeRacker(scopeAccess);
        spy.verifyRackerOrIdentityAdminAccess(scopeAccess);
        verify(spy).authorizeCloudIdentityAdmin(scopeAccess);
    }

    @Test
    public void verifyIdentityAdminLevelAccess_ServiceAdminCallerAndNotIdentityAdmin_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        doReturn(true).when(spy).authorizeCloudServiceAdmin(userScopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(userScopeAccess);
        spy.verifyIdentityAdminLevelAccess(userScopeAccess);
    }

    @Test
    public void verifyIdentityAdminLevelAccess_notServiceAdminCallerAndIdentityAdmin_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        doReturn(false).when(spy).authorizeCloudServiceAdmin(userScopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(userScopeAccess);
        spy.verifyIdentityAdminLevelAccess(userScopeAccess);
    }

    @Test
    public void verifyIdentityAdminLevelAccess_ServiceAdminCallerAndIdentityAdmin_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        doReturn(true).when(spy).authorizeCloudServiceAdmin(userScopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(userScopeAccess);
        spy.verifyIdentityAdminLevelAccess(userScopeAccess);
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
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
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
            assertThat("exception message", ex.getMessage(), equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsUserAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsIdentityAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsIdentityAdminAndUserAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsServiceAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsServiceAdminAndUserAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsServiceAdminAndIdentityAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserAdminLevelAccess_authorizedAsServiceAdminAndIdentityAdminAndUserAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        spy.verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessHasAllRoles_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotCloudUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotUserAdminOrUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotIdentityAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotIdentityAdminOrUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotIdentityAdminOrUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsServiceAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotServiceAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotServiceAdminOrUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessNotServiceAdminOrUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsIdentityAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsUserAdminAndUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudUser(scopeAccess);
        spy.verifyUserLevelAccess(scopeAccess);
        Assert.assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_notValidUserLevelAccess_throwsForbidden() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudUserAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudUser(scopeAccess);
            spy.verifyUserLevelAccess(scopeAccess);
            assertTrue("should throw exception",false);
        } catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifySelf_sameUsernameAndUniqueId_succeeds() throws Exception {
            User user1 = new User();
            user1.setId("foo");
            user1.setUsername("foo");
            user1.setUniqueId("foo");
            User user2 = new User();
            user2.setId("foo");
            user2.setUsername("foo");
            user2.setUniqueId("foo");
            defaultAuthorizationService.verifySelf(user1, user2);
    }

    @Test
    public void verifySelf_differentUsername_throwsForbiddenException() throws Exception {
       try{
           User user1 = new User();
           user1.setId("foo");
           user1.setUsername("foo");
           user1.setUniqueId("foo");
           User user2 = new User();
           user2.setId("foo");
           user2.setUsername("!foo");
           user2.setUniqueId("foo");
           defaultAuthorizationService.verifySelf(user1, user2);
           assertTrue("should throw exception", false);
       }catch (ForbiddenException ex){
           assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
       }
    }

    @Test
    public void verifySelf_differentUniqueId_throwsForbiddenException() throws Exception {
        try{
            User user1 = new User();
            user1.setId("foo");
            user1.setUsername("foo");
            user1.setUniqueId("foo");
            User user2 = new User();
            user2.setId("foo");
            user2.setUsername("foo");
            user2.setUniqueId("!foo");
            defaultAuthorizationService.verifySelf(user1, user2);
            assertTrue("should throw exception", false);
        }catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not authorized."));
        }

    }

    @Test
    public void verifySelf_differentUsernameAndDifferentUniqueId_throwsForbiddenException() throws Exception {
        try{
            User user1 = new User();
            user1.setId("foo");
            user1.setUsername("foo");
            user1.setUniqueId("foo");
            User user2 = new User();
            user2.setId("foo");
            user2.setUsername("!foo");
            user2.setUniqueId("!foo");
            defaultAuthorizationService.verifySelf(user1, user2);
            assertTrue("should throw exception", false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyTokenHasTenantAccess_isServiceAdminAndIdentityAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
    }

    @Test
    public void verifyTokenHasTenantAccess_isServiceAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(true).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
    }

    @Test
    public void verifyTokenHasTenantAccess_isIdentityAdmin_succeeds() throws Exception {
        ScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(true).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
    }

    @Test
    public void verifyTokenHasTenantAccess_tenantIdEquals_succeeds() throws Exception {
        ScopeAccess scopeAccess = new UserScopeAccess();
        Tenant tenant = new Tenant();
        tenant.setTenantId("tenantId");
        List<Tenant> tenantList = new ArrayList<Tenant>();
        tenantList.add(tenant);
        doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
        doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
        when(tenantSerivce.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);
        spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
    }

    @Test
    public void verifyTokenHasTenantAccess_tenantIdNotEquals_throwsForbiddenExcpetion() throws Exception {
        try{
            ScopeAccess scopeAccess = new UserScopeAccess();
            Tenant tenant = new Tenant();
            tenant.setTenantId("notMatch");
            List<Tenant> tenantList = new ArrayList<Tenant>();
            tenantList.add(tenant);
            doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
            when(tenantSerivce.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);
            spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
            assertTrue("should throw exception",false);
        } catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyTokenHasTenantAccess_listEmpty_throwsForbiddenExcpetion() throws Exception {
        try{
            ScopeAccess scopeAccess = new UserScopeAccess();
            List<Tenant> tenantList = new ArrayList<Tenant>();
            doReturn(false).when(spy).authorizeCloudServiceAdmin(scopeAccess);
            doReturn(false).when(spy).authorizeCloudIdentityAdmin(scopeAccess);
            when(tenantSerivce.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);
            spy.verifyTokenHasTenantAccess("tenantId", scopeAccess);
            assertTrue("should throw exception",false);
        } catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyDomain_domainIdIsNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            caller.setId("1");
            User retrievedUser = new User();
            retrievedUser.setId("2");
            retrievedUser.setDomainId("domainId");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not authorized."));
        }
    }

    @Test
    public void verifyDomain_domainIdsAreNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("2");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not authorized."));
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
            assertThat("exception message",ex.getMessage(),equalTo("Not authorized."));
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

    @Test
    public void getIDM_ADMIN_GROUP_DN() throws Exception {
        assertThat("string", defaultAuthorizationService.getIdmAdminGroupDn(), nullValue());
    }
}
