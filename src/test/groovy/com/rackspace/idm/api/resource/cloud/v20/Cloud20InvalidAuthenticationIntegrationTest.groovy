package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 11/13/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20InvalidAuthenticationIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    def "Authentication with credentials containing tenantId and/or tenantName" () {
        given:
        def key = "key"
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def response = cloud20.invalidAuthenticatePassword(userAdmin.username, DEFAULT_PASSWORD)
        def setKeyResponse = cloud11.setUserKey(userAdmin.username, v1Factory.createUserWithOnlyKey(key))
        def responseApiKey = cloud20.invalidAuthenticateApiKey(userAdmin.username, key)

        then:
        response.status == 200
        setKeyResponse.status == 200
        responseApiKey.status == 200

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }
}
