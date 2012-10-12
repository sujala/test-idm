package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/11/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudIdentityAdminAccessibilityTest{

    private CloudUserAccessibility cloudUserAccessibility;

    private Configuration config;

    public ScopeAccess scopeAccess;

    protected TenantService tenantService;

    private DomainService domainService;

    protected AuthorizationService authorizationService;

    protected UserService userService;

    private ObjectFactory objFactory;

    @Before
    public void setUp(){
        scopeAccess = mock(ScopeAccess.class);
        tenantService = mock(TenantService.class);
        domainService = mock(DomainService.class);
        authorizationService = mock(AuthorizationService.class);
        userService =  mock(UserService.class);
        objFactory = mock(ObjectFactory.class);
        config = mock(Configuration.class);

        cloudUserAccessibility = new CloudIdentityAdminAccessibility(tenantService, domainService, authorizationService, userService, config, objFactory, scopeAccess);

    }

    @Test
    public void testHasAccess_identityAdmin_returnsTrue() throws Exception {
        User user1 = new User();
        User user2 = new User();
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:admin");
        tenantRoleList.add(role);

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        when(config.getString(anyString())).thenReturn("identity:admin");

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(true));
    }

    @Test
    public void testHasAccess_serviceAdmin_returnsTrue() throws Exception {
        User user1 = new User();
        User user2 = new User();
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:service-admin");
        tenantRoleList.add(role);

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        when(config.getString(anyString())).thenReturn("identity:service-admin");

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(true));
    }

    @Test
    public void testHasAccess_userAdmin_returnsTrue() throws Exception {
        User user1 = new User();
        User user2 = new User();
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:identity:user-admin");
        tenantRoleList.add(role);

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        when(config.getString(anyString())).thenReturn("identity:service-admin");

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(false));
    }

    @Test
    public void testHasAccess_defaultUser_returnsTrue() throws Exception {
        User user1 = new User();
        User user2 = new User();
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:identity:default");
        tenantRoleList.add(role);

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        when(config.getString(anyString())).thenReturn("identity:service-admin");

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(false));
    }
}
