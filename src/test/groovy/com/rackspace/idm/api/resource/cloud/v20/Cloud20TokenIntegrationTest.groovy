package com.rackspace.idm.api.resource.cloud.v20

import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.*

class Cloud20TokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    Configuration configuration

    @Shared def userAdmin, users

    def cleanup() {
        staticIdmConfiguration.reset()
    }

    def "Federated status should only appear in authenticate response when saml feature flag is true" () {
        when:
        staticIdmConfiguration.setProperty("saml.enabled", samlEnabled)
        (userAdmin, users) = utils.createUserAdmin()
        def auth = utils.authenticate(userAdmin)

        then:
        auth != null
        auth.user.federated == expectedFederatedStatus

        cleanup:
        utils.deleteUsers(users)

        where:
        samlEnabled | expectedFederatedStatus
        true        | false
        false       | null
    }

    def "Authenticate racker with no groups"() {
        when:
        def auth = utils.authenticateRacker(RACKER_NOGROUP, RACKER_NOGROUP_PASSWORD)

        then:
        auth != null
    }
}
