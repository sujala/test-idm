package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*


class Cloud20ValidateTokenIntegrationTest extends RootIntegrationTest{

    @Shared def defaultUser, users

    def "AuthenticatedBy should be displayed for racker token" () {
        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        utils.revokeToken(response.token.id)
        response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def validateResponse = utils.validateToken(response.token.id)

        then:
        validateResponse != null
        validateResponse.token.authenticatedBy.credential.contains("PASSWORD")
    }

    def "Validate racker token" () {
        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def token = response.token.id
        utils.validateToken(token)

        then:
        token != null
    }

    def "Validate Impersonated user's token" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithToken(utils.getIdentityAdminToken(), defaultUser)
        def token = response.token.id
        utils.validateToken(token)

        then:
        token != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validate Impersonated user's token using a racker" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.impersonateWithRacker(defaultUser)
        def token = response.token.id
        utils.validateToken(token)

        then:
        token != null

        cleanup:
        utils.deleteUsers(users)
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
        def resp = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        token != null
        resp.status == SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Validating user's token within a disabled domain should return 404"() {
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
        def authResponse = utils.authenticate(defaultUser)
        utils.updateDomain(domainId, domain)
        def authResponse2 = cloud20.authenticate(defaultUser.username, DEFAULT_PASSWORD)
        def response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id,)

        then:
        authResponse2.status == SC_FORBIDDEN
        response.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
