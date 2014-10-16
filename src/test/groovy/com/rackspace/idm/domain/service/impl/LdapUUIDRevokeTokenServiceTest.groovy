package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.UUIDScopeAccessDao
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class LdapUUIDRevokeTokenServiceTest extends RootServiceTest {

    @Shared LdapUUIDRevokeTokenService service = new LdapUUIDRevokeTokenService()
    @Shared UUIDScopeAccessDao uuidScopeAccessDao;

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
        mockAtomHopperClient(service)
        mockUserService(service)
        mockIdentityUserService(service)

        uuidScopeAccessDao = Mock()
        service.scopeAccessDao = uuidScopeAccessDao
    }

    def "revokeAllTokensForEndUser(id) - atomHopper client is called when expiring all tokens for user" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        uuidScopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.revokeAllTokensForEndUser(user.id)

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)
    }

    def "revokeAllTokensForEndUser(id) - atomHopper client is not called when tokens for user are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        uuidScopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.revokeAllTokensForEndUser(user.id)

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)
    }

    def "revokeAllTokensForEndUser(user) - atomHopper client is called when expiring all tokens for user" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", refreshDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()
        scopeAccessTwo.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        uuidScopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.revokeAllTokensForEndUser(user)

        then:
        2 * atomHopperClient.asyncTokenPost(_,_)
    }

    def "revokeAllTokensForEndUser(user) - atomHopper client is not called when tokens are already expired" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)
        def scopeAccessTwo = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        uuidScopeAccessDao.getScopeAccesses(_) >> [scopeAccessOne, scopeAccessTwo].asList()

        when:
        service.revokeAllTokensForEndUser(user)

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
        def scopeAccessA = entityFactory.createUserToken().with {def id="A"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A"); return it}
        def scopeAccessB = entityFactory.createUserToken().with {def id="B"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("B"); return it}
        def scopeAccessAB = entityFactory.createUserToken().with {def id="AB"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A","B"); return it}
        def scopeAccessBA = entityFactory.createUserToken().with {def id="BA"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("B", "A"); return it}
        def scopeAccessABC = entityFactory.createUserToken().with {def id="ABC"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList("A","B", "C"); return it}
        def scopeAccessEmpty = entityFactory.createUserToken().with {def id="Empty"; it.username = id; it.accessTokenString = id; it.authenticatedBy = Arrays.asList(); return it}

        List<UserScopeAccess> scopeAccessList = [scopeAccessA, scopeAccessB, scopeAccessAB, scopeAccessBA, scopeAccessABC, scopeAccessEmpty]

        uuidScopeAccessDao.getScopeAccesses(_) >> scopeAccessList

        when:
        service.revokeTokensForEndUser(user, authenticatedBy)

        then:
        /*
         the tokens are created with expiration date 1 day from now. When a token is revoked, the expiration is set to "now".
         If the test runs fast enough "now" will be the same for both the date we're checking here AND the date the token was
         expired which could cause unexpected failures (since isExpired would return false if times are equal). To
         avoid this, check if token is set to earlier than one ms from now.
          */
        DateTime expirationDateToCheck = new DateTime().plusMillis(1)
        scopeAccessList.each {
            if (expectedTokensExpired.contains(it.accessTokenString)) {
                assert it.isAccessTokenExpired(expirationDateToCheck)
            }
            else {
                assert !it.isAccessTokenExpired(expirationDateToCheck)
            }
        }

        where:
        authenticatedBy             | expectedTokensExpired
        [["A"] as Set]              | ["A"]
        [["A"] as Set, [] as Set]   | ["A", "Empty"]
        [["C"] as Set]              | []
        [["A", "B"] as Set]         | ["AB", "BA"]
        [["A", "B"] as Set, [] as Set]     | ["AB", "BA", "Empty"]
        [["B", "A"] as Set]         | ["AB", "BA"]
        [["B", "A", "C"] as Set]    | ["ABC"]
        [[] as Set]                 | ["Empty"]
        []                          | []
        [["B", "C"] as Set]         | []
    }

    def "revokeToken(tokenString) - atomHopper client is called when expiring a token" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_, _) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", futureDate)

        scopeAccessOne.getAccessTokenExp() >> new DateTime().plusHours(6).toDate()

        uuidScopeAccessDao.getScopeAccessByAccessToken(_) >> scopeAccessOne

        when:
        service.revokeToken("someToken")

        then:
        1 * atomHopperClient.asyncTokenPost(_,_)

    }

    def "revokeToken(tokenString) - atomHopper client is not called when expiring a token that is already expired" () {
        given:
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_) >> user

        def scopeAccessOne = createUserScopeAccess("tokenString", "userRsId", "clientId", expiredDate)

        uuidScopeAccessDao.getScopeAccessByAccessToken(_) >> scopeAccessOne

        when:
        service.revokeToken("someToken")

        then:
        0 * atomHopperClient.asyncTokenPost(_,_)

    }

}
