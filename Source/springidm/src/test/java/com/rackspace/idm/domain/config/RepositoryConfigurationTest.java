package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.*;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryConfigurationTest {

    RepositoryConfiguration repositoryConfiguration;
    RepositoryConfiguration stuff;

    @Before
    public void setUp() throws Exception {
        repositoryConfiguration = new RepositoryConfiguration();
        stuff = spy(repositoryConfiguration);
    }

    @Test
    public void ldapUserRepository_returnsUserDao() throws Exception {
        assertThat("user dao",repositoryConfiguration.ldapUserRepository(),instanceOf(UserDao.class));
    }

    @Test
    public void ldapClientRepository_returnsApplicationDao() throws Exception {
        assertThat("application dao",repositoryConfiguration.ldapClientRepository(),instanceOf(ApplicationDao.class));
    }

    @Test
    public void ldapCustomerRepository_returnsCustomerDao() throws Exception {
        assertThat("customer dao",repositoryConfiguration.ldapCustomerRepository(),instanceOf(CustomerDao.class));
    }

    @Test
    public void ldapGroupRepository_returnsGroupDao() throws Exception {
        assertThat("group dao",repositoryConfiguration.ldapGroupRepository(),instanceOf(GroupDao.class));
    }

    @Test
    public void authenticationRepository_returnsAuthDao() throws Exception {
        assertThat("auth dao",repositoryConfiguration.authenticationRepository(),instanceOf(AuthDao.class));
    }

    @Test
    public void endpointDao_returnsEndpointDao() throws Exception {
        assertThat("endpoint dao",repositoryConfiguration.endpointDao(),instanceOf(EndpointDao.class));
    }

    @Test
    public void apiDocDao_returnsApiDocDao() throws Exception {
        assertThat("api doc dao",repositoryConfiguration.apiDocDao(),instanceOf(ApiDocDao.class));
    }

    @Test
    public void scopeAccessObjectRepository_returnsScopeAccessDao() throws Exception {
        assertThat("scope access dao",repositoryConfiguration.scopeAccessObjectRepository(),instanceOf(ScopeAccessDao.class));
    }

    @Test
    public void ldapTenantRepository_returnsTenantDao() throws Exception {
        assertThat("tenant dao",repositoryConfiguration.ldapTenantRepository(),instanceOf(TenantDao.class));
    }
}
