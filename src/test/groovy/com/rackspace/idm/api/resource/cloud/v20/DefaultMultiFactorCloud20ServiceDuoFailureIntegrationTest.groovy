package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.providers.duo.domain.FailureResult
import com.rackspace.identity.multifactor.providers.duo.exception.DuoLockedOutException
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionIdReaderWriter
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired

import javax.ws.rs.core.Response

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
class DefaultMultiFactorCloud20ServiceDuoFailureIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    UserDao userRepository;

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private UserService userService

    @Autowired
    private MobilePhoneVerification mobilePhoneVerification;

    @Autowired
    private UserManagement userManagement

    @Autowired
    private RequestContextHolder requestContextHolder

    @Autowired
    private SessionIdReaderWriter sessionIdReaderWriter

    @Autowired
    private DefaultMultiFactorCloud20Service multiFactorCloud20Service

    org.openstack.docs.identity.api.v2.User userAdmin;
    User userAdminUser
    String userAdminToken;
    com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone;
    VerificationCode constantVerificationCode;

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminUser = userRepository.getUserById(userAdmin.id)
        userAdminToken = authenticate(userAdmin.username)
    }

    def cleanup() {
        if (userAdmin != null) {
            deleteUserQuietly(userAdmin)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteMobilePhone(mobilePhoneRepository.getById(responsePhone.getId()))
    }

    def "enableMultiFactor: Fail with 500 when unexpected exception"() {
        setup:
        addPhone()
        sendVerificationCode()
        verifyPhone()

        UserManagement mockedUserManagement = Mock(UserManagement)
        mockedUserManagement.createUser(_) >> {throw new RuntimeException("Error")}
        multiFactorService.userManagement = mockedUserManagement //set the service to the mock

        RequestContextHolder mockRequestContextHolder = Mock(RequestContextHolder)
        mockRequestContextHolder.checkAndGetTargetUser(_) >> userAdminUser
        multiFactorCloud20Service.requestContextHolder = mockRequestContextHolder

        when:
        Response.ResponseBuilder result = multiFactorCloud20Service.updateMultiFactorSettings(null, userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
        Response response = result.build()

        then:
        response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR

        cleanup:
        multiFactorService.userManagement = userManagement
        multiFactorCloud20Service.requestContextHolder = requestContextHolder//reset to original service
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

    def "performMultiFactorChallenge: Fail with 403 when Duo reports account is locked out"() {
        setup:
        addPhone()

        BasicMultiFactorService mockedBasicMultiFactorService = Mock(BasicMultiFactorService)
        multiFactorCloud20Service.multiFactorService = mockedBasicMultiFactorService

        SessionIdReaderWriter mockedSessionIdReaderWriter = Mock(SessionIdReaderWriter)
        mockedSessionIdReaderWriter.writeEncoded(_) >> "dummy"
        multiFactorCloud20Service.sessionIdReaderWriter = mockedSessionIdReaderWriter

        UserService mockUserService = Mock(UserService)
        User user = new User()
        multiFactorCloud20Service.userService = mockUserService
        mockUserService.getUserById(userAdmin.id) >> user

        when:
        Response.ResponseBuilder result = multiFactorCloud20Service.performMultiFactorChallenge(userAdminUser, ["PASSWORD"].asList())

        then:
        1 * mockedBasicMultiFactorService.sendSmsPasscode(_) >> {throw new DuoLockedOutException(new FailureResult(0, "status", "message"))}
        1 * mockedBasicMultiFactorService.isMultiFactorTypePhone(_) >> true
        thrown(ForbiddenException)

        cleanup:
        multiFactorCloud20Service.multiFactorService = multiFactorService
        multiFactorCloud20Service.sessionIdReaderWriter = sessionIdReaderWriter
        multiFactorCloud20Service.userService = userService
    }

    def void addPhone() {
        responsePhone = utils.addPhone(userAdminToken, userAdmin.id)
    }

    def void sendVerificationCode() {
        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, responsePhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
    }

    def void verifyPhone() {
        utils.verifyPhone(userAdminToken, userAdmin.id, responsePhone.id, constantVerificationCode)
    }
}
