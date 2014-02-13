package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.identity.multifactor.domain.BasicPin
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.identity.multifactor.domain.MfaAuthenticationResponse
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.exception.MultiFactorNotEnabledException
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.idm.multifactor.providers.simulator.SimulatedMultiFactorAuthenticationService
import com.rackspace.idm.multifactor.providers.simulator.SimulatedMultiFactorAuthenticationService.SimulatedPasscode
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
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
@ContextConfiguration(locations = ["classpath:app-config.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"
, "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatedMultiFactorAuthenticationService-context.xml"])
class BasicMultiFactorServiceAuthenticationIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private SimulatorMobilePhoneVerification simulatorMobilePhoneVerification

    @Autowired
    private SimulatedMultiFactorAuthenticationService simulatedMultiFactorAuthenticationService

    @Autowired
    private UserManagement duoUserManagement

    @Autowired
    BasicPin simulatorConstantPin

    User userAdmin
    MobilePhone phone

    def setup() {
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        userAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)
        multiFactorService.sendVerificationPin(userAdmin.getId(), phone.getId())
        multiFactorService.verifyPhoneForUser(userAdmin.getId(), phone.getId(), simulatorConstantPin)
        simulatedMultiFactorAuthenticationService.clearSmsPasscodeLog()
    }

    def cleanup() {
        if (userAdmin != null) {
            multiFactorService.removeMultiFactorForUser(userAdmin.id)  //remove duo profile
            userRepository.deleteObject(userAdmin)
        }
        if (phone != null) mobilePhoneRepository.deleteObject(phone)
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
        simulatedMultiFactorAuthenticationService.wasSmsPasscodeSentTo(userAdmin.getExternalMultiFactorUserId(), phone.getExternalMultiFactorPhoneId())
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
        MfaAuthenticationResponse response = multiFactorService.verifyPasscode(userAdmin.getId(), passcode.passcode)

        then:
        response.decision == expectedDecision
        response.decisionReason == expectedDecisionReason

        where:
        passcode   | expectedDecision   | expectedDecisionReason
        SimulatedPasscode.ALLOW_ALLOW | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.ALLOW
        SimulatedPasscode.ALLOW_BYPASS | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.BYPASS
        SimulatedPasscode.ALLOW_UNKNOWN | MfaAuthenticationDecision.ALLOW | MfaAuthenticationDecisionReason.UNKNOWN
        SimulatedPasscode.DENY_DENY | MfaAuthenticationDecision.DENY | MfaAuthenticationDecisionReason.DENY
        SimulatedPasscode.DENY_LOCKEDOUT | MfaAuthenticationDecision.DENY | MfaAuthenticationDecisionReason.LOCKEDOUT
        SimulatedPasscode.DENY_PROVIDER_FAILURE | MfaAuthenticationDecision.DENY | MfaAuthenticationDecisionReason.PROVIDER_FAILURE
        SimulatedPasscode.DENY_PROVIDER_UNAVAILABLE | MfaAuthenticationDecision.DENY | MfaAuthenticationDecisionReason.PROVIDER_UNAVAILABLE
        SimulatedPasscode.DENY_UNKNOWN | MfaAuthenticationDecision.DENY | MfaAuthenticationDecisionReason.UNKNOWN
    }
}
