package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.service.ApiDocService;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import com.rackspace.idm.util.WadlTree;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jmx.export.MBeanExporter;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/28/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceConfigurationTest {

    ScopeAccessDao scopeAccessDao = mock(ScopeAccessDao.class);

    Configuration config = mock(Configuration.class);
    ServiceConfiguration serviceConfiguration;
    ServiceConfiguration spy;

    @Before
    public void setUp() throws Exception {
        serviceConfiguration = new ServiceConfiguration(config);
        spy = new ServiceConfiguration();
        spy = spy(serviceConfiguration);
    }

    @Test
    public void authHeaderHelper_returnsAuthHeaderHelper() throws Exception {
        assertThat("auth header helper",serviceConfiguration.authHeaderHelper(),instanceOf(AuthHeaderHelper.class));
    }

    @Test
    public void loggerMonitoringBean_returnsLoggerMBean() throws Exception {
        assertThat("loggerMBean",serviceConfiguration.loggerMonitoringBean(),instanceOf(LoggerMBean.class));
    }

    @Test
    public void ldapRouterMonitoringBean_returnsLdapRouterMBean() throws Exception {
        assertThat("ldapRouterMBean",serviceConfiguration.ldapRouterMonitoringBean(),instanceOf(LdapRouterMBean.class));
    }

    @Test
    public void clientService_returnsApplicationService() throws Exception {
        assertThat("application service",serviceConfiguration.clientService(),instanceOf(ApplicationService.class));
    }

    @Test
    public void endpointService_returnsEndpointService() throws Exception {
        assertThat("endpoint service",serviceConfiguration.endpointService(),instanceOf(EndpointService.class));
    }

    @Test
    public void customerService_callsTokenService() throws Exception {
        spy.customerService();
        verify(spy).tokenService();
    }

    @Test
    public void customerService_returnsDefaultCustomerService() throws Exception {
        assertThat("default customer service",serviceConfiguration.customerService(),instanceOf(DefaultCustomerService.class));
    }

    @Test
    public void cloudGroupService_callsUserService() throws Exception {
        spy.cloudGroupService();
        verify(spy).userService();
    }

    @Test
    public void cloudGroupService_returnsDefaultGroupService() throws Exception {
        assertThat("default group service",serviceConfiguration.cloudGroupService(),instanceOf(DefaultGroupService.class));
    }

    @Test
    public void wadlTrie_returnsWadlTrie() throws Exception {
        assertThat("wadlTrie",serviceConfiguration.wadlTrie(),instanceOf(WadlTree.class));
    }

    @Test
    public void passwordComplexityService_returnsDefaultPasswordComplexityService() throws Exception {
        assertThat("password complexity", serviceConfiguration.passwordComplexityService(),instanceOf(DefaultPasswordComplexityService.class));
    }

    @Test
    public void scopeAccessService_callsAuthHeaderHelper() throws Exception {
        spy.scopeAccessService();
        verify(spy).authHeaderHelper();
    }

    @Test
    public void scopeAccessService_returnsScopeAccessService() throws Exception {
        assertThat("scope access service",serviceConfiguration.scopeAccessService(),instanceOf(ScopeAccessService.class));
    }

    @Test
    public void userService_callsClientService() throws Exception {
        spy.userService();
        verify(spy,atLeastOnce()).clientService();
    }

    @Test
    public void userService_callsTokenService() throws Exception {
        spy.userService();
        verify(spy,atLeastOnce()).tokenService();
    }

    @Test
    public void userService_callsPasswordComplexityService() throws Exception {
        spy.userService();
        verify(spy,atLeastOnce()).passwordComplexityService();
    }

    @Test
    public void userService_returnsDefaultUserService() throws Exception {
        assertThat("default user service",serviceConfiguration.userService(),instanceOf(DefaultUserService.class));
    }

    @Test
    public void tokenService_callsClientService() throws Exception {
        spy.tokenService();
        verify(spy,atLeastOnce()).clientService();
    }

    @Test
    public void tokenService_callsAuthorizationService() throws Exception {
        spy.tokenService();
        verify(spy,atLeastOnce()).authorizationService();
    }

    @Test
    public void tokenService_callsScopeAccessService() throws Exception {
        spy.tokenService();
        verify(spy,atLeastOnce()).scopeAccessService();
    }

    @Test
    public void tokenService_callsTenantService() throws Exception {
        spy.tokenService();
        verify(spy,atLeastOnce()).tenantService();
    }

    @Test
    public void tokenService_returnDefaultTokenService() throws Exception {
        assertThat("default token service",serviceConfiguration.tokenService(),instanceOf(DefaultTokenService.class));
    }

    @Test
    public void authorizationService_callsWadlTrie() throws Exception {
        spy.authorizationService();
        verify(spy).authorizationService();
    }

    @Test
    public void authorizationService_returnsAuthorizationService() throws Exception {
        assertThat("authorization service",serviceConfiguration.authorizationService(),instanceOf(DefaultAuthorizationService.class));
    }

    @Test
    public void apiDocService_returnsDefaultApiDocService() throws Exception {
        assertThat("default api doc service",serviceConfiguration.apiDocService(),instanceOf(ApiDocService.class));
    }

    @Test
    public void tenantService_returnsTenantService() throws Exception {
        assertThat("default tenant service",serviceConfiguration.tenantService(),instanceOf(DefaultTenantService.class));
    }

    @Test
    public void authenticationService_callsTenantService() throws Exception {
        spy.authenticationService();
        verify(spy,atLeastOnce()).tenantService();
    }

    @Test
    public void authenticationService_callsScopeAccessService() throws Exception {
        spy.authenticationService();
        verify(spy,atLeastOnce()).scopeAccessService();
    }

    @Test
    public void authenticationService_returnsDefaultAuthenticationService() throws Exception {
        assertThat("default authentication service", serviceConfiguration.authenticationService(),instanceOf(DefaultAuthenticationService.class));
    }

    @Test
    public void exporter_callsLoggerMonitoringBean() throws Exception {
        spy.exporter();
        verify(spy).loggerMonitoringBean();
    }

    @Test
    public void exporter_callsLdapRouterMonitoringBean() throws Exception {
        spy.exporter();
        verify(spy).ldapRouterMonitoringBean();
    }

    @Test
    public void exporter_returnsCorrectMBeanExporter() throws Exception {
        assertThat("mBeanExporter",serviceConfiguration.exporter(),instanceOf(MBeanExporter.class));
    }
}
