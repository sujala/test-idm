package com.rackspace.idm.api.resource.cloud.v20

import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.*

class Cloud20TokenIntegrationTest extends RootIntegrationTest {

    @Autowired
    Configuration configuration

    @Shared def userAdmin, users

    def "FederatedIdp should not appear in persistent user authentication response" () {
        when:
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        then:
        auth != null
        assert auth.user.federatedIdp == null

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
