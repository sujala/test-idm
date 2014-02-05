package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.StringUtils
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

/**
 * Tests the multifactor delete multifactor REST service
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DefaultMultiFactorCloud20ServiceDeleteMultiFactorIntegrationTest extends RootConcurrentIntegrationTest {
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

    @Autowired
    private LdapScopeAccessRepository scopeAccessRepository;

    UserScopeAccess userScopeAccess;
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
        userScopeAccess = (UserScopeAccess)scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id))  //remove duo profile
            deleteUserQuietly(userAdmin)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))
    }

    def "Verify that enabling multifactor expires existing token"() {
        setup:
        def token = scopeAccessRepository.getScopeAccessByAccessToken(userScopeAccess.accessTokenString)
        assert(!token.isAccessTokenExpired(new DateTime()))

        when:
        setUpAndEnableMultiFactor()
        def expiredToken = scopeAccessRepository.getScopeAccessByAccessToken(userScopeAccess.accessTokenString)

        then:
        expiredToken.isAccessTokenExpired(new DateTime())

        cleanup:
        resetTokenExpiration()
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)
    }

    /**
     * This tests deleting multi-factor on an account that is fully enabled with multifactor
     *
     * @return
     */
    @Unroll("Successfully delete multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor"() {
        setup:
        setUpAndEnableMultiFactor()
        resetTokenExpiration()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests enabling multi-factor on an account that is fully setup for multifactor, but not enabled.
     *
     * @return
     */
    @Unroll("Successfully delete multifactor when not enabled: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor when not enabled"() {
        setup:
        setUpMultiFactorWithoutEnable()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests enabling multi-factor on an account that is fully setup for multifactor, but not enabled.
     *
     * @return
     */
    @Unroll("Successfully delete multifactor when partially set up with just a phone: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor when partially set up with just a phone"() {
        setup:
        setUpMultiFactorWithUnverifiedPhone()
        resetTokenExpiration()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests removing multifactor from an account that was set up for it, then setting it back up again
     * @return
     */
    @Unroll("Successfully reset up multifactor after removing it wth same phone number: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully reset up multifactor after removing it wth same phone number"() {
        setup:
        setUpAndEnableMultiFactor()
        resetTokenExpiration()
        User retrievedUserAdmin = userRepository.getUserById(userAdmin.getId())
        MobilePhone originalPhone = mobilePhoneRepository.getById(retrievedUserAdmin.getMultiFactorMobilePhoneRsId())
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone newXmlPhone = v2Factory.createMobilePhone(originalPhone.getTelephoneNumber())
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        when:
        //setup multifactor again using same phone as before
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone returnedXmlPhone = utils.addPhone(userAdminToken, userAdmin.id, newXmlPhone)
        MobilePhone newPhone = mobilePhoneRepository.getById(returnedXmlPhone.getId())

        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        resetTokenExpiration()

        then:
        newPhone.externalMultiFactorPhoneId == originalPhone.externalMultiFactorPhoneId //the phones are not deleted in duo
        newPhone.id == originalPhone.id //the phones are not deleted in ldap

        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        cleanup:
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests removing multifactor from an account that was set up for it, then setting it back up again
     * @return
     */
    @Unroll("Successfully reset up multifactor after removing it: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully reset up multifactor after removing it"() {
        setup:
        setUpAndEnableMultiFactor()
        resetTokenExpiration()
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        expect:
        setUpAndEnableMultiFactor() //"test" is that no errors reported from assertions made while enabling multi-factor again
        resetTokenExpiration()

        cleanup:
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def void verifyFinalUserState(User finalUserAdmin) {
        assert finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        assert finalUserAdmin.getMultiFactorDevicePin() == null
        assert !finalUserAdmin.isMultiFactorDeviceVerified()
        assert !finalUserAdmin.isMultiFactorEnabled()
        assert finalUserAdmin.getExternalMultiFactorUserId() == null
        assert finalUserAdmin.getMultiFactorMobilePhoneRsId() == null
    }


    def void setUpAndEnableMultiFactor() {
        setUpMultiFactorWithoutEnable()
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
    }

    def void setUpMultiFactorWithoutEnable() {
        setUpMultiFactorWithUnverifiedPhone()
        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
    }

    def void setUpMultiFactorWithUnverifiedPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
    }

    def void resetTokenExpiration() {
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        userScopeAccess.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(userScopeAccess)
    }
}
