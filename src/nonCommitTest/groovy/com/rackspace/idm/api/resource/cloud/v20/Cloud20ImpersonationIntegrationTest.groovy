package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.util.OTPHelper
import groovy.json.JsonSlurper
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.math.RandomUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType
import java.util.regex.Pattern

import static com.rackspace.idm.Constants.*
/**
 * Impersonation tests.
 */
class Cloud20ImpersonationIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(Cloud20ImpersonationIntegrationTest.class)

    static final Integer ONE_HOUR_IN_SECONDS = 60*60

    @Shared User userAdmin
    @Shared def domainId
    @Shared def userAdminToken

    @Autowired
    OTPHelper otpHelper

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
    ScopeAccessService scopeAccessService

    @Autowired
    TokenRevocationService tokenRevocationService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    AETokenService aeTokenService;

    @Autowired
    Configuration config

    @Autowired
    IdentityConfig identityConfig

    def setup() {
    }

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

    def "impersonating a disabled user is possible"() {
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

    def "get user by id with impersonated token of disabled user returns 404"() {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)
        def impersonatedToken = utils.getImpersonatedToken(specificationIdentityAdmin, localDefaultUser)
        utils.disableUser(localDefaultUser)

        when:
        def response = cloud20.getUserById(impersonatedToken, localDefaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "User not found")
    }

    def "impersonate - impersonation request greater than max service user token lifetime throws exception"() {
        given:
        def token = utils.getToken(specificationIdentityAdmin.username)

        def now = new DateTime()

        //request the maximum impersonation lifetime
        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds() + ONE_HOUR_IN_SECONDS

        //make sure the user token expires before this maximum
        Integer userTokenLifetimeSeconds =  requestedImpersonationTokenLifetimeSeconds - ONE_HOUR_IN_SECONDS
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate)

        when:
        def response = impersonateUserAsIdentityAdminForTokenLifetimeRawResponse(localDefaultUser, requestedImpersonationTokenLifetimeSeconds, token)

        then: "throws error"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response, BadRequestFault, 400, Pattern.compile("Expire in element cannot be more than \\d*"))

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    @Unroll
    def "impersonate federated user as identity admin; request=#requestContentType, accept=#acceptContentType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
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

        when: "impersonate the federated user again"
        impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser, 10800, requestContentType, acceptContentType)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationTokenEntity = getScopeAccessFromImpersonationResponse(impersonationResponse, acceptContentType)

        and: "the impersonation token is referencing a new token under the federated user"
        impersonationTokenEntity.impersonatingToken != null
        def impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)
        impersonatedToken.authenticatedBy.size() == 1
        impersonatedToken.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
        impersonatedToken.accessTokenString != samlToken

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "impersonate federated user as racker: request=#requestContentType, accept=#acceptContentType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def federatedUser = utils.getUserById(authResponse.getUser().getId())
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(rackerToken, federatedUser, 10800, requestContentType, acceptContentType)

        then: "the request was successful"
        impersonationResponse.status == 200
        def impersonationTokenEntity = getScopeAccessFromImpersonationResponse(impersonationResponse, acceptContentType)

        and: "the impersonation token is referencing a new token under the federated user"
        impersonationTokenEntity.impersonatingToken != null
        def impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(impersonationTokenEntity.impersonatingToken)
        impersonatedToken.authenticatedBy.size() == 1
        impersonatedToken.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
        impersonatedToken.accessTokenString != samlToken

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    /**
     * Choose random values for the key properties and verify the inviolable rule is never broken - that the impersonation token
     * must always expire on or before the user token. Do this 20 times to provide additional assurances regardless of the property
     * values, the user token won't expire first.
     */
    @Unroll
    def "impersonate user random - maxServiceImpRequestAllowed=#maxServiceImpRequestAllowed; userTokenLifetimeSeconds=#userTokenLifetimeSeconds; requestedImpersonationTokenLifetimeSeconds=#requestedImpersonationTokenLifetimeSeconds"() {
        given:
        def now = new DateTime()
        staticIdmConfiguration.setProperty(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME, maxServiceImpRequestAllowed)

        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)
        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, false)

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds)
        def actualImpersonationTokenExpirationDate = new DateTime(impersonatedScopeAccess.accessTokenExp)

        //auth as user to get the latest user token
        String userToken = impersonatedScopeAccess.getImpersonatingToken()
        ScopeAccess actualUserTokenUsed = scopeAccessService.getScopeAccessByAccessToken(userToken)
        DateTime actualUserTokenUsedExpirationDate = new DateTime(actualUserTokenUsed.accessTokenExp)

        then: "impersonation token will expire on or before user token"
        impersonatedScopeAccess.impersonatingToken == actualUserTokenUsed.accessTokenString
        (actualImpersonationTokenExpirationDate.isBefore(actualUserTokenUsedExpirationDate) || actualImpersonationTokenExpirationDate.isEqual(actualUserTokenUsedExpirationDate))

        cleanup:
        utils.deleteUsers(localDefaultUser)

        where:
        [maxServiceImpRequestAllowed, userTokenLifetimeSeconds, requestedImpersonationTokenLifetimeSeconds] << impersonateRandomDataProvider(20)
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

    def "impersonating user - racker with impersonate role" () {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)

        when:
        def response = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def rackerToken = response.token.id
        utils.impersonateWithToken(rackerToken, localDefaultUser)

        then:
        rackerToken != null

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "impersonating user - racker with no impersonate role" () {
        given:
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

    Integer serviceImpersonatorTokenMaxLifetimeInSeconds() {
        return config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME);
    }

    Integer rackerImpersonatorTokenMaxLifetimeInSeconds() {
        return config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME);
    }

    def "impersonate user using ae tokens"() {
        given:
        def iAdmin = utils.createUser(specificationServiceAdminToken)
        utils.updateUser(iAdmin)

        def iAdminToken = utils.getToken(iAdmin.username)

        def now = new DateTime()

        Integer userTokenLifetimeSeconds =  -1 * ONE_HOUR_IN_SECONDS
        DateTime userTokenExpirationDate = now.plusSeconds(userTokenLifetimeSeconds)

        def requestedImpersonationTokenLifetimeSeconds = serviceImpersonatorTokenMaxLifetimeInSeconds()

        def localDefaultUser = createUserWithTokenExpirationDate(userTokenExpirationDate, disableUser)

        when:  "impersonate user"
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, requestedImpersonationTokenLifetimeSeconds, iAdminToken)

        then: "get AE tokens back"
        impersonatedScopeAccess.accessTokenString != null
        impersonatedScopeAccess.accessTokenString.length() > 32
        impersonatedScopeAccess.impersonatingToken != null
        impersonatedScopeAccess.impersonatingToken.length() > 32

        and: "user token based on impersonating token"
        UserScopeAccess userScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(impersonatedScopeAccess.impersonatingToken);
        userScopeAccess.accessTokenString == impersonatedScopeAccess.impersonatingToken
        userScopeAccess.userRsId == impersonatedScopeAccess.rsImpersonatingRsId
        userScopeAccess.accessTokenExp == impersonatedScopeAccess.accessTokenExp

        where:
        disableUser << [false, true]
    }

    def "impersonate with invalid IDP returns 404"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
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

   def "creating an impersonation token sets the impersonating username to hardcoded value"() {
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

        and: "the token stored in the directory has the hardcoded username attribute"
        tokenEntity.impersonatingUsername == ImpersonatedScopeAccess.IMPERSONATING_USERNAME_HARDCODED_VALUE

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

        and: "the token stored in the directory still have a hardcoded impersonating username attribute"
        tokenEntity.impersonatingUsername == ImpersonatedScopeAccess.IMPERSONATING_USERNAME_HARDCODED_VALUE

        when: "validate to token to verify that is works correctly"
        validateResponse = cloud20.validateToken(utils.getIdentityAdminToken(), tokenId)

        then:
        validateResponse.status == 200

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
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

    def "impersonate user sets user token authBy to impersonated, sets impersonated token authBy to callers token"() {
        given:
        def localDefaultUser = createUserWithTokenExpirationDate(new DateTime().minusDays(1))

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, serviceImpersonatorTokenMaxLifetimeInSeconds())
        ScopeAccess actualUserTokenUsed = scopeAccessService.getScopeAccessByAccessToken(impersonatedScopeAccess.impersonatingToken)

        then: "impersonation token has authBy of caller"
        //caller is the service admin, whose token was created with password authentication
        CollectionUtils.isEqualCollection(impersonatedScopeAccess.authenticatedBy, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD))

        and: "user token is set to impersonation"
        CollectionUtils.isEqualCollection(actualUserTokenUsed.authenticatedBy, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_IMPERSONATION))

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    /**
     * To be backward compatible with 2.9.x, impersonation tokens must set the username/impersonatingUsername properties on
     * impersonation tokens. Any linked user tokens created must also have the username/userRsId set.
     *
     * Must be able to validate these tokens
     */
    def "impersonated token contains userid for both impersonator and impersonated"() {
        given:
        def localDefaultUser = createUserWithTokenExpirationDate(new DateTime().plusHours(23))

        when:
        ImpersonatedScopeAccess impersonatedScopeAccess = impersonateUserForTokenLifetime(localDefaultUser, serviceImpersonatorTokenMaxLifetimeInSeconds())
        UserScopeAccess actualUserTokenUsed = (UserScopeAccess) scopeAccessService.getScopeAccessByAccessToken(impersonatedScopeAccess.impersonatingToken)

        then: "impersonation token has username and userid set for impersonator and impersonated"
        //caller is the identity admin, whose token was created with password authentication
        impersonatedScopeAccess.userRsId != null

        impersonatedScopeAccess.impersonatingUsername == ImpersonatedScopeAccess.IMPERSONATING_USERNAME_HARDCODED_VALUE
        impersonatedScopeAccess.rsImpersonatingRsId != null

        and: "linked user token contains userid"
        actualUserTokenUsed.userRsId != null

        when:
        AuthenticateResponse authResponse = utils.validateToken(impersonatedScopeAccess.accessTokenString)

        then:
        authResponse.user.name == localDefaultUser.username

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    @Unroll
    def "impersonation tokens cannot be used to view API key credentials : preventAccess = #preventAccess"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PREVENT_RACKER_IMPERSONATE_API_KEY_ACCESS_PROP, preventAccess)
        def defaultUser = utils.createUser(userAdminToken)
        def defaultUser2 = utils.createUser(userAdminToken)
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        def rackerImpersonationToken = utils.impersonateWithToken(rackerToken, defaultUser).token.id
        def identityAdminImpersonationToken = utils.impersonateWithToken(utils.getIdentityAdminToken(), defaultUser2).token.id

        when: "list credentials"
        def rackerResponse = cloud20.listCredentials(rackerImpersonationToken, defaultUser.id)
        def identityAdminResponse = cloud20.listCredentials(identityAdminImpersonationToken, defaultUser2.id)

        then:
        rackerResponse.status == (preventAccess ? 403 : 200)
        identityAdminResponse.status == 200

        when: "update API key"
        rackerResponse = cloud20.addApiKeyToUser(rackerImpersonationToken, defaultUser.id, v1Factory.createApiKeyCredentials(defaultUser.username, "thisismykey"))
        identityAdminResponse = cloud20.addApiKeyToUser(identityAdminImpersonationToken, defaultUser2.id, v1Factory.createApiKeyCredentials(defaultUser.username, "thisismykey"))

        then:
        rackerResponse.status == 403
        identityAdminResponse.status == 403

        when: "delete API key"
        rackerResponse = cloud20.deleteUserApiKey(rackerImpersonationToken, defaultUser.id)
        identityAdminResponse = cloud20.deleteUserApiKey(identityAdminImpersonationToken, defaultUser2.id)

        then:
        rackerResponse.status == (preventAccess ? 403 : 204)
        identityAdminResponse.status == 204

        when: "reset API key"
        rackerResponse = cloud20.resetUserApiKey(rackerImpersonationToken, defaultUser.id)
        identityAdminResponse = cloud20.resetUserApiKey(identityAdminImpersonationToken, defaultUser2.id)

        then:
        rackerResponse.status == (preventAccess ? 403 : 200)
        identityAdminResponse.status == 200

        when: "get API key"
        rackerResponse = cloud20.getUserApiKey(rackerImpersonationToken, defaultUser.id)
        identityAdminResponse = cloud20.getUserApiKey(identityAdminImpersonationToken, defaultUser2.id)

        then:
        rackerResponse.status == (preventAccess ? 403 : 200)
        identityAdminResponse.status == 200

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(defaultUser)
        utils.deleteUsers(defaultUser2)

        where:
        preventAccess << [true, false]
    }


    def "impersonation token should be able to list exact same endpoints as original / impersonated user would expect"() {
        given:
        def domainId = utils.createDomain()
        def nastTenantId = utils.getNastTenant(domainId)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", testUtils.getRandomUUID("http://public/"), "cloudServers", true, "ORD").with {
            it.global = true
            it
        }
        endpointTemplate = utils.createAndUpdateEndpointTemplate(endpointTemplate, endpointTemplateId)
        cloud20.addEndpoint(utils.getServiceAdminToken(), nastTenantId, endpointTemplate, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)

        // Generate Impersonation token for Racker and identity admin
        def userToken = utils.getToken(userAdmin.username)
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        def rackerImpersonationToken = utils.impersonateWithToken(rackerToken, userAdmin).token.id
        def identityAdminImpersonationToken = utils.impersonateWithToken(utils.getIdentityAdminToken(), userAdmin).token.id

        when: "token in url and auth token are same (valid tokens)"
        def userResponse = cloud20.listEndpointsForToken(userToken, userToken)
        def rackerResponse = cloud20.listEndpointsForToken(rackerImpersonationToken, rackerImpersonationToken)
        def identityAdminResponse = cloud20.listEndpointsForToken(identityAdminImpersonationToken, identityAdminImpersonationToken)

        then: "200 status code should be returned"
        userResponse.status == HttpStatus.SC_OK
        rackerResponse.status == HttpStatus.SC_OK
        identityAdminResponse.status == HttpStatus.SC_OK

        // Retrieving response body as entity
        def originalUserEndpoints  = userResponse.getEntity(EndpointList).value.endpoint
        def rackerImpersonationEndpoints = rackerResponse.getEntity(EndpointList).value.endpoint
        def idmAdminImpersonationEndpoints = identityAdminResponse.getEntity(EndpointList).value.endpoint

        and: "response body for impersonator token should have same number of endpoints as original user"
        rackerImpersonationEndpoints.size == originalUserEndpoints.size
        idmAdminImpersonationEndpoints.size == originalUserEndpoints.size

        and: "Response body for impersonator token should have exact same endpoints as original user"
        def originalUserEndpointList =[]
        for( endpoint in originalUserEndpoints){
            originalUserEndpointList.add(endpoint.id)
        }

        def rackerImpersonationEndpointsList =[]
        for( endpoint in rackerImpersonationEndpoints){
            rackerImpersonationEndpointsList.add(endpoint.id)
        }

        def adminImpersonationEndpointsList =[]
        for( endpoint in rackerImpersonationEndpoints){
            adminImpersonationEndpointsList.add(endpoint.id)
        }

        def intersectionWithRackerImpersonator = originalUserEndpointList.intersect(rackerImpersonationEndpointsList)
        def intersectionWithIdentityAdminImpersonator = originalUserEndpointList.intersect(adminImpersonationEndpointsList)

        // Everything in original user Endpoints should intersect with impersonated user
        intersectionWithRackerImpersonator.size() == originalUserEndpointList.size()
        intersectionWithIdentityAdminImpersonator.size() == originalUserEndpointList.size()

        and: "There should be zero difference / delta between response in impersonation and original user endpoints"
        def deltaWithRackerImpersonator = originalUserEndpointList.plus(rackerImpersonationEndpointsList)
        deltaWithRackerImpersonator.removeAll(intersectionWithRackerImpersonator)
        deltaWithRackerImpersonator.size() == 0 // zero difference is expected if lists are same

        def deltaWithAdminImpersonator = originalUserEndpointList.plus(adminImpersonationEndpointsList)
        deltaWithAdminImpersonator.removeAll(intersectionWithIdentityAdminImpersonator)
        deltaWithAdminImpersonator.size() == 0 // zero difference is expected if lists are same

        when: "token in url and auth token are not same (but valid)"
        userResponse = cloud20.listEndpointsForToken(identityAdminImpersonationToken, userToken)
        rackerResponse = cloud20.listEndpointsForToken(rackerImpersonationToken, identityAdminImpersonationToken)
        identityAdminResponse = cloud20.listEndpointsForToken(identityAdminImpersonationToken, rackerImpersonationToken)

        then: "403 status code should be returned"
        userResponse.status == HttpStatus.SC_FORBIDDEN
        rackerResponse.status == HttpStatus.SC_FORBIDDEN
        identityAdminResponse.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(nastTenantId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)
    }

    def "Forbid impersonation of users with 'identity:internal' role" () {
        given:
        def localDefaultUser = utils.createUser(userAdminToken)
        utils.addRoleToUser(localDefaultUser, Constants.IDENTITY_INTERNAL_ROLE_ID)

        when:
        def response = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def rackerToken = response.token.id
        response = cloud20.impersonate(rackerToken, localDefaultUser)

        then:
        response.status == 400

        cleanup:
        utils.deleteUsers(localDefaultUser)
    }

    def "auth with impersonation token returns token with IMPERSONATE in auth by list"() {
        given:
        def cloudUser
        def mfaUser
        def mfaSecret
        (mfaUser, mfaSecret) = utils.createUserWithOtpMfa(utils.getServiceAdminToken())
        cloudUser = utils.createCloudAccount()

        when: "auth with imp token from service admin impersonating the user using a password token"
        def impToken = utils.impersonate(utils.getServiceAdminToken(), cloudUser).token.id
        def response = utils.authenticateTokenWithTenant(impToken, cloudUser.domainId)

        then:
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.IMPERSONATE.value)
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.value)

        when: "auth with imp token from racker impersonating the user using a password token"
        impToken = utils.impersonateWithRacker(cloudUser).token.id
        response = utils.authenticateTokenWithTenant(impToken, cloudUser.domainId)

        then:
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.IMPERSONATE.value)
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.value)

        when: "auth with imp token from identity admin w/ mfa impersonating the user using an mfa token"
        def mfaToken = utils.getMFAToken(mfaUser.username, otpHelper.TOTP(mfaSecret))
        impToken = utils.impersonate(mfaToken, cloudUser).token.id
        response = utils.authenticateTokenWithTenant(impToken, cloudUser.domainId)

        then:
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.IMPERSONATE.value)
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.value)
        response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.OTPPASSCODE.value)
    }

    /**
     * Impersonate the specified user. THe impersonation token should be requested with the given expiration time in seconds
     * @param user
     * @param impersonatedTokenLifetime
     * @return
     */
    def ImpersonatedScopeAccess impersonateUserForTokenLifetime(User user, Integer impersonationTokenExpireInSeconds, impersonatorToken = specificationIdentityAdminToken) {
        ImpersonationResponse impersonationResponse = utils.impersonate(impersonatorToken, user, impersonationTokenExpireInSeconds)

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
    def impersonateUserAsIdentityAdminForTokenLifetimeRawResponse(User user, Integer impersonationTokenExpireInSeconds, impersonatorToken = specificationIdentityAdminToken) {
        def response = cloud20.impersonate(specificationIdentityAdminToken, user, impersonationTokenExpireInSeconds)
        return response
    }

    def impersonateUserAsRackerForTokenLifetimeRawResponse(User user, Integer impersonationTokenExpireInSeconds) {
        def auth = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def response = cloud20.impersonate(auth.token.id, user, impersonationTokenExpireInSeconds)
        return response
    }

    def User createUserWithTokenExpirationDate(DateTime tokenExpirationDate, boolean disableUserAtEnd = false, List<String> tokenAuthBy = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD)) {
        User defaultUser = utils.createUser(userAdminToken)

        //make sure all tokens are expired (they should be, but verify just the same)
        tokenRevocationService.revokeAllTokensForEndUser(defaultUser.id)

        //authenticate normally, to make sure the token is created per usual process
        def defaultUserToken = utils.getToken(defaultUser.username)

        //disable the user. This will expire all tokens on the default user (including the just create one from the authentication)
        if (disableUserAtEnd) {
            disableUser(defaultUser)
        }

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

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, DEFAULT_IDP_ID)
            if (federatedUser != null) {
                federatedUserRepository.deleteObject(federatedUser)
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

    def getScopeAccessFromImpersonationResponse(response, contentType) {
        def impersonationToken
        if(MediaType.APPLICATION_XML_TYPE == contentType) {
            impersonationToken = response.getEntity(ImpersonationResponse).token.id
        } else {
            def responseString = response.getEntity(String)
            impersonationToken = new JsonSlurper().parseText(responseString).access.token.id
        }
        return scopeAccessService.getScopeAccessByAccessToken(impersonationToken)
    }

}
