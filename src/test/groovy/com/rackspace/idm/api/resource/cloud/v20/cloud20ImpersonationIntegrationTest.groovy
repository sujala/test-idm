package com.rackspace.idm.api.resource.cloud.v20

import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class cloud20ImpersonationIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
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
}
