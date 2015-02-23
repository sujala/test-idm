package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionIdReaderWriter
import com.rackspace.idm.api.resource.cloud.v20.multifactor.V1SessionId
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.multifactor.service.MultiFactorService
import com.rackspace.idm.validation.PrecedenceValidator
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.joda.time.DateTime
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
    def sessionIdReaderWriter
    MultiFactorService multiFactorService
    def defaultCloud20Service
    def precedenceValidator
    def authorizationService
    def exceptionHandler
    def requestContextHolder
    def requestContext
    SecurityContext securityContext

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
        sessionIdReaderWriter = Mock(SessionIdReaderWriter)
        service.sessionIdReaderWriter = sessionIdReaderWriter
        multiFactorService = Mock(MultiFactorService)
        service.multiFactorService = multiFactorService
        defaultCloud20Service = Mock(DefaultCloud20Service)
        service.cloud20Service = defaultCloud20Service
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
    }

    def "authenticateSecondFactor calls multifactor service to determine whether user has access to MFA"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithoutMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * multiFactorService.isMultiFactorEnabledForUser(_) >> true
        noExceptionThrown()
    }

    def "authenticateSecondFactor throws BadRequestException when user does not have access to MFA"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithoutMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * multiFactorService.isMultiFactorEnabledForUser(_) >> false
        thrown(BadRequestException)
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
        def response = service.updateMultiFactorSettings(uriInfo, authToken, targetUser.id, settings)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(authToken) >> token
        1 * userService.getUserByScopeAccess(token) >> caller
        1 * requestContextHolder.checkAndGetTargetUser(_) >> targetUser
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_,_)
        1 * authorizationService.authorizeCloudUserAdmin(_) >> false
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.verifyDomain(_,_)
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

        def token = new UserScopeAccess()
        MultiFactor unlockSettings = v2Factory.createMultiFactorSettings(null, true)

        when:
        service.validateUpdateMultiFactorSettingsRequest(caller, token, target, null)

        then:
        thrown(BadRequestException)

        when:
        service.validateUpdateMultiFactorSettingsRequest(caller, token, caller, unlockSettings)

        then:
        thrown(ForbiddenException)
    }

    def setupForMfaAuth(encodedSessionId, userRoles) {
        def userId = "userId"
        user.id = userId
        config.getString("cloudAuth.multiFactorBetaRoleName") >> betaRoleName
        def decodedSessionId = new V1SessionId().with {
            it.expirationDate = new DateTime().plusYears(1)
            it.userId = userId
            it
        }
        sessionIdReaderWriter.readEncoded(encodedSessionId) >> decodedSessionId
        userService.getUserById(userId) >> user
        def mfaAuthResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, "", "")
        multiFactorService.verifyPasscode(_, _) >> mfaAuthResponse
    }

}
