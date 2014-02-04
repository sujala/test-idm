package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

class cloud20ImpersonationIntegrationTest extends RootIntegrationTest {

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
}
