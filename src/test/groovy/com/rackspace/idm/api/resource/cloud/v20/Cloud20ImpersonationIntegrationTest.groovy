package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.math.RandomUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Token
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert

import java.util.regex.Pattern

import static com.rackspace.idm.Constants.*

/**
 * Impersonation tests. Those test referring to use cases refer to: https://one.rackspace.com/display/~robe4218/D-17537+Invalid+Impersonation+Tokens
 */
class Cloud20ImpersonationIntegrationTest extends RootConcurrentIntegrationTest {
    static final Integer ONE_HOUR_IN_SECONDS = 60*60

    @Shared User userAdmin
    @Shared def domainId
    @Shared def userAdminToken

     /**
     * In various cases we request a token be valid for "x" amount of time. The validity period is relative to
     * when the token is created, not requested and there is some finite amount of time between
     * when we request a token be valid for X seconds and when the token is actually generated.
     *
     * For example, if I request a token be valid for 1 minute
     * and make the request at 10:37:000, the request might not actually be processed until 10:37:100 (100 ms later). Because of this I can't test whether
     * the returned token expires exactly X amount of time later than when I requested it. Instead I need a fudge factor to account
     * for the non-constant amount of time that could lapse between the time I make the request and when the token is created
     * as well as the entropy.
     *
     * The value should be high enough to account for various slowness, but low enough to provide enough comfort that the
     * code is working appropriately. For tests, the actual value can be +- this value (exclusive) and be considered valid.
     */
    def LATENCY_FUDGE_AMOUNT_MINUTES = 1

    @Autowired
    DefaultUserService userService

    @Autowired
    DefaultScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired
    Configuration config

