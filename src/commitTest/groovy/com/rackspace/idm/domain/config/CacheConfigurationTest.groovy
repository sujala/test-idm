package com.rackspace.idm.domain.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration

import java.time.Duration

class CacheConfigurationTest extends Specification {

    CacheConfiguration cacheConfiguration

    SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance()
    SingletonReloadableConfiguration reloadableConfiguration = SingletonReloadableConfiguration.getInstance()
    IdentityConfig identityConfig = new IdentityConfig(staticIdmConfiguration, reloadableConfiguration)

    def setup() {
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()

        cacheConfiguration = new CacheConfiguration()
        cacheConfiguration.identityConfig = identityConfig
    }

    def "Cache manager creates appropriate caches"() {
        CacheManager cacheManager = cacheConfiguration.cacheManager()

        // This is auto called by Spring when loading app context, but since this test doesn't use spring need to manually trigger
        cacheManager.initializeCaches()

        expect:
        cacheManager.cacheNames.contains(CacheConfiguration.CLIENT_ROLE_CACHE_BY_ID)
        cacheManager.cacheNames.contains(CacheConfiguration.CLIENT_ROLE_CACHE_BY_NAME)
        cacheManager.cacheNames.contains(CacheConfiguration.RACKER_AUTH_RESULT_CACHE)
        cacheManager.cacheNames.contains(CacheConfiguration.RACKER_GROUPS_CACHE)
        cacheManager.cacheNames.contains(CacheConfiguration.REPOSITORY_PROPERTY_CACHE_BY_NAME)
        cacheManager.cacheNames.contains(CacheConfiguration.USER_LOCKOUT_CACHE_BY_NAME)
    }

    @Unroll
    def "Client Role Id cache configured via properties. test size: #size; test ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createClientRoleCacheBuilder()

        then:
        builder.maximumSize == size
        builder.expireAfterWriteNanos == Duration.parse(ttl).toNanos()

        where:
        size | ttl
        10 | "PT5M"
        30 | "P1DT1H"
        5 | "PT1H5.5S"
        500 | "PT0S"
    }

    /**
     * Tests supplying invalid values to size and ttl properties to verify code will override to use hardcoded values.
     * The durations are invalid because durations can't contain an imprecise datapoint such as months or years (e.g does
     * the month have 28, 29, 30, or 31 days)
     * @return
     */
    @Unroll
    def "Client Role Id cache uses hardcoded values when supplied properties are invalid. size: #size; ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createClientRoleCacheBuilder()

        then:
        builder.maximumSize == IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT
        builder.expireAfterWriteNanos == IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT.toNanos()

        where:
        size | ttl
        "a" | "P1MT5M"
        "1/2" | "P1Y"
        "*" | "abc"
    }

    def "verify 0 ttl caffeine cache never caches"() {
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, 5)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, "PT0S")

        CaffeineCache cache = cacheConfiguration.getClientRoleCache("hello")

        when:
        cache.putIfAbsent("hi", "val")

        then:
        cache.get("hi") == null
    }

    def "verify 0 size cache never caches even if TTL set high"() {
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, 0)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, "PT10M")
        def propName = "hi"
        def propValue = "val"

        CaffeineCache cache = cacheConfiguration.getClientRoleCache("hello")

        when:
        cache.putIfAbsent(propName, propValue)
        def cacheVal = cache.get(propName)

        // Get the value of the cache. Since Caffeine evicts entries in s separate processing thread, there could
        // be a slight delay before the entry is evicted even with a 0 sized cache. This is normal and expected behavior.
        // However, all entries are automatically scheduled for eviction for caches with max size set to 0.
        def timeout = 1000 // 1 sec
        def step = 100 // 100 ms
        def waitedFor = 0
        while (cacheVal != null && waitedFor < timeout) {
            sleep(step)
            waitedFor += step
            cacheVal = cache.get(propName)
        }

        then:
        cacheVal == null
    }

    @Unroll
    def "Repository property cache configured via properties. test size: #size; test ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_REPOSITORY_PROPERTY_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_REPOSITORY_PROPERTY_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createRepositoryPropertyCacheBuilder()

        then:
        builder.maximumSize == size
        builder.expireAfterWriteNanos == Duration.parse(ttl).toNanos()

        where:
        size | ttl
        10 | "PT5M"
        30 | "P1DT1H"
        5 | "PT1H5.5S"
        500 | "PT0S"
    }

    /**
     * Tests supplying invalid values to size and ttl properties to verify code will override to use hardcoded values.
     * The durations are invalid because durations can't contain an imprecise datapoint such as months or years (e.g does
     * the month have 28, 29, 30, or 31 days)
     * @return
     */
    @Unroll
    def "Repository property cache uses hardcoded values when supplied properties are invalid. size: #size; ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_REPOSITORY_PROPERTY_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_REPOSITORY_PROPERTY_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createRepositoryPropertyCacheBuilder()

        then:
        builder.maximumSize == IdentityConfig.CACHE_REPOSITORY_PROPERTY_SIZE_DEFAULT
        builder.expireAfterWriteNanos == IdentityConfig.CACHE_REPOSITORY_PROPERTY_TTL_DEFAULT.toNanos()

        where:
        size | ttl
        "a" | "P1MT5M"
        "1/2" | "P1Y"
        "*" | "abc"
        null | null
    }

    @Unroll
    def "Racker auth cache configured via properties. test size: #size; test ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_RACKER_AUTH_RESULT_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_RACKER_AUTH_RESULT_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createRackerAuthCache()

        then:
        builder.maximumSize == size
        builder.expireAfterWriteNanos == Duration.parse(ttl).toNanos()

        where:
        size | ttl
        10 | "PT5M"
        30 | "P1DT1H"
        5 | "PT1H5.5S"
        500 | "PT0S"
    }

    @Unroll
    def "Racker groups cache configured via properties. test size: #size; test ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_RACKER_GROUPS_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_RACKER_GROUPS_TTL_PROP, ttl)

        when:
        Caffeine builder = cacheConfiguration.createRackerGroupsCache()

        then:
        builder.maximumSize == size
        builder.expireAfterWriteNanos == Duration.parse(ttl).toNanos()

        where:
        size | ttl
        10 | "PT5M"
        30 | "P1DT1H"
        5 | "PT1H5.5S"
        500 | "PT0S"
    }
}
