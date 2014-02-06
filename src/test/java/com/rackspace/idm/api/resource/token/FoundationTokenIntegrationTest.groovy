package com.rackspace.idm.api.resource.token

import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.*


class FoundationTokenIntegrationTest extends RootIntegrationTest {

    @Shared def user

    def "Authenticate and Validate token" () {
        given:
        def domainId = utils.createDomain()
        user = utils.createUser(utils.getIdentityAdminToken(), getRandomUUID('foundationTestUser'), domainId)

        when:
        def token = utils.getToken(user.username)
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def validate = foundationUtils.validateToken(authData.accessToken.id, token)

        then:
        authData != null
        authData.accessToken != null
        validate != null
        validate.accessToken != null

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
    }
}
