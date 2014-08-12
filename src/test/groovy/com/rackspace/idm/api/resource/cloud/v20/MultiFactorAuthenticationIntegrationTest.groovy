package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatedMultiFactorAuthenticationService
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

/**
 * Tests verifying multifactor authentication. In lieu of using real Duo provider, which sends SMS messages, it subsitutes
 * a simulated mfa phone versificatino and authentication provider. It also modifies the config to use encryption keys for the sessionid located
 * in the classpath.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"
, "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatedMultiFactorAuthenticationService-context.xml"])
class MultiFactorAuthenticationIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository

    @Autowired
    private LdapUserRepository userRepository

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification

    @Autowired
    private SimulatedMultiFactorAuthenticationService simulatedMultiFactorAuthenticationService

    @Autowired
    private BasicMultiFactorService multiFactorService

    @Autowired
    private EncryptedSessionIdReaderWriter encryptedSessionIdReaderWriter;

    @Autowired
    private DefaultMultiFactorCloud20Service defaultMultiFactorCloud20Service;

    @Autowired
    private UserDao userDao

    org.openstack.docs.identity.api.v2.User userAdmin
    String userAdminToken
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone
    VerificationCode constantVerificationCode

    /**
     * Override the grizzly start because we want to add additional context file.
     * @return
     */
    @Override
    public void doSetupSpec() {
        ClassPathResource resource = new ClassPathResource("/com/rackspace/idm/api/resource/cloud/v20/keys");
        resource.exists()
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatedMultiFactorAuthenticationService-context.xml " +
                "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml")
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        simulatedMultiFactorAuthenticationService.clearSmsPasscodeLog()
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id))  //remove duo profile
            deleteUserQuietly(userAdmin)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))
    }

    /**
     * This tests authenticating with API key on an mfa enabled account
     *
     * @return
     */
    @Unroll
    def "When successfully enable multifactor appropriate v1.0/v1.1/v2.0 api key auth behavior: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        setUpAndEnableMultiFactor()
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "try to auth via 1.0 with correct API key"
        def auth10ResponseApi = cloud10.authenticate(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())

        then: "receive 204"
        auth10ResponseApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_NO_CONTENT

        when: "try to auth via 1.1 with correct API key"
        def cred = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), finalUserAdmin.getApiKey())
        def auth11ResponseCorrectAPI = cloud11.authenticate(cred, requestContentMediaType, acceptMediaType)

        then: "receive 200"
        auth11ResponseCorrectAPI.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_OK

        when: "try to auth via 1.1 with incorrect API key"
        def cred2 = v1Factory.createUserKeyCredentials(finalUserAdmin.getUsername(), "abcd1234")
        def auth11ResponseWrongApi = cloud11.authenticate(cred2, requestContentMediaType, acceptMediaType)

        then: "receive 401"
        IdmAssert.assertV1AuthFaultResponse(auth11ResponseWrongApi, com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault.class, com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED, AuthWithApiKeyCredentials.AUTH_FAILURE_MSG)

        when: "try to auth via 2.0 with correct API"
        def auth20ResponseCorrectApi = cloud20.authenticateApiKey(finalUserAdmin.getUsername(), finalUserAdmin.apiKey, requestContentMediaType, acceptMediaType)

        then: "Should be allowed"
        auth20ResponseCorrectApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_OK

        when: "try to auth via 2.0 with incorrect API"
        def auth20ResponseWrongApi = cloud20.authenticateApiKey(finalUserAdmin.getUsername(), "abcd1234", requestContentMediaType, acceptMediaType)

        then: "Should not be allowed"
        auth20ResponseWrongApi.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests authenticating with v2.0 password on an mfa enabled account
     *
     * @return
     */
    @Unroll
    def "When successfully enable multifactor appropriate v2.0 pwd auth behavior: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        setUpAndEnableMultiFactor()
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        when: "try to auth via 2.0 with correct pwd"
        def auth20ResponseCorrectPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD, requestContentMediaType, acceptMediaType)

        then: "Should get 401 w/ www-authenticate header"
        auth20ResponseCorrectPwd.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED
        auth20ResponseCorrectPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE) != null

        when: "try to auth via 2.0 with incorrect pwd"
        def auth20ResponseWrongPwd = cloud20.authenticate(finalUserAdmin.getUsername(), Constants.DEFAULT_PASSWORD + "wrong", requestContentMediaType, acceptMediaType)

        then: "Should get 401 without www-authenticate header"
        auth20ResponseWrongPwd.status == com.rackspace.identity.multifactor.util.HttpStatus.SC_UNAUTHORIZED
        auth20ResponseWrongPwd.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE) == null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
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
}
