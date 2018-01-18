package com.rackspace.idm.domain.config

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class LdapConnectionPoolHealthCheckIntegrationTest extends RootIntegrationTest {

    @Autowired
    LdapConnectionPools connectionPools

    @Autowired
    LdapConnectionPoolHealthCheck healthCheck

    def cleanupSpec() {
        reloadableConfiguration.reset()
    }

    def "Ensure valid connections"() {
        given:

        LDAPConnection connection = connectionPools.getAppConnPool().getConnection()

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, false)
        healthCheck.ensureNewConnectionValid(connection)

        then:
        notThrown(LDAPException)

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, true)
        healthCheck.ensureConnectionValidForContinuedUse(connection)

        then:
        notThrown(LDAPException)

        cleanup:
        connection.close()
    }

    def "Invalid connection throws exception"() {
        given:
        LDAPConnection connection = connectionPools.getAppConnPool().getConnection()
        connection.close()

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, false)
        healthCheck.ensureNewConnectionValid(connection)

        then:
        thrown(LDAPException)

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, true)
        healthCheck.ensureConnectionValidForContinuedUse(connection)

        then:
        thrown(LDAPException)
    }

    def "Closed connections throw no exceptions when features are disabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_NEW_CONNECTION_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_HEALTH_CHECK_CONNECTION_FOR_CONTINUED_USE_PROP, false)
        LDAPConnection connection = connectionPools.getAppConnPool().getConnection()
        connection.close()

        when:
        healthCheck.ensureNewConnectionValid(connection)

        then:
        notThrown(LDAPException)

        when:
        healthCheck.ensureConnectionValidForContinuedUse(connection)

        then:
        notThrown(LDAPException)
    }
}
