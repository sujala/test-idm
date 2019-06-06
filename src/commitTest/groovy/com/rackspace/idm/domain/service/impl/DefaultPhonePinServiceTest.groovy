package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.NoPinSetException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.PhonePinLockedException
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class DefaultPhonePinServiceTest extends RootServiceTest {

    @Shared
    DefaultPhonePinService service

    def setupSpec() {
        InitializationService.initialize()
        service = new DefaultPhonePinService()
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

    def "verifyPhonePinOnUser: Verify correct phone pin return true and records success"() {
        given:
        User user = Mock()
        user.id = "id"
        user.phonePin >> "123231"
        user.phonePinAuthenticationFailureCount = 2

        when:
        boolean result = service.verifyPhonePinOnUser(user.id, "123231")

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        1 * user.recordSuccessfulPinAuthentication()
        0 * user.recordFailedPinAuthentication()
        1 * identityUserService.updateEndUser(user)
        result
    }

    def "verifyPhonePinOnUser: Throws com.rackspace.idm.exception.NoPinSetException when user does not have a pin"() {
        given:
        User user = Mock()
        user.id = "id"
        user.phonePinState >> PhonePinStateEnum.INACTIVE

        when:
        service.verifyPhonePinOnUser(user.id, "123231")

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NoPinSetException, ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_FOUND, "The user has not set a Phone PIN.")

        and: "Pin failure count remains the same"
        0 * user.recordSuccessfulPinAuthentication()
        0 * user.recordFailedPinAuthentication()
        0 * identityUserService.updateEndUser(user)
    }

    def "verifyPhonePinOnUser: Throws appropriate com.rackspace.idm.exception.PhonePinLockedException when user's PIN is considered locked"() {
        given:
        User user = Mock()
        user.id = "id"
        user.phonePinState >> PhonePinStateEnum.LOCKED

        when:
        service.verifyPhonePinOnUser(user.id, "123231")

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, PhonePinLockedException, "PP-004", "User's PIN is locked.")

        and: "Pin failure count remains the same"
        0 * user.recordSuccessfulPinAuthentication()
        0 * user.recordFailedPinAuthentication()
        0 * identityUserService.updateEndUser(user)
    }

    def "verifyPhonePinOnUser: Verify incorrect phone pin return false"() {
        given:
        User user = Mock()
        user.id = "id"
        user.phonePin >> "123123"
        user.phonePinState >> PhonePinStateEnum.ACTIVE

        when:
        boolean result = service.verifyPhonePinOnUser(user.id, "999999")

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        !result

        and: "Pin failure is recorded"
        0 * user.recordSuccessfulPinAuthentication()
        1 * user.recordFailedPinAuthentication()
        1 * identityUserService.updateEndUser(user)
    }

    @Unroll
    def "verifyPhonePinOnUser: Verify blank phone pin results in no match: pin: #pin"() {
        given:
        User user = new User().with {
            it.id = "id"
            it.phonePin = pin
            it
        }

        when:
        boolean result = service.verifyPhonePinOnUser(user.id, pin)

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        !result

        where:
        pin << ["", null, "  "]
    }

    def "Reset phone pin for provisioned user" () {
        given:
        User user = new User()
        user.id = "userId"
        user.phonePin = pin

        when:
        def pp = service.resetPhonePin(user)

        then:
        1 * identityUserDao.updateIdentityUser(_)

        pp.getPin() != pin
        pp.getPin().size() == GlobalConstants.PHONE_PIN_SIZE
        pp.getPin().isNumber()
    }

    def "Reset phone pin for federated user" () {
        given:
        FederatedUser user = new FederatedUser()
        user.id = "userId"
        user.phonePin = pin

        when:
        def pp = service.resetPhonePin(user)

        then:
        1 * identityUserDao.updateIdentityUser(_)

        pp.getPin() != pin
        pp.getPin().size() == GlobalConstants.PHONE_PIN_SIZE
        pp.getPin().isNumber()
    }

    def "test that generated phone pin is 6 digit, non sequential and non repeating numbers. 10000 iterations."(){
        expect:
        100000.times {
            def pp = service.generatePhonePin()
            assert pp.size() == GlobalConstants.PHONE_PIN_SIZE
            assert pp.isNumber()
            assert IdmAssert.isPhonePinNonRepeating(pp)
            assert IdmAssert.isPhonePinNonSequential(pp)
        }
    }

    def "unlock phone pin: Verify only locked phone pin can be unlocked"() {
        given:
        User user = Mock()
        user.id >> "id"
        user.phonePin >> "123123"
        user.phonePinState >> PhonePinStateEnum.LOCKED

        when:
        service.unlockPhonePin(user.id)

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user

        and: "Pin get unlocked"
        1 * user.unlockPhonePin()
        1 * identityUserService.updateEndUser(user)
    }

    def "unlock phone pin: Verify exception when user try to unlock phone PIN with active state "() {
        given:
        User user = Mock()
        user.id = "id"
        user.phonePinState >> PhonePinStateEnum.ACTIVE

        when:
        service.unlockPhonePin(user.id)

        then:
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        0 * user.unlockPhonePin()
        0 * identityUserService.updateEndUser(user)

        and: "Exception is thrown"
        Exception exception = thrown()
        IdmExceptionAssert.assertException(exception, ForbiddenException, "PP-005", "The Phone PIN is not locked.")
    }

    def mockServices() {
        mockIdentityUserDao(service)
        mockIdentityUserService(service)
    }
}
