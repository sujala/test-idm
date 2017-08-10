package com.rackspace.idm.domain.config

import org.apache.commons.configuration.Configuration
import spock.lang.Specification
import spock.lang.Unroll

class AuthRepositoryLdapConfigurationTest extends Specification {

    AuthRepositoryLdapConfiguration edirConfiguration;
    IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    Configuration legacyConfig = Mock(Configuration)
    EdirConnectionFactory edirConnectionFactory = Mock(EdirConnectionFactory)

    def setup() {
        edirConfiguration = new AuthRepositoryLdapConfiguration()
        edirConfiguration.config = legacyConfig
        edirConfiguration.identityConfig = Mock(IdentityConfig)
        edirConfiguration.identityConfig.getStaticConfig() >> staticConfig
        edirConfiguration.edirConnectionFactory = edirConnectionFactory

        // set the defaults for the configs
        legacyConfig.getString("auth.ldap.server") >> "not.an.actual.edir.server"
        legacyConfig.getInt("auth.ldap.server.port", AuthRepositoryLdapConfiguration.DEFAULT_SERVER_PORT) >> AuthRepositoryLdapConfiguration.DEFAULT_SERVER_PORT
    }

    @Unroll
    def "eDir config returns a null connection pool for non-trusted servers - trusted == #trusted"() {
        given:
        legacyConfig.getBoolean("ldap.server.trusted", false) >> trusted
        legacyConfig.getBoolean("auth.ldap.useSSL") >> false
        if (trusted) {
            edirConnectionFactory.createAnonymousUnenryptedConnection(_, _) >> null
        }

        when:
        edirConfiguration.connection()

        then:
        if (trusted) {
            1 * edirConnectionFactory.createConnectionPool(_, _, _)
        } else {
            0 * edirConnectionFactory.createConnectionPool(_, _, _)
        }

        where:
        trusted << [true, false]
    }

    def "eDir config only allows authenticated connections with encrypted connections"() {
        given:
        legacyConfig.getBoolean("ldap.server.trusted", false) >> true

        when: "try to create a connection WITHOUT SSL/TLS and WITH authentication"
        legacyConfig.getBoolean("auth.ldap.useSSL") >> false
        staticConfig.shouldEdirConnectionPoolUseAuthenticatedConnections() >> true
        edirConfiguration.connection()

        then:
        thrown IllegalStateException

        when: "try to create a connection WITH SSL/TLS and WITH authentication"
        legacyConfig.getBoolean("auth.ldap.useSSL") >> true
        staticConfig.shouldEdirConnectionPoolUseAuthenticatedConnections() >> true
        edirConfiguration.connection()

        then:
        1 * edirConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _)
    }

    def "eDir config creates authenticated connections only when feature is enabled"() {
        given:
        legacyConfig.getBoolean("ldap.server.trusted", false) >> true

        when: "try to create a connection WITH SSL/TLS and WITHOUT authentication"
        legacyConfig.getBoolean("auth.ldap.useSSL") >> true
        staticConfig.shouldEdirConnectionPoolUseAuthenticatedConnections() >> false
        edirConfiguration.connection()

        then:
        1 * edirConnectionFactory.createAnonymousEnryptedConnection(_, _, _)
        1 * edirConnectionFactory.createConnectionPool(_, _, _)
    }

}
