package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.exception.NotFoundException
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthWithTokenTest extends RootServiceTest {

    @Shared AuthWithToken service

    def setupSpec() {
        service = new AuthWithToken()
    }

    def setup() {
        mockUserService(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockIdentityUserService(service)
        mockRequestContextHolder(service)
    }

    def "authenticate throws a bad request when token is blank"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("", "tenantId", "tenantName")

        when:
        service.authenticate(authRequest)

        then:
        thrown(BadRequestException)
    }

    def "authenticate throws a bad request when tenantId and tenantName are blank"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("tokenId", "", "")

        when:
        service.authenticate(authRequest)

        then:
        thrown(BadRequestException)
    }

    def "authenticate gets impersonated scopeAccess; throws NotAuthorizedException when token is expired"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "")
        def impScopeAccess = createImpersonatedScopeAccess(
                "username",
                "impUsername",
                "tokenString",
                "impToken",
                new DateTime().minusDays(1).toDate()
        )

        scopeAccessService.getScopeAccessByAccessToken("tokenId") >> impScopeAccess
        userService.checkAndGetUserById(_) >> entityFactory.createUser()

        when:
        service.authenticate(authRequest)

        then:
        thrown(NotAuthorizedException)
    }

    def "authenticate gets impersonated scopeAccess; sets value in returnValues and gets the token being impersonated"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "")
        def user = entityFactory.createUser()
        def impScopeAccess = createImpersonatedScopeAccess(
                "username",
                "impUsername",
                "tokenString",
                "impToken",
                new DateTime().plusDays(1).toDate()
        )
        def scopeAccess = createUserScopeAccess()

        scopeAccessService.getScopeAccessByAccessToken("tokenId") >> impScopeAccess
        identityUserService.checkAndGetUserById(_) >> user

        when:
        def result = service.authenticate(authRequest)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken("impToken") >> scopeAccess
        result != null
        result.impersonatedScopeAccess == impScopeAccess
        result.userScopeAccess == scopeAccess
        result.user == user
    }

    def "authenticate gets userScopeAccess; throws NotAuthenticatedException when expired"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "")
        def scopeAccess = createUserScopeAccess(
                "tokenString",
                "userRsId",
                "clientId",
                new DateTime().minusDays(1).toDate()
        )

        when:
        service.authenticate(authRequest)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken("tokenId") >> scopeAccess
        thrown(NotAuthenticatedException)
    }

    def "authenticate gets valid userScopeAccess; does not set impersonatingScopeAccess"() {
        given:
        def authRequest = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "")
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess(
                "tokenString",
                "userRsId",
                "clientId",
                new DateTime().plusDays(1).toDate()
        )

        when:
        def result = service.authenticate(authRequest)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken("tokenId") >> scopeAccess
        1 * identityUserService.checkAndGetUserById(_) >> user
        result != null
        result.impersonatedScopeAccess == null
        result.userScopeAccess == scopeAccess
        result.user == user
    }

    def "a user is returned by getUserByIdForAuthenciation if user is found"() {
        given:
        identityUserService.checkAndGetUserById("id") >> entityFactory.createUser()

        when:
        def result = service.getUserByIdForAuthentication("id")

        then:
        result != null
        notThrown(NotAuthenticatedException)
    }

    def "if user is not found getUserByidForAuthentication throws NotAuthenticatedException"() {
        given:
        identityUserService.checkAndGetUserById("id") >> { throw new NotFoundException() }

        when:
        service.getUserByIdForAuthentication("id")

        then:
        thrown(NotAuthenticatedException)
    }
}
