package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthWithPasswordCredentialsTest extends RootServiceTest {

    @Shared AuthWithPasswordCredentials service

    def setupSpec() {
        service = new AuthWithPasswordCredentials()
    }

    def setup() {
        mockValidator20(service)
        mockScopeAccessService(service)
        mockAuthorizationService(service)
        mockUserService(service)
    }

    def "authenticateForAuthResponse validates credentials"() {
        given:
        def credential = v2Factory.createJAXBPasswordCredentialsBase("username", "Password1")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential)

        when:
        service.authenticateForAuthResponse(authRequest)

        then:
        1 * validator20.validatePasswordCredentials(_) >> { arg1 ->
            assert(arg1.username[0] == "username")
            assert(arg1.password[0] == "Password1")
        }
        userService.authenticate(_, _) >> new UserAuthenticationResult(new User(), true)
    }

    def "authenticateForAuthResponse sets User and UserScopeAccess in authReturnValues"() {
        given:
        def credential = v2Factory.createJAXBPasswordCredentialsBase("username", "Password1")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential)
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()

        when:
        def response = service.authenticateForAuthResponse(authRequest)

        then:
        1 * scopeAccessService.createScopeAccessForUserAuthenticationResult(_) >> new AuthResponseTuple(user, scopeAccess)
        userService.authenticate(_, _) >> new UserAuthenticationResult(user, true)
        response.impersonatedScopeAccess == null
        response.user == user
        response.userScopeAccess == scopeAccess
    }

    def "authenticateForAuthResponse verifies user against auth domain"() {
        given:
        def user = entityFactory.createUser()
        def credential = v2Factory.createJAXBPasswordCredentialsBase("username", "Password1")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential).with {
            it.domainId = user.domainId
            it
        }
        def scopeAccess = createUserScopeAccess()

        when:
        service.authenticateForAuthResponse(authRequest)

        then:
        1 * scopeAccessService.createScopeAccessForUserAuthenticationResult(_) >> new AuthResponseTuple(user, scopeAccess)
        userService.authenticate(_, _) >> new UserAuthenticationResult(user, true)
        1 * authorizationService.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(user, authRequest)
        1 * authorizationService.verifyUserAuthorizedToAuthenticateOnDomain(user, authRequest.domainId)
    }

}
