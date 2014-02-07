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

    def "Authenticate invalid client" () {
        when:
        def response = foundation.authenticate("badClientId", "secret")

        then:
        response.status == 404
    }

    def "authenticate user" () {
        when:
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def user = foundationUtils.createUser(authData.accessToken.id)
        def authUser = foundationUtils.authenticateUser(CLIENT_ID, CLIENT_SECRET, user.username, user.passwordCredentials.currentPassword.password)

        then:
        authData !=null
        user != null
        authUser != null
        authUser.accessToken.id != null

        cleanup:
        utils.deleteUser(user)
    }

    def "authenticate racker" () {
        when:
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def authRacker = foundationUtils.authenticateRacker(CLIENT_ID, CLIENT_SECRET, "test.racker", "password")

        then:
        authData !=null
        authRacker != null
        authRacker.accessToken.id != null
    }
}
