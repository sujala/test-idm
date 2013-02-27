package com.rackspace.idm.domain.service.impl

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
        mockDaos()
        mockMisc()
        mockServices()

        config.getInt("token.cloudAuthExpirationSeconds") >>  defaultExpirationSeconds
        config.getInt("token.expirationSeconds") >> defaultExpirationSeconds
        config.getInt("token.impersonatedExpirationSeconds") >> defaultImpersonationExpirationSeconds
        config.getInt("token.refreshWindowHours") >> defaultRefreshHours
    }

    def "if user is null getOrCreatePasswordResetScopeAccessForUser throws exception"() {
        when:
        service.getOrCreatePasswordResetScopeAccessForUser(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "a passwordResetScopeAccess is retrieved by getOrCreatePasswordResetScopeAccessForUser"() {
        given:
        def prsa = createPasswordResetScopeAccess()

        when:
        service.getOrCreatePasswordResetScopeAccessForUser(entityFactory.createUser())

        then:
        1 * scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> prsa
    }

    def "when getOrCreatePasswordResetScopeAccessForUser and token is expired; old is deleted, and new is added and returned"() {
        given:
        def prsa = createPasswordResetScopeAccess("tokenString", "clientId", "userRsId", expiredDate)

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> prsa

        when:
        def result = service.getOrCreatePasswordResetScopeAccessForUser(entityFactory.createUser())

        then:
        result instanceof PasswordResetScopeAccess
        !result.equals(prsa)

        1 * scopeAccessDao.deleteScopeAccessByDn(_)
        1 * scopeAccessDao.addDirectScopeAccess(_, _)
    }

    def "when getOrCreatePasswordScopeAccessForUser and token is valid, scopeAccess is returned"() {
        given:
        def prsa = createPasswordResetScopeAccess()

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> prsa

        when:
        def result = service.getOrCreatePasswordResetScopeAccessForUser(entityFactory.createUser())

        then:
        result instanceof PasswordResetScopeAccess
        result.equals(prsa)
    }

    def "when getOrCreatePasswordScopeAccessForUser and token DNE, new token is created, added and returned"() {
        given:
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> null

        when:
        def result = service.getOrCreatePasswordResetScopeAccessForUser(entityFactory.createUser())

        then:
        result instanceof PasswordResetScopeAccess

        1 * scopeAccessDao.addDirectScopeAccess(_, _)
    }

    def "updateUserScopeAccessTokenForClientIdByUser throws exception"() {
        when:
        service.updateUserScopeAccessTokenForClientIdByUser(null, "clientId", "token", new Date())

        then:
        thrown(IllegalArgumentException)
    }

    def "updateUserScopeAccessTokenForClientIdByUser gets all scopeAccess for clientId and User"() {
        given:
        applicationDao.getClientByClientId(_) >> entityFactory.createApplication()
        def sa = createUserScopeAccess()
        sa.getAccessTokenExp() >> new DateTime().toDate()

        when:
        service.updateUserScopeAccessTokenForClientIdByUser(entityFactory.createUser(), "clientId", "headerToken", new Date())

        then:
        1 * scopeAccessDao.getScopeAccessesByParentAndClientId(_, _) >> [ sa ].asList()
    }

    def "updateUserScopeAccessTokenForClientIdByUser updates existing scopeAccess"() {
        given:
        def sa = createUserScopeAccess()
        sa.getAccessTokenExp() >> new DateTime().toDate()

        scopeAccessDao.getScopeAccessesByParentAndClientId(_, _) >>  [ sa ].asList()
        applicationDao.getClientByClientId(_) >> entityFactory.createApplication()

        when:
        service.updateUserScopeAccessTokenForClientIdByUser(entityFactory.createUser(), "clientId", sa.getAccessTokenString(), new Date())

        then:
        1 * scopeAccessDao.updateScopeAccess(sa)
    }

    def "updateUserScopeAccessTokenForClientIdByUser deletes oldest token and adds new token"() {
        given:
        def newer_sa = createUserScopeAccess()
        def older_sa = createUserScopeAccess()
        def sa = createUserScopeAccess()

        sa.getAccessTokenExp() >> new Date()
        newer_sa.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        older_sa.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()

        scopeAccessDao.getScopeAccessesByParentAndClientId(_, _) >> [ sa, newer_sa, older_sa ].asList()
        applicationDao.getClientByClientId(_) >> entityFactory.createApplication()

        when:
        service.updateUserScopeAccessTokenForClientIdByUser(entityFactory.createUser(), "clientId", "headerToken", new Date())

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addDirectScopeAccess(_, _)

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
        2 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
    }

    def "update (different parameters) deletes expired"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >>> [
                [ expired_sa ].asList()
        ] >> new ArrayList<ScopeAccess>()

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> sa
        userDao.getUserByDn(_) >> entityFactory.createUser()

        when:
        service.updateExpiredUserScopeAccess("parentUniqueIdString", "clientId")
        service.updateExpiredUserScopeAccess(dn, sharedRandom)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addDirectScopeAccess(dn, _)
    }

    def "getParentDn returns the parentDn"() {

        when:
        def one = service.getBaseDnAsString(dn)
        def two = service.getBaseDnAsString("blah")

        then:
        one.equals(searchDn)
        thrown(IllegalStateException)
    }

    def "getValidUserScopeAccessForClientId adds scopeAccess and deletes old"() {
        given:
        def scopeAccessOne = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("refreshOne", "userRsId", "clientId", refreshDate)
        def scopeAccessThree = createUserScopeAccess("expiredPne", "userRsId", "clientId", expiredDate)
        def scopeAccessFour = createUserScopeAccess("goodOne", "userRsId", "clientId", futureDate)

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [ scopeAccessTwo, scopeAccessThree, scopeAccessFour ]

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId")
        service.getValidUserScopeAccessForClientId(user, "clientId")
        service.getValidUserScopeAccessForClientId(user, "clientId")

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
        2 * scopeAccessDao.addDirectScopeAccess(_, _)
        3 * scopeAccessDao.deleteScopeAccess(_)

    }

    @Ignore
    def "getValidRackerScopeAccessForClientId provisions scopeAccess and adds it"() {
        given:
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> null

        when:
        service.getValidRackerScopeAccessForClientId("1", "12345", "12345")

        then:
        1 * scopeAccessDao.addDirectScopeAccess("1", _)

        then:
        thrown(LDAPSDKUsageException)
    }

    def "getValidRackerScopeAccessForClientId adds new and deletes old as appropriate"() {
        given:
        def rackerScopeAccessOne = createRackerScopeAccess("expired", "rackerId", expiredDate)
        def rackerScopeAccessTwo = createRackerScopeAccess("refresh", "rackerId", refreshDate)
        def rackerScopeAccessThree = createRackerScopeAccess("good", "rackerId", futureDate)

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [ rackerScopeAccessOne, rackerScopeAccessTwo, rackerScopeAccessThree ]

        when:
        service.getValidRackerScopeAccessForClientId("1", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("2", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("3", "12345", "12345")

        then:
        2 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
    }

    def "test parameters for deleteScopeAccessByDn"() {
        when:
        service.deleteScopeAccessByDn(dn)
        service.deleteScopeAccessByDn(null)

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(dn)

        then:
        thrown(IllegalArgumentException)
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
        userDao.getUserByDn(_) >> new User().with() {
            it.username = "username"
            it.id = "id"
            return it
        } >> null

        when:
        UserScopeAccess scopeAccessOne = service.provisionUserScopeAccess(dn, sharedRandom)
        UserScopeAccess scopeAccessTwo = service.provisionUserScopeAccess(dn, sharedRandom)

        then:
        scopeAccessOne.getUsername().equals("username")
        scopeAccessOne.getUserRsId().equals("id")
        scopeAccessOne.getClientId().equals(sharedRandom)
        scopeAccessOne.getAccessTokenString() != null
        scopeAccessOne.getAccessTokenExp() != null

        then:
        thrown(NotFoundException)
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

        scopeAccessDao.getAllImpersonatedScopeAccessForParent(_) >>> [
                expiredList,
                listWithValid,
                listWithTwoImpForOneUser
        ]

        scopeAccessDao.getMostRecentImpersonatedScopeAccessByParentForUser(_, _) >>> [
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
        3 * scopeAccessDao.addImpersonatedScopeAccess(_, _)

        returned.accessTokenString.equals("tokenString6")
    }

    def "atomHopper client is called when expiring all tokens for user" () {
        given:
        User user = new User()
        user.id = "1"
        userDao.getUserByUsername(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getScopeAccessesByParent(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUser("someName")

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is not called when tokens for user are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userDao.getUserByUsername(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccessesByParent(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUser("someName")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is called when expiring all tokens for user by id" () {
        given:
        User user = new User()
        user.id = "1"
        userDao.getUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getScopeAccessesByParent(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUserById("1")

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is not called when tokens for user by id are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userDao.getUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccessesByParent(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUserById("1")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "atomHopper client is called when expiring a token" () {
        given:
        User user = new User()
        user.id = "1"
        defaultUserService.getUserByScopeAccess(_) >> user

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
        defaultUserService.getUserByScopeAccess(_) >> user

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
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess)

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
    }

    def "updateExpiredRackerScopeAccess adds new scopeAccess, keeps existing, and returns new when within refresh window"() {
        given:
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", refreshDate)

        when:
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess)

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addDirectScopeAccess(_, _)
    }

    def "can get endpoints from getOpenstackEndpointsForUser"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant("tenantId", "tenantName")
        def tenantRole = entityFactory.createTenantRole().with { it.roleRsId = "roleRsId"; it.tenantIds = [ "tenantId" ]; return it }
        def tenantRoles = [ tenantRole ].asList()
        def endpoint = entityFactory.createOpenstackEndpoint("tenantId", "tenantName").with { it.baseUrls = [ entityFactory.createCloudBaseUrl() ].asList(); return it }

        when:
        def endpoints = service.getOpenstackEndpointsForUser(user)

        then:
        endpoints != null
        endpoints.size() == 1
        tenantRoleDao.getTenantRolesForUser(user) >> tenantRoles
        tenantDao.getTenant(_) >> tenant
        endpointDao.getOpenstackEndpointsForTenant(_) >> endpoint
    }

    def "isScopeAccessExpired returns true when scopeAccess is null"() {
        given:

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

    def mockDaos() {
        mockScopeAccessDao(service)
        mockUserDao(service)
        mockTenantDao(service)
        mockEndpointDao(service)
        mockApplicationDao(service)
        mockTenantRoleDao(service)
    }

    def mockServices() {
        mockDefaultUserService(service)
        mockDefaultCloud20Service(service)
    }

    def mockMisc() {
        mockConfiguration(service)
        mockAtomHopperClient(service)
        mockAuthHeaderHelper(service)
    }
}
