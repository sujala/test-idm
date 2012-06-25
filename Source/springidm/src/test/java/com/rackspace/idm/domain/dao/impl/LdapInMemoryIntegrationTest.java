package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/3/12
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapInMemoryIntegrationTest {

    private static final int DEFAULT_SERVER_PORT = 636;
    private static InMemoryDirectoryServer server;

    private LdapAuthRepository repo;
    private LDAPConnectionPool connPool;

    @Before
    public void start() throws Exception {
        InMemoryDirectoryServerConfig ldapConfig = new InMemoryDirectoryServerConfig("dc=rackspace,dc=com");
        ldapConfig.addAdditionalBindCredentials("cn=admin,ou=internal,dc=rackspace,dc=com", "password");

        //InMemoryListenerConfig listenerConfig = new InMemoryListenerConfig("test", null, 389, null, null, null);
        //ldapConfig.setListenerConfigs(listenerConfig);

        //config.setSchema(null); // do not check (attribute) schema

        //InputStream schemaLdif = this.getClass().getClassLoader().getResourceAsStream("ldap/RackSchema.dxc");
        //Entry schemaEntry = new Entry(IOUtils.toString(schemaLdif, "UTF-8"));
        //Schema newSchema = new Schema(schemaEntry);

        String host = "10.127.39.220"; //config.getString("auth.ldap.server");
        int port = 636; //config.getInt("auth.ldap.server.port", DEFAULT_SERVER_PORT);
        String bindDN = "cn=admin,ou=internal,dc=rackspace,dc=com";
        String bindPassword = "qwerty";
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

        //LDAPConnection connection = new LDAPConnection(sslUtil.createSSLSocketFactory(), host, port);
        LDAPConnection connection = new LDAPConnection(sslUtil.createSSLSocketFactory(), host, port, bindDN, bindPassword);

        Schema newSchema = Schema.getSchema(connection);
        ldapConfig.setSchema(newSchema);
        

        server = new InMemoryDirectoryServer(ldapConfig);
        server.startListening();

        //server.add("dn: dc=org", "objectClass: top", "objectClass: domain", "dc: org");



        //LDAPConnection conn = server.getConnection();
        //SearchResultEntry entry = conn.getEntry("dc=rackspace,dc=com");
        //String xxx = entry.getDN();
        //addEntry("dn: ou=internal,dc=rackspace,dc=com","objectClass: top","objectClass: organizationalUnit","ou: internal");

        //InputStream baseLdif = this.getClass().getClassLoader().getResourceAsStream("ldap/base.ldif");
        //Entry baseEntry = new Entry(IOUtils.toString(baseLdif, "UTF-8"));
        //server.add(baseEntry);
        
        connPool = getConnPool();
        repo = getRepo(connPool);
    }

    @After
    public void stop() throws Exception {
        LDAPConnection conn = server.getConnection();
        conn.close();
        server.shutDown(true);
    }


    //@Test
    //@Ignore("Still waiting on correct credentials")
    public void shouldNotAuthBadCred() {
        //Assert.assertFalse(repo.authenticate("cmarin2", "Password1"));
    }

    private static LdapAuthRepository getRepo(LDAPConnectionPool connPool) {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration("auth.repository.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapAuthRepository(connPool, config);
    }

    private static LDAPConnectionPool getConnPool() throws Exception {
        LDAPConnectionPool pool = new LDAPConnectionPool(server.getConnection(), 10);
        return pool;
    }

    public void addEntry(String... args) throws LDIFException, LDAPException
    {
        LDAPResult result = server.add(args);
        assert (result.getResultCode().intValue() == 0);
        System.out.println("added entry:" + Arrays.asList(args));
    }
}
