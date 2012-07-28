package com.rackspace.idm.domain.config;

import com.unboundid.ldap.sdk.LDAPConnectionPool;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/24/12
 * Time: 8:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class AuthRepositoryLdapConfigurationTest {

    @Test
    public void constructor_notInTestMode_configNotSet() throws Exception {
        AuthRepositoryLdapConfiguration authRepositoryLdapConfiguration = new AuthRepositoryLdapConfiguration(false);
        assertThat("null config",authRepositoryLdapConfiguration.getConfig(),equalTo(null));
    }

    @Test
    public void constructor_inTestMode_configIsSet() throws Exception {
        AuthRepositoryLdapConfiguration authRepositoryLdapConfiguration = new AuthRepositoryLdapConfiguration(true);
        assertThat("config exists",authRepositoryLdapConfiguration.getConfig(),instanceOf(Configuration.class));

    }

    @Test
    public void defaultConstructor_configNotSet() throws Exception {
        AuthRepositoryLdapConfiguration authRepositoryLdapConfiguration = new AuthRepositoryLdapConfiguration();
        assertThat("null config",authRepositoryLdapConfiguration.getConfig(),equalTo(null));
    }

    @Test
    public void connection_trustedIsTrue_returnsConnection() throws Exception {
        AuthRepositoryLdapConfiguration authRepositoryLdapConfiguration = new AuthRepositoryLdapConfiguration(true);
        LDAPConnectionPool result = authRepositoryLdapConfiguration.connection();
        assertThat("connection pool exists", result, notNullValue());
    }

    @Test
    public void connection_trustedIsFalse_returnsNull() throws Exception {
        AuthRepositoryLdapConfiguration authRepositoryLdapConfiguration = new AuthRepositoryLdapConfiguration(true);
        Configuration config = mock(Configuration.class);
        authRepositoryLdapConfiguration.setConfig(config);
        when(config.getBoolean("ldap.server.trusted", false)).thenReturn(false);
        LDAPConnectionPool result = authRepositoryLdapConfiguration.connection();
        assertThat("connection pool exists", result, equalTo(null));
    }
}
