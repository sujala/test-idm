package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.*
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.rackspace.idm.domain.entity.Credentials
import com.rackspace.idm.domain.entity.ClientAuthenticationResult
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultAuthenticationServiceTest extends RootServiceTest {

    @Shared DefaultAuthenticationService service
    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,ou=users,o=rackspace"
    @Shared def parentDn = "cn=TOKENS,ou=users,o=rackspace"

    @Shared def refreshWindowHours = 6
    @Shared def tokenExpirationHours = 12
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
        mockCustomerService(service)
        mockInputValidator(service)
        mockRSAClient(service)

        expiredDate = new DateTime().minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(refreshWindowHours - 1).toDate()
        futureDate = new DateTime().plusHours(refreshWindowHours + 1).toDate()

        config.getInt("token.refreshWindowHours") >> refreshWindowHours
    }

    def "getAndUpdateUserScopeAccessForClientId adds new scope access and deletes old"() {
        given:
        def userOne = entityFactory.createUser("username", "1", "domainId", "region")
        def userTwo = entityFactory.createUser("username", "2", "domainId", "region")
        def application = entityFactory.createApplication()
        def scopeAccess = createUserScopeAccess("12345", "1", "clientId", new Date()).with {
            it.refreshTokenString = "12345"
            return it
        }
        scopeAccess = expireScopeAccess(scopeAccess)

        scopeAccessService.getUserScopeAccessForClientId(_, _) >>> [null, scopeAccess]

        when:
        service.getAndUpdateUserScopeAccessForClientId(userOne, application)
        service.getAndUpdateUserScopeAccessForClientId(userTwo, application)

        then:
        1 * scopeAccessService.addDirectScopeAccess(userOne.getUniqueId(), _)

        then:
        1 * scopeAccessService.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessService.addDirectScopeAccess(userTwo.getUniqueId(), _)
    }

    def "getAndUpdateUserScopeAccessForClientId returns existing scopeAccess if not exipired"() {
        given:
        def user= entityFactory.createUser()
        def scopeAccess = createUserScopeAccess()
        def application = entityFactory.createApplication()

        scopeAccessService.getUserScopeAccessForClientId(_, _) >> scopeAccess

        when:
        def result = service.getAndUpdateUserScopeAccessForClientId(user, application)

        then:
        result.is(scopeAccess)

    }

    def "getAndUpdateClientScopeAccessForClientId adds new scopeAccess and deletes old"() {
        given:
        def applicationOne = entityFactory.createApplication("1", "client1")
        def applicationTwo = entityFactory.createApplication("2", "client2")
        def scopeAccess = expireScopeAccess(createClientScopeAccess())

        scopeAccessService.getClientScopeAccessForClientId(_, _) >>> [ null, scopeAccess ]

        when:
        service.getAndUpdateClientScopeAccessForClientId(applicationOne)
        service.getAndUpdateClientScopeAccessForClientId(applicationTwo)

        then:
        1 * scopeAccessService.addDirectScopeAccess(applicationOne.getUniqueId(), _)

        then:
        1 * scopeAccessService.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessService.addDirectScopeAccess(applicationTwo.getUniqueId(), _)
    }

    def "getAndUpdateClientScopeAccessForClientId returns scopeAccess if not expired"() {
        given:
        def scopeAccess = createClientScopeAccess()
        def application = entityFactory.createApplication()

        scopeAccessService.getClientScopeAccessForClientId(_, _) >> scopeAccess

        when:
        def result = service.getAndUpdateClientScopeAccessForClientId(application)

        then:
        result.is(scopeAccess)
    }

    def "getAndUpdateRackerScopeAccessForClientId deletes old and adds new"() {
        given:
        config.getString("idm.clientId") >> "idmClientId"
        def application = entityFactory.createApplication("1", "client1").with {
            it.RCN = "RCN"
            return it
        }
        def tenantRole = entityFactory.createTenantRole("Racker").with {
            it.clientId = "idmClientId"
            return it
        }
        def rackerOne = entityFactory.createRacker("racker1")
        def rackerTwo = entityFactory.createRacker("racker2")
        def scopeAccess = expireScopeAccess(createRackerScopeAcccss())

        scopeAccessService.getRackerScopeAccessForClientId(_, _) >>> [ null, scopeAccess ]
        tenantService.getTenantRolesForUser(_) >> [ tenantRole ].asList()

        when:
        service.getAndUpdateRackerScopeAccessForClientId(rackerOne, application)
        service.getAndUpdateRackerScopeAccessForClientId(rackerTwo, application)

        then:
        1 * scopeAccessService.addDirectScopeAccess(rackerOne.getUniqueId(), _)

        then:
        1 * scopeAccessService.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessService.addDirectScopeAccess(rackerTwo.getUniqueId(), _)
    }

    def "getAndUpdateRackerScopeAccessForClientId returns existing scopeAccess if not expired"() {        given:
        given:
        config.getString("idm.clientId") >> "idmClientId"
        def application = entityFactory.createApplication("1", "client1").with {
            it.RCN = "RCN"
            return it
        }
        def tenantRole = entityFactory.createTenantRole("Racker").with {
            it.clientId = "idmClientId"
            return it
        }
        def racker = entityFactory.createRacker("racker")
        def scopeAccess = createRackerScopeAcccss()

        scopeAccessService.getRackerScopeAccessForClientId(_, _) >> scopeAccess
        tenantService.getTenantRolesForUser(_) >> [ tenantRole ].asList()

        when:
        def result = service.getAndUpdateRackerScopeAccessForClientId(racker, application)

        then:
        result.is(scopeAccess)
    }

    def "getTokens with refreshtoken credentials adds new and deletes old scopeaccess"() {
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
        scopeAccessService.getScopeAccessByRefreshToken(_) >> scopeAccess
        userService.getUserById(_) >> user

        when:
        def returned = service.getTokens(credentials, new DateTime())

        then:
        returned.clientId == "clientId"
        1 * scopeAccessService.addDirectScopeAccess(_, _)
        1 * scopeAccessService.deleteScopeAccess(_)
    }

    def "getTokens with Authorization Code credentials adds new and deletes old scopeaccess"() {
        given:
        def scopeAccessOne = new DelegatedClientScopeAccess();
        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = refreshDate
        scopeAccessOne.authCode = "authCode12345"
        scopeAccessOne.authCodeExp = refreshDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessOne.clientId = "clientId"

        def credentials = Mock(Credentials)
        credentials.getOAuthGrantType() >> "AUTHORIZATION_CODE"
        credentials.getGrantType() >> "AUTHORIZATION_CODE"
        credentials.getClientId() >> "12345"

        def authResult = Mock(ClientAuthenticationResult)
        authResult.isAuthenticated() >> true
        authResult.getClient() >> new Application().with() {
            it.clientId = "clientId"
            return it
        }

        applicationService.authenticate(_, _) >> authResult

        scopeAccessService.getScopeAccessByAuthCode(_) >> scopeAccessOne

        when:
        service.getTokens(credentials, new DateTime())

        then:
        1 * scopeAccessService.addDirectScopeAccess(_, _)
        1 * scopeAccessService.deleteScopeAccess(_)
    }

   def attribute() {
            return new Attribute("attribute", "value")
        }

    }
