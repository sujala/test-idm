package com.rackspace.idm.domain.security.tokencache

import com.github.benmanes.caffeine.cache.Cache
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider
import com.rackspace.idm.exception.IdmException
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import spock.lang.Unroll
import testHelpers.FakeTicker
import testHelpers.RootServiceTest

import java.time.Duration
import java.util.concurrent.TimeUnit

class AETokenCacheComponentTest extends RootServiceTest {

    AETokenCache aeTokenCache
    FakeTicker ticker
    ConfigExpiry configExpiry

    TokenCacheConfigJson tokenCacheConfigJson
    CacheableUserJson cacheableUserJson

    def setup() {
        aeTokenCache = new AETokenCache()
        mockIdentityConfig(aeTokenCache)
        mockAeTokenService(aeTokenCache)

        ticker = new FakeTicker()
        aeTokenCache.ticker = ticker

        configExpiry = new ConfigExpiry()
        configExpiry.identityConfig = identityConfig

        aeTokenCache.configExpiry = configExpiry

        tokenCacheConfigJson = Mock()
        cacheableUserJson = Mock()

        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
    }

    def "cache not created if init not called"() {
        expect:
        aeTokenCache.getCache() == null

        and: "cache info has main map collections"
        aeTokenCache.getCacheInfo().get(AETokenCache.CACHE_INFO_CONFIGURATION_MAP_KEY).size() == 1
        aeTokenCache.getCacheInfo().get(AETokenCache.CACHE_INFO_STATISTICS_MAP_KEY).size() == 0

        and: "cache info shows cache as disabled"
        !aeTokenCache.getCacheInfo().get(AETokenCache.CACHE_INFO_CONFIGURATION_MAP_KEY).get(AETokenCache.CONFIGURATION_CACHE_ENABLED)
    }

