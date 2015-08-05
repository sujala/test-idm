package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.identity.multifactor.domain.BasicPin
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.exception.MultiFactorNotEnabledException
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

/**
 * Tests the multi-factor authentication service with a simulated duo authentication and phone verification service.
 *
 * Initially these tests are primarily pass-throughs to a simulated service, so really, not much to test. However, still
 * want to test this.
 *
 * Note - the simulated service for multifactor is only injected into the main spring context. Integration tests also spin up a grizzly container that
 * contains its own spring context. The grizzly container does NOT have the simulated service. This means these tests can NOT use REST API calls to
 * perform services that send SMS messages. Creating users and authenticating and such is fine.
 */
class BasicMultiFactorServiceAuthenticationIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    UserDao userRepository;

    BasicPin simulatorConstantPin = new BasicPin(Constants.MFA_DEFAULT_PIN)

    User userAdmin
    MobilePhone phone

    def setup() {
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), simulatorConstantPin)
    }

    def cleanup() {
        if (userAdmin != null) {
            multiFactorService.removeMultiFactorForUser(userAdmin.id)  //remove duo profile
            userRepository.deleteObject(userAdmin)
        }
    }

    /**
     * Verifies can send an sms passcode when multifactor is enabled.
     *
     * @return
     */
    def "Successfully send sms passcode"() {
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))
        userAdmin = userRepository.getUserById(userAdmin.getId())  //reload to get settings
        phone =  mobilePhoneRepository.getById(userAdmin.getMultiFactorMobilePhoneRsId());

        when:
        multiFactorService.sendSmsPasscode(userAdmin.getId())

        then:
        Mockito.verify(mockMultiFactorAuthenticationService.mock).sendSmsPasscodeChallenge(userAdmin.externalMultiFactorUserId, phone.externalMultiFactorPhoneId)
    }

    /**
     * Verifies error when send an sms passcode when multifactor is not enabled.
     *
     * @return
     */
    def "error when send sms passcode with disabled multifactor"() {
        when:
        multiFactorService.sendSmsPasscode(userAdmin.getId())

        then:
        thrown(MultiFactorNotEnabledException)
    }

    @Unroll("auth - sdk response is simply a pass through: simulated passcode: #passcode")
    def "auth - sdk response is simply a pass through"() {
        multiFactorService.updateMultiFactorSettings(userAdmin.getId(), v2Factory.createMultiFactorSettings(true))

        when:
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(expectedDecision, expectedDecisionReason, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        MfaAuthenticationResponse response = multiFactorService.verifyPasscode(userAdmin.getId(), "1234")

        then:
        response.decision == expectedDecision
        response.decisionReason == expectedDecisionReason

        where:
        expectedDecision                | expectedDecisionReason
        MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.ALLOW
        MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.BYPASS
        MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.UNKNOWN
        MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.DENY
        MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.LOCKEDOUT
        MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.PROVIDER_FAILURE
        MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        MfaAuthenticationDecision.DENY  | MfaAuthenticationDecisionReason.UNKNOWN
    }
}
