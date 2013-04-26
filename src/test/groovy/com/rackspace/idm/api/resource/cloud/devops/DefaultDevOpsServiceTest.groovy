package com.rackspace.idm.api.resource.cloud.devops

import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/26/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultDevOpsServiceTest extends RootServiceTest{
    @Shared DevOpsService service

    def setupSpec(){
        service = new DefaultDevOpsService()
    }

    def "Verify that encrypt users is only allowed for a service admin" () {
        given:
        setupMocks()
        allowUserAccess()

        when:
        service.encryptUsers("token")

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_)
        1 * userService.reEncryptUsers()
    }

    def setupMocks() {
        mockAuthorizationService(service)
        mockUserService(service)
        mockScopeAccessService(service)
    }
}
