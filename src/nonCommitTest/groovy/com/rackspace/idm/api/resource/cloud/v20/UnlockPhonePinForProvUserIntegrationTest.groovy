package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ForbiddenFault
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT

class UnlockPhonePinForProvUserIntegrationTest extends RootIntegrationTest {


    def "Verify that user can unlock his own phone pin but not for other users"() {
        given:
        def user = utils.createGenericUserAdmin()
        def userToken = utils.getToken(user.username)
        def userWithLockedPhonePin1 = utils.createUser(userToken)
        def userWithLockedPhonePin2 = utils.createUser(userToken)

        utils.lockPhonepin(userWithLockedPhonePin1.id)
        utils.lockPhonepin(userWithLockedPhonePin2.id)
        def User1Token = utils.getToken(userWithLockedPhonePin1.username)

        when: "user attempts to unlock phone pin for other user"
        def response = cloud20.unlockPhonePin(User1Token, userWithLockedPhonePin2.id)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "user attempts to unlock phone pin for him self"
        response = cloud20.unlockPhonePin(User1Token, userWithLockedPhonePin1.id)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(user)
        utils.deleteUserQuietly(userWithLockedPhonePin1)
        utils.deleteUserQuietly(userWithLockedPhonePin2)
    }

    def "Verify that user with role identity:phone-pin-admin can unlock phone pin for any user whose phone pin is locked"() {
        given:
        def user = utils.createGenericUserAdmin()
        def userToken = utils.getToken(user.username)
        def userWithLockedPhonePin = utils.createUser(userToken)
        utils.lockPhonepin(userWithLockedPhonePin.id)

        when: "unlock with user without the identity:phone-pin-admin role"
        def response = cloud20.unlockPhonePin(userToken, userWithLockedPhonePin.id)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "Add role to the user"
        utils.addRoleToUser(user, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        response = cloud20.unlockPhonePin(userToken, userWithLockedPhonePin.id)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(user)
        utils.deleteUserQuietly(userWithLockedPhonePin)
    }

    def "Verify that phone pin can be unlocked only for users whose phone pin is in locked state"() {
        given:
        def user = utils.createGenericUserAdmin()
        def token = utils.getToken(user.username)

        // ensure user's current phone pin is not in locked state
        assert user.phonePinState != PhonePinStateEnum.LOCKED

        when: "user attempts to unlock phone pin which is not in locked state"
        def response = cloud20.unlockPhonePin(token, user.id)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_LOCKED, ErrorCodes.ERROR_MESSAGE_PHONE_PIN_NOT_LOCKED))

        when: "user attempts to unlock phone pin for him self when the phone pin is in locked state"
        utils.lockPhonepin(user.id)
        response = cloud20.unlockPhonePin(token, user.id)

        then: "get back 204"
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(user)
    }

    def "Verify that user not found error is thrown"() {
        given:
        def user = utils.createGenericUserAdmin()
        utils.addRoleToUser(user, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        def token = utils.getToken(user.username)

        when: "user attempts to unlock phone pin for using invalid user id"
        def response = cloud20.unlockPhonePin(token, "78544343test")

        then: "returns 404"
        response.status == SC_NOT_FOUND
    }

    def "Verify that phone pin can be unlocked for disabled user"() {
        given:
        def user = utils.createGenericUserAdmin()
        utils.addRoleToUser(user, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        def token = utils.getToken(user.username)

        def userWithLockedPhonePin = utils.createUser(token)
        utils.disableUser(userWithLockedPhonePin)
        utils.lockPhonepin(userWithLockedPhonePin.id)

        when: "user attempts to unlock phone pin for using invalid user id"
        def response = cloud20.unlockPhonePin(token, userWithLockedPhonePin.id)

        then: "returns 204"
        response.status == SC_NO_CONTENT
    }

    def "Verify that phone pin cannot be unlocked using impersonation token"() {

        given:
        def user = utils.createGenericUserAdmin()

        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), user)
        def impersonatedToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        utils.addRoleToUser(user, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)

        when: "user attempts to unlock phone pin with impersonation user token"
        def response = cloud20.unlockPhonePin(impersonatedToken, user.id)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_PHONE_PIN_FORBIDDEN_ACTION, "Not Authorized"))

        when: "user attempts to unlock phone pin with impersonation token of phone pin admin"
        utils.addRoleToUser(user, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), user)
        impersonatedToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        response = cloud20.unlockPhonePin(impersonatedToken, user.id)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_PHONE_PIN_FORBIDDEN_ACTION, "Not Authorized"))

        cleanup:
        utils.deleteUserQuietly(user)
    }
}