    def setupSpec() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
    }

    def cleanupSpec() {
        deleteUserQuietly(userAdmin)
    }

    def cleanup() {
        //for this test we want all properties reset for each test
        staticIdmConfiguration.reset()
    }

    def "impersonating a disabled user should be possible"() {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)
        utils.disableUser(localDefaultUser)

        when:
        def token = utils.getImpersonatedToken(specificationIdentityAdmin, localDefaultUser)
        def response = utils.validateToken(token)

        then:
        response != null
        response.token.id != null

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "impersonating a disabled user - with racker" () {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)
        utils.disableUser(localDefaultUser)

        when:
        def token = utils.impersonateWithRacker(localDefaultUser)
        def response = utils.validateToken(token.token.id)

        then:
        response != null
        response.token.id != null

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "impersonating user - racker with impersonate role feature flag = true" () {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)
        staticIdmConfiguration.setProperty("feature.restrict.impersonation.to.rackers.with.role.enabled", true)

        when:
        def response = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def rackerToken = response.token.id
        utils.impersonateWithToken(rackerToken, localDefaultUser)

        then:
        rackerToken != null

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "impersonating user - racker with no impersonate role - feature flag = true" () {
        given:
        staticIdmConfiguration.setProperty("feature.restrict.impersonation.to.rackers.with.role.enabled", true)
        def localDefaultUser = utils.createUser(userAdminToken)

        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def rackerToken = response.token.id
        def impersonateResponse = cloud20.impersonate(rackerToken, localDefaultUser)

        then:
        rackerToken != null
        impersonateResponse.status == 403

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "impersonating user - racker with no impersonate role - feature flag = false" () {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)

        when:
        def response = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def rackerToken = response.token.id
        utils.impersonateWithToken(rackerToken, localDefaultUser)

        then:
        rackerToken != null

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def Integer serviceImpersonatorTokenMaxLifetimeInSeconds() {
        return config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME);
    }

    def Integer rackerImpersonatorTokenMaxLifetimeInSeconds() {
        return config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME);
    }

    @Unroll
    def "impersonate user with existing user token lifetime later than impersonation request"() {
        given:
        def now = new DateTime()

        //just set to the max lifetime of an imp token
        Integer userTokenLifetimeSeconds =  serviceImpersonatorTokenMaxLifetimeInSeconds()
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)

        //the requested impersonation token must be less than the userToken's lifetime, AND be less than the max lifetime for an impersonation token
        Integer requestedImpersonationTokenLifetimeSeconds = userTokenLifetimeSeconds / 2
        DateTime expectedImpersonationTokenExpirationDate = now.plusSeconds(requestedImpersonationTokenLifetimeSeconds)

        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)

        then: "impersonation token will expire before user token and expire at requested time"
        actualImpersonationTokenExpirationDate.isBefore(userTokenExpirationDate)
        assertDateFallsWithinFudgeFactor(expectedImpersonationTokenExpirationDate, actualImpersonationTokenExpirationDate)

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    @Unroll
    def "impersonate user with existing user token lifetime less than impersonation request"() {
        given:
        def now = new DateTime()

        //request the maximum impersonation lifetime
        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds()
        DateTime expectedImpersonationTokenExpirationDate = now.plusSeconds(requestedImpersonationTokenLifetimeSeconds)

        //make sure the user token expires before this maximum
        Integer userTokenLifetimeSeconds =  requestedImpersonationTokenLifetimeSeconds / 2
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)

        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)
        ScopeAccess userTokenPreImpersonate = getMostRecentTokenForUser(localDefaultUser)

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)

        //auth as user to get the latest user token
        ScopeAccess actualUserTokenUsed = getMostRecentTokenForUser(localDefaultUser)
        DateTime actualUserTokenUsedExpirationDate = new DateTime(actualUserTokenUsed.accessTokenExp)

        then: "impersonation token will expire at requested time. new user token created"
        impersonatedScopeAccess.impersonatingToken == actualUserTokenUsed.accessTokenString
        actualImpersonationTokenExpirationDate.isBefore(actualUserTokenUsedExpirationDate)
        assertDateFallsWithinFudgeFactor(expectedImpersonationTokenExpirationDate, actualImpersonationTokenExpirationDate)

        //verify a new token was issued whose expiration date is after the impersonation token AND the initial token
        userTokenPreImpersonate.accessTokenString != actualUserTokenUsed.accessTokenString
        actualUserTokenUsedExpirationDate.isAfter(userTokenExpirationDate)
        actualUserTokenUsedExpirationDate.isAfter(actualImpersonationTokenExpirationDate)

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    @Unroll
    def "impersonate user with only expired user tokens"() {
        given:
        def now = new DateTime()

        Integer userTokenLifetimeSeconds =  -1 * ONE_HOUR_IN_SECONDS
        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds()

        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        DateTime expectedImpersonationTokenExpirationDate = now.plusSeconds(requestedImpersonationTokenLifetimeSeconds)

        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)
        ScopeAccess userTokenPreImpersonate = getMostRecentTokenForUser(localDefaultUser)

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)
        ScopeAccess actualUserTokenUsed = getMostRecentTokenForUser(localDefaultUser)
        DateTime actualUserTokenUsedExpirationDate = new DateTime(actualUserTokenUsed.accessTokenExp)

        then: "impersonation token will expire before user token and expire at requested time"
        impersonatedScopeAccess.impersonatingToken == actualUserTokenUsed.accessTokenString
        actualImpersonationTokenExpirationDate.isBefore(actualUserTokenUsedExpirationDate)
        assertDateFallsWithinFudgeFactor(expectedImpersonationTokenExpirationDate, actualImpersonationTokenExpirationDate)

        //verify a new token was issued whose expiration date is after the impersonation token AND the initial token
        userTokenPreImpersonate.accessTokenString != actualUserTokenUsed.accessTokenString
        actualUserTokenUsedExpirationDate.isAfter(userTokenExpirationDate)
        actualUserTokenUsedExpirationDate.isAfter(actualImpersonationTokenExpirationDate)

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    @Unroll
    def "impersonate user with no user tokens"() {
        given:
        def now = new DateTime()

        Integer userTokenLifetimeSeconds =  -1 * ONE_HOUR_IN_SECONDS
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)

        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds()
        DateTime expectedImpersonationTokenExpirationDate = now.plusSeconds(requestedImpersonationTokenLifetimeSeconds)

        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)

        //expire and delete all tokens for this user
        def userEntity = userService.getUserById(localDefaultUser.id)
        scopeAccessService.deleteExpiredTokens(userEntity)
        assert getMostRecentTokenForUser(localDefaultUser) == null

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)
        ScopeAccess actualUserTokenUsed = getMostRecentTokenForUser(localDefaultUser)
        DateTime actualUserTokenUsedExpirationDate = new DateTime(actualUserTokenUsed.accessTokenExp)

        then: "impersonation token will expire before user token and expire at requested time"
        impersonatedScopeAccess.impersonatingToken == actualUserTokenUsed.accessTokenString
        actualImpersonationTokenExpirationDate.isBefore(actualUserTokenUsedExpirationDate)
        assertDateFallsWithinFudgeFactor(expectedImpersonationTokenExpirationDate, actualImpersonationTokenExpirationDate)

        //verify a new token was issued whose expiration date is after the impersonation token
        actualUserTokenUsedExpirationDate.isAfter(actualImpersonationTokenExpirationDate)

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    /**
     * Choose random values for the key properties and verify the inviolable rule is never broken - that the impersonation token
     * must always expire on or before the user token. Do this 20 times to provide additional assurances regardless of the property
     * values, the user token won't expire first.
     */
    @Unroll("impersonate user random - maxServiceImpRequestAllowed=#maxServiceImpRequestAllowed; userTokenLifetimeSeconds=#userTokenLifetimeSeconds; requestedImpersonationTokenLifetimeSeconds=#requestedImpersonationTokenLifetimeSeconds")
    def "impersonate user random"() {
        given:
        def now = new DateTime()

        staticIdmConfiguration.setProperty(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME, maxServiceImpRequestAllowed)

        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, false)

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)

        //auth as user to get the latest user token
        ScopeAccess actualUserTokenUsed = getMostRecentTokenForUser(localDefaultUser)
        DateTime actualUserTokenUsedExpirationDate = new DateTime(actualUserTokenUsed.accessTokenExp)

        then: "impersonation token will expire on or before user token"
        impersonatedScopeAccess.impersonatingToken == actualUserTokenUsed.accessTokenString
        (actualImpersonationTokenExpirationDate.isBefore(actualUserTokenUsedExpirationDate) || actualImpersonationTokenExpirationDate.isEqual(actualUserTokenUsedExpirationDate))

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        [maxServiceImpRequestAllowed, userTokenLifetimeSeconds, requestedImpersonationTokenLifetimeSeconds] << impersonateRandomDataProvider(20)
    }

    def impersonateRandomDataProvider(int iterations) {
        List vals = new ArrayList();

        for (int i=0; i<iterations; i++) {
            List innerList = new ArrayList<>()
            def maxServiceImpRequestAllowed = RandomUtils.nextInt(24*ONE_HOUR_IN_SECONDS) + 1
            innerList.add(maxServiceImpRequestAllowed)  //maxServiceImpRequestAllowed
            innerList.add(RandomUtils.nextInt(24*ONE_HOUR_IN_SECONDS))  //userTokenLifetimeSeconds
            innerList.add(RandomUtils.nextInt(maxServiceImpRequestAllowed) + 1)  //requestedImpersonationTokenLifetimeSeconds

            vals.add(innerList)
        }
        return vals
    }

    @Unroll
    def "impersonate - impersonation request greater than max service user token lifetime throws exception"() {
        given:
        def now = new DateTime()

        //request the maximum impersonation lifetime
        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds() + ONE_HOUR_IN_SECONDS

        //make sure the user token expires before this maximum
        Integer userTokenLifetimeSeconds =  requestedImpersonationTokenLifetimeSeconds - ONE_HOUR_IN_SECONDS
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)

        when:
        def response = impersonateUserAsIdentityAdminForTokenLifetimeRawResponse(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)

        then: "throws error"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response, BadRequestFault, 400, Pattern.compile("Expire in element cannot be more than \\d*"))

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    @Unroll
    def "impersonate - impersonation request greater than max racker user token lifetime throws exception"() {
        given:
        def now = new DateTime()

        //request the maximum impersonation lifetime
        def requestedImpersonationTokenLifetimeSeconds = rackerImpersonatorTokenMaxLifetimeInSeconds() + ONE_HOUR_IN_SECONDS

        //make sure the user token expires before this maximum
        Integer userTokenLifetimeSeconds =  requestedImpersonationTokenLifetimeSeconds - ONE_HOUR_IN_SECONDS
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)

        when:
        def response = impersonateUserAsRackerForTokenLifetimeRawResponse(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)

        then: "throws error"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response, BadRequestFault, 400, Pattern.compile("Expire in element cannot be more than \\d*"))

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        disableUser << [false, true]
    }

    /**
     * Impersonate the specified user. THe impersonation token should be requested with the given expiration time in seconds
     * @param user
     * @param impersonatedTokenLifetime
     * @return
     */
    def ImpersonatedScopeAccess impersonateUserForTokenLifetime(User user, Integer impersonationTokenExpireInSeconds) {
        ImpersonationResponse impersonationResponse = utils.impersonate(specificationIdentityAdminToken, user, impersonationTokenExpireInSeconds)

        //get impersonation scope access
        ImpersonatedScopeAccess isa = scopeAccessService.getScopeAccessByAccessToken(impersonationResponse.token.id)
        return isa
    }

    /**
     * Impersonate the specified user and return the raw response. THe impersonation token should be requested with the given expiration time in seconds
     * @param user
     * @param impersonatedTokenLifetime
     * @return
     */
    def impersonateUserAsIdentityAdminForTokenLifetimeRawResponse(User user, Integer impersonationTokenExpireInSeconds) {
        def response = cloud20.impersonate(specificationIdentityAdminToken, user, impersonationTokenExpireInSeconds)
        return response
    }

    def impersonateUserAsRackerForTokenLifetimeRawResponse(User user, Integer impersonationTokenExpireInSeconds) {
        def auth = utils.authenticateRacker(RACKER, RACKER_PASSWORD)
        def response = cloud20.impersonate(auth.token.id, user, impersonationTokenExpireInSeconds)
        return response
    }

    def User createUserWithTokenExpirationDate(DateTime tokenExpirationDate, boolean disableUserAtEnd = false) {
        User defaultUser = utils.createUser(userAdminToken)

        //make sure all tokens are expired (they should be, but verify just the same)
        scopeAccessService.expireAllTokensForUserById(defaultUser.id)

        //authenticate normally, to make sure the token is created per usual process
        def defaultUserToken = utils.getToken(defaultUser.username)

        //disable the user. This will expire all tokens on the default user (including the just create one from the authentication)
        if (disableUserAtEnd) {
            disableUser(defaultUser)
        }

        //now reset the token expiration time to the specified lifetime
        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(defaultUserToken)
        token.setAccessTokenExp(tokenExpirationDate.toDate())
        scopeAccessRepository.updateScopeAccess(token)

        return defaultUser
    }

    def void disableUser(User user) {
        User updateUser = new User().with {
            it.id = user.id
            it.enabled = false
            return it
        }
        utils.updateUser(updateUser)
    }

    def DateTime tokenExpirationAsDateTime(Token token) {
        return new DateTime(token.expires.toGregorianCalendar().getTime())
    }

    def void assertDateFallsWithinFudgeFactor(DateTime expectedDate, DateTime actualDate) {
        DateTime fudgedLowerBound = expectedDate.minusMinutes(LATENCY_FUDGE_AMOUNT_MINUTES)
        DateTime fudgedUpperBound = expectedDate.plusMinutes(LATENCY_FUDGE_AMOUNT_MINUTES)

        assert actualDate.isAfter(fudgedLowerBound) && actualDate.isBefore(fudgedUpperBound);
    }

    def ScopeAccess getMostRecentTokenForUser(User user) {
        com.rackspace.idm.domain.entity.User entityUser = userService.getUserById(user.id)
        return scopeAccessRepository.getMostRecentScopeAccessByClientId(entityUser, scopeAccessService.getCloudAuthClientId());
    }

}
