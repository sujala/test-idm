package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerifyPhonePinResult
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_OK

class VerifyPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    UserService userService

    def setup() {
        reloadableConfiguration.reset()
        wiserWrapper.getWiser().getMessages().clear()
    }

    @Unroll
    def "Verify correct phone pin can be validated by identity:phone-pin-admin; requestType = #requestType" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        def userAdminToken = utils.getToken(userAdmin.username)
        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin, requestType)

        then: "get back 200"
        assert response.status == SC_OK

        when: "Verify with user without role"
        response = cloud20.verifyPhonePin(userAdminToken, userAdmin.id, phonePin, requestType)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "Add role to user and re-verify."
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        response = cloud20.verifyPhonePin(userAdminToken, userAdmin.id, phonePin, requestType)

        then: "get back 200"
        assert response.status == SC_OK

        cleanup:
        utils.deleteUserQuietly(userAdmin)

        where:
        requestType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Verify phone pin can be validated on disabled user" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        utils.disableUser(userAdmin)

        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "get back 200"
        assert response.status == SC_OK

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    def "Verify phone pin can be validated on user in disabled domain" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        utils.disableDomain(userAdmin.domainId)

        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "get back 200"
        assert response.status == SC_OK

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "Verify incorrect phone pin with authorized user returns appropriate error; requestType = #requestType" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin + "a"
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin, requestType)

        then: "Returns 200 response"
        response.status == SC_OK

        and: "Response is appropriate"
        VerifyPhonePinResult result = response.getEntity(VerifyPhonePinResult)
        !result.authenticated
        result.failureCode == "PP-003"
        result.failureMessage == "Incorrect Phone PIN."

        cleanup:
        utils.deleteUserQuietly(userAdmin)

        where:
        requestType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Super admins without identity:phone-pin-admin can not verify pin" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with regular service admin"
        def response = cloud20.verifyPhonePin(utils.getToken("testServiceAdmin_doNotDelete"), userAdmin.id, phonePin)

        then: "Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "Verify with regular identity admin"
        def ia2 = utils.createIdentityAdmin()
        def ia2Token = utils.getToken(ia2.username)
        response = cloud20.verifyPhonePin(ia2Token, userAdmin.id, phonePin)

        then: "Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "Verify must supply a phone pin: specifiedPhonePin: #pin; requestType: #requestType" () {
        User user = userService.checkAndGetUserByName("uaGlobalRolesUser") // pre-existing user
        assert user.phonePin == null

        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), user.id, phonePin, requestType)

        then: "Returns as invalid"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Must supply a Phone PIN.")

        where:
        pin | requestType
        ""  | MediaType.APPLICATION_XML_TYPE
        ""  | MediaType.APPLICATION_JSON_TYPE
        null  | MediaType.APPLICATION_XML_TYPE
        null  | MediaType.APPLICATION_JSON_TYPE
        " "  | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Verifying pin of user without one returns correct error: mediaType: #mediaType" () {
        User user = userService.checkAndGetUserByName("uaGlobalRolesUser") // pre-existing user
        assert user.phonePin == null

        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "12324"
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), user.id, phonePin)

        then: "Returns 200 response"
        response.status == SC_OK

        and: "Response is appropriate"
        VerifyPhonePinResult result = response.getEntity(VerifyPhonePinResult)
        !result.authenticated
        result.failureCode == "PP-000"
        result.failureMessage == "The user has not set a Phone PIN."

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Locking a phone pin sends an email only when the phone pin becomes locked" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        def userEntity = userService.checkAndGetUserById(userAdmin.id)
        def pin = userEntity.phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin + "a"
            it
        }

        when: "Verify the phone pin w/ an invalid pin"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "Returns 200 response"
        response.status == SC_OK
        VerifyPhonePinResult result = response.getEntity(VerifyPhonePinResult)
        !result.authenticated
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        when: "Verify the phone pin and lock the pin"
        userEntity = userService.checkAndGetUserById(userAdmin.id)
        (GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD - 2).times {
            userEntity.recordFailedPinAuthentication()
        }
        userService.updateUser(userEntity)
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "The phone pin locked email was sent"
        response.status == SC_OK
        VerifyPhonePinResult result2 = response.getEntity(VerifyPhonePinResult)
        !result2.authenticated
        CollectionUtils.isNotEmpty(wiserWrapper.wiserServer.getMessages())
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getFrom().length == 1
        message.getFrom()[0].toString() == Constants.PHONE_PIN_LOCKED_EMAIL_FROM
        message.getSubject() == Constants.PHONE_PIN_LOCKED_EMAIL_SUBJECT

        when: "Verify the phone pin again now that it is locked"
        wiserWrapper.getWiser().getMessages().clear()
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "No emails were sent"
        response.status == SC_OK
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

}