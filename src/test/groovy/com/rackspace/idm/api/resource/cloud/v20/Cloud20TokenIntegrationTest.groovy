package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.*

class Cloud20TokenIntegrationTest extends RootIntegrationTest {

    @Shared def userAdmin, users

    def "Federated status should not appear in authenticate response" () {
        when:
        (userAdmin, users) = utils.createUserAdmin()
        def auth = utils.authenticate(userAdmin)

        then:
        auth != null
        auth.user.federated == null

        cleanup:
        utils.deleteUsers(users)
    }

    def "Authenticate racker with no groups"() {
        when:
        def auth = utils.authenticateRacker(RACKER_NOGROUP, RACKER_NOGROUP_PASSWORD)

        then:
        auth != null
    }
}
