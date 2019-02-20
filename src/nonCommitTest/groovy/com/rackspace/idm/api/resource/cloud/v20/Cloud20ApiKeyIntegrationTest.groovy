package com.rackspace.idm.api.resource.cloud.v20

import org.apache.http.HttpStatus
import testHelpers.RootIntegrationTest

class Cloud20ApiKeyIntegrationTest extends RootIntegrationTest {


    def "Users can reset own api key"() {
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUserToken = utils.getToken(defaultUser.username)
        def userManageToken = utils.getToken(userManage.username)

        when: "Identity admin resets own api key"
        def response = cloud20.resetUserApiKey(identityAdminToken, identityAdmin.id)

        then: "Allowed"
        response.status == HttpStatus.SC_OK

        when: "User admin resets own api key"
        response = cloud20.resetUserApiKey(userAdminToken, userAdmin.id)

        then: "Allowed"
        response.status == HttpStatus.SC_OK

        when: "Default user resets own api key"
        response = cloud20.resetUserApiKey(defaultUserToken, defaultUser.id)

        then: "Allowed"
        response.status == HttpStatus.SC_OK

        when: "User manager resets own api key"
        response = cloud20.resetUserApiKey(userManageToken, userManage.id)

        then: "Allowed"
        response.status == HttpStatus.SC_OK

        cleanup:
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        utils.deleteUsersQuietly(users)
    }
}
