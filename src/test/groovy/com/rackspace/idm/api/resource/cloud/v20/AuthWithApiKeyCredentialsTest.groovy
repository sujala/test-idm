package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthWithApiKeyCredentialsTest extends RootServiceTest {

    @Shared AuthWithApiKeyCredentials service

    def setupSpec() {
        service = new AuthWithApiKeyCredentials()
    }

    def setup() {
        mockIdentityConfig(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockValidator20(service)
    }

    def "authenticateForAuthResponse validates apiKeyCredentials"() {
        given:
        def credential = v1Factory.createJAXBApiKeyCredentials("username", "apiKey")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential)

        when:
        service.authenticateForAuthResponse(authRequest)

        then:
        1 * validator20.validateApiKeyCredentials(_) >> { arg1 ->
            assert(arg1.username[0] == "username")
            assert(arg1.apiKey[0] == "apiKey")
        }
        userService.authenticateWithApiKey(_, _) >> new UserAuthenticationResult(new User(), true)
    }

    def "authenticateForAuthResponse sets User and UserScopeAccess in return values"() {
        given:
        def credential = v1Factory.createJAXBApiKeyCredentials("username", "apiKey")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential)
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()

        when:
        def result = service.authenticateForAuthResponse(authRequest)

        then:
        1 * scopeAccessService.getValidUserScopeAccessForClientId(_, _, _) >> scopeAccess
        userService.authenticateWithApiKey(_, _) >> new UserAuthenticationResult(user, true)
        result.impersonatedScopeAccess == null
        result.userScopeAccess == scopeAccess
        result.user == user
    }
}