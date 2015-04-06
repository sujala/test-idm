package com.rackspace.idm.multifactor.service

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService
import com.rackspace.identity.multifactor.providers.ProviderPhone
import com.rackspace.identity.multifactor.providers.ProviderUser
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.BypassDeviceDao
import com.rackspace.idm.domain.dao.OTPDeviceDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.BypassDevice
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.OTPDevice
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ErrorCodeIdmException
import com.rackspace.idm.util.BypassHelper
import com.rackspace.idm.util.OTPHelper
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest


class BasicMultiFactorServiceTest extends RootServiceTest {
    def betaRoleName = "mfaBetaRoleName"
    def rolesWithoutMfaBetaRole
    def rolesWithMfaBetaRole

    @Shared BasicMultiFactorService service

    @Shared UserManagement multiFactorUserManagement;
    @Shared MultiFactorAuthenticationService multiFactorAuthenticationService;
    @Shared MobilePhoneVerification multiFactorMobilePhoneVerification;

    @Shared OTPDeviceDao mockOTPDeviceDao
    @Shared BypassDeviceDao mockBypassDeviceDao
    @Shared OTPHelper mockOTPHelper
    @Shared BypassHelper mockBypassHelper

    def setupSpec() {
        service = new BasicMultiFactorService()
    }

