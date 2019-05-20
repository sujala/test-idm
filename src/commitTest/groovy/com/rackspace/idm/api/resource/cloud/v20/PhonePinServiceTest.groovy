package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NoPinSetException
import com.rackspace.idm.exception.PhonePinLockedException
import com.rackspace.idm.validation.Validator20
import org.opensaml.core.config.InitializationService
import spock.lang.Unroll
import testHelpers.RootServiceTest

class PhonePinServiceTest extends RootServiceTest {

    DefaultCloud20Service service
    JAXBObjectFactories objFactories
    Validator20 realValidator20

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        //service being tested
        service = new DefaultCloud20Service()

        realValidator20 = new Validator20()

        objFactories = new JAXBObjectFactories()
        service.jaxbObjectFactories = objFactories

        exceptionHandler = new ExceptionHandler()
        exceptionHandler.objFactories = objFactories
        service.exceptionHandler = exceptionHandler

        mockAuthorizationService(service)
        mockRequestContextHolder(service)
        mockIdentityConfig(service)
        mockPhonePinService(service)
    }

    def "Verify phone pin appropriately validates the caller before the PIN"() {
        when:
        service.verifyPhonePin(authToken, "userId", null).build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> {throw new ForbiddenException()}

        and: "Verify phone pin is not called"
        0 * phonePinService.verifyPhonePinOnUser(_, _)
    }

    @Unroll
    def "verifyPhonePin: Blank phone pin '#pin' for verify phone pin call results in 400"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin
            it
        }

        when:
        def result = service.verifyPhonePin(authToken, "userId", phonePin).build()

        then:
        result.status == 400

        and: "Verify phone pin is not called"
        0 * phonePinService.verifyPhonePinOnUser(_, _)

        where:
        pin << ["", " ", null]
    }

    def "verifyPhonePin: Correct phone pin results in 200 w/ authenticated set to true"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "2342"
            it
        }
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.verifyPhonePin(authToken, "userId", phonePin).build()

        then:
        1 * phonePinService.verifyPhonePinOnUser(user.id, phonePin.getPin()) >> true
        result.status == 200

        and:
        def responseEntity = result.getEntity()
        responseEntity.authenticated
    }

    def "verifyPhonePin: Incorrect phone pin results in 200 w/ appropriate failure"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "2342"
            it
        }
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.verifyPhonePin(authToken, "userId", phonePin).build()

        then: "Returns ok"
        1 * phonePinService.verifyPhonePinOnUser(user.id, phonePin.getPin()) >> false
        result.status == 200

        and:
        def responseEntity = result.getEntity()
        !responseEntity.authenticated
        responseEntity.failureCode == "PP-003"
        responseEntity.failureMessage == "Incorrect Phone PIN."
    }

    def "verifyPhonePin: If user doesn't have a phone pin, returns 200 w/ appropriate failure"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "2342"
            it
        }
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.verifyPhonePin(authToken, "userId", phonePin).build()

        then: "Returns ok"
        1 * phonePinService.verifyPhonePinOnUser(user.id, phonePin.getPin()) >> {throw new NoPinSetException()}
        result.status == 200

        and:
        def responseEntity = result.getEntity()
        !responseEntity.authenticated
        responseEntity.failureCode == "PP-000"
        responseEntity.failureMessage == "The user has not set a Phone PIN."
    }

    def "verifyPhonePin: If user pin is locked, returns 200 w/ appropriate failure"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "2342"
            it
        }
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.verifyPhonePin(authToken, "userId", phonePin).build()

        then: "Returns ok"
        1 * phonePinService.verifyPhonePinOnUser(user.id, phonePin.getPin()) >> {throw new PhonePinLockedException()}
        result.status == 200

        and:
        def responseEntity = result.getEntity()
        !responseEntity.authenticated
        responseEntity.failureCode == "PP-004"
        responseEntity.failureMessage == "User's PIN is locked."
    }
}
