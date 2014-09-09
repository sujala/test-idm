package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotAuthenticatedException
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response


class DefaultMultiFactorCloud20ServiceTest extends RootServiceTest {
    @Shared DefaultMultiFactorCloud20Service service

    def setupSpec() {
        service = new DefaultMultiFactorCloud20Service()
    }

    def setup() {
        mockConfiguration(service)
        mockCloud20Service(service)
        mockUserService(service)
        mockMultiFactorService(service)
        mockExceptionHandler(service)
        mockPhoneCoverterCloudV20(service)
        mockScopeAccessService(service)
        mockPrecedenceValidator(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockApplicationService(service)
    }

    def "listDevicesForUser validates x-auth-token"() {
        when:
        allowMultiFactorAccess()
        service.listDevicesForUser(null, "token", null)

        then:
        defaultCloud20Service.getScopeAccessForValidToken(_) >> { throw new NotAuthenticatedException() }
    }

    @Unroll
    def "updateMultiFactorSettings(userEnforcementLevel) default user can not call"() {
        User caller = entityFactory.createUser().with{it.id = "caller"; return it}
        User target = entityFactory.createUser().with{it.id = "target"; return it}
        ClientRole callerRole = entityFactory.createClientRole(callerUserIdentityRoleType.name())
        UserScopeAccess callerToken = entityFactory.createUserToken()
        MultiFactor settings = v2Factory.createMultiFactorSettings().with{it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.DEFAULT; return it}
        def capturedException

        defaultCloud20Service.getScopeAccessForValidToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetUser(target.id) >> target
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType

        when:
        service.updateMultiFactorSettings(null, callerToken.accessTokenString, target.id, settings)

        then:
        //capture the exception that was thrown
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}
        capturedException instanceof ForbiddenException

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

        defaultCloud20Service.getScopeAccessForValidToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetUser(target.id) >> target
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
            capturedException instanceof ForbiddenException
        } else {
            responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
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

        defaultCloud20Service.getScopeAccessForValidToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetUser(target.id) >> target
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
            responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        } else {
            capturedException instanceof ForbiddenException
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

        defaultCloud20Service.getScopeAccessForValidToken(_) >> callerToken
        userService.getUserByScopeAccess(callerToken) >> caller
        requestContextHolder.checkAndGetUser(caller.id) >> caller
        applicationService.getUserIdentityRole(caller) >> callerRole
        authorizationService.getIdentityTypeRoleAsEnum(callerRole) >> callerUserIdentityRoleType

        def capturedException
        exceptionHandler.exceptionResponse(_) >> {args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN)}

        when: "call on self"
        Response.ResponseBuilder responseBuilder = service.updateMultiFactorSettings(null, callerToken.accessTokenString, caller.id, settings)

        then:
        if (allowed) {
            responseBuilder.build().status == HttpServletResponse.SC_NO_CONTENT
        } else {
            capturedException instanceof ForbiddenException
        }

        where:
        callerUserIdentityRoleType          | allowed
        IdentityUserTypeEnum.SERVICE_ADMIN  | false
        IdentityUserTypeEnum.IDENTITY_ADMIN | false
        IdentityUserTypeEnum.USER_ADMIN     | true
        IdentityUserTypeEnum.USER_MANAGER   | true
        IdentityUserTypeEnum.DEFAULT_USER   | false
    }
}
