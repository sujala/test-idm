package com.rackspace.idm.domain.entity

import com.rackspace.idm.GlobalConstants
import spock.lang.Specification
import spock.lang.Unroll

/**
 * This test verifies the phone pin methods work per expectation on all concrete implementations of PhonePinProtectedUser
 */
class PhonePinProtectedUserTest extends Specification {

    @Unroll
    def "getPhonePinAuthenticationFailureCount: Returns appropriate count for type #user.class.name and test date #a #b"() {
        given:
        user.phonePinAuthenticationFailureCount = testData[0]

        expect:
        user.@phonePinAuthenticationFailureCount == testData[0]
        user.getPhonePinAuthenticationFailureCount() == testData[1]

        where:
        // testData is of format [testCount, expectedResult]
        [user, testData] << [[new User(), new FederatedUser()], [[null, 0], [0,0],[2,2],[6,6],[9,9]]].combinations()
    }

    @Unroll
    def "getPhonePinState: Returns appropriate pin state based on overall user state for type #user.class.name and test date #a #b"() {
        given:
        user.phonePin = testData[0]
        user.phonePinAuthenticationFailureCount = testData[1]

        expect:
        user.getPhonePinState() == testData[2]

        where:
        // pin, failureCount, expectedState
        [user, testData] <<
        [[new User(), new FederatedUser()],
        [[null, null, PhonePinStateEnum.INACTIVE],
        [null, 2, PhonePinStateEnum.INACTIVE],
        ["123654", null, PhonePinStateEnum.ACTIVE],
        ["123654", 3, PhonePinStateEnum.ACTIVE],
        ["123654", 6, PhonePinStateEnum.LOCKED],
        ["123654", 8, PhonePinStateEnum.LOCKED]]].combinations()
    }

    @Unroll
    def "recordFailedPinAuthentication: Increments failure count by 1 and sets failure date for type #user.class.name"() {
        user.phonePin = "123321"

        expect:
        7.times {
            Date currentFailureDate = user.phonePinAuthenticationLastFailureDate
            sleep(1) // Just to get system date to change so failure date is changed

            user.recordFailedPinAuthentication()
            assert user.getPhonePinAuthenticationFailureCount() == it + 1
            assert user.phonePinAuthenticationLastFailureDate != currentFailureDate

            if (user.getPhonePinAuthenticationFailureCount() >= GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD) {
                assert user.getPhonePinState() == PhonePinStateEnum.LOCKED
            } else {
                assert user.getPhonePinState() == PhonePinStateEnum.ACTIVE
            }
        }

        where:
        user << [new User(), new FederatedUser()]
    }
}
