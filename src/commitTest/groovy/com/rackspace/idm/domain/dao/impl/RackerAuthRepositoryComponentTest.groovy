package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.config.CacheConfiguration
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.RackerAuthDao
import com.rackspace.idm.exception.GatewayException
import com.unboundid.ldap.sdk.*
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Unroll
import spock.mock.DetachedMockFactory
import testHelpers.SingletonTestFileConfiguration

import java.time.Duration

/**
 * Component test the racker repository. Includes the cache as part of component tests, but use a mock connection
 * to LDAP.
 */
@ContextConfiguration(classes=[SingletonTestFileConfiguration.class
        , IdentityConfig.class
        , RackerAuthRepository.class
        , HashedStringKeyGenerator.class
        , TestConfig.class])
class RackerAuthRepositoryComponentTest extends Specification {
    private static final String SAMPLE_GROUP_DN = "cn=team-cloud-identity,ou=Groups,o=rackspace,dc=rackspace,dc=com"
    private static final String SAMPLE_GROUP_NAME = "team-cloud-identity"
    @Autowired
    RackerAuthDao rackerAuthRepository

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    RackerConnectionPoolDelegate rackerConnectionPoolDelegate

    @Autowired
    CacheManager cacheManager

    @Autowired
    @Qualifier("reloadableConfiguration")
    Configuration reloadableConfiguration

    @Autowired
    @Qualifier("staticConfiguration")
    Configuration staticConfiguration

    def setup() {
        // clear caches for each test run
        for (String cacheName : cacheManager.cacheNames ) {
            Cache cache = cacheManager.getCache(cacheName)
            cache.clear()
        }
        testHelpers.SingletonReloadableConfiguration.getInstance().reset()
    }

    def "authenticate: Returns false when user search doesn't find any user"() {
        when:
        def result = rackerAuthRepository.authenticate("u", "p")

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> null
        !result
    }

    def "authenticateWithCache: Returns user not found result when user search doesn't find any user"() {
        when:
        def result = rackerAuthRepository.authenticateWithCache("u", "p")

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> null
        result == RackerAuthResult.USER_NOT_FOUND
    }

    def "authenticateWithCache: throws gatewayexception when user search results in exception"() {
        when:
        rackerAuthRepository.authenticateWithCache("u", "p")

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> { throw new LDAPException(ResultCode.ALIAS_PROBLEM)}
        thrown(GatewayException)
    }

