package com.rackspace.idm.api.resource.cloud

import com.google.common.cache.CacheBuilder
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.config.CacheConfiguration
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.IdentityPropertyDao
import com.rackspace.idm.domain.entity.IdentityProperty
import com.rackspace.idm.domain.entity.IdentityPropertyValueType
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.idm.domain.service.impl.DefaultIdentityPropertyService
import com.rackspace.test.SingleTestConfiguration
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.guava.GuavaCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import testHelpers.SingletonTestFileConfiguration

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Component test that validates that the code appropriate caches the repository properties. The backend repository
 * is mocked to limit interaction between the spring based cache and the identity property service.
 */
@ContextConfiguration(classes=[SingletonTestFileConfiguration.class
        , IdentityConfig.class
        , DefaultIdentityPropertyService.class
        , TestConfig.class])
class IdentityPropertyCacheComponentTest extends Specification {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    IdentityPropertyService identityPropertyService

    @Autowired
    IdentityPropertyDao identityPropertyDao

    @Autowired
    CacheManager cacheManager

    def "Reloadable properties are cached and reloaded from directory"() {
        def propName = UUID.randomUUID().toString()

        when:
        def firstReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then: "get reloadable property"
        1 * identityPropertyDao.getIdentityPropertyByName(propName) >> new IdentityProperty().with {
            it.name = propName
            it.reloadable = true
            it.value = "a value"
            it.valueType = IdentityPropertyValueType.STRING.typeName
            it
        }

        and:
        firstReturnedProperty != null

        when: "get property again within ttl"
        def secondReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then: "the dao isn't called to retrieve the property from backend"
        0 * identityPropertyDao.getIdentityPropertyByName(propName)

        and: "the same property instance is returned"
        secondReturnedProperty == firstReturnedProperty

        and: "the cache is populated"
        getRepositoryPropertyCache().get(propName) != null

        when: "wait past ttl and retrieve property"
        sleep(TestConfig.cacheTtl)

        then: "cache no longer contains entry"
        getRepositoryPropertyCache().get(propName) == null

        when: "when retrieve property"
        def thirdReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then:
        1 * identityPropertyDao.getIdentityPropertyByName(propName) >> new IdentityProperty()

        and: "a new instance is returned"
        thirdReturnedProperty != firstReturnedProperty
    }

    def "Non-reloadable properties are never reloaded"() {
        def propName = UUID.randomUUID().toString()

        when:
        def firstReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then: "get non-reloadable property from backend"
        1 * identityPropertyDao.getIdentityPropertyByName(propName) >> new IdentityProperty().with {
            it.name = propName
            it.reloadable = false
            it.value = "a value"
            it.valueType = IdentityPropertyValueType.STRING.typeName
            it
        }

        and:
        firstReturnedProperty != null

        when: "get property again within ttl"
        def secondReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then: "the backend isn't called"
        0 * identityPropertyDao.getIdentityPropertyByName(propName)

        and: "the same property instance is returned"
        secondReturnedProperty == firstReturnedProperty

        and: "the cache is populated"
        getRepositoryPropertyCache().get(propName) != null

        when: "wait past ttl and retrieve property"
        sleep(TestConfig.cacheTtl)

        then: "cache no longer contains entry"
        getRepositoryPropertyCache().get(propName) == null

        when: "when retrieve property"
        def thirdReturnedProperty = identityPropertyService.getImmutableIdentityPropertyByName(propName)

        then: "repository still not hit"
        0 * identityPropertyDao.getIdentityPropertyByName(propName)

        and: "the same instance is returned"
        thirdReturnedProperty == firstReturnedProperty
    }

    private Cache getRepositoryPropertyCache() {
        return cacheManager.getCache(CacheConfiguration.REPOSITORY_PROPERTY_CACHE_BY_NAME)
    }

    @SingleTestConfiguration
    @EnableCaching
    static class TestConfig {

        // Specifying duration and then converting to millis to remaain consistent with how ttl is specified
        static int cacheTtl = Duration.parse("PT0.01S").toMillis()
        static int cacheSize = 10

        def factory = new DetachedMockFactory()

        @Autowired
        Configuration reloadableConfiguration

        @Bean
        IdentityPropertyDao identityPropertyDao () {
            return factory.Mock(IdentityPropertyDao)
        }

        @Bean
        IdentityPropertyValueConverter identityPropertyValueConverter () {
            return new IdentityPropertyValueConverter()
        }

        @Bean
        CacheManager cacheManager() {
            SimpleCacheManager cacheManager = new SimpleCacheManager()
            cacheManager.setCaches(Arrays.asList(
                    new GuavaCache(CacheConfiguration.REPOSITORY_PROPERTY_CACHE_BY_NAME, createRepositoryPropertyCacheBuilder().build()))
            )
            return cacheManager
        }

        private CacheBuilder createRepositoryPropertyCacheBuilder() {
            // set the TTL to 100 ms to allow for
            int size = 10

            return CacheBuilder.newBuilder()
                    .maximumSize(cacheSize)
                    .expireAfterWrite(cacheTtl, TimeUnit.MILLISECONDS)
        }
    }
}
