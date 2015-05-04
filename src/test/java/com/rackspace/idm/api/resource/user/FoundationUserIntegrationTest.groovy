package com.rackspace.idm.api.resource.user

import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*


class FoundationUserIntegrationTest extends RootIntegrationTest {

    def "Create/Get user" () {
        when:
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def user = foundationUtils.createUser(authData.accessToken.id)
        def getUser = foundationUtils.getUser(authData.accessToken.id, user.id)

        then:
        authData != null
        user != null
        user.firstName == "test"
        getUser != null
        getUser.firstName == "test"

        cleanup:
        utils.deleteUser(getUser)
    }

    def "Get user's password - returns null" () {
        when:
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def user = foundationUtils.createUser(authData.accessToken.id)
        def getUserPassword = foundationUtils.getUserPasswordCredentials(authData.accessToken.id, user.id)

        then:
        authData != null
        user != null
        user.firstName == "test"
        //Returns null since we can no longer retrieve passwords
        getUserPassword.currentPassword.password == null

        cleanup:
        utils.deleteUser(user)
    }
}
