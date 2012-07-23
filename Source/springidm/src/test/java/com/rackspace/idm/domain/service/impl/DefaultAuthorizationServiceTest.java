package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.WadlTree;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
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
    WadlTree wadlTree = mock(WadlTree.class);
    Configuration config = mock(Configuration.class);
    DefaultAuthorizationService spy;

    @Before
    public void setUp() throws Exception {
        defaultAuthorizationService = new DefaultAuthorizationService(scopeAccessDao,clientDao,tenantDao, wadlTree,config);
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
    public void authorizeCloudIdentityAdmin_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeCloudIdentityAdmin(null),equalTo(false));
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
    public void authorizeCloudIdentityAdmin_cloudAdminRoleNotNull_doesNotResetCloudAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCLOUD_ADMIN_ROLE(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudIdentityAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCLOUD_ADMIN_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setCLOUD_ADMIN_ROLE(null);
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
        DefaultAuthorizationService.setRACKER_ROLE(clientRole);
        ScopeAccess scopeAccess = mock(RackerScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeRacker(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getRACKER_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setRACKER_ROLE(null);
    }

    @Test
    public void authorizeCloudServiceAdmin_scopeAccessIsNull_returnsFalse() throws Exception {
        assertThat("boolean", defaultAuthorizationService.authorizeCloudServiceAdmin(null), equalTo(false));
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
    public void authorizeCloudServiceAdmin_cloudServiceAdminRoleNotNull_doesResetCloudServiceAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCLOUD_SERVICE_ADMIN_ROLE(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudServiceAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCLOUD_SERVICE_ADMIN_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setCLOUD_SERVICE_ADMIN_ROLE(null);
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
        DefaultAuthorizationService.setCLOUD_USER_ADMIN_ROLE(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUserAdmin(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCLOUD_USER_ADMIN_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setCLOUD_USER_ADMIN_ROLE(null);
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
        DefaultAuthorizationService.setCLOUD_USER_ROLE(null);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void hasDefaultUserRole_cloudUserAdminRoleNull_setsCloudUserAdminRole() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        DefaultAuthorizationService.setCLOUD_USER_ROLE(null);
        defaultAuthorizationService.hasDefaultUserRole(scopeAccess);
        verify(clientDao).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void authorizeCloudUser_cloudUserAdminRoleNotNull_doesNotResetCloudUserAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        DefaultAuthorizationService.setCLOUD_USER_ROLE(clientRole);
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(null);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        assertThat("client role", DefaultAuthorizationService.getCLOUD_USER_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setCLOUD_USER_ROLE(null);
    }

    @Test
    public void authorizeCloudUser_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        DefaultAuthorizationService.setCLOUD_USER_ROLE(null);
        verify(tenantDao).doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class));
    }

    @Test
    public void hasDefaultUserROle_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        DefaultAuthorizationService.setCLOUD_USER_ROLE(new ClientRole());
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
        DefaultAuthorizationService.setCLOUD_USER_ADMIN_ROLE(cloud_user_admin_role);
        defaultAuthorizationService.hasUserAdminRole(scopeAccess);
        verify(tenantDao).doesScopeAccessHaveTenantRole(scopeAccess, cloud_user_admin_role);
    }

    @Test
    public void authorizeCloudUser_returnsBoolean() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(tenantDao.doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class))).thenReturn(true);
        boolean authorized = defaultAuthorizationService.authorizeCloudUser(scopeAccess);
        DefaultAuthorizationService.setCLOUD_USER_ROLE(null);
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
        assertThat("client role", DefaultAuthorizationService.getIDM_SUPER_ADMIN_ROLE(), equalTo(clientRole));
        DefaultAuthorizationService.setIDM_SUPER_ADMIN_ROLE(null);
    }

    @Test
    public void authorizeIdmSuperAdmin_idmSuperAdminRoleExists_doesNotResetIdmSuperAdminRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        DefaultAuthorizationService.setIDM_SUPER_ADMIN_ROLE(clientRole);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(clientDao.getClientRoleByClientIdAndRoleName(anyString(),anyString())).thenReturn(null);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        assertThat("client role",DefaultAuthorizationService.getIDM_SUPER_ADMIN_ROLE(),equalTo(clientRole));
        DefaultAuthorizationService.setIDM_SUPER_ADMIN_ROLE(null);
    }

    @Test
    public void authorizeIdmSuperAdmin_callsTenantDaoMethod() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        spy.authorizeIdmSuperAdmin(scopeAccess);
        DefaultAuthorizationService.setIDM_SUPER_ADMIN_ROLE(null);
        verify(tenantDao).doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class));
    }

    @Test
    public void authorizeIdmSuperAdmin_returnsBoolean() throws Exception {
        ScopeAccess scopeAccess = mock(UserScopeAccess.class);
        doReturn(false).when(spy).authorizeCustomerIdm(scopeAccess);
        when(((HasAccessToken)scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(tenantDao.doesScopeAccessHaveTenantRole(eq(scopeAccess), any(ClientRole.class))).thenReturn(true);
        boolean authorized = spy.authorizeIdmSuperAdmin(scopeAccess);
        DefaultAuthorizationService.setIDM_SUPER_ADMIN_ROLE(null);
        assertThat("boolean", authorized, equalTo(true));
    }

    @Test
    public void authorizeRackspaceClient_scopeAccessNotInstanceOfClientScopeAccess() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeRackspaceClient(null),equalTo(false));
    }

    @Test
    public void authorizeClient_scopeAccessNotInstanceOfClientScopeAccess() throws Exception {
        assertThat("boolean",defaultAuthorizationService.authorizeClient(null, null, null),equalTo(false));
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
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(true);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"rcn"),equalTo(true));
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserNotInGroupAndIdMatches_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(false);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"rcn"),equalTo(false));
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserInGroupAndIdDoesNotMatch_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(true);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"scn"),equalTo(false));
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN(null);
    }

    @Test
    public void authorizeAdmin_scopeAccessInstanceOfDelegatedClientScopeAccessAndUserNotInGroupAndIdDoesNotMatch_returnsFalse() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("jsmith");
        delegatedClientScopeAccess.setUserRCN("rcn");
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN("dn");
        when(clientDao.isUserInClientGroup("jsmith","dn")).thenReturn(false);
        assertThat("boolean",defaultAuthorizationService.authorizeAdmin(delegatedClientScopeAccess,"scn"),equalTo(false));
        DefaultAuthorizationService.setIDM_ADMIN_GROUP_DN(null);
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
        assertThat("string", DefaultAuthorizationService.getIDM_ADMIN_GROUP_DN(), nullValue());
    }
}
