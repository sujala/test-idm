package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    RoleService roleService = mock(RoleService.class);
    IdentityConfig identityConfig;

    @Before
    public void setUp() throws Exception {
        defaultAuthorizationService = new DefaultAuthorizationService();
        defaultAuthorizationService.setTenantService(tenantSerivce);
        defaultAuthorizationService.setApplicationService(applicationService);
        defaultAuthorizationService.setUserService(userService);
        defaultAuthorizationService.setRoleService(roleService);

        identityConfig = mock(IdentityConfig.class);
        IdentityConfig.ReloadableConfig reloadableConfig = mock(IdentityConfig.ReloadableConfig.class);
        IdentityConfig.StaticConfig staticConfig = mock(IdentityConfig.StaticConfig.class);
        when(identityConfig.getReloadableConfig()).thenReturn(reloadableConfig);
        when(identityConfig.getStaticConfig()).thenReturn(staticConfig);
        when(staticConfig.getImplicitRoleMap()).thenReturn(Collections.EMPTY_MAP);
        defaultAuthorizationService.setIdentityConfig(identityConfig);

        when(roleService.getAllIdentityRoles()).thenReturn(Collections.EMPTY_LIST);

        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultAuthorizationService.retrieveAccessControlRoles();
    }

    @Test
    public void verifyDomain_domainIdIsNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            caller.setId("1");
            User retrievedUser = new User();
            retrievedUser.setId("2");
            retrievedUser.setDomainId("domainId");
            defaultAuthorizationService.verifyDomain(caller, retrievedUser);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_domainIdsAreNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("2");
            defaultAuthorizationService.verifyDomain(caller, retrievedUser);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_callerEqualsRetrievedUser_doNothing() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("1");
            defaultAuthorizationService.verifyDomain(caller, retrievedUser);
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
            defaultAuthorizationService.verifyDomain(caller, retrievedUser);
            assertTrue("should throw exception",false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }
}
