package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.AETokenService
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class SimpleAeRevokeTokenServiceTest extends RootServiceTest {

    @Shared SimpleAETokenRevocationService service = new SimpleAETokenRevocationService()
    @Shared TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;
    @Shared AETokenService aeTokenService;

    @Shared def expiredDate
    @Shared def futureDate

    def setupSpec() {
        expiredDate = new DateTime().minusHours(1).toDate()
        futureDate = new DateTime().plusHours(defaultRefreshHours + 1).toDate()
    }

    def setup() {
        mockAtomHopperClient(service)
        mockUserService(service)

        tokenRevocationRecordPersistenceStrategy = Mock()
        service.tokenRevocationRecordPersistenceStrategy = tokenRevocationRecordPersistenceStrategy

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

}
