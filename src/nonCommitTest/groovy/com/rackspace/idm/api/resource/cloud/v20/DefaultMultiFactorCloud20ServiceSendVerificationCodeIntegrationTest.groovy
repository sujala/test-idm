package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

/**
 * Tests the multifactor sendVerificationCode REST service
 */
class DefaultMultiFactorCloud20ServiceSendVerificationCodeIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    UserDao userRepository;

    org.openstack.docs.identity.api.v2.User userAdmin;
    String userAdminToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone;

    /**
     * Sets up a new user with a phone that has the verification code sent.
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
    }


    @Unroll("Send verification code: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Send verification code"() {
        when:
        def sendVerificationCodeResponse = cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        sendVerificationCodeResponse.getStatus() == HttpStatus.SC_ACCEPTED
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == Constants.MFA_DEFAULT_PIN

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 404 when device id not associated with user: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 404 when device id not associated with user"() {
        when:
        def response = cloud20.sendVerificationCode(userAdminToken, userAdmin.id, "nonExistantId", requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_DEVICE)
        assertUserStateOnFailure(finalUserAdmin)

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when phone already verified on account: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when phone already verified on account"() {
        setup:
        cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, requestContentMediaType, acceptMediaType)
        cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN), requestContentMediaType, acceptMediaType)

        when:
        def response = cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_ALREADY_VERIFIED)
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def void assertUserStateOnFailure(User user) {
        assert user.getMultiFactorDevicePinExpiration() == null
        assert user.getMultiFactorDevicePin() == null
        assert user.getMultiFactorDeviceVerified() == null || !user.getMultiFactorDeviceVerified()
        assert user.getMultifactorEnabled() == null || !user.getMultifactorEnabled()
    }

}