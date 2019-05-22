package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserService
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static javax.servlet.http.HttpServletResponse.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class ResetPhonePinForUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    def "reset phone pin"() {
        given:
        def user = utils.createCloudAccount()

        when: "reset pin"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), user.id, contentType)

        then: "returns no content"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "This service is only available to specific roles"() {
        given:
        def domainId = utils.createDomain()
        def users, identityAdmin, userAdmin, userManager, defaultUser
        (identityAdmin, userAdmin, userManager, defaultUser) = utils.createUsers(domainId)

        def identityAdminToken = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        def userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def userMangeToken = utils.getToken(userManager.username, Constants.DEFAULT_PASSWORD)

        def userManager2 = utils.createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        utils.addRoleToUser(userManager2, Constants.USER_MANAGE_ROLE_ID)
        users = [defaultUser, userManager, userManager2, userAdmin, identityAdmin]

        def domainId2 = utils.createDomain()
        def usersSecondDomain, identityAdminSecondDomain, userAdminSecondDomain, userManagerSecondDomain, defaultUserSecondDomain
        (identityAdminSecondDomain, userAdminSecondDomain, userManagerSecondDomain, defaultUserSecondDomain) = utils.createUsers(domainId2)
        usersSecondDomain = [defaultUserSecondDomain, userManagerSecondDomain, userAdminSecondDomain, identityAdminSecondDomain]

        def fedAuthResponse = utils.createFederatedUserForAuthResponse(domainId)
        def fedUser = fedAuthResponse.user
        def fedUserToken = fedAuthResponse.token.id

        when: "identity:user-admin: Can reset the Phone PIN for user-manage"
        def previousPhonePin = utils.getUserById(userManager.id, userMangeToken, contentType).phonePin
        def response = cloud20.resetPhonePin(userAdminToken, userManager.id, contentType)
        def updatedPhonePin = utils.getUserById(userManager.id, userMangeToken, contentType).phonePin

        then: "returns no content"
        response.status == SC_NO_CONTENT

        and: "the phone pin was updated"
        previousPhonePin != updatedPhonePin

        when: "identity:user-admin: Can reset the Phone PIN for federated user"
        previousPhonePin = utils.getUserById(fedUser.id, fedUserToken, contentType).phonePin
        response = cloud20.resetPhonePin(userAdminToken, fedUser.id, contentType)
        updatedPhonePin = utils.getUserById(fedUser.id, fedUserToken, contentType).phonePin

        then: "returns no content"
        response.status == SC_NO_CONTENT

        and: "the phone pin was updated"
        previousPhonePin != updatedPhonePin

        when: "identity:user-admin: Can reset the Phone PIN for default-user"
        response = cloud20.resetPhonePin(userAdminToken, defaultUser.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "identity:user-admin: Can not reset the Phone PIN for self"
        response = cloud20.resetPhonePin(userAdminToken, userAdmin.id, contentType)

        then: "returns no content"
        response.status == SC_FORBIDDEN

        when: "identity:user-admin: Can not reset the Phone PIN for user-manage in different domain"
        response = cloud20.resetPhonePin(userAdminToken, userManagerSecondDomain.id, contentType)

        then: "returns not found"
        response.status == SC_NOT_FOUND

        when: "identity:user-admin: Can not reset the Phone PIN for default-user in different domain"
        response = cloud20.resetPhonePin(userAdminToken, defaultUserSecondDomain.id, contentType)

        then: "returns not found"
        response.status == SC_NOT_FOUND

        when: "identity:user-manage: Can reset the Phone PIN of user-manage"
        response = cloud20.resetPhonePin(userMangeToken, userManager2.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "identity:user-manage: Can reset the Phone PIN of default-user"
        response = cloud20.resetPhonePin(userMangeToken, defaultUser.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "identity:user-manage: Can not reset the Phone PIN of user-admin"
        response = cloud20.resetPhonePin(userMangeToken, userAdmin.id, contentType)

        then: "returns forbidden"
        response.status == SC_FORBIDDEN

        when: "identity:user-manage: Can not reset the Phone PIN for user in a different domain"
        response = cloud20.resetPhonePin(userMangeToken, defaultUserSecondDomain.id, contentType)

        then: "returns not found"
        response.status == SC_NOT_FOUND

        when: "identity:phone-pin-admin: Can reset the Phone PIN for user-admin"
        response = cloud20.resetPhonePin(utils.getServiceAdminToken(), userAdmin.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "identity:phone-pin-admin: Can reset the Phone PIN for user-manage"
        response = cloud20.resetPhonePin(utils.getServiceAdminToken(), userManager.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "identity:phone-pin-admin: Can reset the Phone PIN for default-user"
        response = cloud20.resetPhonePin(utils.getServiceAdminToken(), defaultUser.id, contentType)

        then: "returns no content"
        response.status == SC_NO_CONTENT

        when: "user does not exist returns"
        response = cloud20.resetPhonePin(utils.getServiceAdminToken(), "missing", contentType)

        then: "returns not found"
        response.status == SC_NOT_FOUND

        when: "caller is an impersonated request"
        def impersonatedToken = utils.impersonateWithRacker(userAdmin).token.id
        response = cloud20.resetPhonePin(impersonatedToken, userManager.id, contentType)

        then: "returns forbidden"
        response.status == SC_FORBIDDEN

        when: "identity:admin: Can not reset the Phone PIN for default-user"
        response = cloud20.resetPhonePin(identityAdminToken, defaultUser.id, contentType)

        then: "returns forbidden"
        response.status == SC_FORBIDDEN
        cleanup:
        utils.deleteUsers(users)
        utils.deleteUsers(usersSecondDomain)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "reset phone pin - only_if_missing" () {
        given:
        def user = utils.createCloudAccount()
        def token = utils.getToken(user.username, Constants.DEFAULT_PASSWORD)
        def phonePin = utils.getUserById(user.id, token).phonePin

        when: "reset phone pin with a phone pin"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), user.id, true, contentType)

        then: "returns conflict"
        assert response.status == SC_CONFLICT
        def updatedPhonePin = utils.getUserById(user.id, token).phonePin

        and: "phone pin is not changed"
        phonePin == updatedPhonePin

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "resetPhonePin: reset phone pin updates invalid count" () {
        given:
        def user = utils.createGenericUserAdmin()

        // Update user to have failed pin attempts
        User userEntity = identityUserService.getEndUserById(user.id)
        userEntity.recordFailedPinAuthentication()
        identityUserService.updateEndUser(userEntity)
        assert userEntity.phonePinAuthenticationFailureCount > 0

        when: "reset phone pin"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), user.id, false, contentType)

        then: "failure count is reset"
        assert response.status == SC_NO_CONTENT

        and:
        User userEntityAfter = identityUserService.getEndUserById(user.id)
        userEntityAfter.phonePinAuthenticationFailureCount == 0

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "resetPhonePin: can not reset phone pin on locked account" () {
        given:
        def user = utils.createGenericUserAdmin()
        def token = utils.getToken(user.username, Constants.DEFAULT_PASSWORD)

        // Update user to have failed pin attempts
        User userEntity = identityUserService.getEndUserById(user.id)

        GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD.times {
            userEntity.recordFailedPinAuthentication()
        }
        identityUserService.updateEndUser(userEntity)
        assert userEntity.getPhonePinState() == PhonePinStateEnum.LOCKED

        when: "reset phone pin"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), user.id, false, contentType)

        then: "Not allowed"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_PHONE_PIN_LOCKED, ErrorCodes.ERROR_MESSAGE_PHONE_PIN_LOCKED)

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

}
