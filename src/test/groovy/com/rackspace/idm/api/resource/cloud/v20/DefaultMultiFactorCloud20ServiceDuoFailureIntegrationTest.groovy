package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification
import com.rackspace.idm.multifactor.providers.UserManagement
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import javax.ws.rs.core.Response

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly

/**
 * This tests where an unexpected exception is encountered in the multifactor provider class. This class uses grizzly
 * to set the state for various things (e.g. - creating useradmin, setting up multifactor as necessary for the test, etc.
 * It then uses a local call, with appropriate mocks put in place to simulate failures, and tests the Response that the
 * service would return.
 *
 * <p>
 * This doesn't perform a full on integration test of parsing the actual json/xml response body. Adding in mocks to the
 * grizzly runtime version of the classes would have requires a bit more effort than perhaps justified.
 * Instead, we're just testing that the response object that Jersey would translate into a the response body will contain
 * the correct status code (500) for internal server errors.
 * </p>
 */
@ContextConfiguration(locations = ["classpath:app-config.xml", "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class DefaultMultiFactorCloud20ServiceDuoFailureIntegrationTest extends RootConcurrentIntegrationTest {
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
    private MobilePhoneVerification mobilePhoneVerification;

    @Autowired
    private UserManagement userManagement

    @Autowired
    private DefaultMultiFactorCloud20Service multiFactorCloud20Service

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
            deleteUserQuietly(userAdmin)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))
    }

    def "enableMultiFactor: Fail with 500 when unexpected exception"() {
        setup:
        addPhone()
        sendVerificationCode()
        verifyPhone()

        UserManagement mockedUserManagement = Mock(UserManagement)
        mockedUserManagement.createUser(_) >> {throw new RuntimeException("Error")}
        multiFactorService.userManagement = mockedUserManagement //set the service to the mock

        when:
        Response.ResponseBuilder result = multiFactorCloud20Service.updateMultiFactorSettings(null, userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
        Response response = result.build()

        then:
        response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR

        cleanup:
        multiFactorService.userManagement = userManagement //reset to original service
    }

    def "sendVerification: Fail with 500 when unexpected exception"() {
        setup:
        addPhone()

        MobilePhoneVerification mockedMobilePhoneVerification = Mock(MobilePhoneVerification)
        mockedMobilePhoneVerification.sendPin(_) >> {throw new RuntimeException("Error")}
        multiFactorService.mobilePhoneVerification = mockedMobilePhoneVerification //set the service to the mock

        when:
        Response.ResponseBuilder result = multiFactorCloud20Service.sendVerificationCode(null, userAdminToken, userAdmin.id, responsePhone.id)
        Response response = result.build()

        then:
        response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR

        cleanup:
        multiFactorService.mobilePhoneVerification = mobilePhoneVerification //reset to original service
    }

    def void addPhone() {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        def responseAddPhoneToUser = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone)
        responsePhone = responseAddPhoneToUser.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
    }

    def void sendVerificationCode() {
        cloud20.sendVerificationCode(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
    }

    def void verifyPhone() {
        def verifyVerificationCodeResponse = cloud20.verifyVerificationCode(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
        assert verifyVerificationCodeResponse.status == HttpStatus.SC_NO_CONTENT
    }
}
