package com.rackspace.idm.multifactor.service


import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService
import com.rackspace.identity.multifactor.providers.ProviderPhone
import com.rackspace.identity.multifactor.providers.ProviderUser
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.MobilePhone
import org.apache.commons.collections.CollectionUtils
import spock.lang.Shared
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
        mockScopeAccessService(service)
        mockAtomHopperClient(service)
        mockEmailClient(service)
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

    def "when enable mfa appropriate tokens are expired on user"() {
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = "id"
            it.externalMultiFactorUserId = null
            it.multifactorEnabled = false
            it.multiFactorMobilePhoneRsId = "id"
            it.multiFactorDeviceVerified = true
            it
        }

        def MobilePhone phone = entityFactory.createMobilePhone()

        List<List<String>> EXPECTED_AUTHENTICATEDBY_LIST_TO_NOT_REVOKE = Arrays.asList(
                Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY, GlobalConstants.AUTHENTICATED_BY_PASSCODE)
                , Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD, GlobalConstants.AUTHENTICATED_BY_PASSCODE)
                , Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY)
        );

        def expirationExceptions

        def mfaSettings = v2Factory.createMultiFactorSettings(true, false)

        when: "enable mfa"
        service.updateMultiFactorSettings(user.id, mfaSettings)

        then: "appropriate tokens are requested to NOT be expired"
        userService.checkAndGetUserById(user.id) >> user
        mobilePhoneDao.getById(_) >> phone
        multiFactorUserManagement.createUser(_) >> new ProviderUser() {
            @Override
            String getProviderId() {
                return "123"
            }
        }
        multiFactorUserManagement.linkMobilePhoneToUser(_, _) >> new ProviderPhone() {
            @Override
            String getProviderId() {
                return "1234"
            }

            @Override
            String getTelephoneNumber() {
                return "1234"
            }
        }

        1 * scopeAccessService.expireAllTokensExceptTypeForEndUser(user, _, false) >> { arguments -> expirationExceptions=arguments[1]}
        expirationExceptions instanceof List
        CollectionUtils.isEqualCollection(expirationExceptions, EXPECTED_AUTHENTICATEDBY_LIST_TO_NOT_REVOKE)
    }
}