    def setup() {
        rolesWithoutMfaBetaRole = []
        rolesWithoutMfaBetaRole << new TenantRole().with {
            it.name = ""
            it
        }
        rolesWithMfaBetaRole = []
        rolesWithMfaBetaRole << new TenantRole().with {
            it.name = betaRoleName
            it
        }

        multiFactorUserManagement = Mock()
        multiFactorAuthenticationService = Mock()
        multiFactorMobilePhoneVerification = Mock()
        mockOTPDeviceDao = Mock()
        mockBypassDeviceDao = Mock()
        mockOTPHelper = Mock()
        mockBypassHelper = Mock()

        mockMobilePhoneRepository(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockAtomHopperClient(service)
        mockEmailClient(service)
        mockDomainService(service)
        mockIdentityUserService(service)
        mockTenantService(service)
        mockTokenRevocationService(service)
        mockIdentityConfig(service)

        service.multiFactorAuthenticationService = multiFactorAuthenticationService
        service.userManagement = multiFactorUserManagement
        service.mobilePhoneVerification = multiFactorMobilePhoneVerification
        service.otpDeviceDao = mockOTPDeviceDao
        service.bypassDeviceDao = mockBypassDeviceDao
        service.otpHelper = mockOTPHelper
        service.bypassHelper = mockBypassHelper
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

        1 * tokenRevocationService.revokeTokensForBaseUser(user, _) >> { arguments -> expirationExceptions=arguments[1]}
        expirationExceptions.size() == 1
        AuthenticatedByMethodGroup.PASSWORD.matches(expirationExceptions.get(0))
    }

    def "updateMultiFactorDomainSettings: appropriate user tokens are revoked when set to required"() {
        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings(DomainMultiFactorEnforcementLevelEnum.REQUIRED)

        String requiredDomainId = "requiredDomainId"
        Domain requiredDomain = entityFactory.createDomain(requiredDomainId).with {it.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED; return it}

        String optionalDomainId = "optionalDomainId"
        Domain optionalDomain = entityFactory.createDomain(optionalDomainId).with {it.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL; return it}

        User normalUser = entityFactory.createUser()
        User userMfaOptional = entityFactory.createUser().with {it.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL; return it}
        User userMfaRequired = entityFactory.createUser().with {it.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED; return it}
        User userWithMfaEnabled = entityFactory.createUser().with {it.multifactorEnabled = true; return it}

        staticConfig.getMultiFactorServicesEnabled() >> true

        when: "update domain to required"
        service.updateMultiFactorDomainSettings(optionalDomainId, settings)

        then:
        1 * domainService.checkAndGetDomain(optionalDomainId) >> optionalDomain //depend on this verifying domain exists
        1 * domainService.updateDomain(optionalDomain)
        1 * identityUserService.getProvisionedUsersByDomainId(optionalDomainId) >> [normalUser, userMfaOptional, userMfaRequired, userWithMfaEnabled ]
        1 * tokenRevocationService.revokeTokensForBaseUser(normalUser, _)
        0 * tokenRevocationService.revokeTokensForBaseUser(userMfaOptional, _)
        1 * tokenRevocationService.revokeTokensForBaseUser(userMfaRequired, _)
        0 * tokenRevocationService.revokeTokensForBaseUser(userWithMfaEnabled, _)
    }

    def "updateMultiFactorDomainSettings: when setting to current enforcement value the domain is not updated"() {
        String requiredDomainId = "requiredDomainId"
        String optionalDomainId = "optionalDomainId"
        Domain requiredDomain = entityFactory.createDomain(requiredDomainId).with {it.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED; return it}
        Domain optionalDomain = entityFactory.createDomain(optionalDomainId).with {it.domainMultiFactorEnforcementLevel = GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL; return it}

        when:
        MultiFactorDomain requiredSettings = v2Factory.createMultiFactorDomainSettings(DomainMultiFactorEnforcementLevelEnum.REQUIRED)
        service.updateMultiFactorDomainSettings(requiredDomainId, requiredSettings)

        then:
        domainService.checkAndGetDomain(requiredDomainId) >> requiredDomain
        0 * domainService.updateDomain(requiredDomain)

        when:
        MultiFactorDomain optionalSettings = v2Factory.createMultiFactorDomainSettings(DomainMultiFactorEnforcementLevelEnum.OPTIONAL)
        service.updateMultiFactorDomainSettings(optionalDomainId, optionalSettings)

        then:
        domainService.checkAndGetDomain(optionalDomainId) >> optionalDomain
        0 * domainService.updateDomain(optionalDomain)
    }

    def "isMultiFactorEnabled checks multifactor feature flag"() {
        when:
        service.isMultiFactorEnabled()

        then:
        1 * staticConfig.getMultiFactorServicesEnabled()
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are disabled"() {
        when:
        def response = service.isMultiFactorEnabled()

        then:
        1 * staticConfig.getMultiFactorServicesEnabled() >> false
        response == false
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are enabled and in beta and user does not have MFA beta role"() {
        def user = new User()

        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * staticConfig.getMultiFactorServicesEnabled() >> true
        1 * staticConfig.getMultiFactorBetaEnabled() >> true
        1 * staticConfig.getMultiFactorBetaRoleName() >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> rolesWithoutMfaBetaRole
        response == false
    }

    def "isMultiFactorEnabledForUser returns true if multifactor services are enabled and in beta and user has MFA beta role"() {
        def user = new User()

        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * staticConfig.getMultiFactorServicesEnabled() >> true
        1 * staticConfig.getMultiFactorBetaEnabled() >> true
        1 * staticConfig.getMultiFactorBetaRoleName() >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> rolesWithMfaBetaRole
        response == true
    }

    /**
     * This tests deleting an OTP device from a user
     *
     * @return
     */
    @Unroll
    def "Delete an OTP device allowed when (deviceVerified: #deviceVerified | mfaEnabled: #mfaEnabled | factorType: #factorType | verifiedOTPCount: #verifiedOTPCount | verifiedPhone: #verifiedPhone)"() {
        setup:
        User user = entityFactory.createUser().with {
            it.multifactorEnabled = mfaEnabled
            if (factorType != null) {
                it.multiFactorType = factorType.value()
            }
            return it
        }
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = deviceVerified
            return it
        }

        userService.checkAndGetUserById(user.id) >> user

        when: "try to delete device"
        service.deleteOTPDeviceForUser(user.id, otpDevice.id)

        then:
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> verifiedOTPCount
        1 * mockOTPDeviceDao.deleteObject(otpDevice)
        notThrown(ErrorCodeIdmException)

        where:
        deviceVerified  |  mfaEnabled   |  factorType           | verifiedOTPCount | verifiedPhone
        false           |  true         |  FactorTypeEnum.OTP   | 1                | true
        false           |  true         |  FactorTypeEnum.OTP   | 2                | true
        false           |  true         |  FactorTypeEnum.SMS   | 0                | true
        false           |  true         |  FactorTypeEnum.SMS   | 1                | true
        false           |  false        |  null                 | 0                | false
        true            |  false        |  null                 | 0                | false
        true            |  true         |  FactorTypeEnum.OTP   | 2                | false
        true            |  true         |  FactorTypeEnum.SMS   | 1                | true
    }