    @Unroll
    def "authenticate: Always auth regardless of previous auth result. First auth result: #resultCode"() {
        def username = "u"
        def userBindDN = "userBindDN"
        def pwd = "p"

        when: "Login for first time"
        def result = rackerAuthRepository.authenticate(username, pwd)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN)
        1 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd) >> { calcBindResult(resultCode) }

        // result is only successful if the result code is success
        result == (resultCode == ResultCode.SUCCESS)

        when: "Login for second time"
        def result2 = rackerAuthRepository.authenticate(username, pwd)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN)
        1 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd) >> { calcBindResult(resultCode) }

        // result is only successful if the result code is success
        result2 == (resultCode == ResultCode.SUCCESS)

        where:
        resultCode << ResultCode.values()
    }

    /**
     * Auth results are only cached if the result is reported as a successful auth or failure due to invalid credentials.
     * Any other failure will not be cached.
     */
    @Unroll
    def "authenticateWithCache: Reuses cache when first auth result is #resultCode"() {
        def username = "u"
        def userBindDN = "userBindDN"
        def pwd = "p"

        when: "Login for first time"
        def result = rackerAuthRepository.authenticateWithCache(username, pwd)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN)
        1 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd) >> { calcBindResult(resultCode) }

        result == RackerAuthResult.SUCCESS

        when: "Login for second time"
        def result2 = rackerAuthRepository.authenticateWithCache(username, pwd)

        then: "Cache is used - no calls to racker pool delegate"
        0 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _)
        0 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd)

        result == RackerAuthResult.SUCCESS

        where:
        resultCode << [ResultCode.SUCCESS]
    }

    @Unroll
    def "authenticateWithCache: Does not cache when first auth result is #resultCode"() {
        def username = "u"
        def userBindDN = "userBindDN"
        def pwd = "p"

        when: "Login for first time"
        def result = rackerAuthRepository.authenticateWithCache(username, pwd)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN)
        1 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd) >> { calcBindResult(resultCode) }

        if (resultCode == ResultCode.SUCCESS) {
            assert result == RackerAuthResult.SUCCESS
        } else if (resultCode == ResultCode.INVALID_CREDENTIALS) {
            assert result == RackerAuthResult.INVALID_CREDENTIALS
        } else {
            assert result == RackerAuthResult.UNKNOWN_FAILURE
        }

        when: "Login for second time"
        def result2 = rackerAuthRepository.authenticateWithCache(username, pwd)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN)
        1 * rackerConnectionPoolDelegate.bindAndRevertAuthentication(userBindDN, pwd) >> { calcBindResult(resultCode) }

        assert result2 == result

        where:
        resultCode << ResultCode.values().minus([ResultCode.SUCCESS])
    }

    def "getRackerRoles: Always retrieves latest groups from AD"() {
        def username = "u"
        def userBindDN = "userBindDN"
        Attribute groups = new Attribute(RackerAuthRepository.ATTR_MEMBERSHIP, SAMPLE_GROUP_DN)
        Attribute[] attributes = [groups]

        when: "Search for first time"
        List<String> result = rackerAuthRepository.getRackerRoles(username)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN, attributes)
        result.contains(SAMPLE_GROUP_NAME)

        when: "Search for second time"
        List<String> result2 = rackerAuthRepository.getRackerRoles(username)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN, attributes)

        result2.contains(SAMPLE_GROUP_NAME)
    }

    def "getRackerRolesWithCache: Stores groups in cache"() {
        def username = "u"
        def userBindDN = "userBindDN"
        Attribute groups = new Attribute(RackerAuthRepository.ATTR_MEMBERSHIP, SAMPLE_GROUP_DN)
        Attribute[] attributes = [groups]

        when: "Search for first time"
        List<String> result = rackerAuthRepository.getRackerRolesWithCache(username)

        then:
        1 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _) >> dummySearchResult(userBindDN, attributes)
        result.contains(SAMPLE_GROUP_NAME)

        when: "Search for second time"
        List<String> result2 = rackerAuthRepository.getRackerRolesWithCache(username)

        then:
        0 * rackerConnectionPoolDelegate.searchForEntry(_, _, _, _)
        result2.contains(SAMPLE_GROUP_NAME)
    }

    def dummySearchResult(String dn, Attribute[] attributes = new Attribute[0]){
        return new SearchResultEntry(dn, attributes, new Control("asdf"))
    }

    /**
     * This mirrors the unboundId auth result, which seems to throw an LDAPException anytime the bind results in anything
     * other than a success.
     *
     * @param resultCode
     * @return
     */
    def calcBindResult(ResultCode resultCode) {
        if (resultCode == ResultCode.SUCCESS) {
            return dummyBindResult(resultCode)
        } else {
            throw new LDAPException(resultCode)
        }
    }

    def dummyBindResult(ResultCode resultCode) {
        return new BindResult(1, resultCode, "", "", null, null)
    }

    @com.rackspace.test.SingleTestConfiguration
    @EnableCaching
    static class TestConfig {
        // Specify duration and convert to millis to remain consistent with how ttl is specified in prop file
        static int cacheTtl = Duration.parse("PT0.5S").toMillis()
        static int cacheSize = 10

        def factory = new DetachedMockFactory()

        @Autowired
        IdentityConfig identityConfig

        @Autowired
        @Qualifier("reloadableConfiguration")
        Configuration reloadableConfiguration

        @Autowired
        @Qualifier("staticConfiguration")
        Configuration staticConfiguration

        /**
         * Dependency of DefaultRackerAuthenticationService that we need to mock
         * @return
         */
        @Bean
        RackerConnectionPoolDelegate connectionPool() {
            return factory.Mock(RackerConnectionPoolDelegate)
        }

        /**
         * Dependency of IdentityConfig that we want to mock
         * @return
         */
        @Bean
        IdentityPropertyValueConverter identityPropertyValueConverter () {
            return new IdentityPropertyValueConverter()
        }

        /**
         * Explicitly creating the caches here to control the cache TTL for this class's tests.
         * @return
         */
        @Bean
        CacheManager cacheManager() {
            CacheConfiguration cacheConfiguration = new CacheConfiguration()
            cacheConfiguration.identityConfig = identityConfig

            // Hardcode the cache size/ttl to facilitate these tests.
            staticConfiguration.setProperty(IdentityConfig.CACHE_RACKER_AUTH_RESULT_TTL_PROP, cacheTtl)
            staticConfiguration.setProperty(IdentityConfig.CACHE_RACKER_GROUPS_TTL_PROP, cacheTtl)

            staticConfiguration.setProperty(IdentityConfig.CACHE_RACKER_AUTH_RESULT_SIZE_PROP, cacheSize)
            staticConfiguration.setProperty(IdentityConfig.CACHE_RACKER_GROUPS_SIZE_PROP, cacheSize)

            SimpleCacheManager cacheManager = cacheConfiguration.cacheManager()
            return cacheManager
        }
    }
}
