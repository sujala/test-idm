package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserLockoutService
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

/**
 * This test relies on static configuration settings within the CA container that enforces password policies with
 * a 6 failed attempt threshold and a 1 second lockout.
 *
 * Note - timing is critical for these tests. In order to test post-lockout aspects the lockout period must be
 * kept short. Otherwise the tests will take too long. Skew between the CA node and the API node will impact the
 * lockout as the CA node will set the lockout period, while the API will compare it's clock against that lockout
 * period. The lockout period for these tests is set to 1s so any tests run after one second from when user
 */
class AuthWithPasswordLockoutIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthWithPasswordLockoutIntegrationTest.class)

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    UserLockoutService userLockoutService

    @Unroll
    def "CA updates lockout fields on failed auth up to threshold regardless of IDM lockout cache; cacheEnabled: #useCache"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, useCache)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, "PT5S")
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, 6)

        def user = utils.createGenericUserAdmin()

        // User starts out with an unset failure date
        User userEntity = identityUserService.getProvisionedUserById(user.id)
        assert userEntity.getPasswordFailureDate() == null
        assert userEntity.getPasswordFailureAttempts() == 0

        // Auth 6 times (threshold set to 6)
        def lastFailedDate = null
        6.times { index ->
            def response = cloud20.authenticate(user.username, "wrongpassword")
            assert response.status == HttpStatus.SC_UNAUTHORIZED

            // Failure date is set and failed attempts incremented
            userEntity = identityUserService.getProvisionedUserById(user.id)
            assert userEntity.getPasswordFailureDate() != null
            lastFailedDate = userEntity.getPasswordFailureDate()
            assert userEntity.getPasswordFailureAttempts() == index + 1
        }

        when: "Login during lockout period with wrong password"
        def response = cloud20.authenticate(user.username, "wrongpassword")
        userEntity = identityUserService.getProvisionedUserById(user.id)

        then: "Returns 401"
        assert response.status == HttpStatus.SC_UNAUTHORIZED

        and: "Failure count not incremented"
        assert userEntity.getPasswordFailureDate() == lastFailedDate
        assert userEntity.getPasswordFailureAttempts() == 6

        when: "Login during lockout period with correct password"
        response = cloud20.authenticate(user.username, Constants.DEFAULT_PASSWORD)
        userEntity = identityUserService.getProvisionedUserById(user.id)

        then: "Returns 401"
        assert response.status == HttpStatus.SC_UNAUTHORIZED

        and: "Failure count not incremented"
        assert userEntity.getPasswordFailureDate() == lastFailedDate
        assert userEntity.getPasswordFailureAttempts() == 6

        where:
        useCache << [true,false]
    }

    @Unroll
    def "CA resets failure attributes after lockout period regardless of cache: cacheEnabled: #useCache"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_LDAP_AUTH_PASSWORD_LOCKOUT_CACHE_PROP, useCache)
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_DURATION_PROP, "PT1S")
        reloadableConfiguration.setProperty(IdentityConfig.LDAP_AUTH_PASSWORD_LOCKOUT_RETRIES_PROP, 6)

        def user = utils.createGenericUserAdmin()

        // Auth 6 times (threshold set to 6)
        6.times { index ->
            def response = cloud20.authenticate(user.username, "wrongpassword")
            assert response.status == HttpStatus.SC_UNAUTHORIZED
        }

        def userEntity = identityUserService.getProvisionedUserById(user.id)
        assert userEntity.getPasswordFailureDate() != null
        assert userEntity.getPasswordFailureAttempts() == 6

        def timeout = 10000 // 10 sec
        def step = 1000 // 1 sec
        def waitedFor = 0
        def lockoutEntry = userLockoutService.performLockoutCheck(userEntity.username)
        while (lockoutEntry != null && waitedFor < timeout) {
            lockoutEntry = userLockoutService.performLockoutCheck(userEntity.username)
            waitedFor += step
            sleep(step)
            // do not explicitly fail the test on timeout here. The next assertions will do that for you
        }

        when: "Login after lockout with wrong password"
        def response = cloud20.authenticate(user.username, "wrongpassword")
        userEntity = identityUserService.getProvisionedUserById(user.id)

        then: "Returns 401"
        response.status == HttpStatus.SC_UNAUTHORIZED

        and: "Failure count reset to 1"
        userEntity.getPasswordFailureDate() != null
        userEntity.getPasswordFailureAttempts() == 1

        when: "Login with correct password"
        response = cloud20.authenticate(user.username, Constants.DEFAULT_PASSWORD)
        userEntity = identityUserService.getProvisionedUserById(user.id)

        then: "Returns 200"
        response.status == HttpStatus.SC_OK

        and: "Failure count reset to 1"
        userEntity.getPasswordFailureDate() == null
        userEntity.getPasswordFailureAttempts() == 0

        where:
        useCache << [true,false]
    }



}
