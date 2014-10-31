package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.UserDao
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
import testHelpers.EntityFactory

/**
 * Integration tests from the revocation down using Memory storage. Token retrieval is mocked.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"
, "classpath:com/rackspace/idm/domain/service/impl/SimpleAETokenRevocationServiceMemIntegrationTest-context.xml"])
class SimpleAETokenRevocationServiceMemIntegrationTest extends Specification {

    @Autowired
    SimpleAETokenRevocationService revocationService;

    @Shared AETokenService aeTokenService

    @Shared UserDao userDao

    @Shared UserService userService

    EntityFactory entityFactory = new EntityFactory()

    @Shared TokenFormatSelector tokenFormatSelector

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

    def "revokeAllTokensForUser(BaseUser) - user based revocation"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = sa.userRsId
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
}
