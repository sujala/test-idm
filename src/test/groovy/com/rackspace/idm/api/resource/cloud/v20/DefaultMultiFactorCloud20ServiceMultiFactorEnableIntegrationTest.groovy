package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
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
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.StringUtils
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

/**
 * Tests the multifactor sendVerificationCode REST service
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DefaultMultiFactorCloud20ServiceMultiFactorEnableIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    @Autowired
    private BasicMultiFactorService multiFactorService;


    org.openstack.docs.identity.api.v2.User userAdmin;
    String userAdminToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone;
    VerificationCode constantVerificationCode;

    /**
     * Override the grizzly start because we want to add another context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        this.resource = startOrRestartGrizzly("classpath:app-config.xml classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
    }

    @Override
    public void doCleanupSpec() {
        startOrRestartGrizzly("classpath:app-config.xml")
    }


    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id))  //remove duo profile
            deleteUserQuietly(userAdmin)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))
    }

    /**
     * This tests enabling multi-factor on an account
     *
     * @return
     */
    @Unroll("Successfully enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully enable multifactor"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Successfully disable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully disable multifactor after it was enabled"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User intialUserAdmin = userRepository.getUserById(userAdmin.getId())
        assert intialUserAdmin.isMultiFactorEnabled()
        assert intialUserAdmin.getExternalMultiFactorUserId() != null

        when:
        settings.setEnabled(false)
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        !finalUserAdmin.isMultiFactorEnabled()
        finalUserAdmin.getExternalMultiFactorUserId() == null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Disable multifactor when not enable is no-op: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Disable multifactor when not enable is no-op"() {
        setup:
        addPhone()
        verifyPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        !finalUserAdmin.isMultiFactorEnabled()
        finalUserAdmin.getExternalMultiFactorUserId() == null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Can re-enable multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Can re-enable multifactor"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        settings.setEnabled(false)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User initialUserAdmin = userRepository.getUserById(userAdmin.getId())
        assert !initialUserAdmin.isMultiFactorEnabled()

        when:
        settings.setEnabled(true)
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when multifactor phone not verified: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when multifactor phone not verified"() {
        setup:
        addPhone()
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_VERIFIED_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() != null
        finalUserAdmin.getMultiFactorDevicePin() == constantVerificationCode.getCode()
        !finalUserAdmin.isMultiFactorDeviceVerified()
        !finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Fail with 400 when no multifactor device on user account: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Fail with 400 when no multifactor device on user account"() {
        setup:
        MultiFactor settings = v2Factory.createMultiFactorSettings(true)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, BasicMultiFactorService.ERROR_MSG_NO_DEVICE)
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        !finalUserAdmin.isMultiFactorDeviceVerified()
        !finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll("Enable multifactor when already enabled is no-op: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Enable multifactor when already enabled is no-op"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests a data inconsistency issue. Generally, when multi-factor is disabled on an account, a user profile will not
     * exist in the external provider. However, there is a chance that the state in ldap could be removed successfully, but the call
     * to remove the profile from the external provider fails. This test verifies that in such a situation, multi-factor can
     * still be enabled on the account and the inconsistency is automatically cleared up.
     *
     * @return
     */
    @Unroll("Successfully enable multifactor when external account and phone already exists for user: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully enable multifactor when external account and phone already exists for user"() {
        setup:
        addPhone()
        verifyPhone()

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings)

        //here we're hacking ldap to get the data into an inconsistent state for testing purposes
        User userEntity = userRepository.getUserById(userAdmin.id)
        userEntity.setMultifactorEnabled(false)
        userEntity.setExternalMultiFactorUserId(null)
        userRepository.updateObjectAsIs(userEntity)

        MobilePhone phoneEntity = mobilePhoneRepository.getById(responsePhone.id)
        phoneEntity.setExternalMultiFactorPhoneId(null)
        mobilePhoneRepository.updateObjectAsIs(phoneEntity)

        when:
        def response = cloud20.updateMultiFactorSettings(userAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        MobilePhone finalPhoneEntity = mobilePhoneRepository.getById(responsePhone.id)

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()
        StringUtils.hasText(finalUserAdmin.getExternalMultiFactorUserId())

        StringUtils.hasText(finalPhoneEntity.getExternalMultiFactorPhoneId())

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def void addPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
    }

    def void verifyPhone() {
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
    }
}
