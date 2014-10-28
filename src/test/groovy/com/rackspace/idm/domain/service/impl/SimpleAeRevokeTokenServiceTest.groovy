package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.UUIDTokenRevocationService
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class SimpleAeRevokeTokenServiceTest extends RootServiceTest {

    @Shared SimpleAETokenRevocationService service = new SimpleAETokenRevocationService()
    @Shared TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;
    @Shared AETokenService aeTokenService;
    @Shared UUIDTokenRevocationService uuidTokenRevocationService

    @Shared def expiredDate
    @Shared def futureDate

    def setupSpec() {
        expiredDate = new DateTime().minusHours(1).toDate()
        futureDate = new DateTime().plusHours(defaultRefreshHours + 1).toDate()
    }

    def setup() {
        mockAtomHopperClient(service)
        mockUserService(service)
        mockIdentityConfig(service)

        tokenRevocationRecordPersistenceStrategy = Mock()
        service.tokenRevocationRecordPersistenceStrategy = tokenRevocationRecordPersistenceStrategy

        uuidTokenRevocationService = Mock()
        service.uuidTokenRevocationService = uuidTokenRevocationService

        aeTokenService = Mock()
        service.aeTokenService = aeTokenService
    }

    def "revokeToken(tokenString) - atomHopper client is called when expiring a token" () {
        given:
        def token = "someToken"
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_, _) >> user

        def scopeAccessOne = createUserScopeAccess(token, user.id, "clientId", futureDate)

        aeTokenService.unmarshallToken(token) >> scopeAccessOne

        when:
        service.revokeToken(token)

        then:
        1 * atomHopperClient.asyncTokenPost(user,token)
    }

    def "revokeToken(tokenString) - atomHopper client is not called when expiring a token that is already expired" () {
        given:
        def token = "someToken"
        User user = new User()
        user.id = "1"
        userService.getUserByScopeAccess(_) >> user

        def scopeAccessOne = createUserScopeAccess(token, user.id, "clientId", expiredDate)

        aeTokenService.unmarshallToken(token) >> scopeAccessOne

        when:
        service.revokeToken(token)

        then:
        0 * atomHopperClient.asyncTokenPost(user,token)
    }

    def "revokeTokensForBaseUser - analagous UUID revoke called as appropriate" () {
        given:
        User user = new User()
        user.id = "1"

        List<AuthenticatedByMethodGroup> authenticatedByMethodGroups =  Arrays.asList(AuthenticatedByMethodGroup.ALL)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeTokensForBaseUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeTokensForBaseUser(user.id, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeTokensForBaseUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeTokensForBaseUser(user, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeTokensForBaseUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeTokensForBaseUser(user.id, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeTokensForBaseUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeTokensForBaseUser(user, _)
    }

    def "revokeAllTokensForBaseUser - analagous UUID revoke called as appropriate" () {
        given:
        User user = new User()
        user.id = "1"

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeAllTokensForBaseUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeAllTokensForBaseUser(user.id)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeAllTokensForBaseUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeAllTokensForBaseUser(user)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeAllTokensForBaseUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeAllTokensForBaseUser(user.id)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeAllTokensForBaseUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeAllTokensForBaseUser(user)
    }

}
