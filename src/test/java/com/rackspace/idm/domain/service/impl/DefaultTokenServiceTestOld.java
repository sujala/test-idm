package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/22/12
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTokenServiceTestOld {

    @InjectMocks
    DefaultTokenService defaultTokenService = new DefaultTokenService();
    @Mock
    UserService userService;
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
    public void getPagingLimit_returnsInt() throws Exception {
        when(config.getInt("ldap.paging.limit.max")).thenReturn(7);
        assertThat("int",defaultTokenService.getPagingLimit(),equalTo(7));
    }
}
