package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

class Cloud20ImpersonationIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser, users
    @Shared def domainId

    def "impersonating a disabled user should be possible"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        utils.disableUser(defaultUser)
        def token = utils.getImpersonatedToken(identityAdmin, defaultUser)
        def response = utils.validateToken(token)

        then:
        response != null
        response.token.id != null

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "impersonating a disabled user - with racker" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        utils.disableUser(defaultUser)
        def token = utils.impersonateWithRacker(defaultUser)
        def response = utils.validateToken(token.token.id)

        then:
        response != null
        response.token.id != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "impersonating user - racker with impersonate role feature flag = true" () {
        given:
        setBooleanConfiguration("feature.restrict.impersonation.to.rackers.with.role.enabled", true)
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def rackerToken = response.token.id
        utils.impersonateWithToken(rackerToken, defaultUser)

        then:
        rackerToken != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "impersonating user - racker with no impersonate role - feature flag = true" () {
        given:
        setBooleanConfiguration("feature.restrict.impersonation.to.rackers.with.role.enabled", true)
        def domainId = utils.createDomain()
       (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def rackerToken = response.token.id
        def impersonateResponse = cloud20.impersonate(rackerToken, defaultUser)

        then:
        rackerToken != null
        impersonateResponse.status == 403

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "impersonating user - racker with no impersonate role - feature flag = false" () {
        given:
        def domainId = utils.createDomain()
        (defaultUser, users) = utils.createDefaultUser(domainId)
        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def rackerToken = response.token.id
        utils.impersonateWithToken(rackerToken, defaultUser)

        then:
        rackerToken != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
