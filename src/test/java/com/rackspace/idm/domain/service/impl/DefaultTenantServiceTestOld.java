package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.RDN;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/21/12
 * Time: 4:10 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTenantServiceTestOld {

    @InjectMocks
    DefaultTenantService defaultTenantService = new DefaultTenantService();
    @Mock
    private TenantDao tenantDao;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ScopeAccessService scopeAccessService;
    @Mock
    private UserService userService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private Configuration config;
    @Mock
    ScopeAccess scopeAccess;
    @Mock
    TenantRoleDao tenantRoleDao;

    @Before
    public void setUp() throws Exception {

    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenant_nullTenant_throwIllegalArgumentException() throws Exception {
        when(tenantDao.getTenant(null)).thenReturn(new Tenant());
        defaultTenantService.addTenant(null);
    }

    @Test (expected = DuplicateException.class)
    public void addTenant_tenantExists_throwDuplicateException() throws Exception {
        when(tenantDao.getTenant(null)).thenReturn(new Tenant());
        defaultTenantService.addTenant(new Tenant());
    }

    @Test
    public void getTenantByName_callsTenantDaoMethod() throws Exception {
        defaultTenantService.getTenantByName(null);
        verify(tenantDao).getTenantByName(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenantRole_nullRole_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.deleteTenantRoleForUser(getUser(), null);
    }

    @Test
    public void deleteGlobalRole_callsTenantDaoMethod() throws Exception {
        defaultTenantService.deleteGlobalRole(null);
        verify(tenantRoleDao).deleteTenantRole(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIsNullAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.addTenantRoleToUser(null,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIsNullAndRoleExists_throwsIllegalArgumentException() throws Exception {
        TenantRole role = new TenantRole();
        defaultTenantService.addTenantRoleToUser(null,role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIdIsBlankAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        defaultTenantService.addTenantRoleToUser(user,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIdIsBlankAndRoleExists_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        TenantRole tenantRole = new TenantRole();
        defaultTenantService.addTenantRoleToUser(user,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_nullRole_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        defaultTenantService.addTenantRoleToUser(user,null);
    }

    @Test
    public void getTenantOnlyRoles_tenantIdsNull_returnsEmptyList() throws Exception {
        TenantRole role = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        List<TenantRole> tenantRoles = defaultTenantService.getTenantOnlyRoles(roles);
        assertThat("number of tenant roles",tenantRoles.size(),equalTo(0));
    }

    @Test
    public void getTenantOnlyRoles_roleListEmpty_returnsEmptyList() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        List<TenantRole> tenantRoles = defaultTenantService.getTenantOnlyRoles(roles);
        assertThat("number of tenant roles",tenantRoles.size(),equalTo(0));
    }

    @Test
    public void isTenantIdContainedInTenantRoles_rolesIsNull_returnsFalse() throws Exception {
        assertThat("boolean",defaultTenantService.isTenantIdContainedInTenantRoles("tenantId",null),equalTo(false));
    }

    @Test
    public void isTenantIdContainedInTenantRoles_rolesSizeIsZero_returnsFalse() throws Exception {
        assertThat("boolean",defaultTenantService.isTenantIdContainedInTenantRoles("tenantId",new ArrayList<TenantRole>()),equalTo(false));
    }

    public ScopeAccess getScopeAccess() {
        when(scopeAccess.getUniqueId()).thenReturn("id=1234,ou=here,o=path,dc=blah");
        return scopeAccess;
    }

    public User getUser() {
        User user = new User();
        return user;
    }
}
