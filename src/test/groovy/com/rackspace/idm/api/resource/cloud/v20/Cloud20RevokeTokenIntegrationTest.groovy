package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.*


class Cloud20RevokeTokenIntegrationTest extends RootIntegrationTest {

    @Shared def users, defaultUser

    def "Revoke token of a disabled user" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def token = utils.getToken(defaultUser.username)
        utils.disableUser(defaultUser)
        def response = cloud20.revokeUserToken(utils.getServiceAdminToken(), token)
        def revokeTokenResponse = cloud20.revokeToken(token)

        then:
        response.status == SC_NOT_FOUND
        revokeTokenResponse.status == SC_UNAUTHORIZED

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
