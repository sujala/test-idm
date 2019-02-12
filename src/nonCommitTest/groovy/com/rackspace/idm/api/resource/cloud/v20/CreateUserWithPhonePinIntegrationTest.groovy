import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.UserService
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class CreateUserWithPhonePinIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser
    @Shared
    def domainId

    @Autowired
    UserService userService

    @Unroll
    def "Create identityAdmin, userAdmin, userManage, defaultUser with phone PIN - featureEnabled == #featureEnabled" () {
        given:
        def domainId = utils.createDomain()
        def pinLength = 6

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, featureEnabled)
        reloadableConfiguration.setProperty(IdentityConfig.USER_PHONE_PIN_SIZE_PROP, pinLength)

        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def identityAdminUserEntity = userService.getUserById(identityAdmin.id)
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def userManageUserEntity = userService.getUserById(userManage.id)
        def defaultUserEntity = userService.getUserById(defaultUser.id)

        then:

        if (featureEnabled) {
            assert identityAdminUserEntity.phonePin != null
            assert identityAdminUserEntity.encryptedPhonePin != null
            assert identityAdminUserEntity.phonePin.size() == pinLength
            assert identityAdminUserEntity.phonePin.isNumber()

            assert userAdminUserEntity.phonePin != null
            assert userAdminUserEntity.encryptedPhonePin != null
            assert userAdminUserEntity.phonePin.size() == pinLength
            assert userAdminUserEntity.phonePin.isNumber()

            assert userManageUserEntity.phonePin != null
            assert userManageUserEntity.encryptedPhonePin != null
            assert userManageUserEntity.phonePin.size() == pinLength
            assert userManageUserEntity.phonePin.isNumber()

            assert defaultUserEntity.phonePin != null
            assert defaultUserEntity.encryptedPhonePin != null
            assert defaultUserEntity.phonePin.size() == pinLength
            assert defaultUserEntity.phonePin.isNumber()
        } else {
            assert identityAdminUserEntity.phonePin == null
            assert userAdminUserEntity.phonePin == null
            assert userManageUserEntity.phonePin == null
            assert defaultUserEntity.phonePin == null
        }

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "Create userAdmin with phone PIN of different length - pinLength == #pinLength" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.USER_PHONE_PIN_SIZE_PROP, pinLength)

        def user = utils.createCloudAccount()

        when:
        def userEntity = userService.getUserById(user.id)

        then:
        if (pinLength > 0) {
            assert userEntity.phonePin != null
            assert userEntity.encryptedPhonePin != null
            assert userEntity.phonePin.size() == pinLength
            assert userEntity.phonePin.isNumber()
        }  else if (pinLength == 0) {
            assert userEntity.phonePin != null
            assert userEntity.encryptedPhonePin != null
            assert userEntity.phonePin.size() == pinLength
            assert userEntity.phonePin.isEmpty()
        }

        cleanup:
        utils.deleteUser(user)

        where:
        pinLength << [10,4,0]
    }

    def "When a user's token is validated - return Phone PIN" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)
        def userWithoutPhonePin = utils.createCloudAccount()
        AuthenticateResponse federatedAuthResponseWithoutPhonePin = utils.createFederatedUserForAuthResponse(userWithoutPhonePin.domainId)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        def user = utils.createCloudAccount()
        AuthenticateResponse federatedAuthResponse = utils.createFederatedUserForAuthResponse(user.domainId)

        when: "authenticate"
        def response = cloud20.authenticate(user.username, Constants.DEFAULT_PASSWORD, contentType)
        AuthenticateResponse authResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "returns phone pin"
        authResponse.user.phonePin != null

        when: "validate"
        response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id, contentType)
        AuthenticateResponse validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "returns phone pin"
        validateResponse.user.phonePin != null

        when: "authenticate"
        response = cloud20.authenticate(userWithoutPhonePin.username, Constants.DEFAULT_PASSWORD, contentType)
        authResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not returns phone pin"
        authResponse.user.phonePin == null

        when: "validate"
        response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not returns phone pin"
        validateResponse.user.phonePin == null

        when: "validate impersonated token"
        ImpersonationResponse impersonationResponse = utils.impersonateWithRacker(user)
        response = cloud20.validateToken(utils.getServiceAdminToken(), impersonationResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not return phone pin"
        validateResponse.user.phonePin == null

        when: "validate federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedAuthResponseWithoutPhonePin.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not return phone pin"
        validateResponse.user.phonePin == null

        when: "validate federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedAuthResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "return phone pin"
        validateResponse.user.phonePin != null

        cleanup:
        utils.deleteUser(user)
        utils.deleteUser(userWithoutPhonePin)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}

