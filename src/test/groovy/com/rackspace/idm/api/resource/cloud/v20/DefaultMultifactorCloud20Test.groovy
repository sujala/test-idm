package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.api.resource.cloud.v20.multifactor.SessionIdReaderWriter
import com.rackspace.idm.api.resource.cloud.v20.multifactor.V1SessionId
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
    def tenantService
    def multiFactorService
    def defaultCloud20Service
    def precedenceValidator
    def authorizationService
    def exceptionHandler

    @Shared def v2Factory = new V2Factory()

    def setup() {
        service = new DefaultMultiFactorCloud20Service()
        tenantService = Mock(TenantService)
        service.tenantService = tenantService
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
        tenantService = Mock(TenantService)
        service.tenantService = tenantService
        multiFactorService = Mock(MultiFactorService)
        service.multiFactorService = multiFactorService
        defaultCloud20Service = Mock(DefaultCloud20Service)
        service.cloud20Service = defaultCloud20Service
        precedenceValidator = Mock(PrecedenceValidator)
        service.precedenceValidator = precedenceValidator
        authorizationService = Mock(AuthorizationService)
        service.authorizationService = authorizationService
    }

    def "isMultiFactorEnabled checks multifactor feature flag"() {
        when:
        service.isMultiFactorEnabled()

        then:
        1 * config.getBoolean("multifactor.services.enabled", false)
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are disabled"() {
        when:
        def response = service.isMultiFactorEnabled()

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> false
        response == false
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are enabled and in beta and user does not have MFA beta role"() {
        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        1 * config.getString("cloudAuth.multiFactorBetaRoleName") >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> rolesWithoutMfaBetaRole
        response == false
    }

    def "isMultiFactorEnabledForUser returns true if multifactor services are enabled and in beta and user has MFA beta role"() {
        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        1 * config.getString("cloudAuth.multiFactorBetaRoleName") >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> rolesWithMfaBetaRole
        response == true
    }

    def "authenticateSecondFactor allows users without mfa beta role to authenticate when mfa services are fully open"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithoutMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> false
        noExceptionThrown()
    }

    def "authenticateSecondFactor does not throw an exception when mfa is in beta and user had mfa beta role"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        noExceptionThrown()
    }

    def "authenticateSecondFactor throws BadRequestException when mfa is in beta and user does not have mfa beta role"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithoutMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        thrown(BadRequestException)
    }

    def "authenticateSecondFactor throws BadRequestException when mfa is disabled for all users"() {
        given:
        def encodedSessionId = "encodedSessionId"
        def cred = new PasscodeCredentials()
        setupForMfaAuth(encodedSessionId, rolesWithMfaBetaRole)

        when:
        service.authenticateSecondFactor(encodedSessionId, cred)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> false
        thrown(BadRequestException)
    }

    def "updateMultifactor unlock - allow unlocking another account account"() {
        User caller = new User().with {
            it.id = "callerId"
            it
        }
        User targetUser = new User().with {
            it.id = "targetId"
            it
        }

        UserScopeAccess token = new UserScopeAccess()

        def authToken = "authToken"
        UriInfo uriInfo = Mock();
        MultiFactor settings = v2Factory.createMultiFactorSettings(null, true)

        when:
        def response = service.updateMultiFactorSettings(uriInfo, authToken, targetUser.id, settings)

        then:
        1 * defaultCloud20Service.getScopeAccessForValidToken(authToken) >> token
        1 * userService.getUserByScopeAccess(token) >> caller
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * userService.checkAndGetUserById(targetUser.id) >> targetUser
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_,_)
        1 * multiFactorService.updateMultiFactorSettings(_,_)
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "validateUpdateMultiFactorSettingsRequest throws errors"() {
        given:
        User caller = new User().with {
            it.id = "callerId"
            it
        }
        def token = new UserScopeAccess()
        def userId = "userId"

        when:
        service.validateUpdateMultiFactorSettingsRequest(caller, token, userId, null)

        then:
        thrown(BadRequestException)
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
        tenantService.getGlobalRolesForUser(user) >> userRoles
        def mfaAuthResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, "", "")
        multiFactorService.verifyPasscode(_, _) >> mfaAuthResponse
    }

}
