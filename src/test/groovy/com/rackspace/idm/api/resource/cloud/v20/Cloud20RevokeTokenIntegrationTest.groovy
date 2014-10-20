package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.security.TokenFormat
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED


class Cloud20RevokeTokenIntegrationTest extends RootIntegrationTest {

    @Shared def users, defaultUser

    @Unroll
    def "Revoke other user's token; tokenFormat: #tokenFormat" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, tokenFormat.name())
        def identityAdmin = utils.createIdentityAdmin()
        def token = utils.getToken(identityAdmin.username)
        def serviceAdminToken = utils.getServiceAdminToken()
        when:
        def response = cloud20.revokeUserToken(serviceAdminToken, token)

        then:
        response.status == SC_NO_CONTENT

        when:
        def validateResponse = cloud20.validateToken(serviceAdminToken, token)

        then:
        validateResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)

        where:
        tokenFormat << [TokenFormat.UUID, TokenFormat.AE]
    }

    @Unroll
    def "Revoke my token; tokenFormat: #tokenFormat" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, tokenFormat.name())
        def identityAdmin = utils.createIdentityAdmin()
        def token = utils.getToken(identityAdmin.username)

        when:
        def response = cloud20.revokeToken(token)

        then:
        response.status == SC_NO_CONTENT

        when:
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        validateResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)

        where:
        tokenFormat << [TokenFormat.UUID, TokenFormat.AE]
    }

    @Unroll
    def "Revoke token of a disabled user; tokenFormat: #tokenFormat" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, tokenFormat.name())

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

        where:
        tokenFormat << [TokenFormat.UUID, TokenFormat.AE]
    }
}
