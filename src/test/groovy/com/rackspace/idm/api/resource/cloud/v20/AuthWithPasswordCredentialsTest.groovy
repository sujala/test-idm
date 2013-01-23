package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 23/01/13
 * Time: 09:54
 * To change this template use File | Settings | File Templates.
 */
class AuthWithPasswordCredentialsTest extends RootServiceTest {

    @Shared AuthWithPasswordCredentials service

    def setupSpec() {
        service = new AuthWithPasswordCredentials()
    }

    def setup() {
        mockValidator20(service)
        mockScopeAccessService(service)
        mockUserService(service)
        mockConfiguration(service)
    }

    def "authenticate validates credentials"() {
        given:
        def credential = v2Factory.createJAXBPasswordCredentialsRequiredUsername("username", "Password1")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential, null)

        when:
        service.authenticate(authRequest)

        then:
        1 * validator20.validatePasswordCredentials(_) >> { arg1 ->
            assert(arg1.username[0] == "username")
            assert(arg1.password[0] == "Password1")
        }
    }

    def "authenticate sets User and UserScopeAccess in authReturnValues"() {
        given:
        def credential = v2Factory.createJAXBPasswordCredentialsRequiredUsername("username", "Password1")
        def authRequest = v2Factory.createAuthenticationRequest(null, null, credential, null)
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()

        when:
        def response = service.authenticate(authRequest)

        then:
        1 * userService.getUserByUsernameForAuthentication("username") >> user
        1 * scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(_, _, _) >> scopeAccess
        response.impersonatedScopeAccess == null
        response.user == user
        response.userScopeAccess == scopeAccess
    }

    def "a clientId is returned by getCloudAuthClientId"() {
        when:
        service.getCloudAuthClientId();

        then:
        1 * config.getString("cloudAuth.clientId")
    }
}
