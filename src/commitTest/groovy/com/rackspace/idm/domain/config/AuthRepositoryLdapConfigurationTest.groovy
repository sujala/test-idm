package com.rackspace.idm.domain.config

import com.unboundid.ldap.sdk.LDAPConnection
import org.apache.commons.configuration.Configuration
import spock.lang.Specification
import spock.lang.Unroll

class AuthRepositoryLdapConfigurationTest extends Specification {

    RackerAuthRepositoryLdapConfiguration rackerAuthRepositoryLdapConfiguration
    IdentityConfig.StaticConfig staticConfig = Mock(IdentityConfig.StaticConfig)
    Configuration legacyConfig = Mock(Configuration)
    RackerConnectionFactory rackerConnectionFactory = Mock(RackerConnectionFactory)

    def setup() {
        rackerAuthRepositoryLdapConfiguration = new RackerAuthRepositoryLdapConfiguration()
        rackerAuthRepositoryLdapConfiguration.identityConfig = Mock(IdentityConfig)
        rackerAuthRepositoryLdapConfiguration.identityConfig.getStaticConfig() >> staticConfig

        rackerAuthRepositoryLdapConfiguration.rackerConnectionFactory = rackerConnectionFactory

        // set the defaults for the configs
        staticConfig.getRackerAuthServer() >> "not.an.actual.edir.server"
        staticConfig.getRackerAuthServerPort() >> IdentityConfig.RACKER_AUTH_LDAP_SERVER_PORT_DEFAULT
    }

    @Unroll
    def "Racker config returns a null connection pool for non-trusted servers - trusted == #trusted"() {
        given:
        rackerConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _) >> GroovyMock(LDAPConnection)
        staticConfig.getEDirServerTrusted() >> trusted
        legacyConfig.getBoolean("auth.ldap.useSSL") >> false
        if (trusted) {
            rackerConnectionFactory.createAuthenticatedEncryptedConnection(_, _) >> null
        }

        when:
        rackerAuthRepositoryLdapConfiguration.connection()

        then:
        if (trusted) {
            1 * rackerConnectionFactory.createConnectionPool(_, _, _)
        } else {
            0 * rackerConnectionFactory.createConnectionPool(_, _, _)
        }

        where:
        trusted << [true, false]
    }

    def "Racker config creates authenticated connections"() {
        given:
        staticConfig.getEDirServerTrusted() >> true

        when: "try to create a connection WITH SSL/TLS and WITHOUT authentication"
        legacyConfig.getBoolean("auth.ldap.useSSL") >> true
        rackerAuthRepositoryLdapConfiguration.connection()

        then:
        1 * rackerConnectionFactory.createAuthenticatedEncryptedConnection(_, _, _, _, _) >> GroovyMock(LDAPConnection)
        1 * rackerConnectionFactory.createConnectionPool(_, _, _)
    }
}
