package com.rackspace.idm.domain.security.tokencache

import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import spock.lang.Unroll
import testHelpers.RootServiceTest

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ConfigExpiryTest extends RootServiceTest {
    ConfigExpiry configExpiry

    Instant fixedClockNow = Instant.now()

    def setup() {
        configExpiry = new ConfigExpiry(Clock.fixed(fixedClockNow, ZoneId.of("UTC")))
        mockIdentityConfig(configExpiry)
    }

    def "areTokensCacheable: Returns false when config for user not found"() {
        BaseUser user = new Racker().with { it.id = "racker"; it}
        Collection<String> authByList = ["PASSWORD"]
        CacheKey key = new CacheKey(user, authByList)

        TokenCacheConfigJson tokenCacheConfigJson = Mock()

        when:
        def result = configExpiry.areTokensCacheable(key)

        then:
        1 * repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        1 * tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.IAM, user.id, authByList) >> null // Straight passthrough

        and:
        !result // Not cacheable
    }

    /**
     * If maximumCacheDuration is explicitly set to <=0, then can not cache for this key
     */
    @Unroll
    def "areTokensCacheable: Returns false when cache duration set to #value"() {
        BaseUser user = new User().with { it.id = "user"; it}
        Collection<String> authByList = ["PASSWORD"]
        CacheKey key = new CacheKey(user, authByList)

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        CacheableUserJson cacheableUserConfig = Mock()

        when:
        def result = configExpiry.areTokensCacheable(key)

        then:
        1 * repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        1 * tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.CID, user.id, authByList) >> cacheableUserConfig // Straight passthrough
        (1.._) * cacheableUserConfig.getMaximumCacheDuration() >> value

        and:
        !result

        where:
        value << [Duration.parse("PT0S"), Duration.parse("-PT1S")]
    }

    /**
     * If the cache duration is null or positive then a guarantee can not be made on whether or not key is cached. It depends on
     * requested expiration and the minimal viability setting.
     */
    @Unroll
    def "areTokensCacheable: Returns true when cache duration is null or positive (#value)"() {
        BaseUser user = new Racker().with { it.id = "racker"; it}
        Collection<String> authByList = ["PASSWORD"]
        CacheKey key = new CacheKey(user, authByList)

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        CacheableUserJson cacheableUserConfig = Mock()

        when:
        def result = configExpiry.areTokensCacheable(key)

        then:
        1 * repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        1 * tokenCacheConfigJson.findConfigForUserWithAuthMethods(UserManagementSystem.IAM, user.id, authByList) >> cacheableUserConfig // Straight passthrough
        (1.._) * cacheableUserConfig.getMaximumCacheDuration() >> value

        and:
        result

        where:
        value << [null, Duration.parse("PT1S"), Duration.parse("PT0.000000001S")]
    }

    def "expireAfterCreate: If no user config match, returns 0"() {
        User user = new User().with {it.id = "user"; it}
        CacheKey key = new CacheKey(user, ["PASSWORD"])
        TokenCacheEntry entry = new TokenCacheEntry("token", Instant.now(), Instant.now().plus(10000), "unique")

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> null

        expect:
        configExpiry.expireAfterCreate(key, entry, 0) == 0
    }

    @Unroll
    def "expireAfterCreate: If configured cache duration for user is zero or negative, returns 0"() {
        User user = new User().with {it.id = "user"; it}
        CacheKey key = new CacheKey(user, ["PASSWORD"])
        TokenCacheEntry entry = new TokenCacheEntry("token", Instant.now(), Instant.now().plus(10000), "unique")

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        CacheableUserJson cacheableUserConfig = Mock()

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserConfig

        cacheableUserConfig.getMinimumValidityDuration() >> Duration.parse("PT1S")

        when:
        long duration = configExpiry.expireAfterCreate(key, entry, 0)

        then:
        (1.._) * cacheableUserConfig.getMaximumCacheDuration() >> value

        and:
        duration == 0

        where:
        value << [Duration.parse("PT0S"), Duration.parse("-PT1S")]
    }

    @Unroll
    def "expireAfterCreate: If min validity (#validity) configured and cache duration for user is null, cache expiration based on min validity"() {
        User user = new User().with {it.id = "user"; it}
        CacheKey key = new CacheKey(user, ["PASSWORD"])
        Instant tokenCreation = fixedClockNow
        Instant tokenExpiration = fixedClockNow.plusSeconds(10000)

        // Set the token to expire 10 seconds from "now"
        TokenCacheEntry entry = new TokenCacheEntry("token", tokenCreation, tokenExpiration, "unique")

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        CacheableUserJson cacheableUserConfig = Mock()

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserConfig

        cacheableUserConfig.getMaximumCacheDuration() >> null
        cacheableUserConfig.getMinimumValidityDuration() >> validity

        when:
        long duration = configExpiry.expireAfterCreate(key, entry, 0)

        then: "Cache TTL is duration between fake now and (token expiration - min validity)"
        duration == Duration.between(fixedClockNow, tokenExpiration.minus(validity)).toNanos()

        where:
        validity << [Duration.parse("PT1S"), Duration.parse("PT1000S")]
    }


    @Unroll
    def "expireAfterCreate: If min validity would would cause cache TTL earlier than now, returns 0"() {
        User user = new User().with {it.id = "user"; it}
        CacheKey key = new CacheKey(user, ["PASSWORD"])
        Instant tokenCreation = fixedClockNow.minusSeconds(10)
        Instant tokenExpiration = fixedClockNow.plus(validity.minusSeconds(1)) // Set expiration so min validity calc would be 1 second earlier than "now"

        // Set the token to expire 10 seconds from "now"
        TokenCacheEntry entry = new TokenCacheEntry("token", tokenCreation, tokenExpiration, "unique")

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        CacheableUserJson cacheableUserConfig = Mock()

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserConfig

        cacheableUserConfig.getMaximumCacheDuration() >> null
        cacheableUserConfig.getMinimumValidityDuration() >> validity

        expect:
        configExpiry.expireAfterCreate(key, entry, 0) == 0

        where:
        validity << [Duration.parse("PT1S"), Duration.parse("PT1000S")]
    }

    @Unroll
    def "expireAfterCreate: If cache duration and min validity specified, TTL set to earliest. cacheDuration: #cacheDuration, validityDuration: #validityDuration, expectedTtl: #expectedTtl"() {
        User user = new User().with {it.id = "user"; it}
        CacheKey key = new CacheKey(user, ["PASSWORD"])
        Instant tokenCreation = fixedClockNow.minusSeconds(10)

        // Set the token to expire 10 seconds from fake "now"
        Instant tokenExpiration = fixedClockNow.plus(10) // Set expiration so min validity calc would be 1 second earlier than "now"

        TokenCacheEntry entry = new TokenCacheEntry("token", tokenCreation, tokenExpiration, "unique")

        TokenCacheConfigJson tokenCacheConfigJson = Mock()
        repositoryConfig.getTokenCacheConfiguration() >> tokenCacheConfigJson
        CacheableUserJson cacheableUserConfig = Mock()

        tokenCacheConfigJson.findConfigForUserWithAuthMethods(_, _, _) >> cacheableUserConfig

        cacheableUserConfig.getMaximumCacheDuration() >> cacheDuration
        cacheableUserConfig.getMinimumValidityDuration() >> validityDuration

        expect:
        configExpiry.expireAfterCreate(key, entry, 0) == expectedTtl

        where:
        cacheDuration | validityDuration || expectedTtl
        Duration.parse("PT1S")   | Duration.parse("PT3S")   || TimeUnit.SECONDS.toNanos(1) // Duration results in shorter ttl
        Duration.parse("PT10S")   | Duration.parse("PT3S")   || TimeUnit.SECONDS.toNanos(7) // Validity results in shorter ttl
        Duration.parse("PT10S")   | Duration.parse("PT11S")   || TimeUnit.SECONDS.toNanos(0) // Validity results in before now
        null   | Duration.parse("PT11S")   || TimeUnit.SECONDS.toNanos(0) // Validity results in before now
        null   | Duration.parse("PT6S")   || TimeUnit.SECONDS.toNanos(4) // Validity results in earliest TTL
        null   | null   || TimeUnit.SECONDS.toNanos(0) // Invalid state, but still return 0 if min validity is null
        Duration.parse("PT1S")   | null   || TimeUnit.SECONDS.toNanos(0) // Invalid state, return 0 if min validity is null
    }
}
