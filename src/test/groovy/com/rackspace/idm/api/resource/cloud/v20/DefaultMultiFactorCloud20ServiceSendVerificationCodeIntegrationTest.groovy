package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
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
class DefaultMultiFactorCloud20ServiceSendVerificationCodeIntegrationTest extends RootConcurrentIntegrationTest {
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
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)

        def responseAddPhoneToUser = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone)
        responsePhone = responseAddPhoneToUser.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
    }


    @Unroll("Send verification code: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Send verification code"() {
        when:
        def sendVerificationCodeResponse = cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        sendVerificationCodeResponse.getStatus() == HttpStatus.SC_ACCEPTED
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == simulatorMobilePhoneVerification.constantPin.pin

        where:
        requestContentMediaType         | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 403 when specify user other than oneself: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 403 when specify user other than oneself"() {
        when:
        def response = cloud20.sendVerificationCode(userAdminToken, "123456", responsePhone.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_TARGET_ACCOUNT)
        assertUserStateOnFailure(finalUserAdmin)

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
        cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin), requestContentMediaType, acceptMediaType)

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
