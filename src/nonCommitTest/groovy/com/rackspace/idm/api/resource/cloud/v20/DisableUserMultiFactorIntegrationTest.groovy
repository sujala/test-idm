package com.rackspace.idm.api.resource.cloud.v20

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

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

    def "when a MFA user is disabled, they must enable MFA again in order to use MFA"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def authToken = authenticate(user.username)
        def responsePhone = addPhone(authToken, user)
        cloud20.updateMultiFactorSettings(authToken, user.id, settings)

        when: "authenticate with MFA"
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)
        String wwwHeader = authResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234")

        then:
        mfaAuthResponse.status == 200

        when: "disable the user and try to authenticate"
        user.enabled = false
        utils.updateUser(user)
        authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then:
        authResponse.status == 403

        when: "re-enable the user and try to authenticate (MFA should no longer be enabled for this user)"
        user.enabled = true
        utils.updateUser(user)
        def userByIdResponse = cloud20.getUserById(utils.getServiceAdminToken(), user.id)
        authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then:
        utils.checkUserMFAFlag(userByIdResponse, false)
        authResponse.status == 200

        when: "re-enable MFA on the user and try to authenticate"
        authToken = authenticate(user.username)
        cloud20.updateMultiFactorSettings(authToken, user.id, settings)
        authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)

        then:
        authResponse.status == 401

        when: "complete the second step of MFA authentication"
        wwwHeader = authResponse.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234")

        then:
        mfaAuthResponse.status == 200

        cleanup:
        deleteUserQuietly(user)
    }

    def "when a MFA user's account is disabled, their Duo profile is removed"() {
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

        then:
        Mockito.verify(userManagement.mock, Mockito.atLeastOnce()).deleteUserById(Constants.MFA_DEFAULT_USER_PROVIDER_ID)

        cleanup:
        deleteUserQuietly(user)
        mobilePhoneRepository.deleteMobilePhone(mobilePhoneRepository.getById(responsePhone.id))

    }

    def "when a MFA user is disabled, then the user's MFA status shows as disabled"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)

        when:
        def userByIdResponse = cloud20.getUserById(utils.getServiceAdminToken(), user.id)

        then:
        utils.checkUserMFAFlag(userByIdResponse, true)

        when:
        user.enabled = false
        utils.updateUser(user)
        userByIdResponse = cloud20.getUserById(utils.getServiceAdminToken(), user.id)

        then:
        utils.checkUserMFAFlag(userByIdResponse, false)

        cleanup:
        deleteUserQuietly(user)
        mobilePhoneRepository.deleteMobilePhone(mobilePhoneRepository.getById(responsePhone.id))
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
