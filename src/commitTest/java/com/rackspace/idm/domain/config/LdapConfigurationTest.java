package com.rackspace.idm.domain.config;

import com.unboundid.util.LDAPSDKUsageException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LdapConfigurationTest {
    Configuration configuration;
    LdapConfiguration ldapConfiguration;
    IdentityConfig identityConfig;
    LdapConnectionPoolHealthCheck ldapConnectionPoolHealthCheck;

    @Before
    public void setUp() throws Exception {
        configuration = mock(Configuration.class);
        identityConfig = new IdentityConfig(configuration, configuration);
        ldapConnectionPoolHealthCheck = mock(LdapConnectionPoolHealthCheck.class);
        ldapConfiguration = new LdapConfiguration(identityConfig, ldapConnectionPoolHealthCheck);
    }

    @Test
    public void constructor_succeeds() throws Exception {
        new LdapConfiguration();
    }

    @Test
    public void connection_callsConfigMethodGetStringArray() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:123", "two:345"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            verify(configuration).getStringArray("ldap.serverList");
        }
    }

    @Test
    public void connection_portIsNot389AndUsingSSLAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:123", "two:345"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            when(configuration.getBoolean("ldap.server.useSSL")).thenReturn(true);
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test
    public void connection_portIsNot389AndNotUsingSSLAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:123", "two:345"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            when(configuration.getBoolean("ldap.server.useSSL")).thenReturn(false);
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test
    public void connection_portIs389AndUsingSSLAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:389", "two:389"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            when(configuration.getBoolean("ldap.server.useSSL")).thenReturn(true);
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test
    public void connection_portIs389AndNotUsingSSLAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one:389", "two:389"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            when(configuration.getBoolean("ldap.server.useSSL")).thenReturn(false);
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test
    public void connection_noPortNumberAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{"one: ", "two: "});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max",100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test
    public void connection_noHostsAndThrowsLdapException_throwsIllegalStateException() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{":123", ":456"});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            assertTrue("should throw exception",false);
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct."));
        }
    }

    @Test (expected = LDAPSDKUsageException.class)
    public void connection_emptyServerList_throwsLdapUsageException() throws Exception {
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
    }

    @Test
    public void connection_getsInitPoolSizeFromConfig() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (LDAPSDKUsageException ex){
            verify(configuration).getInt("ldap.server.pool.size.init",1);
        }
    }

    @Test
    public void connection_getsMaxPoolSizeFromConfig() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (LDAPSDKUsageException ex){
            verify(configuration).getInt("ldap.server.pool.size.max",100);
        }
    }

    @Test
    public void connection_getsBindDnFromConfig() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (LDAPSDKUsageException ex){
            verify(configuration).getString("ldap.bind.dn");
        }
    }

    @Test
    public void connection_getsPasswordFromConfig() throws Exception {
        try{
            when(configuration.getStringArray("ldap.serverList")).thenReturn(new String[]{});
            when(configuration.getInt("ldap.server.pool.size.init",1)).thenReturn(1);
            when(configuration.getInt("ldap.server.pool.size.max", 100)).thenReturn(100);
            when(configuration.getString("ldap.bind.dn")).thenReturn("bindDn");
            when(configuration.getString("ldap.bind.password")).thenReturn("bindPassword");
            ldapConfiguration.connection();
            fail("should throw exception");
        }catch (LDAPSDKUsageException ex){
            verify(configuration).getString("ldap.bind.password");
        }
    }

}
