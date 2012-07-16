package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/12/12
 * Time: 10:56 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class InMemoryLdapIntegrationTest {
    private static InMemoryDirectoryServer server;

    @BeforeClass
    public static void setUpServer() throws Exception {
        Configuration config = new PropertiesConfiguration(System.getProperty("idm.properties.location") + "/idm.properties");
        InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig("dc=com");
        serverConfig.setSchema(null);

        serverConfig.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", config.getInt("ldap.testServer.port")));
        serverConfig.addAdditionalBindCredentials(config.getString("ldap.bind.dn"), config.getString("ldap.bind.password"));
        serverConfig.addAdditionalBindCredentials("rsId=10013387,ou=users,o=rackspace,dc=rackspace,dc=com", "Password1"); //hectorServiceAdmin
        serverConfig.addAdditionalBindCredentials("rsid=10022622,ou=users,o=rackspace,dc=rackspace,dc=com", "Password1"); //kurtUserAdmin
        serverConfig.addAdditionalBindCredentials("rsid=10022623,ou=users,o=rackspace,dc=rackspace,dc=com", "Password1"); //kurtDefaultUser

        server = new InMemoryDirectoryServer(serverConfig);
        server.startListening();

        Attribute attribute = new Attribute("");

        server.add("dc=com", attribute);
        server.add("dc=rackspace,dc=com", attribute);

        server.importFromLDIF(false, config.getString("ldap.testServer.ldif"));
    }

    @AfterClass
    public static void tearDownServer() throws Exception {
        server.shutDown(true);
        server = null;
    }
}
