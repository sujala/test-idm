package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.security.TokenFormat
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class SimpleAeRevokeTokenServiceTest extends RootServiceTest {

    @Shared SimpleAETokenRevocationService service = new SimpleAETokenRevocationService()
    @Shared TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;

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
        mockAeTokenService(service)

        tokenRevocationRecordPersistenceStrategy = Mock()
        service.tokenRevocationRecordPersistenceStrategy = tokenRevocationRecordPersistenceStrategy

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

    def "revokeTokensForBaseUser - authenticatedByMethodGroups using tokenRevocationRecordPersistenceStrategy" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        List<AuthenticatedByMethodGroup> authenticatedByMethodGroups =  Arrays.asList(AuthenticatedByMethodGroup.ALL)

        when:
        service.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeTokensForEndUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeTokensForEndUser(user.id, authenticatedByMethodGroups)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeTokensForEndUser(user, Collections.EMPTY_LIST)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
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

    def "revokeAllTokensForEndUser using tokenRevocationRecordPersistenceStrategy" () {
        given:
        User user = new User()
        user.id = "1"
        identityUserService.getEndUserById(_) >> user

        when:
        service.revokeAllTokensForEndUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeAllTokensForEndUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeAllTokensForEndUser(user.id)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)

        when:
        service.revokeAllTokensForEndUser(user)

        then:
        1 * tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.id, _)
    }

}
