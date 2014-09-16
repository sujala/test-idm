package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.Constants
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
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
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

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
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
        if (responsePhone != null) {
            MobilePhone ldapPhone = mobilePhoneRepository.getById(responsePhone.getId())
            mobilePhoneRepository.deleteObject(ldapPhone)
        }
    }

    /**
     * This tests verifying a phone attached to a user
     *
     * @return
     */
    @Unroll("Successfully verify verification code: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully verify verification code"() {
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

    def "verifyVerificationCode - User admin can add a phone, send a verification code, and verify the code on a subuser"() {
        setup:
        org.openstack.docs.identity.api.v2.User defaultUser = createDefaultUser(userAdminToken)
        def defPhone = utils.addPhone(userAdminToken, defaultUser.id)
        cloud20.sendVerificationCode(userAdminToken, defaultUser.id, defPhone.id)
        def verCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);

        when:
        def verifyVerificationCodeResponse = cloud20.verifyVerificationCode(userAdminToken, defaultUser.id, defPhone.id, verCode)
        User finalDefaultUser = userRepository.getUserById(defaultUser.getId())

        then:
        verifyVerificationCodeResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        finalDefaultUser.getMultiFactorDevicePinExpiration() == null
        finalDefaultUser.getMultiFactorDevicePin() == null
        finalDefaultUser.getMultiFactorDeviceVerified()
        !finalDefaultUser.getMultifactorEnabled()

        cleanup:
        deleteUserQuietly(defaultUser)
    }

    def "verifyVerificationCode - User manager can add a phone, send a verification code, and verify the code on a subuser"() {
        setup:
        org.openstack.docs.identity.api.v2.User userManager = createDefaultUser(userAdminToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID, userAdminToken)
        def userManagerToken = authenticate(userManager.username)

        org.openstack.docs.identity.api.v2.User defaultUser = createDefaultUser(userAdminToken)
        def defPhone = utils.addPhone(userManagerToken, defaultUser.id)
        cloud20.sendVerificationCode(userManagerToken, defaultUser.id, defPhone.id)
        def verCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);

        when:
        def verifyVerificationCodeResponse = cloud20.verifyVerificationCode(userManagerToken, defaultUser.id, defPhone.id, verCode)
        User finalDefaultUser = userRepository.getUserById(defaultUser.getId())

        then:
        verifyVerificationCodeResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        finalDefaultUser.getMultiFactorDevicePinExpiration() == null
        finalDefaultUser.getMultiFactorDevicePin() == null
        finalDefaultUser.getMultiFactorDeviceVerified()
        !finalDefaultUser.getMultifactorEnabled()

        cleanup:
        deleteUserQuietly(defaultUser)
        deleteUserQuietly(userManager)
    }

    def "verifyVerificationCode - identity admin can add a phone, send a verification code, and verify the code on another user"() {
        setup:
        responsePhone = utils.addPhone(specificationIdentityAdminToken, userAdmin.id)
        cloud20.sendVerificationCode(specificationIdentityAdminToken, userAdmin.id, responsePhone.id)
        def verCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);

        when:
        def verifyVerificationCodeResponse = cloud20.verifyVerificationCode(specificationIdentityAdminToken, userAdmin.id, responsePhone.id, verCode)
        User finalUser = userRepository.getUserById(userAdmin.getId())

        then:
        verifyVerificationCodeResponse.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUser.getMultiFactorDevicePinExpiration() == null
        finalUser.getMultiFactorDevicePin() == null
        finalUser.getMultiFactorDeviceVerified()
        !finalUser.getMultifactorEnabled()
    }

    @Unroll("Fail with 404 when device id not associated with user: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 404 when device id not associated with user"() {
        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, "nonExistantId", constantVerificationCode, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when invalid pin provided: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when invalid pin provided"() {
        when:
        def response = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, v2Factory.createVerificationCode("invalidcode"), requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PIN_OR_EXPIRED)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.getMultiFactorDeviceVerified()
        !finalUserAdmin.getMultifactorEnabled()

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

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }
}
