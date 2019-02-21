package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_NO_CONTENT

class VerifyPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    UserService userService

    def setup() {
        reloadableConfiguration.reset()
    }

    @Unroll
    def "Verify correct phone pin can be validated by identity:phone-pin-admin; requestType = #requestType" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        def userAdmin = utils.createGenericUserAdmin()
        def userAdminToken = utils.getToken(userAdmin.username)
        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin, requestType)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        when: "Verify with user without role"
        response = cloud20.verifyPhonePin(userAdminToken, userAdmin.id, phonePin, requestType)

        then: "get back 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "Add role to user and re-verify."
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        response = cloud20.verifyPhonePin(userAdminToken, userAdmin.id, phonePin, requestType)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(userAdmin)

        where:
        requestType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Verify phone pin can be validated on disabled user" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        def userAdmin = utils.createGenericUserAdmin()
        utils.disableUser(userAdmin)

        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    def "Verify phone pin can be validated on user in disabled domain" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)

        def userAdmin = utils.createGenericUserAdmin()
        utils.disableDomain(userAdmin.domainId)

        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then: "get back 204"
        assert response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "Verify incorrect phone pin with authorized user returns appropriate error; requestType = #requestType" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        def userAdmin = utils.createGenericUserAdmin()
        def pin = userService.checkAndGetUserById(userAdmin.id).phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin + "a"
            it
        }

        when: "Verify with authorized user"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin, requestType)

        then: "get back error"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Incorrect Phone PIN.")

        cleanup:
        utils.deleteUserQuietly(userAdmin)

        where:
        requestType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Super admins without identity:phone-pin-admin can not verify pin" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
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

    /**
     * This test assumes the specified user does *not* have a phone pin associated with it
     * @return
     */
    @Unroll
    def "Verify must supply a phone pin to verify against: specifiedPhonePin: #pin; requestType: #requestType" () {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)

        User user = userService.checkAndGetUserByName("uaGlobalRolesUser") // pre-existing user
        assert user.phonePin == null

        when: "Verify with authorized user"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }
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

    def "Verify verifying pin of user without one returns correct error" () {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)

        User user = userService.checkAndGetUserByName("uaGlobalRolesUser") // pre-existing user
        assert user.phonePin == null

        when: "Verify with authorized user"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "12324"
            it
        }
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), user.id, phonePin)

        then: "Returns as invalid"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-000'; The user has not set a Phone PIN.")
    }

    /**
     * This test assumes the specified user does *not* have a phone pin associated with it
     * @return
     */
    @Unroll
    def "Verify user with no phone pin returns as unmatched against null object phone pin: requestType: #requestType" () {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)

        User user = userService.checkAndGetUserByName("uaGlobalRolesUser") // pre-existing user
        assert user.phonePin == null

        when: "Send in null request body"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), user.id, null, requestType)

        then: "Get invalid request back"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, expectedMessage)

        where:
        requestType | expectedMessage
        MediaType.APPLICATION_XML_TYPE | "Invalid XML"
        MediaType.APPLICATION_JSON_TYPE | "Invalid json request body"
    }

    def "Verify phone pin returns 404 when phone pin feature is disabled" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)

        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "1234"
            it
        }

        when:
        def response = cloud20.verifyPhonePin(utils.getServiceAdminToken(), "anyUserId", phonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Service Not Found")
    }
}