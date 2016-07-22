package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.api.common.fault.v1.BadRequestFault
import com.rackspace.idm.validation.entity.Constants
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

    def "auth with token validates token length"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def token = utils.getToken(userAdmin.username)

        when:
        def response = cloud20.authenticateTokenAndTenant(token, domainId)

        then:
        response.status == 200

        when:
        response = cloud20.authenticateTokenAndTenant('token' * 100, domainId)

        then:
        response.status == 400
        def fault = response.getEntity(BadRequestFault)
        fault.message == "token.id: size must be between 0 and " + Constants.MAX_TOKEN_LENGTH

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "auth with token returns 401 with invalid token"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def token = utils.getToken(userAdmin.username)

        when: "use valid token"
        def response = cloud20.authenticateTokenAndTenant(token, domainId)

        then: "get valid response"
        response.status == 200

        when: "revoke token and reauth"
        utils.revokeToken(token)
        response = cloud20.authenticateTokenAndTenant(token, domainId)

        then:
        response.status == 401

        when: "send never valid UUID formatted token"
        response = cloud20.authenticateTokenAndTenant(UUID.randomUUID().toString().replaceAll("-",""), domainId)

        then:
        response.status == 401

        when: "send never valid non-UUID (so processed as AE) formatted token"
        response = cloud20.authenticateTokenAndTenant("A_token_that_is_not_uuid_or_valid", domainId)

        then:
        response.status == 401

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

}
