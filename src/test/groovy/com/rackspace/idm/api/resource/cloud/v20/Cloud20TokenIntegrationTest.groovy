package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Created by jorge on 3/27/14.
 */
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
}
