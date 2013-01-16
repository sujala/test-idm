package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.unboundid.util.LDAPSDKUsageException
import spock.lang.Ignore
import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import com.rackspace.idm.util.AuthHeaderHelper
import spock.lang.Shared
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.dao.ScopeAccessDao
import org.joda.time.DateTime
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.UserScopeAccess

import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.unboundid.ldap.sdk.Attribute
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Application
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
class DefaultScopeAccessServiceGroovyTest extends RootServiceTest {

    @Shared DefaultScopeAccessService service = new DefaultScopeAccessService()
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,rsId=12345,ou=users,o=rackspace"
    @Shared def searchDn = "rsId=12345,ou=users,o=rackspace"

    @Shared def refreshWindow = 6

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        mockDaos()
        mockMisc()
        mockServices()
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
        def prsa = createPasswordResetScopeAccess("tokenString", "clientId", "userRsId", true, false)

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
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", false, false)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", true, false)
        def refresh_sa = createUserScopeAccess("refreshTokenString", "userRsId", "clientId", false, true)

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
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", false, false)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", true, false)

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
        def scopeAccessOne = createUserScopeAccess("expiredOne", "userRsId", "clientId", true, false)
        def scopeAccessTwo = createUserScopeAccess("refreshOne", "userRsId", "clientId", false, true)
        def scopeAccessThree = createUserScopeAccess("expiredPne", "userRsId", "clientId", true, false)
        def scopeAccessFour = createUserScopeAccess("goodOne", "userRsId", "clientId", false, false)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(4).toDate()
        scopeAccessThree.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()
        scopeAccessFour.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [ scopeAccessTwo, scopeAccessThree, scopeAccessFour ]

        when:
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")

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
        def rackerScopeAccessOne = createRackerScopeAccess("expired", "rackerId", true, false)
        def rackerScopeAccessTwo = createRackerScopeAccess("refresh", "rackerId", false, true)
        def rackerScopeAccessThree = createRackerScopeAccess("good", "rackerId", false, false)

        rackerScopeAccessOne.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()
        rackerScopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(4).toDate()
        rackerScopeAccessThree.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

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
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impToken1", true, false)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user2", "impUser2", "tokenString2", "impToken2", true, false)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user3", "impUser3", "tokenString3", "impToken3", true, false)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user4", "impUser4", "tokenString4", "impToken4", true, false)
        ImpersonatedScopeAccess scopeAccessFive = createImpersonatedScopeAccess("user5", "impUser5", "tokenString5", "impToken5", false, true)
        ImpersonatedScopeAccess scopeAccessSix = createImpersonatedScopeAccess("user6", "impUser6", "tokenString6", "impToken6", false, false)

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser("userId", "userToBeImpersonated", "displayName", "email@email.com", true)
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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()

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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()

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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

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

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", false, true)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().minusHours(6).toDate()

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
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", true, false)

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
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", false, true)

        when:
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess)

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addDirectScopeAccess(_, _)
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

        config.getInt("token.cloudAuthExpirationSeconds") >> 21600 // 6 hours
        config.getInt("token.expirationSeconds") >> 43200 //12 hours
    }
}
