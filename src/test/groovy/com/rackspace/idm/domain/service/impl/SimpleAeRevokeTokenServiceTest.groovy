package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.security.TokenFormat
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
        mockTokenFormatSelector(service)
        mockIdentityUserService(service)

        tokenRevocationRecordPersistenceStrategy = Mock()
        service.tokenRevocationRecordPersistenceStrategy = tokenRevocationRecordPersistenceStrategy

        uuidTokenRevocationService = Mock()
        service.uuidTokenRevocationService = uuidTokenRevocationService

        aeTokenService = Mock()
        service.aeTokenService = aeTokenService

        tokenFormatSelector.formatForExistingToken(_) >> TokenFormat.AE
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
        identityUserService.getEndUserById(_) >> user

        List<AuthenticatedByMethodGroup> authenticatedByMethodGroups =  Arrays.asList(AuthenticatedByMethodGroup.ALL)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeTokensForEndUser(user, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeTokensForEndUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeTokensForEndUser(user, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeTokensForEndUser(user, _)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeTokensForEndUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeTokensForEndUser(user, _)
    }

    def "revokeTokensForEndUser - atom hopper trr event sent appropriately based on user type" () {
        given:
        List<AuthenticatedByMethodGroup> authenticatedByMethodGroups =  Arrays.asList(AuthenticatedByMethodGroup.ALL)

        when:
        User user = new User()
        user.id = "1"
        service.revokeTokensForEndUser(user, authenticatedByMethodGroups)

        then:
        1 * atomHopperClient.asyncPostUserTrr(_,_)

        when:
        identityUserService.getEndUserById(user.id) >> user
        service.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        1 * atomHopperClient.asyncPostUserTrr(_,_)

        when:
        FederatedUser federatedUser = new FederatedUser()
        federatedUser.id = "1"
        service.revokeTokensForEndUser(federatedUser, authenticatedByMethodGroups)

        then:
        0 * atomHopperClient.asyncPostUserTrr(_,_)

        when:
        identityUserService.getEndUserById(federatedUser.id) >> federatedUser
        service.revokeTokensForEndUser(federatedUser.id, authenticatedByMethodGroups)

        then:
        0 * atomHopperClient.asyncPostUserTrr(_,_)
    }

    def "revokeAllTokensForEndUser - analagous UUID revoke called as appropriate" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeAllTokensForEndUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeAllTokensForEndUser(user)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> true
        service.revokeAllTokensForEndUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        1 * uuidTokenRevocationService.revokeAllTokensForEndUser(user)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeAllTokensForEndUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeAllTokensForEndUser(user)

        when:
        identityConfig.getFeatureAeTokenCleanupUuidOnRevokes() >> false
        service.revokeAllTokensForEndUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
        0 * uuidTokenRevocationService.revokeAllTokensForEndUser(user)
    }

}
