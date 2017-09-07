package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperHelper
import com.rackspace.idm.domain.config.IdentityConfig

import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.MemoryTokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.test.SingleTestConfiguration
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import testHelpers.EntityFactory
import testHelpers.SingletonTestFileConfiguration

/**
 * Integration tests from the revocation down using Memory storage. Token retrieval is mocked.
 */
@ContextConfiguration(classes=[MockServiceProvider.class
        , SimpleAETokenRevocationService.class
        , SingletonTestFileConfiguration.class
        , IdentityConfig.class
])
class SimpleAETokenRevocationServiceMemIntegrationTest extends Specification {

    @Autowired
    SimpleAETokenRevocationService revocationService;

    @Shared AETokenService aeTokenService

    @Shared UserDao userDao

    @Shared UserService userService

    @Shared IdentityUserService identityUserService

    EntityFactory entityFactory = new EntityFactory()

    @Shared TokenFormatSelector tokenFormatSelector

    def setup() {
        aeTokenService = Mock()
        userDao = Mock()
        userService = Mock()
        tokenFormatSelector = Mock()
        identityUserService = Mock()

        revocationService.aeTokenService = aeTokenService
        revocationService.userService = userService
        revocationService.tokenFormatSelector = tokenFormatSelector
        revocationService.identityUserService = identityUserService


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
        identityUserService.getEndUserById(_) >> entityFactory.createUser().with {it.id = sa.getIssuedToUserId(); return it;}

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeAllTokensForEndUser(sa.userRsId)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeAllTokensForUser(EndUser) - user based revocation"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = sa.userRsId
            it
        }

        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa
        identityUserService.getEndUserById(_) >> entityFactory.createUser().with {it.id = sa.getIssuedToUserId(); return it;}

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when:
        revocationService.revokeAllTokensForEndUser(user)

        then:
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    def "revokeTokensForEndUser(userId) - when revoke password tokens, password tokens are revoked"() {
        def sa = entityFactory.createUserToken().with {
            it.createTimestamp = new DateTime().minusHours(1).toDate()
            it.authenticatedBy = Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            return it
        }
        String token = sa.accessTokenString

        aeTokenService.unmarshallToken(token) >> sa
        identityUserService.getEndUserById(_) >> entityFactory.createUser().with {it.id = sa.getIssuedToUserId(); return it;}

        expect:
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when: "expire all api tokens"
        revocationService.revokeTokensForEndUser(sa.userRsId, TokenRevocationService.AUTH_BY_LIST_API_TOKENS)

        then: "password token is not revoked"
        !revocationService.isTokenRevoked(token)
        !revocationService.isTokenRevoked(sa)

        when: "expire all password tokens"
        revocationService.revokeTokensForEndUser(sa.userRsId, TokenRevocationService.AUTH_BY_LIST_PASSWORD_TOKENS)

        then: "password token is expired"
        revocationService.isTokenRevoked(token)
        revocationService.isTokenRevoked(sa)
    }

    /**
     * Need to mock the autowired dependencies of any concrete class that is Mocked
     */
    @SingleTestConfiguration
    static class MockServiceProvider {
        def mockFactory = new DetachedMockFactory()

        @Bean
        public IdentityPropertyValueConverter identityPropertyValueConverter () {
            return  mockFactory.Mock(IdentityPropertyValueConverter.class);
        }
        @Bean
        public IdentityPropertyService identityPropertyService () {
            return  mockFactory.Mock(IdentityPropertyService.class);
        }
        @Bean
        public AETokenService aeTokenService() {
            return  mockFactory.Mock(AETokenService.class);
        }
        @Bean
        public ScopeAccessService scopeAccessService() {
            return  mockFactory.Mock(ScopeAccessService.class);
        }
        @Bean
        public AtomHopperClient atomHopperClient() {
            return  mockFactory.Mock(AtomHopperClient.class);
        }
        @Bean
        public UserService userService() {
            return  mockFactory.Mock(UserService.class);
        }
        @Bean
        public IdentityUserService identityUserService() {
            return  mockFactory.Mock(IdentityUserService.class);
        }
        @Bean
        public AtomHopperHelper atomHopperHelper() {
            return  mockFactory.Mock(AtomHopperHelper.class);
        }
        @Bean
        public TenantService tenantService() {
            return  mockFactory.Mock(TenantService.class);
        }
        @Bean
        public TokenFormatSelector tokenFormatSelector() {
            return  mockFactory.Mock(TokenFormatSelector.class)
        }

        @Bean
        public UserDao userDao() {
            return mockFactory.Mock(UserDao.class);
        }

        @Bean
        MemoryTokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy() {
            return new MemoryTokenRevocationRecordPersistenceStrategy()
        }
    }
}
