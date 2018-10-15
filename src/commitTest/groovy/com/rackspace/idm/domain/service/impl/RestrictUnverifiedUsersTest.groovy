package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import org.opensaml.core.config.InitializationService
import spock.lang.Shared

import testHelpers.IdmAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.UriInfo

class RestrictUnverifiedUsersTest extends RootServiceTest {

    @Shared
    DefaultMultiFactorCloud20Service serviceMfa

    def setupSpec() {
        InitializationService.initialize()
        serviceMfa = new DefaultMultiFactorCloud20Service()
    }

    def setup() {
        mockConfiguration(serviceMfa)
        mockUserService(serviceMfa)
        mockMultiFactorService(serviceMfa)
        mockExceptionHandler(serviceMfa)
        mockPhoneCoverterCloudV20(serviceMfa)
        mockOTPDeviceConverterCloudV20(serviceMfa)
        mockScopeAccessService(serviceMfa)
        mockPrecedenceValidator(serviceMfa)
        mockRequestContextHolder(serviceMfa)
        mockAuthorizationService(serviceMfa)
        mockApplicationService(serviceMfa)
    }

    def "restrict unverified user for endpoint v2.0 MFA Add phone for user"() {
        given:
        allowAccess()
        User unverifiedUser = entityFactory.createUnverifiedUser()
        MobilePhone mobilePhone = new MobilePhone()
        UriInfo uriInfo = Mock(UriInfo)
        requestContextHolder.checkAndGetTargetUser("786") >> unverifiedUser

        when: "v2.0 MFA Add phone for user - endpoint is invoked with unverified user"
        serviceMfa.addPhoneToUser(uriInfo, authToken, "786", mobilePhone)

        then: "403 exception with error message is thrown"
        1 * exceptionHandler.exceptionResponse(_) >> { args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
        }
    }

    def "restrict unverified user for endpoint v2.0 MFA Add OTP device for user"() {
        given:
        allowAccess()
        User unverifiedUser = entityFactory.createUnverifiedUser()
        OTPDevice otpDevice = new OTPDevice()
        UriInfo uriInfo = Mock(UriInfo)
        requestContextHolder.checkAndGetTargetUser("786") >> unverifiedUser

        when: "v2.0 MFA Add OTP device for user - endpoint is invoked with unverified user"
        serviceMfa.addOTPDeviceToUser(uriInfo, authToken, "786", otpDevice)

        then: "403 exception with error message is thrown"
        1 * exceptionHandler.exceptionResponse(_) >> { args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
        }
    }

    def "restrict unverified user for endpoint v2.0 MFA Update user settings with locking"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        User target = entityFactory.createUnverifiedUser()
        ClientRole callerRole = entityFactory.createClientRole(IdentityUserTypeEnum.DEFAULT_USER.name())
        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings()
        settings.setUnlock(true)

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> IdentityUserTypeEnum.DEFAULT_USER

        when: "v2.0 MFA Update user settings - endpoint is invoked with unverified user"
        serviceMfa.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then: "403 exception with error message is thrown"
        1 * exceptionHandler.exceptionResponse(_) >> { args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
        }
    }

    def "restrict unverified user for endpoint v2.0 MFA Update user settings with enabling"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        User target = entityFactory.createUnverifiedUser()
        ClientRole callerRole = entityFactory.createClientRole(IdentityUserTypeEnum.DEFAULT_USER.name())
        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings()
        settings.setEnabled(true)

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> IdentityUserTypeEnum.DEFAULT_USER

        when: "v2.0 MFA Update user settings - endpoint is invoked with unverified user"
        serviceMfa.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then: "403 exception with error message is thrown"
        1 * exceptionHandler.exceptionResponse(_) >> { args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
        }
    }

    def "restrict unverified user for endpoint v2.0 MFA Update user settings with enforcement"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        User target = entityFactory.createUnverifiedUser()
        ClientRole callerRole = entityFactory.createClientRole(IdentityUserTypeEnum.SERVICE_ADMIN.name())
        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = new MultiFactor()
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> IdentityUserTypeEnum.SERVICE_ADMIN

        when: "v2.0 MFA Update user settings - endpoint is invoked to set only enforcement level with unverified user"
        serviceMfa.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then: "403 exception with error message should not be thrown"
        0 * exceptionHandler.exceptionResponse(_) >> { args ->
            IdmAssert.assertIdmExceptionWithMessagePattern(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.RESTRICT_UNVERIFIED_USER_MESSAGE)
        }
    }
}
