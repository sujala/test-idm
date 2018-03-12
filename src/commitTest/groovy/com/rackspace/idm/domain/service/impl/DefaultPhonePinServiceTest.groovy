package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import spock.lang.Specification

class DefaultPhonePinServiceTest extends Specification {

    @Shared DefaultPhonePinService phonePinService

    def setupSpec() {
        phonePinService = new DefaultPhonePinService();
    }

    def pin = "3432"

    def "Retrieve phone pin from provisioned user" () {
        given:
        User user = new User()
        user.phonePin = pin

        when:
        def pp = phonePinService.checkAndGetPhonePin(user)

        then:
        pp.getPin() == pin

    }

    def "Retrieve phone pin from federated user" () {
        given:
        FederatedUser user = new FederatedUser()
        user.phonePin = pin

        when:
        def pp = phonePinService.checkAndGetPhonePin(user)

        then:
        pp.getPin() == pin

    }


    def "Retrieve phone pin throws not found" () {
        given:
        User user = new User()
        user.phonePin = null

        when:
        phonePinService.checkAndGetPhonePin(user)

        then:
        thrown(NotFoundException)
    }
}
