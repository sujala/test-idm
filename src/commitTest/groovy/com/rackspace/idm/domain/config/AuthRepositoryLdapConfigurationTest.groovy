package com.rackspace.idm.domain.config

import com.unboundid.ldap.sdk.LDAPConnection
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

    def "eDir config uses identity configuration for timeouts"() {
        given:
        staticConfig.getEDirServerTrusted() >> true
        edirConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _) >> GroovyMock(LDAPConnection)

        when: "create a new connection"
        edirConfiguration.connection()

        then: "identity configuration is used"
        1 * staticConfig.getEDirConnectionConnectTimeout()
        1 * staticConfig.getEDirConnectionBindTimeout()
        1 * staticConfig.getEDirConnectionSearchTimeout()
    }

    @Unroll
    def "eDir config returns a null connection pool for non-trusted servers - trusted == #trusted"() {
        given:
        edirConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _) >> GroovyMock(LDAPConnection)
        staticConfig.getEDirServerTrusted() >> trusted
        legacyConfig.getBoolean("auth.ldap.useSSL") >> false
        if (trusted) {
            edirConnectionFactory.createAuthenticatedEncryptedConnection(_, _) >> null
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

    def "eDir config creates authenticated connections"() {
        given:
        staticConfig.getEDirServerTrusted() >> true

        when: "try to create a connection WITH SSL/TLS and WITHOUT authentication"
        legacyConfig.getBoolean("auth.ldap.useSSL") >> true
        edirConfiguration.connection()

        then:
        1 * edirConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _) >> GroovyMock(LDAPConnection)
        1 * edirConnectionFactory.createConnectionPool(_, _, _)
    }
}