    /**
     * can't delete last OTP device when MFA enabled and set to OTP
     *
     * @return
     */
    @Unroll
    def "Delete an OTP device not allowed when (deviceVerified: #deviceVerified | mfaEnabled: #mfaEnabled | factorType: #factorType | verifiedOTPCount: #verifiedOTPCount | verifiedPhone: #verifiedPhone)"() {
        setup:
        User user = entityFactory.createUser().with {
            it.multifactorEnabled = mfaEnabled
            if (factorType != null) {
                it.multiFactorType = factorType.value()
            }
            return it
        }
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = deviceVerified
            return it
        }

        userService.checkAndGetUserById(user.id) >> user
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> verifiedOTPCount

        when: "try to delete device"
        service.deleteOTPDeviceForUser(user.id, otpDevice.id)

        then:
        ErrorCodeIdmException ex = thrown()
        ex.errorCode == ErrorCodes.ERROR_CODE_DELETE_OTP_DEVICE_FORBIDDEN_STATE
        0 * mockOTPDeviceDao.deleteObject(otpDevice)

        where:
        deviceVerified  |  mfaEnabled   |  factorType           | verifiedOTPCount | verifiedPhone
        true            |  true         |  FactorTypeEnum.OTP   | 1                | false
        true            |  true         |  FactorTypeEnum.OTP   | 1                | true
    }

    @Unroll
    def "test bypass code logic: local=#local, type=#type"() {
        setup:
        staticConfig.getBypassDefaultNumber() >> BigInteger.ONE
        staticConfig.getBypassMaximumNumber() >> BigInteger.TEN
        reloadableConfig.getFeatureLocalMultifactorBypassEnabled() >> local

        User user = entityFactory.createUser().with {
            it.multifactorEnabled = true
            it.multiFactorType = type
            return it
        }
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = true
            return it
        }
        BypassDevice bypassDevice = new BypassDevice()
        def arrayOfCodes = ["1", "2"]
        List<String> fakeCodes = arrayOfCodes.asList()

        userService.checkAndGetUserById(user.id) >> user
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> 1

        when: "try to add bypass codes"
        def exception = null
        def codes = null
        try {
            codes = service.getSelfServiceBypassCodes(user, 120, 2)
        } catch (Exception e) {
            exception = e
        }

        then:
        if (local) {
            1 * mockBypassHelper.createBypassDevice(2, 120) >> bypassDevice
            1 * mockBypassDeviceDao.deleteAllBypassDevices(user)
            1 * mockBypassDeviceDao.addBypassDevice(user, bypassDevice)
            1 * mockBypassHelper.calculateBypassCodes(bypassDevice) >> fakeCodes
        } else if (type == FactorTypeEnum.SMS.value()) {
            1 * multiFactorUserManagement.getBypassCodes(_, _, _) >> arrayOfCodes
        }

        then:
        if (exception == null) {
            codes.size() == 2
            codes == fakeCodes
        } else {
            exception instanceof BadRequestException
        }

        where:
        local | type
        true  | FactorTypeEnum.OTP.value()
        false | FactorTypeEnum.OTP.value()
        false | FactorTypeEnum.SMS.value()
    }

}
