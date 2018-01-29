package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.config.CacheConfiguration
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.LockoutCheckResult
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.test.SingleTestConfiguration
import org.joda.time.DateTime
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration
import testHelpers.SingletonTestFileConfiguration

import java.time.Duration

@ContextConfiguration(classes=[SingletonTestFileConfiguration.class
        , IdentityConfig.class
        , UserLockoutServiceImpl.class
        , CacheConfiguration.class
        , TestConfig.class])
class UserLockoutServiceImplComponentTest extends Specification{

    @Autowired
    UserDao userDao

    @Autowired
    UserLockoutServiceImpl userLockoutService

    @Shared SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance()
    @Shared SingletonReloadableConfiguration reloadableConfiguration = SingletonReloadableConfiguration.getInstance()

    def "test user does not exist"() {
        def username = "doesNotExist"
        Mockito.when(userDao.getUserByUsername(username)).thenReturn(null)

        when:
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(username)

        then:
        lockoutCheckResult.getUser() == null
        lockoutCheckResult.getLockoutExpiration() == null
        lockoutCheckResult.isUserLoaded()
        !lockoutCheckResult.isUserLockedOut()
    }

    def "User populated when test user exists if cache disabled"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, false)

        def user = new User().with {
            it.username = UUID.randomUUID().toString()
            it
        }
        Mockito.when(userDao.getUserByUsername(user.username)).thenReturn(user)

        when:
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.getUser() == user
        lockoutCheckResult.getLockoutExpiration() == null
        lockoutCheckResult.isUserLoaded()
        !lockoutCheckResult.isUserLockedOut()
    }

    def "User populated when test user exists when cache enabled but no cache hit"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, "PT1S")
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, 6)

        def user = new User().with {
            it.username = UUID.randomUUID().toString()
            it
        }
        Mockito.when(userDao.getUserByUsername(user.username)).thenReturn(user)

        when:
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.getUser() == user
        lockoutCheckResult.getLockoutExpiration() == null
        lockoutCheckResult.isUserLoaded()
        !lockoutCheckResult.isUserLockedOut()
    }

    @Unroll
    def "User lock decision uses CA attribute numattempts: retriesConfig: #retriesConfig; userAttempts:#userAttempts "() {
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, "PT5S")
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retriesConfig)

        def lastFailureDate = new DateTime()
        def user = new User().with {
            it.username = UUID.randomUUID().toString()
            if (userAttempts != null) {
                it.passwordFailureAttempts = userAttempts
                it.passwordFailureDate = lastFailureDate.toDate() // last attempt just occurred
            }
            it
        }
        Mockito.when(userDao.getUserByUsername(user.username)).thenReturn(user)

        when: "cache disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, false)
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.getUser() == user
        if (retriesConfig <= userAttempts) {
            assert lockoutCheckResult.isUserLockedOut()
            assert lockoutCheckResult.getLockoutExpiration() != null
            assert lockoutCheckResult.getLockoutExpiration().equals(lastFailureDate.plusSeconds(5))
        } else {
            assert !lockoutCheckResult.isUserLockedOut()
            assert lockoutCheckResult.getLockoutExpiration() == null
        }
        lockoutCheckResult.isUserLoaded()

        when: "cache enabled w/ no entry"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, true)
        lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.getUser() == user
        if (retriesConfig <= userAttempts) {
            assert lockoutCheckResult.isUserLockedOut()
            assert lockoutCheckResult.getLockoutExpiration() != null
            assert lockoutCheckResult.getLockoutExpiration().equals(lastFailureDate.plusSeconds(5))
        } else {
            assert !lockoutCheckResult.isUserLockedOut()
            assert lockoutCheckResult.getLockoutExpiration() == null
        }
        lockoutCheckResult.isUserLoaded()

        where:
        retriesConfig | userAttempts
        3 | 2
        4 | null
        3 | 3
        4 | 5
    }

    @Unroll
    def "User lock decision uses CA attribute lastFailureDate: lockoutDuration: #lockoutDuration; lastFailureMsDelta:#lastFailureMsDelta; expectedLockOut:#expectedLockOut "() {
        def retries = 3
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, lockoutDuration)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retries)

        def lastFailureDate = new DateTime().plus(lastFailureMsDelta)
        def user = new User().with {
            it.username = UUID.randomUUID().toString()
            it.passwordFailureAttempts = retries
            it.passwordFailureDate = lastFailureDate.toDate()
            it
        }
        Mockito.when(userDao.getUserByUsername(user.username)).thenReturn(user)

        when: "cache disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, false)
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.isUserLockedOut() == expectedLockOut

        when: "cache enabled - w/ no entry"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, true)
        lockoutCheckResult = userLockoutService.performLockoutCheck(user.username)

        then:
        lockoutCheckResult.isUserLockedOut() == expectedLockOut

        where:
        lockoutDuration | lastFailureMsDelta | expectedLockOut
        "PT1S" | 0 | true
        "PT1S" | -1 | true
        "PT1S" | -1001 |false
        "PT1S" | 1 | true
        "PT2S" | -1001 | true
    }

    @Unroll
    def "Cache entry added for locked users only when cache enabled. cacheEnabled: #cacheEnabled"() {
        def retries = 3
        Duration duration = Duration.parse("PT5S")
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, cacheEnabled)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, duration.toString())
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retries)

        def lastFailureDate = new DateTime()
        def lockedOutUser = new User().with {
            it.username = UUID.randomUUID().toString()
            it.passwordFailureAttempts = retries
            it.passwordFailureDate = lastFailureDate.toDate()
            it
        }

        Mockito.when(userDao.getUserByUsername(lockedOutUser.username)).thenReturn(lockedOutUser)
        assert userLockoutService.getUserLockoutCache().get(lockedOutUser.username) == null

        when: "Locked user auths"
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(lockedOutUser.username)

        then: "cache entry added if enabled"
        lockoutCheckResult.isUserLockedOut()
        lockoutCheckResult.isUserLoaded() // Loaded the first time when no cache entry
        lockoutCheckResult.getLockoutExpiration() == lastFailureDate.plus(duration.toMillis())
        if (cacheEnabled) {
            Cache.ValueWrapper cacheEntry = userLockoutService.getUserLockoutCache().get(lockedOutUser.username)
            assert cacheEntry != null
            UserLockoutServiceImpl.UserLockoutCacheEntry uloCe = (UserLockoutServiceImpl.UserLockoutCacheEntry) cacheEntry.get()
            assert uloCe.getUserName() == lockedOutUser.username
            assert uloCe.getLastPasswordFailureDate() == lastFailureDate
            assert uloCe.getLockoutExpiration() == lastFailureDate.plus(duration.toMillis())
        } else {
            assert userLockoutService.getUserLockoutCache().get(lockedOutUser.username) == null
        }

        where:
        cacheEnabled << [false, true]
    }

    def "When lockout cached, user object not returned"() {
        def retries = 3
        Duration duration = Duration.parse("PT5M")
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, duration.toString())
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retries)

        def lastFailureDate = new DateTime()
        def lockedOutUser = new User().with {
            it.username = UUID.randomUUID().toString()
            it.passwordFailureAttempts = retries
            it.passwordFailureDate = lastFailureDate.toDate()
            it
        }

        Mockito.when(userDao.getUserByUsername(lockedOutUser.username)).thenReturn(lockedOutUser)
        assert userLockoutService.getUserLockoutCache().get(lockedOutUser.username) == null

        LockoutCheckResult initialLockoutCheckResult = userLockoutService.performLockoutCheck(lockedOutUser.username)
        assert initialLockoutCheckResult.isUserLockedOut()
        assert userLockoutService.getUserLockoutCache().get(lockedOutUser.username) != null

        when: "Perform second check"
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(lockedOutUser.username)

        then: "Result reflects use of cache"
        lockoutCheckResult.isUserLockedOut()
        !lockoutCheckResult.isUserLoaded()
        lockoutCheckResult.getLockoutExpiration() == initialLockoutCheckResult.getLockoutExpiration()

        where:
        cacheEnabled << [false, true]
    }

    def "Cache entries with expired lockouts are removed. Expiration dynamically calculated"() {
        def retries = 3
        Duration initialLockoutDuration = Duration.parse("PT5M")
        Duration unlockedLockoutDuration = Duration.parse("PT1S")
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, initialLockoutDuration.toString())
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retries)

        /*
         Set the last password failure to be 2M before the lockout. Value doesn't matter as long as < initialLockoutDuration
          and greater than unlockedLockoutDuration.
        */
        def lastFailureDate = new DateTime().minus(initialLockoutDuration.toMillis()).plusMinutes(2)

        def initialExpiration = lastFailureDate.plus(initialLockoutDuration.toMillis())

        def lockedOutUser = new User().with {
            it.username = UUID.randomUUID().toString()
            it.passwordFailureAttempts = retries
            it.passwordFailureDate = lastFailureDate.toDate()
            it
        }

        Mockito.when(userDao.getUserByUsername(lockedOutUser.username)).thenReturn(lockedOutUser)

        when: "Perform initial check"
        LockoutCheckResult lockoutCheck = userLockoutService.performLockoutCheck(lockedOutUser.username)

        then: "user is locked out until initial duration"
        lockoutCheck.isUserLockedOut()
        lockoutCheck.getLockoutExpiration() == initialExpiration
        Cache.ValueWrapper cacheEntry = userLockoutService.getUserLockoutCache().get(lockedOutUser.username)

        and: "cache entry reflects this"
        assert cacheEntry != null
        UserLockoutServiceImpl.UserLockoutCacheEntry loCacheEntry = (UserLockoutServiceImpl.UserLockoutCacheEntry) cacheEntry.get()
        loCacheEntry.getLockoutExpiration() == initialExpiration
        loCacheEntry.inLockoutPeriod()

        when: "lower configured expiration"
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, unlockedLockoutDuration.toString())
        Cache.ValueWrapper postUpdateCacheEntry = userLockoutService.getUserLockoutCache().get(lockedOutUser.username)

        then: "cache entry dynamically reflects lowered lockout period"
        assert postUpdateCacheEntry != null
        UserLockoutServiceImpl.UserLockoutCacheEntry postUpdateCacheEntryLoCacheEntry = (UserLockoutServiceImpl.UserLockoutCacheEntry) postUpdateCacheEntry.get()
        postUpdateCacheEntryLoCacheEntry.getLockoutExpiration() == null // a value is only returned if in lockout period
        !postUpdateCacheEntryLoCacheEntry.inLockoutPeriod()

        when: "Recheck user with lowered lockout period"
        lockoutCheck = userLockoutService.performLockoutCheck(lockedOutUser.username)

        then: "User is no longer locked out and user is loaded"
        !lockoutCheck.isUserLockedOut()
        lockoutCheck.getLockoutExpiration() == null
        lockoutCheck.getUser() != null

        and: "Cache entry was removed"
        userLockoutService.getUserLockoutCache().get(lockedOutUser.username) == null
    }

    /**
     * Test that an unlocked user is not cached when cache enabled, and doesn't cause issues when cache disabled
     */
    @Unroll
    def "Cache entry not added for unlocked users. cacheEnabled: #cacheEnabled"() {
        def retries = 3
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, cacheEnabled)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, "PT5S")
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, retries)

        def nonLockedUser = new User().with {
            it.username = UUID.randomUUID().toString()
            it.passwordFailureAttempts = retries - 1
            it.passwordFailureDate = new Date()
            it
        }

        Mockito.when(userDao.getUserByUsername(nonLockedUser.username)).thenReturn(nonLockedUser)
        assert userLockoutService.getUserLockoutCache().get(nonLockedUser.username) == null

        when: "Locked user auths"
        LockoutCheckResult lockoutCheckResult = userLockoutService.performLockoutCheck(nonLockedUser.username)

        then: "cache entry not added"
        !lockoutCheckResult.isUserLockedOut()
        userLockoutService.getUserLockoutCache().get(nonLockedUser.username) == null

        where:
        cacheEnabled << [false, true]
    }

    @SingleTestConfiguration
    static class TestConfig {
        @Bean
        UserDao userDao () {
            return Mockito.mock(UserDao.class)
        }

        @Bean
        IdentityPropertyValueConverter identityPropertyValueConverter () {
            return new IdentityPropertyValueConverter()
        }
        @Bean
        IdentityPropertyService identityPropertyService () {
            return  Mockito.mock(IdentityPropertyService.class)
        }
    }
}
