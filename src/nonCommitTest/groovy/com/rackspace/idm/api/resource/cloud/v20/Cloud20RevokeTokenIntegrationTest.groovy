package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.TokenRevocationService
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static org.apache.http.HttpStatus.SC_CREATED
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
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)
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
        utils.deleteUsers(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }

    @Unroll
    def "Revoke my token; tokenFormat: #tokenFormat" () {
        given:
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)

        when:
        def response = cloud20.revokeToken(token)

        then:
        response.status == SC_NO_CONTENT

        when:
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        validateResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }

    @Unroll
    def "Revoke token of a disabled user; tokenFormat: #tokenFormat" () {
        given:
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)

        when:
        utils.disableUser(user)
        def response = cloud20.revokeUserToken(utils.getServiceAdminToken(), token)
        def revokeTokenResponse = cloud20.revokeToken(token)

        then:
        response.status == SC_NOT_FOUND
        revokeTokenResponse.status == SC_UNAUTHORIZED

        cleanup:
        utils.deleteUsers(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }

    @Unroll
    def "Revoke user's tokens by id when set to #tokenFormat" () {
        given:
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)

        def serviceAdminToken = utils.getServiceAdminToken()

        when: "revoke token by userid"
        tokenRevocationService.revokeAllTokensForEndUser(user.id)

        then:
        cloud20.validateToken(serviceAdminToken, token).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }

    @Unroll
    def "Revoke provisioned user's tokens by user when set to #tokenFormat" () {
        given:
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)
        def userEntity = identityUserService.getProvisionedUserById(user.id)

        when: "revoke tokens by userid"
        tokenRevocationService.revokeAllTokensForEndUser(userEntity)

        then:
        cloud20.validateToken(utils.getIdentityAdminToken(), token).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }

    @Unroll
    def "Revoke user token with authenticatedByMethodGroups when set to #tokenFormat" () {
        given:
        def user = createUserWithFormat(tokenFormat)
        def token = utils.getToken(user.username)
        List<AuthenticatedByMethodGroup> authenticatedByMethodGroups =  Arrays.asList(AuthenticatedByMethodGroup.ALL)

        when:
        tokenRevocationService.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        cloud20.validateToken(utils.getIdentityAdminToken(), token).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUser(user)

        where:
        tokenFormat << [TokenFormatEnum.AE]
    }


    def "Revoke AE token using v1.1" () {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def aeToken = utils.getToken(identityAdmin.username)
        def serviceAdminToken = utils.getServiceAdminToken()

        assert tokenFormatSelector.formatForExistingToken(aeToken) == TokenFormat.AE

        when: "revoke all tokens by userid"
        cloud11.revokeToken(aeToken)
        tokenRevocationService.revokeAllTokensForEndUser(identityAdmin.id)

        then:
        cloud20.validateToken(serviceAdminToken, aeToken).status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(identityAdmin)
    }

    User createUserWithFormat(TokenFormatEnum tokenFormat) {
        def user = v2Factory.createUserForCreate(UUID.randomUUID().toString(), "display", "email@email.com", true, null, UUID.randomUUID().toString(), DEFAULT_PASSWORD).with {
            it.tokenFormat = tokenFormat
            it
        }
        def response = cloud20.createUser(utils.getIdentityAdminToken(), user)
        assert (response.status == SC_CREATED)
        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }
}
