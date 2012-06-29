package com.rackspace.idm.domain.config;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/28/12
 * Time: 1:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapConfigurationTest {
    Configuration configuration;
    LdapConfiguration ldapConfiguration;

    @Before
    public void setUp() throws Exception {
        configuration = mock(Configuration.class);
        ldapConfiguration = new LdapConfiguration();
    }

    @Test
    public void connect_() throws Exception {
        when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:123", "two:345"});
    }
}
