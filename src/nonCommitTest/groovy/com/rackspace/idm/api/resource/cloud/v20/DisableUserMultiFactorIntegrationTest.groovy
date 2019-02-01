package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.mockito.Mockito
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Unroll

/**
 * Tests to verify that when a user is disabled, they are not able to
 * conduct multi-factor functions on their account
 */
class DisableUserMultiFactorIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired BasicMultiFactorService multiFactorService;

    @Autowired MobilePhoneDao mobilePhoneRepository;

    @Autowired ScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired UserManagement<DuoUser, DuoPhone> userManagement

    @Autowired UserService userService

    def "when a MFA user with a passcode is disabled, they cannot finish authN with the passcode"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def authToken = authenticate(user.username)
        def responsePhone = addPhone(authToken, user)
        cloud20.updateMultiFactorSettings(authToken, user.id, settings)

        when: "get the passcode for the user"
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then:
        authResponse.status == 401

        when: "disable the user and attempt to finish authentication with the passcode"
        String wwwHeader = authResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        user.enabled = false
        utils.updateUser(user)
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234")

        then:
        // When MFA session id is generated the AE tokens way(restricted token), it will be revoked when user is disabled
        mfaAuthResponse.status == 401

        cleanup:
        deleteUserQuietly(user)
        mobilePhoneRepository.deleteMobilePhone(mobilePhoneRepository.getById(responsePhone.id))
    }

    def "when an SMS MFA user is disabled, MFA is still enabled if user account is re-enabled"() {
        given:
        // Any passcode passes
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)

        def user = createUserAdmin()
        utils.setUpAndEnableUserForMultiFactorSMS(specificationServiceAdminToken, user)

        when: "authenticate with MFA"
        def token = utils.getMFAToken(user.username, "1234")

        then:
        token != null

        when: "disable the user and try to authenticate w/ first step of MFA login"
        user.enabled = false
        utils.updateUser(user)
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then: "Denied"
        authResponse.status == 403

        when: "re-enable the user"
        user.enabled = true
        utils.updateUser(user)
        def userById = utils.getUserById(user.id)

        then: "MFA should be enabled for this user still"
        userById.enabled
        userById.multiFactorEnabled
        userById.factorType == FactorTypeEnum.SMS

        and: "Can authenticate with SMS MFA"
        utils.getMFAToken(user.username, "1234") != null

        cleanup:
        deleteUserQuietly(user)
    }

    def "when an OTP MFA user is disabled, MFA is still enabled if user account is enabled"() {
        given:
        def user = createUserAdmin()
        OTPDevice mfaDevice = utils.setUpAndEnableUserForMultiFactorOTP(specificationServiceAdminToken, user)

        when: "authenticate with MFA"
        def token = utils.authenticateWithOTPDevice(user, mfaDevice)

        then:
        token != null

        when: "disable the user and try to authenticate w/ first step of MFA login"
        user.enabled = false
        utils.updateUser(user)
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then: "Denied"
        authResponse.status == 403

        when: "re-enable the user"
        user.enabled = true
        utils.updateUser(user)
        def userById = utils.getUserById(user.id)

        then: "MFA should be enabled for this user still"
        userById.enabled
        userById.multiFactorEnabled
        userById.factorType == FactorTypeEnum.OTP

        and: "Can authenticate with OTP MFA"
        utils.authenticateWithOTPDevice(user, mfaDevice) != null

        cleanup:
        deleteUserQuietly(user)
    }

    def "when an SMS MFA user's account is disabled, their Duo profile is removed"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        def responsePhone = addPhone(token, user)

        when: "enabling MFA on the use should create their account in Duo"
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def userEntity = userService.getUserById(user.id)

        then:
        userManagement.getUserById(userEntity.getExternalMultiFactorUserId()) != null

        when: "disabling the user should remove their account in Duo"
        user.enabled = false
        utils.updateUser(user)

        then: "calls the Duo user management service to delete the profile"
        Mockito.verify(userManagement.mock, Mockito.atLeastOnce()).deleteUserById(Constants.MFA_DEFAULT_USER_PROVIDER_ID)

        cleanup:
        deleteUserQuietly(user)
        mobilePhoneRepository.deleteMobilePhone(mobilePhoneRepository.getById(responsePhone.id))
    }

    @Unroll
    def "when a MFA user is disabled, then the user's MFA is still enabled with same factor type and no external profile exists: #factorType"() {
        given:
        def user = createUserAdmin()
        if (factorType == FactorTypeEnum.SMS) {
            utils.setUpAndEnableUserForMultiFactorSMS(specificationServiceAdminToken, user)
        } else {
            utils.setUpAndEnableUserForMultiFactorOTP(specificationServiceAdminToken, user)
        }
        clearEmailServerMessages() // Clear emails previously sent

        when:
        User userById = utils.getUserById(user.id)

        then:
        userById.multiFactorEnabled
        userById.factorType == factorType

        when:
        user.enabled = false
        utils.updateUser(user)
        userById = utils.getUserById(user.id)

        then: "MFA settings have not changed"
        !userById.enabled
        userById.multiFactorEnabled
        userById.factorType == factorType

        and: "Regardless of factor type, there is no external profile"
        com.rackspace.idm.domain.entity.User userEntity = userService.getUserById(userById.id)
        userEntity.externalMultiFactorUserId == null

        and: "No email was sent"
        wiserWrapper.wiserServer.getMessages().size() == 0

        cleanup:
        deleteUserQuietly(user)

        where:
        factorType << [FactorTypeEnum.SMS, FactorTypeEnum.OTP]
    }

    def addPhone(token, user, verify=true) {
        def responsePhone = utils.addPhone(token, user.id)
        utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
        if(verify) {
            def constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
            utils.verifyPhone(token, user.id, responsePhone.id, constantVerificationCode)
        }
        responsePhone
    }

}
