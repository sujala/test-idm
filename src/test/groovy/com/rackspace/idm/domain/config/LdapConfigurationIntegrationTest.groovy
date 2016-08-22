package com.rackspace.idm.domain.config

import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.RoundRobinServerSet
import com.unboundid.ldap.sdk.SimpleBindRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.net.ssl.SSLSocketFactory

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapConfigurationIntegrationTest  extends Specification {

    @Autowired
    IdentityConfig autoWiredConfig

    @Shared LdapConfiguration ldapConfiguration;

    @Shared IdentityConfig identityConfig;
    @Shared IdentityConfig.StaticConfig staticConfig;
    @Shared IdentityConfig.ReloadableConfig reloadableConfig;


    def setup() {
        identityConfig = Mock()
        staticConfig = Mock()
        reloadableConfig = Mock()
        identityConfig.getStaticConfig() >> staticConfig
        identityConfig.getReloadableConfig() >> reloadableConfig
        ldapConfiguration = new LdapConfiguration(identityConfig)
    }

    def "init can create ldap configuration"() {
        given:
        String serverList = autoWiredConfig.getStaticConfig().getLDAPServerList()[0]
        def (server, port) = serverList.tokenize(":")
        port = Integer.valueOf(port)
        def bindDN = autoWiredConfig.getStaticConfig().getLDAPServerBindDN()
        def password = autoWiredConfig.getStaticConfig().getLDAPServerBindPassword()

        when:
        LDAPConnectionPool connection = ldapConfiguration.connection()

        then:
        1 * staticConfig.getLDAPServerList() >> [serverList]
        1 * staticConfig.getLDAPServerUseSSL() >> true
        1 * staticConfig.getLDAPServerPoolSizeInit() >> 1
        1 * staticConfig.getLDAPServerPoolSizeMax() >> 100
        1 * staticConfig.getLDAPServerBindDN() >> bindDN
        1 * staticConfig.getLDAPServerBindPassword() >> password
        1 * staticConfig.getLDAPServerPoolAgeMax() >> 0
        1 * staticConfig.getLDAPServerPoolCreateIfNecessary() >> true
        1 * staticConfig.getLDAPServerPoolMaxWaitTime() >> 0
        1 * staticConfig.getLDAPServerPoolHeathCheckInterval() >> 100
        1 * staticConfig.getLDAPServerPoolCheckConnectionAgeOnRelease() >> false
        1 * staticConfig.getLDAPServerPoolAllowConcurrentSocketFactoryUse() >> false

        then: "configured correctly"
        assert connection != null
        ((RoundRobinServerSet)connection.serverSet).addresses[0] == server
        ((RoundRobinServerSet)connection.serverSet).ports[0] == port
        ((RoundRobinServerSet)connection.serverSet).socketFactory instanceof SSLSocketFactory
        connection.numConnections == 100
        ((SimpleBindRequest)connection.bindRequest).getBindDN() == bindDN
        ((SimpleBindRequest)connection.bindRequest).password.stringValue() == password
        connection.getMaxConnectionAgeMillis() == 0
        connection.getCreateIfNecessary() == true
        connection.getMaxWaitTimeMillis() == 0
        connection.getHealthCheckIntervalMillis() == 100
        connection.checkConnectionAgeOnRelease == false
        ((RoundRobinServerSet)connection.serverSet).connectionOptions.allowConcurrentSocketFactoryUse == false
    }
}
