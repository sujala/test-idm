package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

/**
 * Tests the multifactor sendVerificationCode REST service
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DefaultMultiFactorCloud20ServiceVerifyVerificationCodeIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    org.openstack.docs.identity.api.v2.User userAdmin;
    String userAdminToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone;
    VerificationCode constantVerificationCode;

    /**
     * Override the grizzly start because we want to add another context file.
     * @return
     */
    @Override
    public void doSetupSpec(){
        this.resource = startOrRestartGrizzly("classpath:app-config.xml classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
    }

    /**
     * Sets up a new user with a phone that has the verification code sent.
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
        cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
    }

    def cleanup() {
        deleteUserQuietly(userAdmin)
        MobilePhone ldapPhone = mobilePhoneRepository.getById(responsePhone.getId())
        mobilePhoneRepository.deleteObject(ldapPhone)
    }

    /**
     * This tests verifying a phone attached to a user
     *
     * @return
     */
    @Unroll("Successfully verify verification code: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully verify verification code"() {
        setup:

        when:
        def verifyVerificationCodeResponse = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        verifyVerificationCodeResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 403 when specify user other than oneself: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 403 when specify user other than oneself"() {
        setup:

        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, "123456", responsePhone.id, constantVerificationCode, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 404 when device id not associated with user: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 404 when device id not associated with user"() {
        setup:

        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, "nonExistantId", constantVerificationCode, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when invalid pin provided: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when invalid pin provided"() {
        setup:

        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, v2Factory.createVerificationCode("invalidcode"), requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when expired pin provided: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when expired pin provided"() {
        setup:
        //expire the verification code
        User entityUserAdmin = userRepository.getUserById(userAdmin.getId())
        entityUserAdmin.setMultiFactorDevicePinExpiration(new DateTime().minusDays(1).toDate())
        userRepository.updateUserAsIs(entityUserAdmin)

        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        cleanup:

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }
}
