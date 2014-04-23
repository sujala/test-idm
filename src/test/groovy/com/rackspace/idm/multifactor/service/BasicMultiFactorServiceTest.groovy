package com.rackspace.idm.multifactor.service

import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.idm.domain.dao.MobilePhoneDao
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.RootServiceTest


class BasicMultiFactorServiceTest extends RootServiceTest {

    @Shared BasicMultiFactorService service

    @Shared UserManagement multiFactorUserManagement;
    @Shared MultiFactorAuthenticationService multiFactorAuthenticationService;
    @Shared MobilePhoneVerification multiFactorMobilePhoneVerification;

    def setupSpec() {
        service = new BasicMultiFactorService()
    }

    def setup() {
        multiFactorUserManagement = Mock()
        multiFactorAuthenticationService = Mock()
        multiFactorMobilePhoneVerification = Mock()

        mockMobilePhoneRepository(service)
        mockUserService(service)
        service.multiFactorAuthenticationService = multiFactorAuthenticationService
        service.userManagement = multiFactorUserManagement
        service.mobilePhoneVerification = multiFactorMobilePhoneVerification
    }

    def "listing mobile phones returns empty list if not defined"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = null
            it
        }

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        result != null
        result.size() == 0
    }

    def "listing mobile phones returns device if defined"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it
        }
        def mobilePhone = entityFactory.createMobilePhone()

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        1 * mobilePhoneDao.getById(_) >> mobilePhone
        result != null
        result.size() == 1
        result.get(0).telephoneNumber == mobilePhone.telephoneNumber
    }

    def "listing mobile phones returns empty list even if directory inconsistent"() {
        given:
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it
        }
        def mobilePhone = entityFactory.createMobilePhone()

        when:
        def result = service.getMobilePhonesForUser(user)

        then:
        1 * mobilePhoneDao.getById(_) >> null
        result != null
        result.size() == 0
    }

    @Unroll("unlock mfa - mfa unlock service called #unlockUserCallCount time(s) when user mfa status is #mfaEnabled; mfaSettingsUnlock: #mfaSettingsUnlock")
    def "unlock mfa - mfa service called to unlock when user has external id regardless of mfa enabled setting"() {
        def providerUserId = "extUserId"
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it.externalMultiFactorUserId = providerUserId
            it.multifactorEnabled = userMfaEnabledState
            it.multiFactorMobilePhoneRsId = "id"
            it.multiFactorDeviceVerified = true
            it
        }
        //set enable to whatever it is on user so a state change is not triggered.
        def mfaSettings = v2Factory.createMultiFactorSettings(userMfaEnabledState, mfaSettingsUnlock)

        when:
        service.updateMultiFactorSettings(user.id, mfaSettings)

        then: "mfa sdk called to unlock user with providerUserId"
        userService.checkAndGetUserById(user.id) >> user
        unlockUserCallCount * multiFactorUserManagement.unlockUser(providerUserId)

        where:
        userMfaEnabledState | mfaSettingsUnlock | unlockUserCallCount
        true                | true              | 1
        true                | true              | 1
        true                | true              | 1

        false                | true              | 1
        false                | true              | 1
        false                | true              | 1

        null                | true              | 1
        null                | true              | 1
        null                | true              | 1
    }

}
