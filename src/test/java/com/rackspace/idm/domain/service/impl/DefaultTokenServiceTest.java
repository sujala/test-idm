package com.rackspace.idm.domain.service.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/22/12
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTokenServiceTest {

    @InjectMocks
    DefaultTokenService defaultTokenService = new DefaultTokenService();
    @Mock
    UserDao userDao;
    @Mock
    ApplicationService clientService;
    @Mock
    AuthorizationService authorizationService;
    @Mock
    Configuration config;
    @Mock
    ScopeAccessService scopeAccessService;
    @Mock
    TenantService tenantService;

    DefaultTokenService spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(defaultTokenService);
    }

    @Test
    public void getAccessTokenByAuthHeader_returnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getAccessTokenByAuthHeader(null)).thenReturn(scopeAccess);
        assertThat("scope access",defaultTokenService.getAccessTokenByAuthHeader(null),equalTo(scopeAccess));
    }

    @Test
    public void getAccessTokenByToken_returnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        assertThat("scope access",defaultTokenService.getAccessTokenByToken(null),equalTo(scopeAccess));
    }

    @Test
    public void doesTokenHaveAccessToApplication_returnsBoolean() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(scopeAccessService.doesAccessTokenHaveService(scopeAccess,null)).thenReturn(true);
        assertThat("boolean",defaultTokenService.doesTokenHaveAccessToApplication(null,null),equalTo(true));
    }

    @Test
    public void doesTokenHaveApplicationRole_rolesExistAndAllIdsMatch_returnsTrue() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        TenantRole role = new TenantRole();
        role.setRoleRsId("123");
        role.setClientId("456");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        assertThat("boolean",defaultTokenService.doesTokenHaveApplicationRole(null,"456","123"),equalTo(true));
    }

    @Test
    public void doesTokenHaveApplicationRole_rolesExistAndRoleIdsMatchAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        TenantRole role = new TenantRole();
        role.setRoleRsId("123");
        role.setClientId("4");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        assertThat("boolean",defaultTokenService.doesTokenHaveApplicationRole(null,"456","123"),equalTo(false));
    }

    @Test
    public void doesTokenHaveApplicationRole_rolesExistAndRoleIdsDoNotMatchAndClientIdsMatch_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        TenantRole role = new TenantRole();
        role.setRoleRsId("1");
        role.setClientId("456");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        assertThat("boolean",defaultTokenService.doesTokenHaveApplicationRole(null,"456","123"),equalTo(false));
    }

    @Test
    public void doesTokenHaveApplicationRole_rolesExistAndRoleIdsDoNotMatchAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        TenantRole role = new TenantRole();
        role.setRoleRsId("1");
        role.setClientId("4");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        assertThat("boolean",defaultTokenService.doesTokenHaveApplicationRole(null,"456","123"),equalTo(false));
    }

    @Test
    public void doesTokenHaveApplicationRole_rolesExistAndRoleListEmpty_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(scopeAccessService.loadScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        assertThat("boolean",defaultTokenService.doesTokenHaveApplicationRole(null,"456","123"),equalTo(false));
    }

    @Test
    public void revokeAccessToken_isGoodAsAdminAndAuthorized_doesNothing() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCustomerIdm(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeAsRequestorOrOwner(scopeAccess,scopeAccess)).thenReturn(true);
        defaultTokenService.revokeAccessToken(null,null);
    }

    @Test
    public void revokeAccessToken_isGoodAsAdminAndNotAuthorized_doesNothing() throws Exception {
            ScopeAccess scopeAccess = new ScopeAccess();
            when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
            when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
            when(authorizationService.authorizeCustomerIdm(scopeAccess)).thenReturn(true);
            when(authorizationService.authorizeAsRequestorOrOwner(scopeAccess,scopeAccess)).thenReturn(false);
            defaultTokenService.revokeAccessToken(null,null);
    }

    @Test
    public void revokeAccessToken_isNotGoodAsAdminAndAuthorized_doesNothing() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(scopeAccessService.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCustomerIdm(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeAsRequestorOrOwner(scopeAccess,scopeAccess)).thenReturn(true);
        defaultTokenService.revokeAccessToken(null,null);
    }

    @Test
    public void revokeAllTokensForCustomer_usersExist_callScopeServiceMethod() throws Exception {
        List<User> usersList = new ArrayList<User>();
        usersList.add(new User());
        doReturn(usersList).when(spy).getAllUsersForCustomerId(null);
        doReturn(new ArrayList<Application>()).when(spy).getAllClientsForCustomerId(null);
        spy.revokeAllTokensForCustomer(null);
        verify(scopeAccessService).expireAllTokensForUser(null);
    }

    @Test
    public void revokeAllTokensForCustomer_usersDoNotExist_doesNotCallScopeServiceMethod() throws Exception {
        List<User> usersList = new ArrayList<User>();
        doReturn(usersList).when(spy).getAllUsersForCustomerId(null);
        doReturn(new ArrayList<Application>()).when(spy).getAllClientsForCustomerId(null);
        spy.revokeAllTokensForCustomer(null);
        verify(scopeAccessService,never()).expireAllTokensForUser(anyString());
    }

    @Test
    public void revokeAllTokensForCustomer_clientsExist_callScopeServiceMethod() throws Exception {
        List<Application> clientList = new ArrayList<Application>();
        clientList.add(new Application());
        doReturn(new ArrayList<User>()).when(spy).getAllUsersForCustomerId(null);
        doReturn(clientList).when(spy).getAllClientsForCustomerId(null);
        spy.revokeAllTokensForCustomer(null);
        verify(scopeAccessService).expireAllTokensForClient(null);
    }

    @Test
    public void revokeAllTokensForCustomer_clientsDoNotExist_callScopeServiceMethod() throws Exception {
        doReturn(new ArrayList<User>()).when(spy).getAllUsersForCustomerId(null);
        doReturn(new ArrayList<Application>()).when(spy).getAllClientsForCustomerId(null);
        spy.revokeAllTokensForCustomer(null);
        verify(scopeAccessService,never()).expireAllTokensForClient(anyString());
    }

    @Test
    public void getAllClientsForCustomerId_returnsPopulatedApplicationList() throws Exception {
        List<Application> list = new ArrayList<Application>();
        list.add(new Application());
        Applications clientsObj = new Applications();
        clientsObj.setClients(list);
        clientsObj.setTotalRecords(0);
        doReturn(5).when(spy).getPagingLimit();
        when(clientService.getByCustomerId(null, 0, 5)).thenReturn(clientsObj);
        List<Application> clientsList = spy.getAllClientsForCustomerId(null);
        assertThat("size",clientsList.size(),equalTo(1));
    }

    @Test
    public void getAllUsersForCustomerId_returnsPopulatedUserList() throws Exception {
        List<User> list = new ArrayList<User>();
        list.add(new User());
        Users usersObj = new Users();
        usersObj.setUsers(list);
        usersObj.setTotalRecords(0);
        doReturn(5).when(spy).getPagingLimit();
        when(userDao.getAllUsers(any(FilterParam[].class), eq(0), eq(5))).thenReturn(usersObj);
        List<User> usersList = spy.getAllUsersForCustomerId(null);
        assertThat("size",usersList.size(),equalTo(1));
    }

    @Test
    public void getPagingLimit_returnsInt() throws Exception {
        when(config.getInt("ldap.paging.limit.max")).thenReturn(7);
        assertThat("int",defaultTokenService.getPagingLimit(),equalTo(7));
    }
}
