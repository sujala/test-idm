package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.domain.service.UserService
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.EntityFactory

/**
 * Integration tests from the revocation down using CA storage. Token retrieval is mocked.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class SimpleAETokenRevocationServiceIntegrationTest extends Specification {

    @Autowired
    SimpleAETokenRevocationService revocationService;

    @Shared AETokenService aeTokenService

    @Shared UserDao userDao

    @Shared UserService userService

    @Shared TokenFormatSelector tokenFormatSelector

    EntityFactory entityFactory = new EntityFactory()

    def setup() {
        aeTokenService = Mock()
        userDao = Mock()
        userService = Mock()
        tokenFormatSelector = Mock()

        revocationService.aeTokenService = aeTokenService
        revocationService.userService = userService
        revocationService.tokenFormatSelector = tokenFormatSelector

        tokenFormatSelector.formatForExistingToken(_) >> TokenFormat.AE
        tokenFormatSelector.formatForNewToken(_) >> TokenFormat.AE
    }

    def "revokeToken - token based revocation"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = sa.userRsId
            return it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa
        userService.getUserByScopeAccess(_,_) >> user

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeToken(token)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeAllTokensForUser(userId) - user based revocation"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            return it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeAllTokensForBaseUser(sa.userRsId)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeAllTokensForUser(userId) - revoke all user based revocation does not revoke impersonation tokens"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            it.authenticatedBy = Arrays.asList(AuthenticatedByMethodEnum.IMPERSONATION.value)
            return it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeAllTokensForBaseUser(sa.userRsId)

        then:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)
    }

    def "revokeTokensForBaseUser(userId, Impersonation) - can revoke impersonation user tokens via user TRR"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            it.authenticatedBy = Arrays.asList(AuthenticatedByMethodEnum.IMPERSONATION.value)
            return it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeTokensForBaseUser(sa.userRsId, TokenRevocationService.AUTH_BY_LIST_IMPERSONATION_TOKENS)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeAllTokensForUser(BaseUser) - user based revocation"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = sa.userRsId
            return it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeAllTokensForBaseUser(user)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeTokensForBaseUser(userId) - when revoke password tokens, password tokens are revoked"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            it.authenticatedBy = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            return it
        }
        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when: "expire all api tokens"
        revocationService.revokeTokensForBaseUser(sa.userRsId, TokenRevocationService.AUTH_BY_LIST_API_TOKENS)

        then: "password token is not revoked"
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when: "expire all password tokens"
        revocationService.revokeTokensForBaseUser(sa.userRsId, TokenRevocationService.AUTH_BY_LIST_PASSWORD_TOKENS)

        then: "password token is expired"
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    @Unroll
    def "revokeTokensForBaseUser(userId) - when revoke (#expireTypes) only those tokens revoked"() {
        def userId = UUID.randomUUID().toString();

        def List<ScopeAccess> tokens = createTokenAssortment(userId)

        expect: "None are expired"
        tokens.each {assert !revocationService.isTokenRevoked(it)}

        when: "expire specified types"
        revocationService.revokeTokensForBaseUser(userId, expireTypes)

        then: "only tokens with selected types are revoked"
        tokens.each {token ->
            boolean shouldBeExpired = false
            expireTypes.each {
                authBy ->
                    if ((authBy.matches(AuthenticatedByMethodGroup.ALL) && !token.getAuthenticatedBy().contains(AuthenticatedByMethodEnum.IMPERSONATION.value))
                            || authBy.matches(AuthenticatedByMethodGroup.getGroup(token.getAuthenticatedBy()))) {
                        shouldBeExpired = true
                    }
            }
            assert revocationService.isTokenRevoked(token) == shouldBeExpired
        }

        where:
        expireTypes | _
        TokenRevocationService.AUTH_BY_LIST_PASSWORD_TOKENS | _
        Arrays.asList(AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.APIKEY) | _
        Arrays.asList(AuthenticatedByMethodGroup.PASSWORD_PASSCODE, AuthenticatedByMethodGroup.APIKEY) | _
        TokenRevocationService.AUTH_BY_LIST_ALL_TOKENS | _
        TokenRevocationService.AUTH_BY_LIST_NULL_TOKENS | _
    }

    def createTokenAssortment(String userId) {
        def saPwd = createToken(userId, GlobalConstants.AUTHENTICATED_BY_PASSWORD)
        def saApi =  createToken(userId, GlobalConstants.AUTHENTICATED_BY_APIKEY)
        def saMfa =  createToken(userId, GlobalConstants.AUTHENTICATED_BY_PASSWORD, GlobalConstants.AUTHENTICATED_BY_PASSCODE)
        def saImpersonation =  createToken(userId, GlobalConstants.AUTHENTICATED_BY_IMPERSONATION)
        def saFederation =  createToken(userId, GlobalConstants.AUTHENTICATED_BY_FEDERATION)
        def saRsa =  createToken(userId, GlobalConstants.AUTHENTICATED_BY_FEDERATION)
        def saBlank =  createToken(userId)

        return [saPwd, saApi, saMfa, saImpersonation, saFederation, saRsa, saBlank].asList()
    }

    def UserScopeAccess createToken(String userId, String... authBy) {
        def sa = entityFactory.createUserToken().with {
            it.userRsId = userId
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            it.authenticatedBy = Arrays.asList(authBy)
            return it
        }
        aeTokenService.unmarshallToken(sa.accessTokenString) >> sa
        return sa
    }

}
