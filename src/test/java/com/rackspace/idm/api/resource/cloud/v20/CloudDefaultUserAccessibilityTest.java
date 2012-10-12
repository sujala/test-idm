package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ScopeAccess;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/11/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudDefaultUserAccessibilityTest{

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

        cloudUserAccessibility = new CloudDefaultUserAccessibility(tenantService, domainService, authorizationService, userService, config, objFactory, scopeAccess);

    }

    @Test
    public void testHasAccess_DifferentUser() throws Exception {
        User user1 = new User();
        User user2 = new User();

        user1.setId("1");
        user2.setId("2");

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(false));
    }

    @Test
    public void testHasAccess_SameUser() throws Exception {
        User user1 = new User();
        User user2 = new User();

        user1.setId("1");
        user2.setId("1");

        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(user1).thenReturn(user2);

        Boolean access = cloudUserAccessibility.hasAccess(scopeAccess);
        assertThat("Access", access, equalTo(true));
    }
}
