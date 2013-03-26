package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.exception.NotFoundException
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.TokenForAuthenticationRequest
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

    def "authenticate retrieves the scopeAccess for the token in the request"() {
        given:
        passTenantAccess()

        def authRequestWithTenantId = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "")
        def authRequestWithTenantName = v2Factory.createAuthenticationRequest("tokenId", "", "tenantName")
        def authRequestWithBoth = v2Factory.createAuthenticationRequest("tokenId", "tenantId", "tenantName")

        when:
        service.authenticate(authRequestWithTenantId)
        service.authenticate(authRequestWithTenantName)
        service.authenticate(authRequestWithBoth)

        then:
        3 * scopeAccessService.getScopeAccessByAccessToken("tokenId") >> createUserScopeAccess()
        3 * userService.checkAndGetUserById(_) >> entityFactory.createUser()
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
        passTenantAccess()

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
        userService.checkAndGetUserById(_) >> user

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
        passTenantAccess()

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
        1 * userService.checkAndGetUserById(_) >> user
        result != null
        result.impersonatedScopeAccess == null
        result.userScopeAccess == scopeAccess
        result.user == user
    }

    def "ImpersonatedScopeAccess: authenticate throws NotAuthenticatedException because of tenantAccess after token has been validated"() {
        given:
        def authRequestWithTenantId = v2Factory.createAuthenticationRequest("tokenOne", "tenantId", "")
        def authRequestWithTenantName = v2Factory.createAuthenticationRequest("tokenTwo", "", "tenantName")
        def user = entityFactory.createUser()

        def mockedImpersonatedScopeAccess = Mock(ImpersonatedScopeAccess)
        def mockedUserScopeAccess = Mock(UserScopeAccess)

        scopeAccessService.getScopeAccessByAccessToken("tokenOne") >> mockedImpersonatedScopeAccess
        scopeAccessService.getScopeAccessByAccessToken(_) >> mockedUserScopeAccess
        userService.checkAndGetUserById(_) >> user

        when:
        service.authenticate(authRequestWithTenantId)

        then:
        1 * mockedImpersonatedScopeAccess.isAccessTokenExpired(_) >> false
        1 * mockedUserScopeAccess.isAccessTokenExpired(_) >> false
        1 * scopeAccessService.updateExpiredUserScopeAccess(_, _, _) >> { return createUserScopeAccess() }

        then:
        1 * tenantService.hasTenantAccess(_, _) >> false
        thrown(NotAuthenticatedException)
    }

    def "UserScopeAccess: authenticate throws NotAuthenticatedException because of tenantAccess after token has been validated"() {
        given:
        def authRequestWithTenantName = v2Factory.createAuthenticationRequest("tokenTwo", "", "tenantName")
        def user = entityFactory.createUser()

        def mockedUserScopeAccess = Mock(UserScopeAccess)

        scopeAccessService.getScopeAccessByAccessToken("tokenTwo") >> mockedUserScopeAccess
        userService.checkAndGetUserById(_) >> user

        when:
        service.authenticate(authRequestWithTenantName)

        then:
        1 * mockedUserScopeAccess.isAccessTokenExpired(_) >> false
        1 * scopeAccessService.updateExpiredUserScopeAccess(_, _, _) >> { return createUserScopeAccess() }

        then:
        1 * tenantService.hasTenantAccess(_, _) >> false
        thrown(NotAuthenticatedException)
    }

    def "a user is returned by getUserByIdForAuthenciation if user is found"() {
        given:
        userService.checkAndGetUserById("id") >> entityFactory.createUser()

        when:
        def result = service.getUserByIdForAuthentication("id")

        then:
        result != null
        notThrown(NotAuthenticatedException)
    }

    def "if user is not found getUserByidForAuthentication throws NotAuthenticatedException"() {
        given:
        userService.checkAndGetUserById("id") >> { throw new NotFoundException() }

        when:
        service.getUserByIdForAuthentication("id")

        then:
        thrown(NotAuthenticatedException)
    }
    def passTenantAccess() {
        tenantService.hasTenantAccess(_, _) >> true
    }
}
