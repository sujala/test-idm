package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.math.RandomUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Token
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.saml.SamlAssertionFactory

import java.util.regex.Pattern

import static com.rackspace.idm.Constants.*

/**
 * Impersonation tests. Those test referring to use cases refer to: https://one.rackspace.com/display/~robe4218/D-17537+Invalid+Impersonation+Tokens
 */
class Cloud20ImpersonationIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(Cloud20ImpersonationIntegrationTest.class)

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
    LdapScopeAccessRepository scopeAccessRepository

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

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
        staticIdmConfiguration.setProperty("feature.restrict.impersonation.to.rackers.with.role.enabled", false)
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

    def "impersonate with invalid IDP returns 404"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when:
        federatedUser.federatedIdp = "invalid"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then:
        impersonationResponse.status == 404

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "impersonate with invalid username returns 404"() {
        given:
        def federatedUser = v2Factory.createUser().with {
            it.federatedIdp = DEFAULT_IDP_URI
            it.username = "invalid"
            it
        }

        when:
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then:
        impersonationResponse.status == 404
    }

    def "impersonate federated user as identity admin"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersonationToken)

        and: "the impersonation token is referencing a new token under the federated user"
        impersonationTokenEntity.impersonatingToken != null
        def impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)
        impersonatedToken.authenticatedBy.size() == 1
        impersonatedToken.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
        impersonatedToken.accessTokenString != samlToken

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "impersonate federated user as racker"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(rackerToken, federatedUser)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersonationToken)

        and: "the impersonation token is referencing a new token under the federated user"
        impersonationTokenEntity.impersonatingToken != null
        def impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)
        impersonatedToken.authenticatedBy.size() == 1
        impersonatedToken.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
        impersonatedToken.accessTokenString != samlToken

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "impersonate federated user impersonates existing impersonated tokens if they are within the requested window"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then: "a new impersonated token is created"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersonationToken)
        def impersonatedToken1 = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)
        assertImpersonatedToken(impersonatedToken1)

        when: "impersonate the federated user with another user"
        impersonationResponse = cloud20.impersonate(rackerToken, federatedUser)

        then: "the impersonation token is referencing the previously created impersonated token"
        impersonationResponse.status == 200
        def impersonationToken2 = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity2 = scopeAccessService.getScopeAccessByAccessToken(impersonationToken2)
        def impersonatedToken2 = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity2.impersonatingToken)
        assertImpersonatedToken(impersonatedToken2)
        impersonatedToken2.accessTokenString == impersonatedToken1.accessTokenString

        when: "expire the impersonated token and impersonate the user again using the first user"
        expireToken(impersonatedToken1.accessTokenString)
        impersonationResponse = cloud20.impersonate(rackerToken, federatedUser)

        then: "a new impersonated token is created"
        impersonationResponse.status == 200
        def impersonationToken3 = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity3 = scopeAccessService.getScopeAccessByAccessToken(impersonationToken3)
        def impersonatedToken3 = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity3.impersonatingToken)
        assertImpersonatedToken(impersonatedToken3)
        impersonatedToken3.accessTokenString != impersonatedToken1.accessTokenString

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "a federated user cannot get an impersonated token when authenticating with saml"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        when: "authenticate as the federated user again"
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        then: "the request was successful"
        samlResponse.status == 200

        and: "the token returned is not the previously created impersonated token"
        authResponse2.token.id != impersonationToken

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "authenticating as a federated user deletes expired federated tokens"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersonationToken)
        def impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)

        when: "expire the impersonated token and authenticate as the federated user again"
        expireToken(impersonatedToken.accessTokenString)
        def fedUserTokensBefore = scopeAccessService.getScopeAccessListByUserId(authResponse.user.id)
        //we need to call 'hasNext' here in order to have the paginator query the directory, otherwise it will query after we authN
        fedUserTokensBefore.iterator().hasNext()
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion)
        def fedUserTokensAfter = scopeAccessService.getScopeAccessListByUserId(authResponse.user.id)

        then: "the request was successful"
        samlResponse2.status == 200

        and: "the expired impersonated token was in the directory before the authentication request"
        def hasImpersonationTokenBefore = false
        for(def token : fedUserTokensBefore) {
            if(token.accessTokenString == impersonatedToken.accessTokenString) {
                hasImpersonationTokenBefore = true
            }
        }
        hasImpersonationTokenBefore == true

        and: "the expired impersonated token is deleted after the authentication request"
        def hasImpersonationTokenAfter = false
        for(def token : fedUserTokensAfter) {
            if(token.accessTokenString == impersonatedToken.accessTokenString) {
                hasImpersonationTokenAfter = true
            }
        }
        hasImpersonationTokenAfter == false

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "federated user impersonation - a new impersonation token is only created if one does not exist that expires on or after requested time"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def impersonateExpireTime = 600 //10 minutes
        def longerImpersonateExpireTime = 3600 //1 hour

        when: "impersonate federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser, impersonateExpireTime)

        then: "a new impersonation token is created"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        when: "impersonate federated user with expiration time greater than previous requested expiration time"
        def impersonationResponse2 = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser, longerImpersonateExpireTime)

        then: "a new impersonation token is created"
        impersonationResponse2.status == 200
        def impersonationToken2 = impersonationResponse2.getEntity(ImpersonationResponse).token.id
        impersonationToken != impersonationToken2

        when: "impersonate federated user with expiration time less than first requested expiration time"
        //we want to get the same token as the request above. In order to be able to do this we need to request an expiration time that is
        //greater than the first request but less than the second request. We can try to approximate this this area by subtracting
        //30 minutes from the longest requested time (1 hour)
        def impersonationResponse3 = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser, longerImpersonateExpireTime - (60 * 30))

        then: "the token with the latest expiration time (the one created second) is returned"
        impersonationResponse3.status == 200
        def impersonationToken3 = impersonationResponse3.getEntity(ImpersonationResponse).token.id
        impersonationToken3 != impersonationToken
        impersonationToken3 == impersonationToken2

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    /**
     * This test case shows that impersonation tokens that do not have an impersonatingRsId attribute are not valid to use for federated users
     * even if the impersonatingUsername on the token matches the federated user's username. These tokens are tokens created before the
     * impersonatingRsId attribute was added and should only be considered valid for provisioned users.
     */
    def "impersonating a federated user does not return impersonation tokens for provisioned users with the same username (even if the token does not have the impersonatingRsId attribute)"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def impersonateExpireTime = 3600 //1 hour
        def userAdminToken = utils.getToken(userAdmin.username)
        def provUserWithSameUsername = utils.createUser(userAdminToken, username, domainId)

        when: "impersonate a provisioned user with the same name as the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), provUserWithSameUsername, impersonateExpireTime)

        then: "the impersonation token is created"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        def impersonationTokenEntity = scopeAccessService.getScopeAccessByAccessToken(impersonationToken)

        when: "delete the impersonatingRsId attribute on the created impersonation token and impersonate the federated user"
        impersonationTokenEntity.impersonatingRsId = null
        scopeAccessService.updateScopeAccess(impersonationTokenEntity)
        //we want to try to impersonate the federated user but with a shorter expiration time than the expiration time
        //for the impersonation token created for the provisioned user. If we do not do this, then we would create
        //a new impersonation token for the federated user anyways because there would be no impersonation tokens with
        //a sufficiently long expiration time.
        def impersonationResponse2 = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser, impersonateExpireTime - (60 * 10))

        then: "a new token is created for the federated user (the token for the provisioned user is not returned)"
        impersonationResponse2.status == 200
        def impersonationToken2 = impersonationResponse2.getEntity(ImpersonationResponse).token.id
        impersonationToken2 != impersonationToken

        when: "now try to impersonate the provisioned user again"
        def impersonationResponse3 = cloud20.impersonate(utils.getIdentityAdminToken(), provUserWithSameUsername, impersonateExpireTime)

        then: "the token originally created for the user us returned"
        impersonationResponse3.status == 200
        def impersonationToken3 = impersonationResponse3.getEntity(ImpersonationResponse).token.id
        impersonationToken3 != impersonationToken2
        impersonationToken3 == impersonationToken

        and: "the impersonation token for the provisioned user has the impersonatingRsId attribute updated to the user's ID"
        def impersonationTokenEntity2 = scopeAccessService.getScopeAccessByAccessToken(impersonationToken3)
        impersonationTokenEntity2.impersonatingRsId == provUserWithSameUsername.id

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUser(provUserWithSameUsername)
        utils.deleteUsers(users)
    }

    def "impersonating a federated user only cleans up tokens for the federated user"() {
        given:
        staticIdmConfiguration.setProperty(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def impersonateExpireTime = 3600 //1 hour
        def userAdminToken = utils.getToken(userAdmin.username)
        def provUserWithSameUsername = utils.createUser(userAdminToken, username, domainId)
        def adminToken = utils.getIdentityAdminToken()

        when: "impersonate a federated user"
        def impersonationResponse = cloud20.impersonate(adminToken, federatedUser, impersonateExpireTime)

        then: "the token is created"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        impersonationToken != null

        when: "impersonate a provisioned user"
        def provImpersonationResponse = cloud20.impersonate(adminToken, provUserWithSameUsername, impersonateExpireTime)

        then: "a different token is created for the provisioned user"
        provImpersonationResponse.status == 200
        def provImpersonationToken = provImpersonationResponse.getEntity(ImpersonationResponse).token.id
        provImpersonationToken != impersonationToken

        when: "expire that token for both users and impersonate the federated user again"
        expireToken(impersonationToken)
        expireToken(provImpersonationToken)
        def impersonationResponse2 = cloud20.impersonate(adminToken, federatedUser, impersonateExpireTime)

        then: "a new token is created"
        impersonationResponse2.status == 200
        def impersonationToken2 = impersonationResponse2.getEntity(ImpersonationResponse).token.id
        impersonationToken2 != impersonationToken

        and: "the expired token for the federated user was deleted"
        scopeAccessService.getScopeAccessByAccessToken(impersonationToken) == null

        and: "the expired token for the provisioned user was not deleted"
        scopeAccessService.getScopeAccessByAccessToken(provImpersonationToken) != null

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUser(provUserWithSameUsername)
        utils.deleteUsers(users)
        staticIdmConfiguration.clearProperty(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME)
    }

    def "impersonating a provisioned user only cleans up tokens for the provisioned user"() {
        given:
        staticIdmConfiguration.setProperty(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME, true)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def impersonateExpireTime = 3600 //1 hour
        def userAdminToken = utils.getToken(userAdmin.username)
        def provUserWithSameUsername = utils.createUser(userAdminToken, username, domainId)
        def adminToken = utils.getIdentityAdminToken()

        when: "impersonate a federated user"
        def impersonationResponse = cloud20.impersonate(adminToken, federatedUser, impersonateExpireTime)

        then: "the token is created"
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id
        impersonationToken != null

        when: "impersonate a provisioned user"
        def provImpersonationResponse = cloud20.impersonate(adminToken, provUserWithSameUsername, impersonateExpireTime)

        then: "a different token is created for the provisioned user"
        provImpersonationResponse.status == 200
        def provImpersonationToken = provImpersonationResponse.getEntity(ImpersonationResponse).token.id
        provImpersonationToken != impersonationToken

        when: "expire the provisioned and federated users' token and impersonate the provisioned user again"
        expireToken(impersonationToken)
        expireToken(provImpersonationToken)
        def provImpersionationResponse2 = cloud20.impersonate(adminToken, provUserWithSameUsername, impersonateExpireTime)

        then: "a different token is created"
        provImpersonationResponse.status == 200
        def provImpersonationToken2 = provImpersionationResponse2.getEntity(ImpersonationResponse).token.id
        provImpersonationToken2 != provImpersonationToken

        and: "the expired token for the provisioned user is deleted"
        scopeAccessService.getScopeAccessByAccessToken(provImpersonationToken) == null

        and: "the impersonation token for the federated user was not deleted"
        scopeAccessService.getScopeAccessByAccessToken(impersonationToken) != null

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUser(provUserWithSameUsername)
        utils.deleteUsers(users)
        staticIdmConfiguration.clearProperty(DefaultScopeAccessService.LIMIT_IMPERSONATED_TOKEN_CLEANUP_TO_IMPERSONATEE_PROP_NAME)
    }

    def "creating an impersonation token no longer sets the username on the token"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForImpersonate")
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def identityAdmin = utils.getUserByName(IDENTITY_ADMIN_USERNAME)
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id

        when: "impersonate a user using a provisioned user"
        def response = cloud20.impersonate(utils.getIdentityAdminToken(), userAdmin)
        def tokenId = response.getEntity(ImpersonationResponse).token.id
        def tokenEntity = scopeAccessService.getScopeAccessByAccessToken(tokenId)

        then: "the token stored in the directory has a userRsId attribute"
        tokenEntity.userRsId == identityAdmin.id

        and: "the token stored in the directory does not have a username attribute"
        tokenEntity.username == null

        when: "validate to token to verify that is works correctly"
        def validateResponse = cloud20.validateToken(utils.getIdentityAdminToken(), tokenId)

        then:
        validateResponse.status == 200

        when: "impersonate a user using a racker"
        response = cloud20.impersonate(rackerToken, userAdmin)
        tokenId = response.getEntity(ImpersonationResponse).token.id
        tokenEntity = scopeAccessService.getScopeAccessByAccessToken(tokenId)

        then: "the token stored in the directory has a rackerId attribute"
        tokenEntity.rackerId == RACKER_IMPERSONATE

        and: "the token stored in the directory does not have a userRsId attribute"
        tokenEntity.userRsId == null

        and: "the token stored in the directory does not have a username attribute"
        tokenEntity.username == null

        when: "validate to token to verify that is works correctly"
        validateResponse = cloud20.validateToken(utils.getIdentityAdminToken(), tokenId)

        then:
        validateResponse.status == 200

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
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
        scopeAccessRepository.updateObject(token)

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

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

    def assertImpersonatedToken(token) {
        token.authenticatedBy.size() == 1
        token.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
    }

    def void expireToken(tokenString) {
        Date now = new Date()
        Date past = new Date(now.year - 1, now.month, now.day)
        setTokenExpiration(tokenString, past)
    }

    def void setTokenExpiration(tokenString, tokenExp) {
        def userScopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenString)
        userScopeAccess.setAccessTokenExp(tokenExp)
        scopeAccessRepository.updateScopeAccess(userScopeAccess)
    }

}
