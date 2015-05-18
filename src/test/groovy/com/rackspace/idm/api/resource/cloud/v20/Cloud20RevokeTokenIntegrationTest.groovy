package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TokenRevocationService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED


class Cloud20RevokeTokenIntegrationTest extends RootIntegrationTest {

    @Shared def users, defaultUser

    @Autowired
    TokenRevocationService tokenRevocationService;

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    TokenFormatSelector tokenFormatSelector

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

    def "Revoke user's tokens by id when set to AE will also revoke all UUID" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, TokenFormat.UUID.name())
        def identityAdmin = utils.createIdentityAdmin()
        def uuidToken = utils.getToken(identityAdmin.username)

        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, TokenFormat.AE.name())
        def aeToken = utils.getToken(identityAdmin.username)
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "revoke all tokens by userid"
        tokenRevocationService.revokeAllTokensForBaseUser(identityAdmin.id)

        then:
        cloud20.validateToken(serviceAdminToken, aeToken).status == SC_NOT_FOUND
        cloud20.validateToken(serviceAdminToken, uuidToken).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)
    }

    def "Revoke user's tokens by user when set to AE will also revoke all UUID" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, TokenFormat.UUID.name())
        def identityAdmin = utils.createIdentityAdmin()
        def uuidToken = utils.getToken(identityAdmin.username)

        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, TokenFormat.AE.name())
        def aeToken = utils.getToken(identityAdmin.username)
        def serviceAdminToken = utils.getServiceAdminToken()

        def user = identityUserService.getProvisionedUserById(identityAdmin.id)

        when: "revoke all tokens by user"
        tokenRevocationService.revokeAllTokensForBaseUser(user)

        then:
        cloud20.validateToken(serviceAdminToken, aeToken).status == SC_NOT_FOUND
        cloud20.validateToken(serviceAdminToken, uuidToken).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)
    }

    def "Revoke AE token using v1.1" () {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, TokenFormat.AE.name())
        def identityAdmin = utils.createIdentityAdmin()
        def aeToken = utils.getToken(identityAdmin.username)
        def serviceAdminToken = utils.getServiceAdminToken()

        assert tokenFormatSelector.formatForExistingToken(aeToken) == TokenFormat.AE

        when: "revoke all tokens by userid"
        cloud11.revokeToken(aeToken)
        tokenRevocationService.revokeAllTokensForBaseUser(identityAdmin.id)

        then:
        cloud20.validateToken(serviceAdminToken, aeToken).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)
    }
}
