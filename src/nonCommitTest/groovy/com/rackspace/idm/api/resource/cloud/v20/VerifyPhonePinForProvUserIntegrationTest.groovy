package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.PhonePinService
import com.rackspace.idm.domain.service.UserService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT

class VerifyPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    UserService userService

    @Autowired
    PhonePinService phonePinService

    @Unroll
    def "Verify phone pin on userAdmin; media = #accept" () {
        given:
        def userAdmin = cloud20.createCloudAccount(utils.getIdentityAdminToken())
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def phonePinEntity = phonePinService.checkAndGetPhonePin(userAdminUserEntity)
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = phonePinEntity.pin
            it
        }

        when: "verify phone pin with default identityAdminToken that has got identity:phone-pin-admin added to it"
        def response1 = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then:
        assert response1.status == SC_NO_CONTENT

        when: "verify phone pin with userAdmin auth token"
        def response2 = cloud20.verifyPhonePin(utils.getToken(userAdmin.username), userAdmin.id, phonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "verify phone pin with userAdmin auth token that has got identity:phone-pin-admin role added to it"
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        response2 = cloud20.verifyPhonePin(utils.getToken(userAdmin.username), userAdmin.id, phonePin)

        then:
        assert response2.status == SC_NO_CONTENT

        when: "verify phone pin with empty phone pin"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin emptyPhonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = ""
            it
        }
        def response3 = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, emptyPhonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response3, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Invalid phone pin")

        when: "verify phone pin with incorrect phone pin"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin incorrectPhonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "12345433"
            it
        }
        response2 = cloud20.verifyPhonePin(utils.getToken(userAdmin.username), userAdmin.id, incorrectPhonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response2, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Incorrect phone pin for userId: '"+userAdmin.id+"'")


        when: "verify phone pin with userAdmin that is disabled"
        userAdmin.enabled = false
        utils.updateUser(userAdmin)

        response2 = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then:
        assert response2.status == SC_NO_CONTENT

        when: "verify phone pin with user domain that is disabled"

        def domainDisable = v2Factory.createDomain().with {
            it.id = userAdmin.domainId
            it.name = userAdmin.domainId
            it.enabled = false
            it
        }
        utils.updateDomain(userAdmin.domainId, domainDisable)
        response2 = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), userAdmin.id, phonePin)

        then:
        assert response2.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUser(userAdmin)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Verify phone pin on userAdmin returns 404 when phone pin is not assigned at the time of create user" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)
        def userAdmin = cloud20.createCloudAccount(utils.getIdentityAdminToken())

        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "1234"
            it
        }

        when:
        def response = cloud20.verifyPhonePin(utils.getServiceAdminToken(), userAdmin.id, phonePin)

        then:
        assert response.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUser(userAdmin)
        staticIdmConfiguration.reset()
    }
}