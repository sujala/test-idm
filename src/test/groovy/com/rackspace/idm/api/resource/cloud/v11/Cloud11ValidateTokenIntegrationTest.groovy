package com.rackspace.idm.api.resource.cloud.v11

import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static org.apache.http.HttpStatus.*

class Cloud11ValidateTokenIntegrationTest extends RootIntegrationTest {

    @Shared def defaultUser, users


    def "Validate user's token within a disabled domain should return 404" () {
        given:
        def user = utils11.createUser()
        def domainId = Integer.toString(user.mossoId)
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }

        when:
        String key = utils11.getUserKey(user).key
        def user20 = utils.getUserByName(user.id)
        def authResponse = utils.authenticateApiKey(user20, key)
        utils.updateDomain(domainId, domain)
        def response = cloud11.validateToken(authResponse.token.id)

        then:
        response.status == SC_NOT_FOUND

        cleanup:
        utils11.deleteUser(user)
        utils.deleteTenantById(String.valueOf(user.mossoId))
        utils.deleteTenantById(user.nastId)
        utils.deleteDomain(domainId)
    }

    def "Validate Impersonated token should return 200 even if user is in a disabled domain" () {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.updateDomain(domainId, domain)
        def resp = cloud11.validateToken(token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
