package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import com.unboundid.util.LDAPSDKUsageException
import spock.lang.Ignore
import spock.lang.Shared
import com.rackspace.idm.domain.entity.ScopeAccess
import org.joda.time.DateTime
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import spock.lang.Unroll
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/29/12
 * Time: 6:14 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultScopeAccessServiceTest extends RootServiceTest {

    @Shared DefaultScopeAccessService service = new DefaultScopeAccessService()
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,rsId=12345,ou=users,o=rackspace"
    @Shared def searchDn = "rsId=12345,ou=users,o=rackspace"

    @Shared def expiredDate
    @Shared def refreshDate
    @Shared def futureDate

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")

        expiredDate = new DateTime().minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(defaultRefreshHours - 1).toDate()
        futureDate = new DateTime().plusHours(defaultRefreshHours + 1).toDate()
    }

    def setup() {
        mockConfiguration(service)
        mockAtomHopperClient(service)
        mockAuthHeaderHelper(service)
        mockScopeAccessDao(service)
        mockUserService(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockApplicationService(service)

        config.getInt("token.cloudAuthExpirationSeconds") >>  defaultCloudAuthExpirationSeconds
        config.getInt("token.expirationSeconds") >> defaultExpirationSeconds
        config.getInt("token.impersonatedExpirationSeconds") >> defaultImpersonationExpirationSeconds
        config.getInt("token.refreshWindowHours") >> defaultRefreshHours
    }

    def "update expired user scope access gets all scope access for parent"() {
        given:
        def sa = createUserScopeAccess()
        def sa_2 = createUserScopeAccess("newToken", null, null, null)
        def user = entityFactory.createUser()

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.getScopeAccesses(user) >> [sa].asList()
    }

    def "update expired user scope access adds new scope access entity to the directory"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def refresh_sa = createUserScopeAccess("refreshTokenString", "userRsId", "clientId", refreshDate)

        when:
        service.updateExpiredUserScopeAccess(sa, true)
        service.updateExpiredUserScopeAccess(expired_sa, false)
        service.updateExpiredUserScopeAccess(refresh_sa, false)

        then:
        2 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "update (different parameters) deletes expired"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >>> [
                [ sa, expired_sa ].asList()
        ] >> new ArrayList<ScopeAccess>()

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)
        service.updateExpiredUserScopeAccess(user, sharedRandom, null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "updateExpiredUserScopeAccess gets token entropy and adjusts expiration"() {
        given:
        def expiredScopeAccess = createUserScopeAccess().with {
            it.accessTokenExp = new DateTime().minusSeconds(86400).toDate()
            return it
        }

        HashMap<String, Date> range
        if (impersonated)
            range = getRange(defaultImpersonationExpirationSeconds, entropy)
        else {
            range = getRange(defaultCloudAuthExpirationSeconds, entropy)
        }

        when:
        def scopeAccess = service.updateExpiredUserScopeAccess(expiredScopeAccess, impersonated)

        then:
        1 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= range.get("max")
        scopeAccess.accessTokenExp >= range.get("min")

        where:
        impersonated | entropy
        false        | 0.01
        true         | 0.05
    }

    def "getValidUserScopeAccessForClientId adds scopeAccess and deletes old"() {
        given:
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)
        def mostRecentScopeAccess = createUserScopeAccess("refreshOne", "userRsId", "clientId", refreshDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccess].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientId(_, _) >> mostRecentScopeAccess

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "getValidUserScopeAccessForClientId deletes expired tokens and add new ones"() {
        given:
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)
        def mostRecentScopeAccess = createUserScopeAccess("expiredPne", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccess, mostRecentScopeAccess].asList()

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId", null)

        then:
        2 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "getValidUserScopeAccessForClientId deletes expired token and keeps future token"() {
        given:
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)
        def mostRecentScopeAccess = createUserScopeAccess("goodOne", "userRsId", "clientId", futureDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccess].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientId(_, _) >> mostRecentScopeAccess

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    @Ignore
    def "getValidRackerScopeAccessForClientId provisions scopeAccess and adds it"() {
        given:
        scopeAccessDao.getMostRecentScopeAccessByClientId(_, _) >> null

        when:
        service.getValidRackerScopeAccessForClientId(entityFactory.createUser(), "12345", null)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _)

        then:
        thrown(LDAPSDKUsageException)
    }

    def "getValidRackerScopeAccessForClientId adds new and deletes old as appropriate"() {
        given:
        def rackerScopeAccessOne = createRackerScopeAccess("expired", "rackerId", expiredDate)
        def rackerScopeAccessTwo = createRackerScopeAccess("refresh", "rackerId", refreshDate)
        def rackerScopeAccessThree = createRackerScopeAccess("good", "rackerId", futureDate)
        Racker racker = entityFactory.createRacker()

        scopeAccessDao.getMostRecentScopeAccessByClientId(_, _) >>> [ rackerScopeAccessOne, rackerScopeAccessTwo, rackerScopeAccessThree ]

        when:
        service.getValidRackerScopeAccessForClientId(racker, "12345", null)
        service.getValidRackerScopeAccessForClientId(racker, "12345", null)
        service.getValidRackerScopeAccessForClientId(racker, "12345", null)

        then:
        2 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "updateScopeAccess updates scopeAccess (no methods using modify accessToken)"() {
        given:
        def mockedScopeAccess = Mock(ScopeAccess)

        when:
        service.updateScopeAccess(mockedScopeAccess)

        then:
        1 * scopeAccessDao.updateScopeAccess(_)
    }

    def "provisionUserScopeAccess adds necessary values to scopeAccess"() {
        given:
        def user = new User().with() {
            it.username = "username"
            it.id = "id"
            return it
        }

        when:
        UserScopeAccess scopeAccessOne = service.provisionUserScopeAccess(user, sharedRandom)

        then:
        scopeAccessOne.getUsername().equals("username")
        scopeAccessOne.getUserRsId().equals("id")
        scopeAccessOne.getClientId().equals(sharedRandom)
        scopeAccessOne.getAccessTokenString() != null
        scopeAccessOne.getAccessTokenExp() != null
    }

    def "addImpersonatedScopeAccess deletes expired scopeAccess and creates new scopeAccess"() {
        given:
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impToken1", expiredDate)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user2", "impUser2", "tokenString2", "impToken2", expiredDate)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user3", "impUser3", "tokenString3", "impToken3", expiredDate)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user4", "impUser4", "tokenString4", "impToken4", expiredDate)
        ImpersonatedScopeAccess scopeAccessFive = createImpersonatedScopeAccess("user5", "impUser5", "tokenString5", "impToken5", refreshDate)
        ImpersonatedScopeAccess scopeAccessSix = createImpersonatedScopeAccess("user6", "impUser6", "tokenString6", "impToken6", futureDate)

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser("userId", "userToBeImpersonated")
            return it
        }

        def expiredList = [ scopeAccessOne, scopeAccessTwo ].asList()
        def listWithValid = [ scopeAccessThree, scopeAccessFour ].asList()
        def listWithTwoImpForOneUser = [ scopeAccessFour, scopeAccessFive, scopeAccessSix ].asList()

        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >>> [
                expiredList,
                listWithValid,
                listWithTwoImpForOneUser
        ]

        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUser(_, _) >>> [
                null,
                scopeAccessThree,
                scopeAccessSix
        ]

        when:
        service.addImpersonatedScopeAccess(new User(), "clientId", "impersonating-token", request)
        service.addImpersonatedScopeAccess(new User(), "clientId", "impersonating-token", request)
        ImpersonatedScopeAccess returned = service.addImpersonatedScopeAccess(new User(), "clientId", "impersonating-token", request)

        then:
        6 * scopeAccessDao.deleteScopeAccess(_)
        3 * scopeAccessDao.addScopeAccess(_, _)

        returned.accessTokenString.equals("tokenString6")
    }

    def "atomHopper client is called when expiring all tokens for user" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUser(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUser("someName")

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is not called when tokens for user are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUsersByUsername(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUser("someName")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is called when expiring all tokens for user by id" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUserById("1")

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is not called when tokens for user by id are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUserById("1")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is called when expiring a token" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", futureDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getScopeAccessByAccessToken(_) >> scopeAccessOne

        when:
        service.expireAccessToken("someToken")

        then:
        1 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is not called when expiring a token that is already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccessByAccessToken(_) >> scopeAccessOne

        when:
        service.expireAccessToken("someToken")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "isExpired with null date"() {
        when:
        service.isExpired(null)
        then:
        true

    }

    def "updateExpiredRackerScopeAccess adds new scopeAccess, deletes existing expired scopeAccess, and returns new scopeAccess"() {
        given:
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", expiredDate)

        when:
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess, ["PASSWORD"].asList())

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "updateExpiredRackerScopeAccess adds new scopeAccess, keeps existing, and returns new when within refresh window"() {
        given:
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", refreshDate)

        when:
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess, ["PASSWORD"].asList())

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "can get endpoints from getOpenstackEndpointsForUser"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant("tenantId", "tenantName")
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "roleRsId"
            it.tenantIds = [ "tenantId" ].asList()
            return it
        }
        def tenantRoles = [ tenantRole ].asList()
        def endpoint = entityFactory.createOpenstackEndpoint("tenantId", "tenantName").with {
            it.baseUrls = [ entityFactory.createCloudBaseUrl() ].asList()
            return it
        }

        when:
        def endpoints = service.getOpenstackEndpointsForUser(user)

        then:
        endpoints != null
        endpoints.size() == 1
        tenantService.getTenantRolesForUser(user) >> tenantRoles
        tenantService.getTenant(_) >> tenant
        endpointService.getOpenStackEndpointForTenant(_, _, _) >> endpoint
    }

    def "isScopeAccessExpired returns true when scopeAccess is null"() {
        when:
        def result = service.isScopeAccessExpired(null)

        then:
        result == true
    }

    def "isScopeAccessExpired returns true when scopeAccess is expired"() {
        given:
        def scopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.isScopeAccessExpired(scopeAccess)

        then:
        result == true
    }

    def "isScopeAccessExpired returns false when scopeAccess is not expired"() {
        given:
        def scopeAccess = createUserScopeAccess()

        when:
        def result = service.isScopeAccessExpired(scopeAccess)

        then:
        result == false
    }

    def "the Dao is used to retrieve a list of ScopeAccess for the user"() {
        when:
        service.getScopeAccessesForUser(entityFactory.createUser())

        then:
        1 * scopeAccessDao.getScopeAccesses(_)
    }

    def "calling getValidRackerScopeAccessForClientId sets authenticatedBy"() {
        given:
        def authenticatedBy = ["RSA"].asList()

        when:
        service.getValidRackerScopeAccessForClientId(entityFactory.createRacker(), "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling updateExpiredUserScopeAccess without scopeAccess sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [].asList()

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling updateExpiredUserScopeAccess with scopeAccess sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [createScopeAccess(), expireScopeAccess(createUserScopeAccess())].asList()

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling getValidUserScopeAccessForClientId sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [createScopeAccess(),expireScopeAccess(createUserScopeAccess())].asList()

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling getUserScopeAccessForClientIdByUsernameAndApiCredentials sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.getUserScopeAccessForClientIdByUsernameAndApiCredentials("username", "apiKey", "clientId")

        then:
        1 * userService.authenticateWithApiKey(_, _) >> new UserAuthenticationResult(user, true)

        1 * scopeAccessDao.getScopeAccesses(_) >> [].asList()

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == [GlobalConstants.AUTHENTICATED_BY_APIKEY] as Set)
        }
    }

    def "calling getUserScopeAccessForClientIdByUsernameAndPassword sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()

        when:
        service.getUserScopeAccessForClientIdByUsernameAndPassword("username", "password", "clientId")

        then:
        1 * userService.authenticate(_, _) >> new UserAuthenticationResult(user, true)

        1 * scopeAccessDao.getScopeAccesses(_) >> [].asList()

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == [GlobalConstants.AUTHENTICATED_BY_PASSWORD] as Set)
        }
    }

    def "Duplicate tenant reference in roles doesn't create duplicate endpoints" (){
            def role = Mock(com.rackspace.idm.domain.entity.TenantRole)
            def listRoles = [role, role].asList()
            def token = Mock(ScopeAccess)
            def endPoint = Mock(OpenstackEndpoint)
            def tenant = Mock(com.rackspace.idm.domain.entity.Tenant)
            def tenant2 = Mock(com.rackspace.idm.domain.entity.Tenant)

            tenantService.getTenantRolesForScopeAccess(_) >>listRoles
            tenantService.getTenant(_)>>>[tenant, tenant2];
            role.getTenantIds() >> ["1"].asList()
            endPoint.getBaseUrls() >> [Mock(CloudBaseUrl)].asList()
            endpointService.getOpenStackEndpointForTenant(_, _, _) >> endPoint
        when:
            def endPointList = service.getOpenstackEndpointsForScopeAccess(token)
        then:
            endPointList.size() == 1
    }

    def "Verify provision user scope access adds token expiration entropy"() {
        when:
        def range = getRange(exSeconds, entropy)
        UserScopeAccess scopeAccess = service.provisionUserScopeAccess(entityFactory.createUser(), "clientId")

        then:
        1 * config.getInt("token.cloudAuthExpirationSeconds") >> exSeconds
        1 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= ((Date)range.get("max"))
        scopeAccess.accessTokenExp >= range.get("min")

        where:
        exSeconds | entropy
        2600      | 0.01

    }

    def "Set entropy"() {
        given:
        def seconds = 2600
        def max = seconds * 1.01
        def min = seconds * 0.99

        when:
        def result = service.getTokenExpirationSeconds(seconds)

        then:
        1 * config.getDouble(_) >> 0.01
        result <= max
        result >= min
    }

    @Unroll
    def "setImpersonatedScopeAccess adds entropy to token expiration"() {
        given:
        config.getInt("token.impersonatedByRackerMaxSeconds") >> exSeconds * 2
        config.getInt("token.impersonatedByServiceMaxSeconds") >> exSeconds * 2
        config.getInt("token.impersonatedByRackerDefaultSeconds") >> exSeconds
        config.getInt("token.impersonatedByServiceDefaultSeconds") >> exSeconds

        def impersonationRequest = v1Factory.createImpersonationRequest(v2Factory.createUser())
        if (notNullExpireIn) {
            impersonationRequest.expireInSeconds = exSeconds
        }

        def caller
        def range
        if (isRacker) {
            caller = entityFactory.createRacker()
            range = getRange(exSeconds, entropy)
        } else {
            caller = entityFactory.createUser()
            range = getRange(exSeconds, entropy)
        }
        def scopeAccess = createImpersonatedScopeAccess().with {
            it.accessTokenExp = new DateTime().minusSeconds(3600).toDate()
            return it
        }

        when:
        def returnedSA = service.setImpersonatedScopeAccess(caller, impersonationRequest, scopeAccess)

        then:
        if (notNullExpireIn){
            0 * config.getDouble("token.entropy") >> entropy
        } else{
            1 * config.getDouble("token.entropy") >> entropy
        }

        returnedSA.accessTokenExp <= range.get("max")
        returnedSA.accessTokenExp >= range.get("min")

        where:
        isRacker | exSeconds | entropy | notNullExpireIn
        true     | 3600      | 0.01    | false
        false    | 9000      | 0.05    | false
        true     | 3600      | 0.01    | true
        false    | 9000      | 0.05    | true
    }

    def "validateExpireInElement accounts for expiration entropy"() {
        given:
        config.getInt("token.impersonatedByRackerMaxSeconds") >> expireIn * 2
        config.getInt("token.impersonatedByServiceMaxSeconds") >> expireIn * 2

        def caller
        if (isRacker) {
            caller = entityFactory.createRacker()
        } else {
            caller = entityFactory.createUser()
        }

        def request = v1Factory.createImpersonationRequest(v2Factory.createUser())
        request.expireInSeconds = expireIn

        when:
        service.validateExpireInElement(caller, request)

        then:
        0 * config.getDouble("token.entropy") >> entropy

        where:
        isRacker | expireIn | entropy
        true     | 3600     | 0.01
        false    | 3600     | 0.01
    }

    def "calling getValidRackerScopeAccessForClientId sets expiration with entropy if non existing"() {
        given:
        def entropy = 0.01
        def clientId = "clientId"
        def authedBy = ["PASSWORD"].asList()
        def range = getRange(defaultCloudAuthExpirationSeconds, entropy)

        scopeAccessDao.getMostRecentScopeAccessByClientId(_, _) >> null

        when:
        def scopeAccess = service.getValidRackerScopeAccessForClientId(entityFactory.createRacker(), clientId, authedBy)

        then:
        2 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= range.get("max")
        scopeAccess.accessTokenExp >= range.get("min")
    }

    def "updateExpiredRackerScopeAccess sets token expiration with entropy"() {
        given:
        def range = getRange(defaultCloudAuthExpirationSeconds, entropy)
        def scopeAccess = createRackerScopeAcccss()
        scopeAccess.accessTokenExp = new DateTime().minusSeconds(3600).toDate()
        if (refresh) {
            scopeAccess.accessTokenExp = new DateTime().plusSeconds(defaultRefreshSeconds - 60).toDate()
        }

        when:
        def sa = service.updateExpiredRackerScopeAccess(scopeAccess, ["PASSWORD"].asList())

        then:
        1 * config.getDouble("token.entropy") >> entropy
        sa.accessTokenExp <= range.get("max")
        sa.accessTokenExp >= range.get("min")

        where:
        refresh | entropy
        true    | 0.01
        false   | 0.01
    }

    def "authenticateAccessToken returns true when token is valid" () {
        given:
        def sa = entityFactory.createScopeAccess()
        sa.accessTokenExp = new Date()
        sa.accessTokenExp.hours += 24

        when:
        def result = service.authenticateAccessToken("abc123")

        then:
        1 * scopeAccessDao.getScopeAccessByAccessToken(_) >> sa
        result == true
    }

    def "authenticateAccessToken with null token returns false" () {
        when:
        def result = service.authenticateAccessToken("abc123")

        then:
        1 * scopeAccessDao.getScopeAccessByAccessToken(_) >> null
        result == false
    }

    def "delete expired tokens" () {
        given:
        def user = Mock(User)

        and:
        user.getId() >> "id"
        scopeAccessDao.getScopeAccessesByUserId("id") >> [createScopeAccess(), createScopeAccess("tokenString", new DateTime().minusDays(1).toDate())].asList()

        when:
        service.deleteExpiredTokens(user)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "calling getOpenStackType returns the correct value"() {
        given:
        def role = entityFactory.createTenantRole()
        def application = entityFactory.createApplication().with {
            it.openStackType = applicationType
            it
        }

        when:
        def result = service.getOpenStackType(role)

        then:
        1 * applicationService.getById(_) >> application
        result == expectedResult

        where:
        applicationType | expectedResult
        "object-store"  | "NAST"
        "compute"       | "MOSSO"
        null            | null
    }

    def "calling getRegion returns correct value"() {
        given:
        def token = entityFactory.createScopeAccess()

        when:
        def result = service.getRegion(token)

        then:
        1 * userService.getUserByScopeAccess(token) >> user
        result == expectedRegion

        where:
        expectedRegion  | user
        "ORD"           | entityFactory.createUser().with {it.region="ORD";it}
        null            | entityFactory.createRacker()
        null            | null

    }

    def getRange(seconds, entropy) {
        HashMap<String, Date> range = new HashMap<>()
        range.put("min", new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate())
        range.put("max", new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate())
        return range
    }
}
