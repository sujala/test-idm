package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.idm.api.security.AuthorizationContext
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.multifactor.service.MultiFactorService
import com.rackspace.idm.validation.PrecedenceValidator
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.V2Factory

import javax.ws.rs.core.UriInfo

class DefaultMultifactorCloud20Test extends Specification {

    def config
    DefaultMultiFactorCloud20Service service
    def user;
    def betaRoleName = "mfaBetaRoleName"
    def rolesWithoutMfaBetaRole
    def rolesWithMfaBetaRole
    def scopeAccessService
    def userService
    MultiFactorService multiFactorService
    def defaultCloud20Service
    def precedenceValidator
    def authorizationService
    def exceptionHandler
    def requestContextHolder
    RequestContext requestContext
    SecurityContext securityContext
    AuthorizationContext authorizationContext

    @Shared def v2Factory = new V2Factory()

    def setup() {
        service = new DefaultMultiFactorCloud20Service()
        config = Mock(Configuration)
        service.config = config
        user = new User()
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
        scopeAccessService = Mock(ScopeAccessService)
        service.scopeAccessService = scopeAccessService
        userService = Mock(UserService)
        service.userService = userService
        multiFactorService = Mock(MultiFactorService)
        service.multiFactorService = multiFactorService
        defaultCloud20Service = Mock(DefaultCloud20Service)
        precedenceValidator = Mock(PrecedenceValidator)
        service.precedenceValidator = precedenceValidator
        authorizationService = Mock(AuthorizationService)
        service.authorizationService = authorizationService
        requestContextHolder = Mock(RequestContextHolder)
        service.requestContextHolder = requestContextHolder
        requestContext = Mock(RequestContext)
        requestContextHolder.getRequestContext() >> requestContext
        securityContext = Mock(SecurityContext)
        requestContext.getSecurityContext() >> securityContext
        authorizationContext = Mock(AuthorizationContext)
        requestContext.getEffectiveCallerAuthorizationContext() >> authorizationContext
    }

    def "updateMultifactor unlock - allow unlocking another account"() {
        User caller = new User().with {
            it.id = "callerId"
            it.domainId = "domainId"
            it
        }
        User targetUser = new User().with {
            it.id = "targetId"
            it.domainId = "domainId"
            it
        }

        UserScopeAccess token = new UserScopeAccess()

        def authToken = "authToken"
        UriInfo uriInfo = Mock();
        MultiFactor settings = v2Factory.createMultiFactorSettings(null, true)

        when:
        def response = service.updateMultiFactorSettings(uriInfo, authToken, targetUser.id, settings).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> token
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * requestContextHolder.checkAndGetTargetUser(_) >> targetUser
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_)
        2 * requestContext.getEffectiveCaller() >> caller
        1 * multiFactorService.updateMultiFactorSettings(_,_)
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "validateUpdateMultiFactorSettingsRequest throws errors"() {
        given:
        User caller = new User().with {
            it.id = "callerId"
            it
        }

        User target = new User().with {
            it.id = "userId"
            it
        }

        requestContext.getEffectiveCaller() >> caller
        MultiFactor unlockSettings = v2Factory.createMultiFactorSettings(null, true)

        when:
        service.validateUpdateMultiFactorSettingsRequest(target, null)

        then:
        thrown(BadRequestException)

        when:
        service.validateUpdateMultiFactorSettingsRequest(caller, unlockSettings)

        then:
        thrown(ForbiddenException)
    }
}
