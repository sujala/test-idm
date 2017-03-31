package com.rackspace.idm.domain.config

import com.google.common.cache.CacheBuilder
import org.springframework.cache.guava.GuavaCache
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

    @Unroll
    def "Client Role Id cache configured via properties. test size: #size; test ttl: #ttl"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, size)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, ttl)

        when:
        CacheBuilder builder = cacheConfiguration.createClientRoleCacheBuilder()

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
        CacheBuilder builder = cacheConfiguration.createClientRoleCacheBuilder()

        then:
        builder.maximumSize == IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_DEFAULT
        builder.expireAfterWriteNanos == IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_DEFAULT.toNanos()

        where:
        size | ttl
        "a" | "P1MT5M"
        "1/2" | "P1Y"
        "*" | "abc"
    }

    def "verify 0 ttl guava cache never caches"() {
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, 5)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, "PT0S")

        GuavaCache cache = cacheConfiguration.getClientRoleCache("hello")

        when:
        cache.putIfAbsent("hi", "val")

        then:
        cache.get("hi") == null
    }

    def "verify 0 size guava cache never caches"() {
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_SIZE_PROP, 0)
        staticIdmConfiguration.setProperty(IdentityConfig.CACHE_CLIENT_ROLES_BY_ID_TTL_PROP, "PT10M")

        GuavaCache cache = cacheConfiguration.getClientRoleCache("hello")

        when:
        cache.putIfAbsent("hi", "val")

        then:
        cache.get("hi") == null
    }

}
