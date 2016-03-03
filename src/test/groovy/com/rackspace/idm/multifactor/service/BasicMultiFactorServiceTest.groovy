package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.identity.multifactor.providers.MobilePhoneVerification
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService
import com.rackspace.identity.multifactor.providers.ProviderPhone
import com.rackspace.identity.multifactor.providers.ProviderUser
import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil
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
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ErrorCodeIdmException
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.idm.util.BypassDeviceCreationResult
import com.rackspace.idm.util.BypassHelper
import com.rackspace.idm.util.OTPHelper
import org.apache.commons.configuration.Configuration
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

        config = Mock(Configuration)
        service.globalConfig = config

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

    @Unroll
    def "adding new mobile phone deletes old phone duo profile if last linked user and delete duo phone feature enabled. delete duo enabled: #deleteDuoPhone "() {
        given:
        Phonenumber.PhoneNumber telephoneNumber1 = PhoneNumberGenerator.randomUSNumber();
        Phonenumber.PhoneNumber telephoneNumber2 = PhoneNumberGenerator.randomUSNumber();
        reloadableConfig.getFeatureDeleteUnusedDuoPhones() >> deleteDuoPhone
        config.getBoolean(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, false) >> true;

        def mobilePhone1 = entityFactory.createMobilePhone().with {
            it.id = 1
            it.telephoneNumber = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(telephoneNumber1)
            it.externalMultiFactorPhoneId = "duo"
            it
        }
        def mobilePhone2 = entityFactory.createMobilePhone().with {
            it.id = 2
            it.telephoneNumber = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(telephoneNumber2)
            it
        }
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = mobilePhone1.id
            it.multifactorEnabled = false
            it
        }
        mobilePhoneDao.addMobilePhone(_) >> {throw new DuplicateException()}
        userService.checkAndGetUserById(user.id) >> user
        mobilePhoneDao.getById(mobilePhone1.id) >> mobilePhone1
        mobilePhoneDao.getByTelephoneNumber(mobilePhone1.telephoneNumber) >> mobilePhone1
        mobilePhoneDao.getByTelephoneNumber(mobilePhone2.telephoneNumber) >> mobilePhone2

        def result = service.addPhoneToUser(user.id, telephoneNumber1)

        when:
        def addSecondReponse = service.addPhoneToUser(user.id, telephoneNumber2)

        then:
        1 * userService.updateUserForMultiFactor(user)
        mobilePhoneDao.getById(mobilePhone2.id) >> mobilePhone2

        user.multiFactorMobilePhoneRsId == mobilePhone2.id

        where:
        deleteDuoPhone | _
        true    | _
        false   | _
    }

    @Unroll
    def "adding new mobile phone does not try to delete old phone duo profile if old phone does not have duo profile"() {
        given:
        Phonenumber.PhoneNumber telephoneNumber1 = PhoneNumberGenerator.randomUSNumber();
        Phonenumber.PhoneNumber telephoneNumber2 = PhoneNumberGenerator.randomUSNumber();
        reloadableConfig.getFeatureDeleteUnusedDuoPhones() >> true
        config.getBoolean(BasicMultiFactorService.CONFIG_PROP_PHONE_MEMBERSHIP_ENABLED, false) >> true;

        def mobilePhone1 = entityFactory.createMobilePhone().with {
            it.id = 1
            it.telephoneNumber = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(telephoneNumber1)
            it.externalMultiFactorPhoneId = null
            it
        }
        def mobilePhone2 = entityFactory.createMobilePhone().with {
            it.id = 2
            it.telephoneNumber = IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(telephoneNumber2)
            it
        }
        def user = entityFactory.createUser().with {
            it.multiFactorMobilePhoneRsId = mobilePhone1.id
            it.multifactorEnabled = false
            it
        }
        mobilePhoneDao.addMobilePhone(_) >> {throw new DuplicateException()}
        userService.checkAndGetUserById(user.id) >> user
        mobilePhoneDao.getById(mobilePhone1.id) >> mobilePhone1
        mobilePhoneDao.getByTelephoneNumber(mobilePhone1.telephoneNumber) >> mobilePhone1
        mobilePhoneDao.getByTelephoneNumber(mobilePhone2.telephoneNumber) >> mobilePhone2

        def result = service.addPhoneToUser(user.id, telephoneNumber1)

        when:
        def addSecondReponse = service.addPhoneToUser(user.id, telephoneNumber2)

        then:
        1 * userService.updateUserForMultiFactor(user)
        mobilePhoneDao.getById(mobilePhone2.id) >> mobilePhone2

        user.multiFactorMobilePhoneRsId == mobilePhone2.id
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
        def mfaSettings = v2Factory.createMultiFactorSettings(null, mfaSettingsUnlock)

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

        List expirationExceptions

        def mfaSettings = v2Factory.createMultiFactorSettings(true)

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

        1 * tokenRevocationService.revokeTokensForEndUser(user, _) >> { arguments -> expirationExceptions=arguments[1]}
        expirationExceptions.size() == 2
        expirationExceptions.find { it.matches(AuthenticatedByMethodGroup.PASSWORD); it} != null
        expirationExceptions.find { it.matches(AuthenticatedByMethodGroup.EMAIL); it} != null
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
        1 * tokenRevocationService.revokeTokensForEndUser(normalUser, _)
        0 * tokenRevocationService.revokeTokensForEndUser(userMfaOptional, _)
        1 * tokenRevocationService.revokeTokensForEndUser(userMfaRequired, _)
        0 * tokenRevocationService.revokeTokensForEndUser(userWithMfaEnabled, _)
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
        1 * mockOTPDeviceDao.deleteOTPDevice(otpDevice)
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
        0 * mockOTPDeviceDao.deleteOTPDevice(otpDevice)

        where:
        deviceVerified  |  mfaEnabled   |  factorType           | verifiedOTPCount | verifiedPhone
        true            |  true         |  FactorTypeEnum.OTP   | 1                | false
        true            |  true         |  FactorTypeEnum.OTP   | 1                | true
    }

    /**
     * Local bypass codes can be used for both SMS and OTP
     * @return
     */
    @Unroll
    def "test local bypass code logic for type=#type"() {
        setup:
        staticConfig.getBypassDefaultNumber() >> BigInteger.ONE
        staticConfig.getBypassMaximumNumber() >> BigInteger.TEN
        reloadableConfig.getFeatureLocalMultifactorBypassEnabled() >> true
        reloadableConfig.getLocalBypassCodeIterationCount() >> 1000
        def setOfCodes = ["1", "2"] as Set

        User user = entityFactory.createUser().with {
            it.multifactorEnabled = true
            it.multiFactorType = type.value()
            return it
        }
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = true
            return it
        }
        BypassDeviceCreationResult bypassDeviceCreationResult = new BypassDeviceCreationResult(new BypassDevice(), setOfCodes)

        userService.checkAndGetUserById(user.id) >> user
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> 1

        when: "try to add bypass codes"
        def codes = service.getSelfServiceBypassCodes(user, 120, 2)

        then:
        1 * mockBypassDeviceDao.deleteAllBypassDevices(user)
        1 * mockBypassDeviceDao.addBypassDevice(user, _)
        0 * multiFactorUserManagement.getBypassCodes(_, _, _) //local bypass codes don't use Duo regardless of type
        1 * mockBypassHelper.createBypassDevice(_, _) >> bypassDeviceCreationResult

        codes.size() == 2
        codes == setOfCodes.toList()

        where:
        type | _
        FactorTypeEnum.OTP | _
        FactorTypeEnum.SMS | _
    }

    /**
     * Duo bypass codes are unavailable for OTP, but can be used for SMS
     * @return
     */
    @Unroll
    def "test duo bypass code logic for type=#type"() {
        setup:
        staticConfig.getBypassDefaultNumber() >> BigInteger.ONE
        staticConfig.getBypassMaximumNumber() >> BigInteger.TEN
        reloadableConfig.getFeatureLocalMultifactorBypassEnabled() >> false

        def arrayOfCodes = ["1", "2"]
        List<String> fakeCodes = arrayOfCodes.asList()

        User user = entityFactory.createUser().with {
            it.multifactorEnabled = true
            it.multiFactorType = type.value()
            return it
        }
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = true
            return it
        }

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
        0 * mockBypassDeviceDao.deleteAllBypassDevices(user)
        0 * mockBypassDeviceDao.addBypassDevice(user, _)
        interaction {
            def userManagementInteractionCount = 0
            if (type == FactorTypeEnum.SMS) {
                userManagementInteractionCount = 1
            }
            userManagementInteractionCount * multiFactorUserManagement.getBypassCodes(_, _, _) >> arrayOfCodes
        }

        if (type == FactorTypeEnum.SMS) {
            assert codes == fakeCodes
            assert codes.size() == 2
        } else if (type == FactorTypeEnum.OTP) {
            assert exception instanceof BadRequestException
        }

        where:
        type | _
        FactorTypeEnum.OTP | _
        FactorTypeEnum.SMS | _
    }

    def "test multi-factor setting validation for 'disable multi-factor'"() {
        given:
        def userId = "123"
        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = true
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        MultiFactor settings = new MultiFactor();
        settings.setEnabled(false);

        when: // cannot unlock user
        settings.setUnlock(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUnlock(null)

        when: // cannot set user enforcement level
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUserMultiFactorEnforcementLevel(null)
    }

    def "test multi-factor setting validation for 'unlock multi-factor'"() {
        given:
        def userId = "123"
        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = true
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        MultiFactor settings = new MultiFactor();
        settings.setUnlock(true);

        when: // cannot enable multi-factor
        settings.setEnabled(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setEnabled(null)

        when: // cannot set user enforcement level
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUserMultiFactorEnforcementLevel(null)

        when: // cannot set factor type
        settings.setFactorType(FactorTypeEnum.SMS)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
    }

    def "test multi-factor setting validation for 'user enforcement level'"() {
        given:
        def userId = "123"
        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = true
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        MultiFactor settings = new MultiFactor();
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.REQUIRED);

        when: // cannot enable multi-factor
        settings.setEnabled(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setEnabled(null)

        when: // cannot unlock multi-factor
        settings.setUnlock(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUnlock(null)

        when: // cannot set factor type
        settings.setFactorType(FactorTypeEnum.SMS)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
    }

    def "test multi-factor setting validation for 'enable multi-factor'"() {
        given:
        def userId = "123"
        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = false
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        MultiFactor settings = new MultiFactor();
        settings.setEnabled(true);

        when: // cannot unlock multi-factor
        settings.setUnlock(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUnlock(null)

        when: // cannot set user enforcement level
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUserMultiFactorEnforcementLevel(null)

        when:
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = true
            return it
        }
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> 1
        user.setMultiFactorDeviceVerified(true)
        user.setMultiFactorMobilePhoneRsId('1234')
        service.updateMultiFactorSettings(userId, settings)

        then: // factor type is required
        thrown(BadRequestException)
    }

    def "test multi-factor setting validation for 'set factor type'"() {
        given:
        def userId = "123"
        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = true
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        User userDisabledMfa = entityFactory.createUser().with {
            it.id = "disabled"
            it.multifactorEnabled = false
            return it
        }
        userService.checkAndGetUserById(user.id) >> user
        userService.checkAndGetUserById(userDisabledMfa.id) >> userDisabledMfa

        MultiFactor settings = new MultiFactor();
        settings.setFactorType(FactorTypeEnum.OTP);

        when: "try to update factor type on disable MFA user"
        service.updateMultiFactorSettings(userDisabledMfa.id, settings)

        then:
        thrown(BadRequestException)

        when: // cannot unlock multi-factor
        settings.setUnlock(true)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUnlock(null)

        when: // cannot set user enforcement level
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)
        service.updateMultiFactorSettings(userId, settings)

        then:
        thrown(BadRequestException)
        settings.setUserMultiFactorEnforcementLevel(null)

        when:
        OTPDevice otpDevice = new OTPDevice().with {
            it.id = "1"
            it.multiFactorDeviceVerified = true
            return it
        }
        mockOTPDeviceDao.getOTPDeviceByParentAndId(user, otpDevice.id) >> otpDevice
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> 1
        settings.setFactorType(FactorTypeEnum.SMS);
        settings.enabled = true
        service.updateMultiFactorSettings(userDisabledMfa.id, settings)

        then: // cannot set SMS to an OTP user
        thrown(BadRequestException)

        when:
        mockOTPDeviceDao = Mock()
        service.otpDeviceDao = mockOTPDeviceDao
        mockOTPDeviceDao.countVerifiedOTPDevicesByParent(user) >> 0
        user.setMultiFactorDeviceVerified(true)
        user.setMultiFactorMobilePhoneRsId('1234')
        settings.setFactorType(FactorTypeEnum.OTP);
        service.updateMultiFactorSettings(userId, settings)

        then: // cannot set OTP to an SMS user
        thrown(BadRequestException)

        when:
        user.setMultifactorEnabled(false)
        settings.setEnabled(null)
        settings.setFactorType(FactorTypeEnum.SMS)
        service.updateMultiFactorSettings(userId, settings)

        then: // bad request trying to set just factor type and the user has multi-factor disabled
        thrown(BadRequestException)
    }

    @Unroll
    def "test auto-unlock on Duo when local locking is: #localLocking"() {
        given:
        def userId = "123"
        def phoneId = "234"
        def passcode = "123456"
        def externalId = "345"

        User user = entityFactory.createUser().with {
            it.id = userId
            it.multifactorEnabled = true
            it.multiFactorType = FactorTypeEnum.SMS.value()
            it.multiFactorMobilePhoneRsId = phoneId
            it.multiFactorDeviceVerified = true
            it.externalMultiFactorUserId = externalId
            return it
        }
        userService.checkAndGetUserById(userId) >> user

        MobilePhone phone = entityFactory.createMobilePhoneWithId(phoneId).with {
            it.externalMultiFactorPhoneId = externalId
            return it
        }
        mobilePhoneDao.getById(phoneId) >> phone

        identityConfig.getReloadableConfig().getFeatureMultifactorLockingEnabled() >> localLocking
        mockBypassDeviceDao.getAllBypassDevices(user) >> []

        def responses = [
                new GenericMfaAuthenticationResponse(
                        MfaAuthenticationDecision.DENY,
                        MfaAuthenticationDecisionReason.LOCKEDOUT,
                        "", new Object()),
                new GenericMfaAuthenticationResponse(
                        MfaAuthenticationDecision.ALLOW,
                        MfaAuthenticationDecisionReason.ALLOW,
                        "", new Object())
        ]

        when:
        def response = service.verifyPasscode(userId, passcode)

        then:
        duoCalls * multiFactorAuthenticationService.verifyPasscodeChallenge(externalId, externalId, passcode) >>> responses
        unlockCalls * multiFactorUserManagement.unlockUser(externalId);
        response != null
        response == responses[responseIndex]

        where:
        localLocking | duoCalls | unlockCalls | responseIndex
        true         | 2        | 1           | 1
        false        | 1        | 0           | 0
    }

}
