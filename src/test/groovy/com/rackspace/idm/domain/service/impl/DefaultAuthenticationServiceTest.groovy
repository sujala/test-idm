package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.*
import com.unboundid.ldap.sdk.Attribute
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultAuthenticationServiceTest extends RootServiceTest {

    @Shared DefaultAuthenticationService service
    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,ou=users,o=rackspace"

    @Shared def refreshWindowHours = 6
    @Shared def expiredDate
    @Shared def refreshDate
    @Shared def futureDate

    def setupSpec() {
        service = new DefaultAuthenticationService()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        mockApplicationService(service)
        mockTenantService(service)
        mockScopeAccessService(service)
        mockAuthDao(service)
        mockConfiguration(service)
        mockUserService(service)
        mockInputValidator(service)
        mockRSAClient(service)
        mockIdentityUserService(service)

        expiredDate = new DateTime().minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(refreshWindowHours - 1).toDate()
        futureDate = new DateTime().plusHours(refreshWindowHours + 1).toDate()

        config.getInt("token.refreshWindowHours") >> refreshWindowHours
        config.getInt("token.expirationSeconds", _) >> defaultExpirationSeconds
        config.getInt("token.rackerExpirationSeconds", _) >> defaultRackerExpirationSeconds
    }

    def "Calls getAndUpdateUserScopeAccessForClientId adds new scope access and deletes old"() {
        given:
        def userOne = entityFactory.createUser("username", "1", "domainId", "region")
        def userTwo = entityFactory.createUser("username", "2", "domainId", "region")
        def application = entityFactory.createApplication()
        def scopeAccess = createUserScopeAccess("12345", "1", "clientId", new Date()).with {
            it.refreshTokenString = "12345"
            return it
        }
        scopeAccess = expireScopeAccess(scopeAccess)

        scopeAccessService.getUserScopeAccessByClientId(_, _) >>> [null, scopeAccess]

        when:
        service.getAndUpdateUserScopeAccessForClientId(userOne, application)
        service.getAndUpdateUserScopeAccessForClientId(userTwo, application)

        then:
        1 * scopeAccessService.addUserScopeAccess(userOne, _)

        then:
        1 * scopeAccessService.deleteScopeAccess(_)

        then:
        1 * scopeAccessService.addUserScopeAccess(userTwo, _)
    }

    def "Calls getAndUpdateUserScopeAccessForClientId returns existing scopeAccess if not expired"() {
        given:
        def user= entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()

        scopeAccessService.getUserScopeAccessByClientId(_, _) >> scopeAccess

        when:
        def result = service.getAndUpdateUserScopeAccessForClientId(user, application)

        then:
        result.is(scopeAccess)

    }

    def "Calls getAndUpdateClientScopeAccessForClientId adds new scopeAccess and deletes old"() {
        given:
        def applicationOne = entityFactory.createApplication("1", "client1")
        def applicationTwo = entityFactory.createApplication("2", "client2")
        def scopeAccess = expireScopeAccess(createClientScopeAccess())

        scopeAccessService.getApplicationScopeAccess(_) >>> [ null, scopeAccess ]

        when:
        service.getAndUpdateClientScopeAccessForClientId(applicationOne)
        service.getAndUpdateClientScopeAccessForClientId(applicationTwo)

        then:
        1 * scopeAccessService.addApplicationScopeAccess(applicationOne, _)

        then:
        1 * scopeAccessService.deleteScopeAccess(_)

        then:
        1 * scopeAccessService.addApplicationScopeAccess(applicationTwo, _)
    }

    def "Calls getAndUpdateClientScopeAccessForClientId returns scopeAccess if not expired"() {
        given:
        def scopeAccess = createClientScopeAccess()
        def application = entityFactory.createApplication()

        scopeAccessService.getApplicationScopeAccess(_) >> scopeAccess

        when:
        def result = service.getAndUpdateClientScopeAccessForClientId(application)

        then:
        result.is(scopeAccess)
    }

    def "Calls getAndUpdateRackerScopeAccessForClientId deletes old and adds new"() {
        given:
        config.getString("idm.clientId") >> "idmClientId"
        def application = entityFactory.createApplication("1", "client1")
        def tenantRole = entityFactory.createTenantRole("Racker").with {
            it.clientId = "idmClientId"
            return it
        }
        def rackerOne = entityFactory.createRacker("racker1")
        def rackerTwo = entityFactory.createRacker("racker2")
        def scopeAccess = expireScopeAccess(createRackerScopeAcccss())

        scopeAccessService.getRackerScopeAccessByClientId(_, _) >>> [ null, scopeAccess ]
        tenantService.getTenantRolesForUser(_) >> [ tenantRole ].asList()

        when:
        service.getAndUpdateRackerScopeAccessForClientId(rackerOne, application)
        service.getAndUpdateRackerScopeAccessForClientId(rackerTwo, application)

        then:
        1 * scopeAccessService.addUserScopeAccess(rackerOne, _)

        then:
        1 * scopeAccessService.deleteScopeAccess(_)

        then:
        1 * scopeAccessService.addUserScopeAccess(rackerTwo, _)
    }

    def "Calls getAndUpdateRackerScopeAccessForClientId returns existing scopeAccess if not expired"() {
        given:
        config.getString("idm.clientId") >> "idmClientId"
        def application = entityFactory.createApplication("1", "client1")
        def tenantRole = entityFactory.createTenantRole("Racker").with {
            it.clientId = "idmClientId"
            return it
        }
        def racker = entityFactory.createRacker("racker")
        def scopeAccess = createRackerScopeAcccss()

        scopeAccessService.getRackerScopeAccessByClientId(_, _) >> scopeAccess
        tenantService.getTenantRolesForUser(_) >> [ tenantRole ].asList()

        when:
        def result = service.getAndUpdateRackerScopeAccessForClientId(racker, application)

        then:
        result.is(scopeAccess)
    }

    def "Calls getTokens with refreshtoken credentials adds new and deletes old scopeaccess"() {
        given:
        def scopeAccess = createUserScopeAccess().with {
            it.refreshTokenString = "refresh"
            it.refreshTokenExp = refreshDate
            return it
        }

        def credentials = Mock(Credentials)
        credentials.getOAuthGrantType() >> OAuthGrantType.REFRESH_TOKEN
        credentials.getGrantType() >> "REFRESH_TOKEN"
        credentials.getClientId() >> "12345"

        def authResult = Mock(ClientAuthenticationResult)
        authResult.isAuthenticated() >> true
        authResult.getClient() >> new Application().with() {
            it.clientId = "clientId"
            return it
        }

        def user = entityFactory.createUser()

        applicationService.authenticate(_, _) >> authResult
        userService.getUserByScopeAccess(_) >> user
        scopeAccessService.getScopeAccessByRefreshToken(_) >> scopeAccess
        identityUserService.getEndUserById(_) >> user

        when:
        def returned = service.getTokens(credentials, new DateTime())

        then:
        returned.clientId == "clientId"
        1 * scopeAccessService.addUserScopeAccess(_, _)
        1 * scopeAccessService.deleteScopeAccess(_)
    }

    def "Calls getTokens with refreshtoken Racker credentials adds new and deletes old scopeaccess"() {
        given:
        def scopeAccess = createRackerScopeAcccss().with {
            it.refreshTokenString = "refresh"
            it.refreshTokenExp = refreshDate
            return it
        }

        def credentials = Mock(Credentials)
        credentials.getOAuthGrantType() >> OAuthGrantType.REFRESH_TOKEN
        credentials.getGrantType() >> "REFRESH_TOKEN"
        credentials.getClientId() >> "12345"

        def authResult = Mock(ClientAuthenticationResult)
        authResult.isAuthenticated() >> true
        authResult.getClient() >> new Application().with() {
            it.clientId = "clientId"
            return it
        }

        def user = entityFactory.createRacker()

        applicationService.authenticate(_, _) >> authResult
        userService.getUserByScopeAccess(_) >> user
        scopeAccessService.getScopeAccessByRefreshToken(_) >> scopeAccess
        userService.getUserById(_) >> user

        when:
        def returned = service.getTokens(credentials, new DateTime())

        then:
        returned.clientId == "clientId"
        1 * scopeAccessService.addUserScopeAccess(_, _)
        1 * scopeAccessService.deleteScopeAccess(_)
        1 * scopeAccessService.getTokenExpirationSeconds(defaultRackerExpirationSeconds)
    }

    def "Calls getTokens sets token expiration with entropy"() {
        given:
        def credentials = Mock(Credentials)
        credentials.getGrantType() >> "REFRESH_TOKEN"
        credentials.getOAuthGrantType() >> OAuthGrantType.REFRESH_TOKEN
        credentials.getClientId() >> "clientId"

        def scopeAccess = Mock(UserScopeAccess)
        scopeAccess.isRefreshTokenExpired(_) >> false
        scopeAccess.getClientId() >> "clientId"
        scopeAccess.getUniqueId() >> "accessToken=accessToken,cn=TOKENS,rsId=1234"

        def application = entityFactory.createApplication()
        application.clientId = "clientId"

        def caResult = Mock(ClientAuthenticationResult)
        caResult.isAuthenticated() >> true
        caResult.getClient() >> application

        applicationService.authenticate(_, _) >> caResult
        scopeAccessService.getScopeAccessByRefreshToken(_) >> scopeAccess
        identityUserService.getEndUserById(_) >> entityFactory.createUser()

        when:
        service.getTokens(credentials , new DateTime())

        then:
        1 * scopeAccessService.getTokenExpirationSeconds(_)
    }

    def "Calls getAndUpdateUserScopeAccessForClientId sets expiration with entropy"() {
        given:
        def application = entityFactory.createApplication()
        def user = entityFactory.createUser()
        def expired = expireScopeAccess(createUserScopeAccess())

        scopeAccessService.getUserScopeAccessByClientId(_) >> expired

        when:
        service.getAndUpdateUserScopeAccessForClientId(user, application)

        then:
        1 * scopeAccessService.getTokenExpirationSeconds(_)
    }

    def "Calls getAndUpdateClientScopeAccessForClientId sets expiration with entropy"() {
        given:
        def application = entityFactory.createApplication()
        def scopeAccess = expireScopeAccess(createClientScopeAccess())

        scopeAccessService.getApplicationScopeAccess(_) >> scopeAccess

        when:
        service.getAndUpdateClientScopeAccessForClientId(application)

        then:
        1 * scopeAccessService.getTokenExpirationSeconds(_)
    }

    def "Calls getAndUpdateRackerScopeAccess sets expiration with entropy"() {
        given:
        def user = entityFactory.createRacker()
        def application = entityFactory.createApplication()
        def expired = expireScopeAccess(createRackerScopeAcccss())

        scopeAccessService.getRackerScopeAccessForClientId(_) >> expired

        //pass through validateRackerHasRackerRole
        def role = entityFactory.createTenantRole("Racker")
        role.clientId = "clientId"
        tenantService.getTenantRolesForUser(_) >> [role].asList()
        config.getString("idm.clientId") >> role.clientId
        config.getInt("token.rackerExpirationSeconds") >> defaultRackerExpirationSeconds

        when:
        service.getAndUpdateRackerScopeAccessForClientId(user, application)

        then:
        1 * scopeAccessService.getTokenExpirationSeconds(defaultRackerExpirationSeconds)
    }

    def attribute() {
        return new Attribute("attribute", "value")
    }
}
