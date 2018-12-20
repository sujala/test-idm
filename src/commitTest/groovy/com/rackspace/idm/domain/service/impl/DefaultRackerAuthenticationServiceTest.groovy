package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.impl.RackerAuthResult
import spock.lang.Unroll
import testHelpers.RootServiceTest

class DefaultRackerAuthenticationServiceTest extends RootServiceTest {
    DefaultRackerAuthenticationService service

    def setup() {
        service = new DefaultRackerAuthenticationService()
        mockIdentityConfig(service)
        mockUserService(service)
        mockRSAClient(service)
        mockRackerAuthDao(service)

        staticConfig.isRackerAuthAllowed() >> true
    }

    @Unroll
    def "When cache is disabled, call non cache method"() {
        reloadableConfig.cacheRackerAuthResult() >> false
        def username = "u"
        def pwd = "p"

        when: "Auth"
        service.authenticateRackerUsernamePassword(username, pwd)

        then:
        1 * rackerAuthDao.authenticate(username, pwd) >> true
        0 * rackerAuthDao.authenticateWithCache(_, _)
    }

    @Unroll
    def "When cache is enabled, call cache method"() {
        reloadableConfig.cacheRackerAuthResult() >> true
        def username = "u"
        def pwd = "p"

        when: "Auth"
        service.authenticateRackerUsernamePassword(username, pwd)

        then:
        1 * rackerAuthDao.authenticateWithCache(username, pwd) >> RackerAuthResult.SUCCESS
        0 * rackerAuthDao.authenticate(_, _)
    }
}
