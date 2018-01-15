package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.TokenFormat
import com.unboundid.util.LDAPSDKUsageException
import org.joda.time.DateTime
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultScopeAccessServiceTest extends RootServiceTest {

    @Shared DefaultScopeAccessService service = new DefaultScopeAccessService()
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,rsId=12345,ou=users,o=rackspace"
    @Shared def searchDn = "rsId=12345,ou=users,o=rackspace"

    private ScopeAccessDao uuidScopeAccessDao;

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
        uuidScopeAccessDao = Mock(UUIDScopeAccessDao)

        mockConfiguration(service)
        mockAtomHopperClient(service)
        mockAuthHeaderHelper(service)
        mockScopeAccessDao(service)
        mockUserService(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockApplicationService(service)
        mockIdentityUserService(service)
        mockTokenFormatSelector(service)
        mockIdentityConfig(service)
        mockAuthorizationService(service)
        service.uuidScopeAccessDao = uuidScopeAccessDao

        config.getInt("token.cloudAuthExpirationSeconds", _) >>  defaultCloudAuthExpirationSeconds
        config.getInt("token.cloudAuthRackerExpirationSeconds", _) >>  defaultCloudAuthRackerExpirationSeconds
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
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa_2
    }

    def "update expired user scope access adds new scope access entity to the directory"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def refresh_sa = createUserScopeAccess("refreshTokenString", "userRsId", "clientId", refreshDate)

        when:
        service.updateExpiredUserScopeAccess(sa)
        service.updateExpiredUserScopeAccess(expired_sa)
        service.updateExpiredUserScopeAccess(refresh_sa)

        then:
        2 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "update expired user scope access delete failure ignored when ignore delete failure enabled"() {
        given:
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)

        when:
        service.updateExpiredUserScopeAccess(expired_sa)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update (different parameters) deletes expired"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >>> [
                [ expired_sa ].asList()
        ] >> new ArrayList<ScopeAccess>()

        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)
        service.updateExpiredUserScopeAccess(user, sharedRandom, null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "update expired user scope access handles error in DAO when cleaning single token with ignore failures enabled, stop cleanup disabled"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access multiple token cleanup - failure on first delete when ignore failures enabled, stop cleanup disabled results in 2 deletes"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def expired_sa2 = createUserScopeAccess("expiredTokenString2", "userRsId2", "clientId2", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa, expired_sa2 ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        2 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "updateExpiredUserScopeAccess gets token entropy and adjusts expiration"() {
        given:
        def expiredScopeAccess = createUserScopeAccess().with {
            it.accessTokenExp = new DateTime().minusSeconds(86400).toDate()
            return it
        }

        HashMap<String, Date> range = getRange(defaultCloudAuthExpirationSeconds, entropy)

        when:
        def scopeAccess = service.updateExpiredUserScopeAccess(expiredScopeAccess)

        then:
        1 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= range.get("max")
        scopeAccess.accessTokenExp >= range.get("min")

        where:
        entropy | _
        0.01 | _
    }

    def "getValidUserScopeAccessForClientId adds scopeAccess and deletes old"() {
        given:
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)
        def mostRecentScopeAccess = createUserScopeAccess("refreshOne", "userRsId", "clientId", refreshDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccess].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> mostRecentScopeAccess

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

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccess].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> mostRecentScopeAccess

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
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> mostRecentScopeAccess

        when:
        service.getValidUserScopeAccessForClientId(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    @Ignore
    def "getValidRackerScopeAccessForClientId provisions scopeAccess and adds it"() {
        given:
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> null

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
        scopeAccessOne.getUserRsId().equals("id")
        scopeAccessOne.getClientId().equals(sharedRandom)
        scopeAccessOne.getAccessTokenString() != null
        scopeAccessOne.getAccessTokenExp() != null
    }

    def "processImpersonatedScopeAccessRequest deletes expired scopeAccess and _last_ valid token and creates new scopeAccess"() {
        given:
        def existingValidImpersonationTokenString = "validImpersonatingToken"
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impersonatedToken1", expiredDate)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user1", "impUser2", "tokenString2", "impersonatedToken2", expiredDate)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user1", "impUser3", "tokenString3", "impersonatedToken2", futureDate)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user1", "impUser3", existingValidImpersonationTokenString, "impersonatedToken4", futureDate)
        UserScopeAccess existingUserScopeAccess = createUserScopeAccess("userScope", "userid", "clientId", new DateTime().plusDays(5).toDate())

        def impersonatedUser = entityFactory.createUser("username", "userId", "domainId", "region")
        def impersonator = entityFactory.createUser("username2", "userId2", "domainId2", "region2")

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser(impersonatedUser.id, impersonatedUser.username)
            it.expireInSeconds = 1000
            return it
        }

        def expiredList = [ scopeAccessOne, scopeAccessTwo ].asList()
        def listWithValid = [ scopeAccessThree, scopeAccessFour].asList()
        def listAll = expiredList + listWithValid

        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.UUID

        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >>> [
                listAll,
        ]

        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(_, _, _) >>> [
                scopeAccessFour,
        ]

        when:
        ImpersonatedScopeAccess returned = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then:
        3 * scopeAccessDao.deleteScopeAccess(_)
        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * uuidScopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_, _, _) >> existingUserScopeAccess

        returned.accessTokenString.equals(existingValidImpersonationTokenString)
    }

    def "addImpersonatedScopeAccess deleting expired tokens logic based on property"() {
        given:
        def impersonatingUser = "userToBeImpersonated"
        def nonExpiredTokenStr = "impToken4"
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impToken1", expiredDate)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user1", "impUser1", "tokenString2", "impToken2", expiredDate)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user1", "impUser1", "tokenString3", "impToken3", expiredDate)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user1", "impUser1", nonExpiredTokenStr, "impToken4", futureDate)
        ImpersonatedScopeAccess scopeAccessFive = createImpersonatedScopeAccess("user1", "impUser2", "tokenString5", "impToken5", expiredDate)
        ImpersonatedScopeAccess scopeAccessSix = createImpersonatedScopeAccess("user1", "impUser2", "tokenString6", "impToken6", futureDate)

        //return valid user scope token
        UserScopeAccess existingUserScopeAccess = createUserScopeAccess("userScope", "userid", "clientId", new DateTime().plusDays(5).toDate())
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_, _, _) >> existingUserScopeAccess
        config.getString("cloudAuth.clientId") >> "clientId"

        def impersonatedUser = entityFactory.createUser(impersonatingUser, "userId", "domainId", "region")
        def impersonator = entityFactory.createUser("username2", "userId2", "domainId2", "region2")

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser(impersonatedUser.id, impersonatedUser.username)
            it.expireInSeconds = 1000
            return it
        }

        def expiredList = [ scopeAccessOne, scopeAccessTwo, scopeAccessThree ].asList()
        def listWithValid = [ scopeAccessFour ].asList()
        def otherUserList = [scopeAccessFive, scopeAccessSix].asList()

        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >> [
                expiredList,
                listWithValid,
                otherUserList
        ].asList().flatten()

        scopeAccessDao.getAllImpersonatedScopeAccessForUserOfUserByRsId(_, _) >> [
                expiredList,
                listWithValid
        ].asList().flatten()

        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(_, _, _) >> scopeAccessFour

        scopeAccessDao.getAllImpersonatedScopeAccessForUserOfUserByUsername(_, _) >> [].asList()

        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.UUID

        when: "optimize is turned off"
        ImpersonatedScopeAccess nonOptResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "all expired tokens are deleted regardless of user"
        1 * config.getBoolean(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME, _) >> false
        5 * scopeAccessDao.deleteScopeAccess(_) //all 4 expired will be deleted, plus the non-expired of impersonated user
        1 * scopeAccessDao.addScopeAccess(_, _)
        nonOptResult != scopeAccessFour
        nonOptResult.accessTokenString.equals(nonExpiredTokenStr)

        when: "optimize is turned on"
        ImpersonatedScopeAccess optResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "all expired tokens are deleted regardless of user"
        1 * config.getBoolean(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME, _) >> true
        4 * scopeAccessDao.deleteScopeAccess(_) //all 3 expired of the impersonated user will be deleted, plus the non-expired of impersonated user. but NOT the other user expired
        1 * scopeAccessDao.addScopeAccess(_, _)
        optResult != scopeAccessFour
        optResult.accessTokenString.equals(nonExpiredTokenStr)
    }


    def "addImpersonatedScopeAccess if expired token deletion causes exception then exception bubbles up if feature ignore token delete disabled"() {
        given:
        def exceptionToThrow = new RuntimeException("Throwing exception to verify that no more deletes are called once exception thrown")

        def deleteCount = 0
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impToken1", expiredDate)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user2", "impUser1", "tokenString2", "impToken2", expiredDate)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user3", "impUser1", "tokenString3", "impToken3", expiredDate)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user4", "impUser1", "tokenString4", "impToken4", futureDate)
        ImpersonatedScopeAccess scopeAccessFive = createImpersonatedScopeAccess("user5", "impUser5", "tokenString5", "impToken5", expiredDate)
        ImpersonatedScopeAccess scopeAccessSix = createImpersonatedScopeAccess("user6", "impUser6", "tokenString6", "impToken6", futureDate)

        //return valid user scope token
        UserScopeAccess existingUserScopeAccess = createUserScopeAccess("userScope", "userid", "clientId", new DateTime().plusDays(5).toDate())
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> existingUserScopeAccess
        config.getString("cloudAuth.clientId") >> "clientId"

        def impersonatedUser = entityFactory.createUser("username", "userId", "domainId", "region")
        def impersonator = entityFactory.createUser("username2", "userId2", "domainId2", "region2")

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser(impersonatedUser.id, impersonatedUser.username)
            it.expireInSeconds = 1000
            return it
        }

        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >> [ scopeAccessOne, scopeAccessTwo, scopeAccessThree, scopeAccessFour, scopeAccessFive, scopeAccessSix].asList()
        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUser(_, _) >> scopeAccessFour
        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.UUID

        when: "exception encountered deleting second of three expired tokens"
        ImpersonatedScopeAccess optResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "no attempt is made to delete the other expired tokens or the valid token, and exception is bubbled"
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false
        2 * scopeAccessDao.deleteScopeAccess(_) >> { if (++deleteCount > 1) throw exceptionToThrow }
        RuntimeException ex = thrown()
        ex == exceptionToThrow
    }

    def "addImpersonatedScopeAccess if expired token deletion causes exception, exception is ignored and cleanup routine exited if feature ignore token delete enabled"() {
        given:
        def exceptionToThrow = new RuntimeException("Throwing exception to verify that no more deletes are called once exception thrown")
        def deleteCount = 0
        ImpersonatedScopeAccess scopeAccessOne = createImpersonatedScopeAccess("user1", "impUser1", "tokenString1", "impToken1", expiredDate)
        ImpersonatedScopeAccess scopeAccessTwo = createImpersonatedScopeAccess("user2", "impUser1", "tokenString2", "impToken2", expiredDate)
        ImpersonatedScopeAccess scopeAccessThree = createImpersonatedScopeAccess("user3", "impUser1", "tokenString3", "impToken3", expiredDate)
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user4", "impUser1", "tokenString4", "impToken4", futureDate)
        ImpersonatedScopeAccess scopeAccessFive = createImpersonatedScopeAccess("user5", "impUser2", "tokenString5", "impToken5", expiredDate)
        ImpersonatedScopeAccess scopeAccessSix = createImpersonatedScopeAccess("user6", "impUser3", "tokenString6", "impToken6", futureDate)

        //return valid user scope token
        UserScopeAccess existingUserScopeAccess = createUserScopeAccess("userScope", "userid", "clientId", new DateTime().plusDays(5).toDate())
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_, _, _) >> existingUserScopeAccess
        config.getString("cloudAuth.clientId") >> "clientId"

        def impersonatedUser = entityFactory.createUser("username", "userId", "domainId", "region")
        def impersonator = entityFactory.createUser("username2", "userId2", "domainId2", "region2")

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser(impersonatedUser.id, impersonatedUser.username)
            it.expireInSeconds = 1000
            return it
        }
        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >> [ scopeAccessOne, scopeAccessTwo, scopeAccessThree, scopeAccessFour, scopeAccessFive, scopeAccessSix].asList()
        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(_, _, _) >> scopeAccessFour
        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.UUID

        when: "exception encountered deleting second of three expired tokens"
        ImpersonatedScopeAccess optResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "no attempt is made to delete third expired token, but valid token is still deleted, scope access added and no exception thrown"
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true
        3 * scopeAccessDao.deleteScopeAccess(_) >> { if (++deleteCount == 2) throw exceptionToThrow }
        1 * scopeAccessDao.addScopeAccess(_, _)
        notThrown(RuntimeException)
    }

    def "addImpersonatedScopeAccess if valid token deletion causes exception, exception is bubbled"() {
        given:
        def exceptionToThrow = new RuntimeException("Throwing exception to verify that no more deletes are called once exception thrown")
        ImpersonatedScopeAccess scopeAccessFour = createImpersonatedScopeAccess("user4", "impUser1", "tokenString4", "impToken4", futureDate)

        //return valid user scope token
        UserScopeAccess existingUserScopeAccess = createUserScopeAccess("userScope", "userid", "clientId", new DateTime().plusDays(5).toDate())
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> existingUserScopeAccess
        config.getString("cloudAuth.clientId") >> "clientId"

        def impersonatedUser = entityFactory.createUser("username", "userId", "domainId", "region")
        def impersonator = entityFactory.createUser("username2", "userId2", "domainId2", "region2")

        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser(impersonatedUser.id, impersonatedUser.username)
            it.expireInSeconds = 1000
            return it
        }

        scopeAccessDao.getAllImpersonatedScopeAccessForUser(_) >> [scopeAccessFour].asList()
        scopeAccessDao.getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(_, _, _) >> scopeAccessFour
        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.UUID

        when: "exception encountered deleting valid token"
        ImpersonatedScopeAccess optResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "no attempt is made to delete third expired token, but valid token is deleted and no exception thrown"
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw exceptionToThrow }
        RuntimeException ex = thrown()
        ex == exceptionToThrow
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

    def "updateExpiredRackerScopeAccess adds new scopeAccess, ignore deletes existing expired scopeAccess failure, and returns new scopeAccess when ignore delete enabled"() {
        given:
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", expiredDate)

        when:
        def newScopeAccess = service.updateExpiredRackerScopeAccess(oldScopeAccess, ["PASSWORD"].asList())

        then:
        newScopeAccess != oldScopeAccess
        newScopeAccess instanceof RackerScopeAccess
        ! newScopeAccess.isAccessTokenExpired(new DateTime())

        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException("error")}
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
        endpointService.getOpenStackEndpointForTenant(_, _, _,_) >> endpoint
        applicationService.getById(_) >> new Application().with {
            it.openStackType = "compute"
            it
        }
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

    def "calling updateExpiredUserScopeAccess with impersonated token expiring after user token"() {
        given:
        def user = entityFactory.createUser()
        def impersonatedScopeAccess = new ImpersonatedScopeAccess().with {
            it.impersonatingToken = "impToken"
            it.impersonatingUsername = "user"
            it.accessTokenExp = new Date().plus(2)
            it.accessTokenString = "tokenString"
            it
        }
        def userScopeAccess = new UserScopeAccess().with {
            it.accessTokenString = "token"
            it.clientId = "clientId"
            it.accessTokenExp = new Date().plus(1)
            it
        }

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [impersonatedScopeAccess, userScopeAccess].asList()
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> createUserScopeAccess()
        0 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "calling updateExpiredUserScopeAccess with impersonated token and user token both expired"() {
        given:
        def user = entityFactory.createUser()
        def impersonatedScopeAccess = new ImpersonatedScopeAccess().with {
            it.impersonatingToken = "impToken"
            it.impersonatingUsername = "user"
            it.accessTokenExp = new Date().minus(1)
            it.accessTokenString = "tokenString"
            it
        }
        def userScopeAccess = new UserScopeAccess().with {
            it.accessTokenString = "token"
            it.clientId = "clientId"
            it.accessTokenExp = new Date().minus(2)
            it
        }

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [impersonatedScopeAccess, userScopeAccess].asList()
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> expireScopeAccess(createUserScopeAccess())
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "calling updateExpiredUserScopeAccess with only impersonated token"() {
        given:
        def user = entityFactory.createUser()
        def impersonatedScopeAccess = new ImpersonatedScopeAccess().with {
            it.impersonatingToken = "impToken"
            it.impersonatingUsername = "user"
            it.accessTokenExp = new Date().plus(2)
            it.accessTokenString = "tokenString"
            it
        }


        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [impersonatedScopeAccess].asList()
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> expireScopeAccess(createUserScopeAccess())
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "calling updateExpiredUserScopeAccess with scopeAccess sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.getScopeAccesses(_) >> [createScopeAccess()].asList()
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> expireScopeAccess(createUserScopeAccessWithAuthBy("RSA"))

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
        1 * scopeAccessDao.getScopeAccesses(_) >> [createScopeAccess()].asList()
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> expireScopeAccess(createUserScopeAccessWithAuthBy("RSA"))

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

    def "Verify provision user scope access adds token expiration entropy"() {
        when:
        def range = getRange(exSeconds, entropy)
        UserScopeAccess scopeAccess = service.provisionUserScopeAccess(entityFactory.createUser(), "clientId")

        then:
        1 * config.getInt("token.cloudAuthExpirationSeconds", _) >> exSeconds
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

    def "calling getValidRackerScopeAccessForClientId sets expiration with entropy if non existing"() {
        given:
        def entropy = 0.01
        def clientId = "clientId"
        def authedBy = ["PASSWORD"].asList()
        def range = getRange(defaultCloudAuthRackerExpirationSeconds, entropy)

        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> null

        when:
        def scopeAccess = service.getValidRackerScopeAccessForClientId(entityFactory.createRacker(), clientId, authedBy)

        then:
        2 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= range.get("max")
        scopeAccess.accessTokenExp >= range.get("min")
    }

    def "updateExpiredRackerScopeAccess sets token expiration with entropy"() {
        given:
        def range = getRange(defaultCloudAuthRackerExpirationSeconds, entropy)
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
        if(result){
            result.getName() == expectedResult
        } else {
            result == expectedResult
        }

        where:
        applicationType | expectedResult
        "object-store"  | "NAST"
        "compute"       | "MOSSO"
        null            | null
        ""              | null
    }

    def "calling getRegion returns correct value"() {
        given:
        def token = entityFactory.createScopeAccess()

        when:
        def result = service.getRegion(token)

        then:
        1 * userService.getUserByScopeAccess(token, false) >> user
        result == expectedRegion

        where:
        expectedRegion  | user
        "ORD"           | entityFactory.createUser().with {it.region="ORD";it}
        null            | entityFactory.createRacker()
        null            | null

    }

    def getRange(seconds, entropy) {
        /*
        to account for processing time, add +- the fudgeSeconds to the calculated range.

        - We add to the max in case this range is calculated before the call to generate the token - which means as long
        as it doesn't take longer than the fudgeSeconds between the time we calculate the range and when the code generates
        the token we're guaranteed that the token expiration will fall before the max of the range

        - We subtract from the min in case this range is calculated after the call to generate the token - which means as long
        as it doesn't take longer than the fudgeSeconds between the time the code generates the token and we calculate this
        range we're guaranteed that the token expiration will fall after the min of the range
         */
        int fudgeSeconds = 30
        HashMap<String, Date> range = new HashMap<>()
        range.put("min", new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))-fudgeSeconds).toDate())
        range.put("max", new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))+fudgeSeconds).toDate())
        return range
    }
}
