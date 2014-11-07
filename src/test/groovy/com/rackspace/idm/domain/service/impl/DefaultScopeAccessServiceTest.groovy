package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType
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
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import spock.lang.Unroll
import testHelpers.RootServiceTest

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
        mockIdentityUserService(service)

        config.getInt("token.cloudAuthExpirationSeconds", _) >>  defaultCloudAuthExpirationSeconds
        config.getInt("token.cloudAuthRackerExpirationSeconds", _) >>  defaultCloudAuthRackerExpirationSeconds
        config.getInt("token.expirationSeconds", _) >> defaultExpirationSeconds
        config.getInt("token.rackerExpirationSeconds", _) >> defaultRackerExpirationSeconds
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
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true

        when:
        service.updateExpiredUserScopeAccess(expired_sa)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access delete failure causes exception when ignore delete failure disabled"() {
        given:
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false

        when:
        service.updateExpiredUserScopeAccess(expired_sa)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
        thrown(RuntimeException)
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
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> false

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access single token cleanup - failure when ignore failures enabled, stop cleanup enabled does not result in error"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> true

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        //only called one time because after first exception, cleanup routine exits
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access single token cleanup - failure when ignore failures disabled, stop cleanup disabled results in error"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> false

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException("error")}
        thrown(RuntimeException)
    }

    def "update expired user scope access single token cleanup - failure when ignore failures disabled, stop cleanup enabled does result in error"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> true

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException("error")}
        thrown(RuntimeException)
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
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> false

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        2 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access multiple token cleanup - failure on first delete when ignore failures enabled, stop cleanup enabled results in 1 delete"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def expired_sa2 = createUserScopeAccess("expiredTokenString2", "userRsId2", "clientId2", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa, expired_sa2 ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> true

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException()}
    }

    def "update expired user scope access multiple token cleanup - failure on first delete when ignore failures disabled, stop cleanup disabled results in error"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> false

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException("error")}
        thrown(RuntimeException)
    }

    def "update expired user scope access multiple token cleanup - failure on first delete when ignore failures disabled, stop cleanup enabled results in 1 deletes"() {
        given:
        def sa = createUserScopeAccess("goodTokenString", "userRsId", "clientId", futureDate)
        def expired_sa = createUserScopeAccess("expiredTokenString", "userRsId", "clientId", expiredDate)
        def user =  entityFactory.createUser()
        user.setUniqueId(dn)

        scopeAccessDao.getScopeAccesses(_) >> [ expired_sa ].asList()
        scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_,_,_) >> sa
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> false
        config.getBoolean(DefaultScopeAccessService.FEATURE_AUTHENTICATION_TOKEN_DELETE_FAILURE_STOPS_CLEANUP_PROP_NAME, _) >> true

        when:
        service.updateExpiredUserScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw new RuntimeException("error")}
        thrown(RuntimeException)
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
        1 * scopeAccessDao.getMostRecentScopeAccessByClientIdAndAuthenticatedBy(_, _, _) >> existingUserScopeAccess

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

        when: "exception encountered deleting valid token"
        ImpersonatedScopeAccess optResult = service.processImpersonatedScopeAccessRequest(impersonator, impersonatedUser, request, ImpersonatorType.SERVICE, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        then: "no attempt is made to delete third expired token, but valid token is deleted and no exception thrown"
        1 * scopeAccessDao.deleteScopeAccess(_) >> {throw exceptionToThrow }
        RuntimeException ex = thrown()
        ex == exceptionToThrow
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
        identityUserService.getEndUserById(_) >> user

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
        identityUserService.getEndUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        scopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.expireAllTokensForUserById("1")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

    @Unroll
    def "expireAllTokensExceptTypeForEndUser: When various exceptions are provided, the right tokens are expired - authenticatedBy: #authenticatedBy | keepEmpty: #keepEmpty | expectedTokensNotExpired: #expectedTokensNotExpired" () {
        given:
        User user = new User()
        user.id = "1"
        user.uniqueId = "blah"
        identityUserService.getEndUserById(_) >> user

        //create tokens representing various scenarios. Set username/token to same value so can easily tell which token failed test (since only username/customerid printed for token)
        def scopeAccessA = entityFactory.createUserToken().with {def id="A"; it.userRsId = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A"); return it}
        def scopeAccessB = entityFactory.createUserToken().with {def id="B"; it.userRsId= id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("B"); return it}
        def scopeAccessAB = entityFactory.createUserToken().with {def id="AB"; it.userRsId= id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A","B"); return it}
        def scopeAccessBA = entityFactory.createUserToken().with {def id="BA"; it.userRsId = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("B", "A"); return it}
        def scopeAccessABC = entityFactory.createUserToken().with {def id="ABC"; it.userRsId = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A","B", "C"); return it}
        def scopeAccessEmpty = entityFactory.createUserToken().with {def id="Empty"; it.userRsId = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList(); return it}

        List<UserScopeAccess> scopeAccessList = [scopeAccessA, scopeAccessB, scopeAccessAB, scopeAccessBA, scopeAccessABC, scopeAccessEmpty]

        scopeAccessDao.getScopeAccesses(_) >> scopeAccessList

        when:
        service.expireAllTokensExceptTypeForEndUser(user, authenticatedBy, keepEmpty)

        then:
        /*
         the tokens are created with expiration date 1 day from now. When a token is revoked, the expiration is set to "now".
         If the test runs fast enough "now" will be the same for both the date we're checking here AND the date the token was
         expired which could cause unexpected failures (since isExpired would return false if times are equal). To
         avoid this, check if token is set to earlier than one ms from now.
          */
        DateTime expirationDateToCheck = new DateTime().plusMillis(1)
        scopeAccessList.each {
            if (expectedTokensNotExpired.contains(it.accessTokenString)) {
                assert !it.isAccessTokenExpired(expirationDateToCheck)
            }
            else {
                assert it.isAccessTokenExpired(expirationDateToCheck)
            }
        }

        where:
        authenticatedBy         | keepEmpty     | expectedTokensNotExpired
        [["A"]]                 | false         | ["A"]
        [["A"]]                 | true          | ["A", "Empty"]
        [["C"]]                 | false         | []
        [["A", "B"]]            | false         | ["AB", "BA"]
        [["A", "B"]]            | true          | ["AB", "BA", "Empty"]
        [["B", "A"]]            | false         | ["AB", "BA"]
        [["B", "A", "C"]]       | false         | ["ABC"]
        [["B", "C"]]            | false         | []
        []                      | true          | ["Empty"]
        []                      | false         | []
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

    def "updateExpiredRackerScopeAccess adds new scopeAccess, ignore deletes existing expired scopeAccess failure, and returns new scopeAccess when ignore delete enabled"() {
        given:
        def oldScopeAccess = createRackerScopeAccess("tokenString", "rackerId", expiredDate)
        config.getBoolean(DefaultScopeAccessService.FEATURE_IGNORE_AUTHENTICATION_TOKEN_DELETE_FAILURE_PROP_NAME, _) >> true

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
