package com.rackspace.idm.domain.security.globalauth

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.UnmarshallTokenException
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.FakeTicker

import java.util.concurrent.TimeUnit

/**
 * This test is meant to test issuing a regular token to a user. "Regular" is whatever type of token is appropriate for
 * the user being authenticationed (e.g. - UserScopeAccess for a provisioned or federated user, RackerScopeAccess for a racker).
 *
 * This test does not include verifying the AE format for ImpersonatedScopeAccess tokens. For those,
 * see {@link DefaultAETokenServiceImpersonationIntegrationTest)
 */
class DefaultAETokenServiceUserTokenIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedProvisionedUser
    @Shared Racker hardCodedRackerUser

    @Shared def sampleToken

    def setupSpec() {
        hardCodedProvisionedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "random@random.com"
            return it
        }
        hardCodedRackerUser = entityFactory.createRacker().with {
            it.rackerId = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonatorRackerUser"
            return it
        }

        identityUserService.getBaseUserById(hardCodedProvisionedUser.id) >> hardCodedProvisionedUser
        identityUserService.getBaseUserById(hardCodedRackerUser.id) >> hardCodedRackerUser

        sampleToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, createUserToken(hardCodedProvisionedUser))
    }

    @Unroll
    def "marshall/unmarshall fully populated #hardCodedUser.getClass().getName() token ; #cache"() {
        setTokenCaching(cache)

        ScopeAccess originalUSA = createUserToken(hardCodedUser)

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | cache | methodDesc
        hardCodedProvisionedUser | true | "Provisioned User"
        hardCodedProvisionedUser | false | "Provisioned User"
        hardCodedRackerUser | true | "Racker"
        hardCodedRackerUser | false | "Racker"
    }

    @Unroll
    def "unmarshallTokenAndValidate: Returns null when token can't be decrypted"() {
        when:
        def result = aeTokenService.unmarshallTokenAndValidate("asdf")

        then:
        result == null
    }

    @Unroll
    def "unmarshallTokenAndValidate: Returns null when token is expired"() {
        // Create expired scope access
        ScopeAccess expiredToken = createUserToken(hardCodedProvisionedUser).with {
            it.accessTokenExp = new DateTime().minusHours(1).toDate()
            it
        }
        disableTokenCaching() // Since testing unmarshall, don't need to test with cache
        String token = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, expiredToken)

        when:
        def result = aeTokenService.unmarshallTokenAndValidate(token)

        then: "Doesn't check if revoked"
        0 * aeTokenRevocationService.isTokenRevoked(_)

        and: "Returns null"
        result == null
    }

    @Unroll
    def "unmarshallTokenAndValidate: Returns null when token is revoked"() {
        // Create expired scope access
        ScopeAccess validToken = createUserToken(hardCodedProvisionedUser)
        disableTokenCaching() // Since testing unmarshall, don't need to test with cache
        String token = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, validToken)

        when:
        def result = aeTokenService.unmarshallTokenAndValidate(token)

        then: "Checks if revoked"
        1 * aeTokenRevocationService.isTokenRevoked(_) >> true

        and: "Returns null"
        result == null
    }

    /**
     * Assumes cache lifetime is 60 seconds per base class
     */
    def "marshall token w/ cache and revocation"() {
        given:
        String cacheConfig = '{"tokenCacheConfig":{"enabled":true, "maxSize":1000, "cacheableUsers":[{"type":"CID","userIds":["*"],"minimumValidityDuration":"PT10S","maximumCacheDuration":"PT60S","authenticatedByLists":[["PASSWORD","APIKEY"],["PASSWORD"], ["APIKEY"]]}]}}'
        setTokenCachingConfig(cacheConfig)

        ScopeAccess pwdUSA = createUserToken(hardCodedProvisionedUser, Arrays.asList(AuthenticatedByMethodEnum.PASSWORD.value))
        ScopeAccess apiUSA = createUserToken(hardCodedProvisionedUser, Arrays.asList(AuthenticatedByMethodEnum.APIKEY.value))
        ScopeAccess apiPwdUSA = createUserToken(hardCodedProvisionedUser, Arrays.asList(AuthenticatedByMethodEnum.APIKEY.value, AuthenticatedByMethodEnum.PASSWORD.value))
        ScopeAccess pwdApiUSA = createUserToken(hardCodedProvisionedUser, Arrays.asList(AuthenticatedByMethodEnum.PASSWORD.value, AuthenticatedByMethodEnum.APIKEY.value))

        when:
        String firstToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdUSA)

        then:
        firstToken != null
        aeTokenService.unmarshallToken(firstToken) != null

        when: "request a token within cache period"
        String secondToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdUSA)

        then: "get the first token back"
        secondToken == firstToken

        when: "request a token within cache period, but the cached token itself is revoked"
        aeTokenRevocationService.isTokenRevoked({it.accessTokenString == secondToken}) >> true
        String thirdToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdUSA)

        then: "get a new token"
        thirdToken != null
        thirdToken != secondToken
        aeTokenService.unmarshallToken(thirdToken) != null

        when: "request a token within cache period again"
        String fourthToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdUSA)

        then: "get the third token back"
        thirdToken == fourthToken

        when: "cached token entry expires"
        ((FakeTicker)aeTokenService.aeTokenCache.ticker).advance(60, TimeUnit.SECONDS)
        String fifthToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdUSA)

        then: "get a new token"
        fifthToken != null
        fifthToken != secondToken
        aeTokenService.unmarshallToken(fifthToken) != null

        when: "request a token for different auth by"
        String apiToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, apiUSA)

        then:
        apiToken != fifthToken
        aeTokenService.unmarshallToken(apiToken) != null

        when: "request a token for different auth by methods"
        String apiPwdToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, apiPwdUSA)
        String pwdApiToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, pwdApiUSA)

        then: "tokens created based on unique set of authBy - order is irrelevent"
        apiToken != apiPwdToken
        fifthToken != pwdApiToken
        pwdApiToken == apiPwdToken
        aeTokenService.unmarshallToken(pwdApiToken) != null
        aeTokenService.unmarshallToken(apiPwdToken) != null
    }

    @Unroll
    def "marshallTokenForUser() - maximize provisioned user token length; cache: #cache; enableDomainTokens: #enableDomainTokens"() {
        repositoryConfig.shouldWriteDomainTokens() >> enableDomainTokens
        repositoryConfig.shouldReadDomainTokens() >> enableDomainTokens
        setTokenCaching(cache)

        UserScopeAccess originalUSA =  new UserScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.userRsId = hardCodedProvisionedUser.id
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = GlobalConstants.SETUP_MFA_SCOPE
            it.authenticationDomainId = UUID.randomUUID().toString().replaceAll("-", "")
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof UserScopeAccess
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)

        where:
        [cache, enableDomainTokens] << [[true, false], [true, false]].combinations()
    }

    @Unroll
    def "can marshall/unmarshall scope access of type #hardCodedUser.getClass().getName() with null token string. cache: #cache"() {
        setTokenCaching(cache)

        ScopeAccess originalUSA =  createUserToken(hardCodedUser).with {
            it.accessTokenString = null
            return it
        }

        when: "generate token"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | cache
        hardCodedProvisionedUser | true
        hardCodedProvisionedUser | false
        hardCodedRackerUser | true
        hardCodedRackerUser | false
    }

    @Unroll
    def "marshall/unmarshall multiple authby #hardCodedUser.getClass().getName() token. cache: #cache"() {
        setTokenCaching(cache)

        ScopeAccess originalUSA = createUserToken(hardCodedUser, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD, GlobalConstants.AUTHENTICATED_BY_PASSCODE))

        when: "generate token with multiple auth by"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then: "get scope access with multiple auth by"
        unmarshalledScopeAccess.authenticatedBy.size() == 2
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | cache
        hardCodedProvisionedUser | true
        hardCodedProvisionedUser | false
        hardCodedRackerUser | true
        hardCodedRackerUser | false
    }

    @Unroll
    def "marshallTokenForUser throws errors appropriately for provisioned user token. cache: #cache"() {
        setTokenCaching(cache)

        when: "null userId in token"
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedProvisionedUser).with {
            it.userRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user"
        originalUSA = createProvisionedUserToken(hardCodedProvisionedUser).with {
            it.userRsId += "9"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        where:
        cache << [true, false]
    }

    @Unroll
    def "marshallTokenForUser throws errors appropriately for racker user token.  cache: #cache"() {
        setTokenCaching(cache)

        when: "null userId in token"
        ScopeAccess originalUSA = createUserToken(hardCodedRackerUser).with {
            it.rackerId = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedRackerUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token rackerId does not match provided user"
        originalUSA = createUserToken(hardCodedRackerUser).with {
            it.rackerId += "9"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedRackerUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        where:
        cache << [true, false]
    }

    def "Can unmarshall sampleToken: '#sampleToken'"() {
        when:
        aeTokenService.unmarshallToken(sampleToken)

        then:
        notThrown(UnmarshallTokenException)
    }

    @Unroll
    def "Unmarshall throws UnmarshallTokenException when token '#webSafeToken' is truncated to length '#lengthToTest'. cache: #cache"() {
        setTokenCaching(cache)

        when:

        aeTokenService.unmarshallToken(webSafeToken.substring(0,lengthToTest))

        then:
        thrown(UnmarshallTokenException)

        where:
        [webSafeToken, lengthToTest, cache] << [[sampleToken], 1..sampleToken.length()-1, [true, false]].combinations()
    }

    ScopeAccess createUserToken(BaseUser user, List<String> authBy = Arrays.asList(AuthenticatedByMethodEnum.PASSWORD.value)) {
        if (user instanceof EndUser) {
            return createProvisionedUserToken((User)user).with {
                it.authenticatedBy = authBy
                return it
            }
        } else if (user instanceof Racker) {
            return createRackerToken((Racker) user).with {
                it.authenticatedBy = authBy
                return it
            }
        }
    }

}
