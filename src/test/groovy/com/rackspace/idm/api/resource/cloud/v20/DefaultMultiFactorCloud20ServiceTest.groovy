package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.*
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

class DefaultMultiFactorCloud20ServiceTest extends RootServiceTest {
    @Shared DefaultMultiFactorCloud20Service service

    def setupSpec() {
        service = new DefaultMultiFactorCloud20Service()
    }

    def setup() {
        mockConfiguration(service)
        mockUserService(service)
        mockMultiFactorService(service)
        mockExceptionHandler(service)
        mockPhoneCoverterCloudV20(service)
        mockOTPDeviceConverterCloudV20(service)
        mockScopeAccessService(service)
        mockPrecedenceValidator(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockApplicationService(service)
    }

    def "listDevicesForUser validates x-auth-token"() {
        when:
        allowMultiFactorAccess()
        service.listMobilePhoneDevicesForUser(null, "token", null)

        then:
        securityContext.getAndVerifyEffectiveCallerToken(_) >> { throw new NotAuthenticatedException() }
    }

    @Unroll
    def "updateMultiFactorSettings(userEnforcementLevel) default user can not call"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; return it}
        User target = entityFactory.createUser().with{it.id = "target"; return it}
        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())
        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings().with{it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.DEFAULT; return it}
        def capturedException

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType

        when:
        service.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then:
        //capture the exception that was thrown
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}
        assert capturedException instanceof ForbiddenException

        where:
        callerUserIdentityRoleType          |   targetUserIdentityRoleType
        IdentityUserTypeEnum.DEFAULT_USER  |   IdentityUserTypeEnum.SERVICE_ADMIN
        IdentityUserTypeEnum.DEFAULT_USER  |   IdentityUserTypeEnum.IDENTITY_ADMIN
        IdentityUserTypeEnum.DEFAULT_USER  |   IdentityUserTypeEnum.USER_ADMIN
        IdentityUserTypeEnum.DEFAULT_USER  |   IdentityUserTypeEnum.USER_MANAGER
        IdentityUserTypeEnum.DEFAULT_USER  |   IdentityUserTypeEnum.DEFAULT_USER
    }

    @Unroll
    def "updateMultiFactorSettings(userEnforcementLevel) #callerUserIdentityRoleType calls appropriate auth logic when called against #targetUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; return it}
        User target = entityFactory.createUser().with{it.id = "target"; return it}

        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())
        ClientRole targetRole = entityFactory.createClientRole(targetUserIdentityRoleType.name())

        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings().with{it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.DEFAULT; return it}
        def capturedException

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType
        applicationService.getUserIdentityRole(target) >> targetRole
        authorizationService.getIdentityTypeRoleAsEnum(targetRole) >> targetUserIdentityRoleType
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when:
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then:
        /*
        Verify correct auth call is made
         */
        if (allowsEqualAccess) {
            1 * precedenceValidator.verifyHasGreaterOrEqualAccess(callerRole, targetRole) >> {if (validatorWouldThrowException) throw new ForbiddenException()}
        } else {
            1 * precedenceValidator.verifyHasGreaterAccess(callerRole, targetRole) >> {if (validatorWouldThrowException) throw new ForbiddenException()}
        }

        /*
        if an exception should be thrown, verify the exception handler is called with forbidden exception. Otherwise, verify a 204 is returned
         */
        if (validatorWouldThrowException) {
            assert capturedException instanceof ForbiddenException
        } else {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        }

        //verify the domain auth logic is called appropriately
        if (verifyDomain) {
            1 * authorizationService.verifyDomain(caller, target)
        } else {
            0 * authorizationService.verifyDomain(_, _)
        }

        where:
        callerUserIdentityRoleType          |   targetUserIdentityRoleType          | allowsEqualAccess | validatorWouldThrowException      | verifyDomain
        IdentityUserTypeEnum.SERVICE_ADMIN  |   IdentityUserTypeEnum.SERVICE_ADMIN  | false             | true                             | false
        IdentityUserTypeEnum.SERVICE_ADMIN  |   IdentityUserTypeEnum.IDENTITY_ADMIN | false             | false                             | false
        IdentityUserTypeEnum.SERVICE_ADMIN  |   IdentityUserTypeEnum.USER_ADMIN     | false             | false                             | false
        IdentityUserTypeEnum.SERVICE_ADMIN  |   IdentityUserTypeEnum.USER_MANAGER   | false             | false                             | false
        IdentityUserTypeEnum.SERVICE_ADMIN  |   IdentityUserTypeEnum.DEFAULT_USER   | false             | false                             | false
        IdentityUserTypeEnum.IDENTITY_ADMIN |   IdentityUserTypeEnum.SERVICE_ADMIN  | false             | true                             | false
        IdentityUserTypeEnum.IDENTITY_ADMIN  |   IdentityUserTypeEnum.IDENTITY_ADMIN | false             | true                            | false
        IdentityUserTypeEnum.IDENTITY_ADMIN  |   IdentityUserTypeEnum.USER_ADMIN     | false             | false                            | false
        IdentityUserTypeEnum.IDENTITY_ADMIN  |   IdentityUserTypeEnum.USER_MANAGER   | false             | false                            | false
        IdentityUserTypeEnum.IDENTITY_ADMIN  |   IdentityUserTypeEnum.DEFAULT_USER   | false             | false                            | false
        IdentityUserTypeEnum.USER_ADMIN  |   IdentityUserTypeEnum.SERVICE_ADMIN     | true              | true                             | false
        IdentityUserTypeEnum.USER_ADMIN  |   IdentityUserTypeEnum.IDENTITY_ADMIN    | true             | true                             | false
        IdentityUserTypeEnum.USER_ADMIN  |   IdentityUserTypeEnum.USER_ADMIN        | true             | false                             | true
        IdentityUserTypeEnum.USER_ADMIN  |   IdentityUserTypeEnum.USER_MANAGER      | true             | false                             | true
        IdentityUserTypeEnum.USER_ADMIN  |   IdentityUserTypeEnum.DEFAULT_USER      | true             | false                             | true
    }

    @Unroll
    def "updateMultiFactorSettings(userEnforcementLevel) userManager allowed to modify #targetUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; return it}
        User target = entityFactory.createUser().with{it.id = "target"; return it}

        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())
        ClientRole targetRole = entityFactory.createClientRole(targetUserIdentityRoleType.name())

        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings().with{it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.DEFAULT; return it}

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType
        applicationService.getUserIdentityRole(target) >> targetRole
        authorizationService.getIdentityTypeRoleAsEnum(targetRole) >> targetUserIdentityRoleType

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when:
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then:
        //calls we care about
        1 * precedenceValidator.hasGreaterOrEqualAccess(callerRole, targetRole) >> validatorResult

        //need to put setting expectations and testing results into separate "if" blocks
        if (allowed) {
            1 * authorizationService.verifyDomain(caller, target)
        }
        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        callerUserIdentityRoleType          |   targetUserIdentityRoleType          | validatorResult   | allowed
        IdentityUserTypeEnum.USER_MANAGER  |   IdentityUserTypeEnum.SERVICE_ADMIN   | false             | false
        IdentityUserTypeEnum.USER_MANAGER  |   IdentityUserTypeEnum.IDENTITY_ADMIN  | false             | false
        IdentityUserTypeEnum.USER_MANAGER  |   IdentityUserTypeEnum.USER_ADMIN      | false             | true
        IdentityUserTypeEnum.USER_MANAGER  |   IdentityUserTypeEnum.USER_MANAGER    | true              | true
        IdentityUserTypeEnum.USER_MANAGER  |   IdentityUserTypeEnum.DEFAULT_USER    | true              | true
    }

    @Unroll
    def "updateMultiFactorSettings(userEnforcementLevel) #callerUserIdentityRoleType should be able to change self (#allowed)"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; return it}
        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())

        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings().with{it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.DEFAULT; return it}

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(caller.id) >> caller
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when: "call on self"
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorSettings(null, callerToken.accessTokenString, caller.id, settings)

        then:
        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        callerUserIdentityRoleType          | allowed
        IdentityUserTypeEnum.SERVICE_ADMIN  | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false
        IdentityUserTypeEnum.USER_ADMIN     | true
        IdentityUserTypeEnum.USER_MANAGER   | true
        IdentityUserTypeEnum.DEFAULT_USER   | false
    }

    @Unroll
    def "updateMultiFactorDomainSettings() - Should #callerUserIdentityRoleType be able to change own domain when user MFA enforcement level set to #userEnforcementLevel? #allowed"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; it.userMultiFactorEnforcementLevel = userEnforcementLevel; return it}
        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())

        UserScopeAccess callerToken = entityFactory.createUserToken()
        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken

        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()

        securityContext.getAndVerifyEffectiveCallerToken(callerToken.accessTokenString) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType
        1 * authorizationService.verifyUserManagedLevelAccess(callerUserIdentityRoleType) >>  {if (!roleIsUserManagerOrHigher) throw new ForbiddenException()}

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when: "update domain"
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorDomainSettings(null, callerToken.accessTokenString, caller.domainId, settings)

        then:
        if (allowed) {
            1 * multiFactorService.updateMultiFactorDomainSettings(caller.domainId, settings)
        }

        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
            assert capturedException == null
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        callerUserIdentityRoleType          | userEnforcementLevel                                          | allowed  | roleIsUserManagerOrHigher
        IdentityUserTypeEnum.SERVICE_ADMIN  | null                                                          | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | null                                                          | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | null                                                          | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | null                                                          | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | null                                                          | false    | false
        IdentityUserTypeEnum.SERVICE_ADMIN  | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | false
        IdentityUserTypeEnum.SERVICE_ADMIN  | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | false
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | true     | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | true     | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | false    | false
    }

    @Unroll
    def "updateMultiFactorDomainSettings() - Should #callerUserIdentityRoleType be able to change OTHER domain when user MFA enforcement level set to #userEnforcementLevel? #allowed"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; it.userMultiFactorEnforcementLevel = userEnforcementLevel; return it}
        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())

        UserScopeAccess callerToken = entityFactory.createUserToken()
        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken

        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()

        securityContext.getAndVerifyEffectiveCallerToken(callerToken.accessTokenString) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType
        1 * authorizationService.verifyUserManagedLevelAccess(callerUserIdentityRoleType) >>  {if (!roleIsUserManagerOrHigher) throw new ForbiddenException()}

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when: "update domain"
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorDomainSettings(null, callerToken.accessTokenString, "otherDomainId", settings)

        then:
        if (allowed) {
            1 * multiFactorService.updateMultiFactorDomainSettings("otherDomainId", settings)
        }

        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
            assert capturedException == null
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        callerUserIdentityRoleType          | userEnforcementLevel                                          | allowed  | roleIsUserManagerOrHigher
        IdentityUserTypeEnum.SERVICE_ADMIN  | null                                                          | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | null                                                          | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | null                                                          | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | null                                                          | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | null                                                          | false    | false
        IdentityUserTypeEnum.SERVICE_ADMIN  | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT   | false    | false
        IdentityUserTypeEnum.SERVICE_ADMIN  | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | true     | true
        IdentityUserTypeEnum.IDENTITY_ADMIN | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | true     | true
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED  | false    | false
        IdentityUserTypeEnum.USER_ADMIN     | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | false     | true
        IdentityUserTypeEnum.USER_MANAGER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | false     | true
        IdentityUserTypeEnum.DEFAULT_USER   | GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL  | false    | false
    }

    def "test create OTP device for non provisioned user is not allowed"() {
        given:
        BaseUser caller = entityFactory.createFederatedUser().with { it.id = "caller"; return it }
        ScopeAccess callerToken = entityFactory.createFederatedToken()
        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(caller.id) >> { throw new NotFoundException() }
        exceptionHandler.exceptionResponse(_ as NotFoundException) >> Response.status(HttpServletResponse.SC_NOT_FOUND)

        when:
        Response.ResponseBuilder responseBuilder = service.addOTPDeviceToUser(null, callerToken.accessTokenString, caller.id, null)

        then:
        responseBuilder.build().status == HttpServletResponse.SC_NOT_FOUND
    }

    def "listOTPDevices: verify authorization logic called appropriately for non-self calls for identity admin"() {
        given:
        BaseUser caller = entityFactory.createUser().with { it.id = "caller"; return it }
        ScopeAccess callerToken = entityFactory.createUserToken(caller.getId())
        BaseUser targetUser = entityFactory.createUser().with { it.id = "target"; return it }

        when:
        Response.ResponseBuilder responseBuilder = service.listOTPDevicesForUser(null, callerToken.accessTokenString, targetUser.id)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken //validates caller provided token
        1 * userService.checkUserDisabled(caller) //validates caller user state
        1 * requestContext.getEffectiveCaller() >> caller
        1 * requestContextHolder.checkAndGetTargetUser(targetUser.id) >> targetUser
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, targetUser)
    }

    def "listOTPDevices: verify authorization logic called appropriately for non-self calls for user admin"() {
        given:
        BaseUser caller = entityFactory.createUser().with { it.id = "caller"; return it }
        ScopeAccess callerToken = entityFactory.createUserToken(caller.getId())
        BaseUser targetUser = entityFactory.createUser().with { it.id = "target"; return it }

        when:
        Response.ResponseBuilder responseBuilder = service.listOTPDevicesForUser(null, callerToken.accessTokenString, targetUser.id)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken //validates caller provided token
        1 * userService.checkUserDisabled(caller) //validates caller user state
        1 * requestContext.getEffectiveCaller() >> caller
        1 * requestContextHolder.checkAndGetTargetUser(targetUser.id) >> targetUser
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, targetUser)
        1 * authorizationService.authorizeCloudUserAdmin(callerToken) >> true
        1 * authorizationService.verifyDomain(caller, targetUser)
    }

    def "listOTPDevices: verify authorization logic called appropriately for self calls"() {
        given:
        BaseUser caller = entityFactory.createUser().with { it.id = "caller"; return it }
        ScopeAccess callerToken = entityFactory.createUserToken(caller.getId())

        when:
        Response.ResponseBuilder responseBuilder = service.listOTPDevicesForUser(null, callerToken.accessTokenString, caller.id)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken //validates caller provided token
        1 * userService.checkUserDisabled(caller) //validates caller user state
        1 * requestContext.getEffectiveCaller() >> caller
        0 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, _)
        0 * authorizationService.authorizeCloudUserAdmin(callerToken)
        0 * authorizationService.verifyDomain(caller, _)
    }

    @Unroll
    def "test delete OTP device for user access control (#allowed)"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user
        if (callerUserIdentityRoleType == "self") {
            user = caller
        } else {
            user = entityFactory.createUser().with { it.id = "user"; return it }
        }

        // Sample device
        OTPDevice device = new OTPDevice()
        device.setName("test")
        device.setId("1")

        // Sample entity
        com.rackspace.idm.domain.entity.OTPDevice entity = new com.rackspace.idm.domain.entity.OTPDevice()
        entity.setId("1")

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(caller.id) >> caller
        requestContextHolder.checkAndGetTargetUser(user.id) >> user

        if (allowed) {
            authorizationService.authorizeCloudUserAdmin(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_ADMIN)
            authorizationService.authorizeUserManageRole(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_MANAGER)
        } else {
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user) >> {throw new ForbiddenException()}
        }

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when:
        Response.ResponseBuilder responseBuilder = service.deleteOTPDeviceFromUser(uriInfo, callerToken.accessTokenString, user.id, device.id)

        then:
        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        allowed | callerUserIdentityRoleType          | userIdentityRoleType
        true    | "self"                              | _

        false   | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.SERVICE_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.IDENTITY_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.IDENTITY_ADMIN
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_MANAGER
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.DEFAULT_USER
    }


    @Unroll
    def "test create OTP device for user access control (#allowed)"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user
        if (callerUserIdentityRoleType == "self") {
            user = caller
        } else {
            user = entityFactory.createUser().with { it.id = "user"; return it }
        }

        // Sample device
        OTPDevice device = new OTPDevice()
        device.setName("test")

        // Sample entity
        com.rackspace.idm.domain.entity.OTPDevice entity = new com.rackspace.idm.domain.entity.OTPDevice()
        entity.setId("1")

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(caller.id) >> caller
        requestContextHolder.checkAndGetTargetUser(user.id) >> user

        multiFactorService.addOTPDeviceToUser(user.id, device.getName()) >> entity

        if (allowed) {
            authorizationService.authorizeCloudUserAdmin(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_ADMIN)
            authorizationService.authorizeUserManageRole(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_MANAGER)
        } else {
            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user) >> {throw new ForbiddenException()}
        }

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when:
        Response.ResponseBuilder responseBuilder = service.addOTPDeviceToUser(uriInfo, callerToken.accessTokenString, user.id, device)

        then:
        if (allowed) {
            assert responseBuilder.build().status == HttpServletResponse.SC_CREATED
        } else {
            assert capturedException instanceof ForbiddenException
        }

        where:
        allowed | callerUserIdentityRoleType          | userIdentityRoleType
        true    | "self"                              | _

        false   | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.SERVICE_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.IDENTITY_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.SERVICE_ADMIN  | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.IDENTITY_ADMIN
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.IDENTITY_ADMIN | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_ADMIN
        true    | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.USER_ADMIN     | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_ADMIN
        false   | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.USER_MANAGER
        true    | IdentityUserTypeEnum.USER_MANAGER   | IdentityUserTypeEnum.DEFAULT_USER

        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.SERVICE_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.IDENTITY_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_ADMIN
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.USER_MANAGER
        false   | IdentityUserTypeEnum.DEFAULT_USER   | IdentityUserTypeEnum.DEFAULT_USER
    }

    @Unroll
    def "test verify OTP device (scenario: #scenario)"() {
        given:
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }
        UserScopeAccess callerToken = entityFactory.createUserToken()
        User user = entityFactory.createUser().with { it.id = "user"; return it }

        // Sample device
        OTPDevice device = new OTPDevice()
        device.setName("test")
        device.setId("1")

        // Sample code
        VerificationCode code = new VerificationCode()
        code.setCode("123")

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetTargetUser(caller.id) >> caller
        requestContextHolder.checkAndGetTargetUser(user.id) >> user

        if (scenario == "invalid") multiFactorService.verifyOTPDeviceForUserById(user.id, device.id, code.code) >> { throw new MultiFactorDevicePinValidationException() }
        if (scenario == "notfound") multiFactorService.verifyOTPDeviceForUserById(user.id, device.id, code.code) >> { throw new NotFoundException() }
        if (scenario == "reverify") multiFactorService.verifyOTPDeviceForUserById(user.id, device.id, code.code) >> { throw new MultiFactorDeviceAlreadyVerifiedException() }

        when:
        def responseBuilder = service.verifyOTPCode(callerToken.accessTokenString, user.id, device.id, code)

        then:
        if (scenario == "invalid") {
            1 * exceptionHandler.badRequestExceptionResponse(_)
        } else if (scenario == "notfound") {
            1 * exceptionHandler.notFoundExceptionResponse(_)
        } else if (scenario == "reverify") {
            1 * exceptionHandler.badRequestExceptionResponse(_)
        }

        then:
        if (scenario == "valid") {
            responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        }

        where:
        scenario   | _
        "valid"    | _
        "invalid"  | _
        "notfound" | _
        "reverify" | _
    }

    @Unroll
    def "delete phone verifies access for callers of type: #callerUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }

        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user = entityFactory.createUser().with { it.id = "user"; return it }

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.getAndCheckTargetEndUser(user.id) >> user

        authorizationService.authorizeCloudUserAdmin(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_ADMIN)
        authorizationService.authorizeUserManageRole(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_MANAGER)

        when:
        service.deletePhoneFromUser(uriInfo, callerToken.accessTokenString, user.id, "blah")

        then: "verify calls appropriate external services to validate"
        1 * userService.validateUserIsEnabled(caller)
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)

        interaction {
            def domainVerifyCount = callerUserIdentityRoleType.isDomainBasedAccessLevel() && callerUserIdentityRoleType != IdentityUserTypeEnum.DEFAULT_USER ? 1 : 0
            domainVerifyCount * authorizationService.verifyDomain(caller, user)
        }

        where:
        callerUserIdentityRoleType              | _
        IdentityUserTypeEnum.SERVICE_ADMIN      | _
        IdentityUserTypeEnum.IDENTITY_ADMIN     | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }

    @Unroll
    def "delete phone allows self access for callers of type: #callerUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }

        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user = caller

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.getAndCheckTargetEndUser(user.id) >> user

        when:
        service.deletePhoneFromUser(uriInfo, callerToken.accessTokenString, user.id, "blah")

        then: "verify calls appropriate external services to validate"
        1 * userService.validateUserIsEnabled(caller)
        0 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        0 * authorizationService.verifyDomain(caller, user)

        where:
        callerUserIdentityRoleType              | _
        IdentityUserTypeEnum.SERVICE_ADMIN      | _
        IdentityUserTypeEnum.IDENTITY_ADMIN     | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }

    @Unroll
    def "get phone verifies access for callers of type: #callerUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }

        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user = entityFactory.createUser().with { it.id = "user"; return it }

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.getAndCheckTargetEndUser(user.id) >> user

        authorizationService.authorizeCloudUserAdmin(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_ADMIN)
        authorizationService.authorizeUserManageRole(callerToken) >> (callerUserIdentityRoleType == IdentityUserTypeEnum.USER_MANAGER)

        when:
        service.getPhoneFromUser(uriInfo, callerToken.accessTokenString, user.id, "blah")

        then: "verify calls appropriate external services to validate"
        1 * userService.validateUserIsEnabled(caller)
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)

        interaction {
            def domainVerifyCount = callerUserIdentityRoleType.isDomainBasedAccessLevel() && callerUserIdentityRoleType != IdentityUserTypeEnum.DEFAULT_USER ? 1 : 0
            domainVerifyCount * authorizationService.verifyDomain(caller, user)
        }

        where:
        callerUserIdentityRoleType              | _
        IdentityUserTypeEnum.SERVICE_ADMIN      | _
        IdentityUserTypeEnum.IDENTITY_ADMIN     | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }

    @Unroll
    def "get phone allows self access for callers of type: #callerUserIdentityRoleType"() {
        User caller = entityFactory.createUser().with { it.id = "caller"; return it }

        UserScopeAccess callerToken = entityFactory.createUserToken()

        def User user = caller

        // Mock UriInfo
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        URI uri = new URI("http://a.com")
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> uri

        securityContext.getAndVerifyEffectiveCallerToken(_) >> callerToken
        requestContext.getEffectiveCaller() >> caller
        requestContextHolder.getAndCheckTargetEndUser(user.id) >> user

        when:
        service.getPhoneFromUser(uriInfo, callerToken.accessTokenString, user.id, "blah")

        then: "verify calls appropriate external services to validate"
        1 * userService.validateUserIsEnabled(caller)
        0 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        0 * authorizationService.verifyDomain(caller, user)

        where:
        callerUserIdentityRoleType              | _
        IdentityUserTypeEnum.SERVICE_ADMIN      | _
        IdentityUserTypeEnum.IDENTITY_ADMIN     | _
        IdentityUserTypeEnum.USER_ADMIN         | _
        IdentityUserTypeEnum.USER_MANAGER       | _
        IdentityUserTypeEnum.DEFAULT_USER       | _
    }
}
