package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.unboundid.ldap.sdk.LDAPConnectionPool
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class RackerAuthRepositoryIntegrationTest extends RootIntegrationTest {

    @Autowired
    private RackerAuthRepository repo

    @Unroll
    def "Can auth with correct password. Optimized search: #optimizeSearch"() {
        reloadableConfiguration.setProperty(IdentityConfig.RACKER_AUTH_OPTIMIZE_SEARCH_PROP, optimizeSearch)

        expect:
        repo.authenticate("test.racker", "password")

        where:
        optimizeSearch << [true, false]
    }

    @Unroll
    def "A wrong password fails validation. Optimized search: #optimizeSearch"() {
        reloadableConfiguration.setProperty(IdentityConfig.RACKER_AUTH_OPTIMIZE_SEARCH_PROP, optimizeSearch)

        expect:
        !repo.authenticate("test.racker", "passwordWrong")

        where:
        optimizeSearch << [true, false]
    }

    @Unroll
    def "Can retrieve racker roles. Optimized search: #optimizeSearch"() {
        reloadableConfiguration.setProperty(IdentityConfig.RACKER_AUTH_OPTIMIZE_SEARCH_PROP, optimizeSearch)

        when:
        List<String> roles = repo.getRackerRoles("test.racker")

        then:
        roles.contains("team-cloud-identity")

        where:
        optimizeSearch << [true, false]
    }
}
