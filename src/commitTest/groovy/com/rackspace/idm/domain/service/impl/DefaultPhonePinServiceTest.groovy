package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultPhonePinServiceTest extends RootServiceTest {

    @Shared
    DefaultPhonePinService service

    def setupSpec() {
        InitializationService.initialize()
        service = new DefaultPhonePinService()
        service.identityConfig = identityConfig
    }

    def setup() {
        mockServices()
    }

    def pin = "343223"

    def "Retrieve phone pin from provisioned user"() {
        given:
        User user = new User()
        user.id = "userId"
        user.phonePin = pin

        when:
        def pp = service.checkAndGetPhonePin(user)

        then:
        pp.getPin() == pin
    }

    def "Retrieve phone pin from federated user"() {
        given:
        FederatedUser user = new FederatedUser()
        user.phonePin = pin
        user.id = "userId"

        when:
        def pp = service.checkAndGetPhonePin(user)

        then:
        pp.getPin() == pin

    }

    def "Retrieve phone pin throws not found"() {
        given:
        User user = new User()
        user.phonePin = null

        when:
        service.checkAndGetPhonePin(user)

        then:
        thrown(NotFoundException)
    }

    def "Verify phone pin from provisioned user"() {
        given:
        User user = new User()
        user.phonePin = pin

        when:
        service.verifyPhonePin(user, "123231")

        then:
        thrown(BadRequestException)
    }

    def "Verify phone pin from federated user"() {
        given:
        FederatedUser user = new FederatedUser()
        user.phonePin = pin

        when:
        service.verifyPhonePin(user, "123231")

        then:
        thrown(BadRequestException)
    }

    def "Reset phone pin for provisioned user" () {
        given:
        identityConfig.getReloadableConfig().getUserPhonePinSize() >> 6

        User user = new User()
        user.id = "userId"
        user.phonePin = pin

        when:
        def pp = service.resetPhonePin(user)

        then:
        1 * identityUserDao.updateIdentityUser(_)

        pp.getPin() != pin
        pp.getPin().size() == 6
        pp.getPin().isNumber()
    }

    def "Reset phone pin for federated user" () {
        given:
        identityConfig.getReloadableConfig().getUserPhonePinSize() >> 6

        FederatedUser user = new FederatedUser()
        user.id = "userId"
        user.phonePin = pin

        when:
        def pp = service.resetPhonePin(user)

        then:
        1 * identityUserDao.updateIdentityUser(_)

        pp.getPin() != pin
        pp.getPin().size() == 6
        pp.getPin().isNumber()
    }

    def mockServices() {
        mockIdentityUserDao(service)
        mockIdentityConfig(service)
        mockIdentityUserService(service)
    }
}
