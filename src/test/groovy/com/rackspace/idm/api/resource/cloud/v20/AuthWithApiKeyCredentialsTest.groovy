package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthWithApiKeyCredentialsTest extends RootServiceTest {

    @Shared AuthWithApiKeyCredentials service

    def setupSpec() {
        service = new AuthWithApiKeyCredentials()
    }

    def setup() {
        mockConfiguration(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockValidator20(service)
    }

    def "authenticate validates apiKeyCredentials"() {
        given:
        def credential = v1Factory.createJAXBApiKeyCredentials("username", "apiKey")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential, null)

        when:
        service.authenticate(authRequest)

        then:
        1 * validator20.validateApiKeyCredentials(_) >> { arg1 ->
            assert(arg1.username[0] == "username")
            assert(arg1.apiKey[0] == "apiKey")
        }
    }

    def "authenticate sets User and UserScopeAccess in return values"() {
        given:
        def credential = v1Factory.createJAXBApiKeyCredentials("username", "apiKey")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential, null)
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()

        when:
        def result = service.authenticate(authRequest)

        then:
        1 * userService.getUserByUsernameForAuthentication("username") >> user
        1 * scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(_, _, _) >> scopeAccess
        result.impersonatedScopeAccess == null
        result.userScopeAccess == scopeAccess
        result.user == user
    }

    def "a clientId is returned by getCloudAuthClientId"() {
        when:
        service.getCloudAuthClientId();

        then:
        1 * config.getString("cloudAuth.clientId")
    }
}