    @Unroll
    def "init: cache loads properties from config on initialization"() {
        when:
        aeTokenCache.init()

        then:
        1 * reloadableConfig.cachedAETokenCacheMaxSize() >> size
        1 * reloadableConfig.cachedAETokenCacheInitialCapacity() >> capacity
        1 * reloadableConfig.cachedAETokenCacheRecordStats() >> stats

        and:
        aeTokenCache.cacheBlock.configuredInitialCapacity == capacity
        aeTokenCache.cacheBlock.configuredMaxSize == size
        aeTokenCache.cacheBlock.configuredRecordStats == stats

        and: "Cache info config reflects values"
        Map<String, Object> cacheInfo = aeTokenCache.getCacheInfo().get(AETokenCache.CACHE_INFO_CONFIGURATION_MAP_KEY)
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_MAX_SIZE) == size
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_INITIAL_CAPACITY) == capacity
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_RECORDING_STATS) == stats

        where:
        size    | capacity  | concurrency   | stats
        3       | 4         | 6             | true
        5       | 6         | 8             | false
    }

    def "cache not recreated if configuration is invalid"() {
        1 * reloadableConfig.cachedAETokenCacheInitialCapacity() >> 5
        reloadableConfig.cachedAETokenCacheMaxSize() >> 6
        reloadableConfig.cachedAETokenCacheRecordStats() >> true

        aeTokenCache.init()
        def cache = aeTokenCache.getCache()

        when: "Try to recreate with invalid settings"
        aeTokenCache.recreateCache()

        then: "Request to recreate cache throws exception"
        1 * reloadableConfig.cachedAETokenCacheInitialCapacity() >> -10 // Will throw error as can't be negative
        thrown(IdmException)

        and:
        cache == aeTokenCache.getCache() // Old cache still exists
    }

    @Unroll
    def "recreateCache: cache reloads properties from config"() {
        when:
        aeTokenCache.init()
        def cacheBlockInit = aeTokenCache.cacheBlock
        aeTokenCache.recreateCache()

        then:
        aeTokenCache.cacheBlock != cacheBlockInit
        // Increment all values by 1 the second time the config is retrieved
        2 * reloadableConfig.cachedAETokenCacheMaxSize() >>> [size, size+1]
        2 * reloadableConfig.cachedAETokenCacheInitialCapacity() >>> [capacity, capacity+1]
        2 * reloadableConfig.cachedAETokenCacheRecordStats() >>> [stats, !stats]

        and: "cacheblock reflects last set of configs retrieved"
        aeTokenCache.cacheBlock.configuredInitialCapacity == capacity+1
        aeTokenCache.cacheBlock.configuredMaxSize == size+1
        aeTokenCache.cacheBlock.configuredRecordStats == !stats

        and: "Cache info config reflects last set of configs retrieved"
        Map<String, Object> cacheInfo = aeTokenCache.getCacheInfo().get(AETokenCache.CACHE_INFO_CONFIGURATION_MAP_KEY)
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_MAX_SIZE) == size+1
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_INITIAL_CAPACITY) == capacity+1
        cacheInfo.get(AETokenCache.CONFIGURATION_CACHE_RECORDING_STATS) == !stats

        where:
        size | capacity | concurrency | stats
        3 | 4 | 6 | true
        5 | 6 | 8 | false
    }

    def "invalidateCache: Calls invalidate on cache itself"() {
        AETokenCache.CacheBlock mockBlock = Mock()
        Cache mockCache = Mock()
        mockBlock.getCache() >> mockCache
        aeTokenCache.cacheBlock = mockBlock

        when: "invalidate"
        aeTokenCache.invalidateCache()

        then:
        1 * mockCache.invalidateAll()
    }

    def "performMaintenance: Calls performMaintenance on cache itself"() {
        Cache mockCache = Mock()

        AETokenCache.CacheBlock mockBlock = Mock()
        mockBlock.getCache() >> mockCache
        aeTokenCache.cacheBlock = mockBlock

        when:
        aeTokenCache.performMaintenance()

        then:
        1 * mockCache.cleanUp()
    }

    @Unroll
    def "isTokenCacheableForUser: Not cached due to #reason"() {
        expect:
        !aeTokenCache.isTokenCacheableForUser(user, token)

        where:
        user                                 | token                                                                      | reason
        null                                 | entityFactory.createUserToken()                                            | "Provided user is null"
        new Racker()                         | null                                                                       | "Provided token is null"
        new Racker()                         | entityFactory.createUserToken()                                            | "Provided user id is null"
        new Racker().with {it.id = "id"; it} | new ImpersonatedScopeAccess().with {it.authenticatedBy = ["PASSWORD"]; it} | "Unsupported type of token"
    }

    /**
     * To re-use a token the token must be issued to the same user (userId) and the exact same set of unordered
     * authenticated by.
     */
    @Unroll
    def "getOrCreateTokenForUser: Userscope tokens with authBy: '#authByTypes' are cached appropriately"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser()
        def otherUser = entityFactory.createRandomUser()

        ScopeAccess firstAuthScopeAccess = createToken(user.id, authByTypes)
        ScopeAccess sameUserDifferentAuthByScopeAccess = createToken(user.id, authByTypes.collect() << AuthenticatedByMethodEnum.PASSWORD.value)
        ScopeAccess secondAuthScopeAccess = createToken(user.id, authByTypes)
        ScopeAccess otherUserAuthScopeAccess = createToken(otherUser.id, authByTypes)

        // All users are cached
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S") // Set low so cache duration used instead
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT5S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)

        when: "Auth w/ different method"
        aeTokenCache.getOrCreateTokenForUser(user, sameUserDifferentAuthByScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, sameUserDifferentAuthByScopeAccess)

        when: "Auth w/ same user and auth by"
        aeTokenCache.getOrCreateTokenForUser(user, secondAuthScopeAccess, mockTokenProvider)

        then: "Return cached token"
        1 * aeTokenService.unmarshallTokenAndValidate(_) >> secondAuthScopeAccess
        0 * mockTokenProvider.marshallTokenForUser(user, secondAuthScopeAccess)

        when: "Auth w/ different user and same auth by"
        aeTokenCache.getOrCreateTokenForUser(otherUser, otherUserAuthScopeAccess, mockTokenProvider)

        then:
        1 * mockTokenProvider.marshallTokenForUser(otherUser, otherUserAuthScopeAccess)

        where:
        authByTypes | _
        [AuthenticatedByMethodEnum.PASSWORD.value] | _
        [AuthenticatedByMethodEnum.APIKEY.value] | _
        [AuthenticatedByMethodEnum.PASSWORD.value, AuthenticatedByMethodEnum.OTPPASSCODE.value] | _
        [AuthenticatedByMethodEnum.PASSWORD.value, AuthenticatedByMethodEnum.PASSCODE.value] | _
    }

    /**
     * This verifies that some tokens are programmatically restricted from being cached. Even though the cache
     * config will match on anything, the token is still not cached.
     */
    @Unroll
    def "getOrCreateTokenForUser: Tokens with authBys of #authByTypes are never cached"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser()

        ScopeAccess firstAuthScopeAccess = createToken(user.id, authByTypes)
        ScopeAccess secondAuthScopeAccess = createToken(user.id, authByTypes)

        // All users are cached
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S")
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT5S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)

        when: "Auth w/ same user and auth by"
        aeTokenCache.getOrCreateTokenForUser(user, secondAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, secondAuthScopeAccess)

        where:
        authByTypes << (AuthenticatedByMethodEnum.values().findAll() {!AETokenCache.cacheableAuthBys.contains(it.value)}.collect {[it.value]})
    }

    /**
     * This verifies that some tokens are programmatically restricted from being cached. Even though the cache
     * config will match on anything, the token is still not cached.
     */
    @Unroll
    def "isTokenCacheableForUser: Tokens with authBys of #authByTypes can't be cached"() {
        initCache()
        def user = entityFactory.createRandomUser()

        ScopeAccess firstAuthScopeAccess = createToken(user.id, authByTypes)

        // All users are cached
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S")
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT5S")

        expect:
        !aeTokenCache.isTokenCacheableForUser(user, firstAuthScopeAccess)

        where:
        authByTypes << (AuthenticatedByMethodEnum.values().findAll() {!AETokenCache.cacheableAuthBys.contains(it.value)}.collect {[it.value]})
    }

    def "Cached tokens are retrieved from cache"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def token = entityFactory.createRackerScopeAccess().with {it.rackerId = "hi"; it.authenticatedBy = [AuthenticatedByMethodEnum.PASSWORD.value]; it.createTimestamp = new Date(); it.accessTokenExp = new DateTime().plusHours(6).toDate(); it}
        def user = entityFactory.createRacker().with {
            it.id = token.issuedToUserId
            it
        }
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.IAM, user.id, {org.apache.commons.collections4.CollectionUtils.isEqualCollection(it, token.authenticatedBy)}) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT10S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, token)

        when: "Auth w/ same"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Do not generate a new token"
        1 * aeTokenService.unmarshallTokenAndValidate(_) >> token
        0 * mockTokenProvider.marshallTokenForUser(user, token)
    }

    def "If error encountered reusing a cached token, a new token is generated"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def token = entityFactory.createRackerScopeAccess().with {it.rackerId = "hi"; it.authenticatedBy = [AuthenticatedByMethodEnum.PASSWORD.value]; it.createTimestamp = new Date(); it.accessTokenExp = new DateTime().plusHours(6).toDate(); it}
        def user = entityFactory.createRacker().with {
            it.id = token.issuedToUserId
            it
        }
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.IAM, user.id, {org.apache.commons.collections4.CollectionUtils.isEqualCollection(it, token.authenticatedBy)}) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT10S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, token)

        when: "Auth w/ same"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Do not generate a new token"
        1 * aeTokenService.unmarshallTokenAndValidate(_) >> {throw new RuntimeException("Simulating error")}
        1 * mockTokenProvider.marshallTokenForUser(user, token)
    }

    @Unroll
    def "#testDescription are not cacheable"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser().with {
            it.id = token.issuedToUserId
            it
        }

        // The configuration allows for cacheing some types of the user's tokens
        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT5S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, token)

        when: "Auth w/ same"
        aeTokenCache.getOrCreateTokenForUser(user, token, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, token)

        where:
        token                                                                                                                                        | testDescription
        new UserScopeAccess().with {it.userRsId = "hi"; it.authenticatedBy = [AuthenticatedByMethodEnum.IMPERSONATION.value]; it}                    | "Tokens with Impersonation Auth by"
        new UserScopeAccess().with {it.userRsId = "hi"; it.authenticatedBy = [AuthenticatedByMethodEnum.IMPERSONATE.value]; it}                      | "Tokens with Impersonate Auth by"
        new UserScopeAccess().with {it.userRsId = "hi"; it.authenticatedBy = [AuthenticatedByMethodEnum.PASSWORD.value]; it.scope = "something"; it} | "Tokens with scope"
    }

    @Unroll
    def "Cache entry expires after configurable ttl of #ttl"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser()

        ScopeAccess firstAuthScopeAccess = createToken(user.id, [AuthenticatedByMethodEnum.PASSWORD.value])

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S") // Set low so cache duration used instead
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT" + ttl + "S")

        when: "First auth"
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)

        when: "Auth w/ same user and auth by 1 second before entry expires"
        ticker.advance(ttl-1, TimeUnit.SECONDS)
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Return cached token"
        1 * aeTokenService.unmarshallTokenAndValidate(_) >> firstAuthScopeAccess

        when: "Wait for TTL to pass and auth again"
        ticker.advance(1, TimeUnit.SECONDS)
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)
        0 * aeTokenService.unmarshallTokenAndValidate(_)

        where:
        ttl << [5, 10]
    }

    def "A new token is generated if cached hit, but cache token is expired"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser()
        def cachedToken = "myTokenStr"
        def cachedToken2 = "myTokenStr2"

        ScopeAccess firstAuthScopeAccess = createToken(user.id, [AuthenticatedByMethodEnum.PASSWORD.value])

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.CID, user.id, firstAuthScopeAccess.authenticatedBy) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S") // Set low so cache duration used instead
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT5S")

        when: "First auth"
        def resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess) >> {
            firstAuthScopeAccess.accessTokenString = cachedToken
            cachedToken
        }
        0 * aeTokenService.unmarshallTokenAndValidate(_) // Don't check newly generated tokens for revocation
        resultToken == cachedToken

        when: "Auth w/ same user for non-revoked cached token"
        resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Return cached token"
        1 * aeTokenService.unmarshallTokenAndValidate(cachedToken) >> firstAuthScopeAccess
        0 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)
        resultToken == cachedToken

        when: "Auth w/ same user for revoked cached token"
        resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Return new token"
        1 * aeTokenService.unmarshallTokenAndValidate(cachedToken) >> null
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess) >> {
            firstAuthScopeAccess.accessTokenString = cachedToken2
            cachedToken2
        }
        resultToken == cachedToken2

        when: "Auth w/ same user for non-revoked cached token"
        resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Return new cached token"
        1 * aeTokenService.unmarshallTokenAndValidate(cachedToken2) >> firstAuthScopeAccess
        0 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)
        resultToken == cachedToken2
    }

    def "Invalidating cache removes entries"() {
        initCache()
        TokenProvider mockTokenProvider = Mock()
        def user = entityFactory.createRandomUser()
        def cachedToken = RandomStringUtils.randomAlphabetic(20)

        ScopeAccess firstAuthScopeAccess = createToken(user.id, [AuthenticatedByMethodEnum.PASSWORD.value])

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.CID, user.id, firstAuthScopeAccess.authenticatedBy) >> cacheableUserJson
        cacheableUserJson.getMinimumValidityDuration() >> Duration.parse("PT1S") // Set low so cache duration used instead
        cacheableUserJson.getMaximumCacheDuration() >> Duration.parse("PT10M") // Cache token for 10 minutes

        when: "First auth"
        def resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Generate a new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess) >> {
            firstAuthScopeAccess.accessTokenString = cachedToken
            cachedToken
        }
        resultToken == cachedToken

        when: "Second auth"
        resultToken = aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Returns existing cached token"
        0 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess) // Don't create new token
        1 * aeTokenService.unmarshallTokenAndValidate(cachedToken) >> firstAuthScopeAccess // unmarshall the cached token
        resultToken == cachedToken

        when: "Invalidate cache, re-auth w/ same user"
        aeTokenCache.invalidateCache()
        aeTokenCache.getOrCreateTokenForUser(user, firstAuthScopeAccess, mockTokenProvider)

        then: "Doesn't find token in cache"
        0 * aeTokenService.unmarshallTokenAndValidate(cachedToken) >> firstAuthScopeAccess // unmarshall the cached token
        0 * aeTokenRevocationService.isTokenRevoked(cachedToken)

        and: "Generates new token"
        1 * mockTokenProvider.marshallTokenForUser(user, firstAuthScopeAccess)
    }

    def createToken(userId, authByTypes) {
        return entityFactory.createUserToken().with {
            it.userRsId = userId
            it.authenticatedBy = authByTypes
            it
        }
    }

    def initCache() {
        reloadableConfig.cachedAETokenCacheMaxSize() >> 3000
        reloadableConfig.cachedAETokenCacheInitialCapacity() >> 200
        reloadableConfig.cachedAETokenCacheRecordStats() >> true
        aeTokenCache.init()
    }
}
