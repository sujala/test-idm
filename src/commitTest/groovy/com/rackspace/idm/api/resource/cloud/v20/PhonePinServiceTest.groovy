package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.*
import com.rackspace.idm.validation.Validator20
import org.apache.http.HttpStatus
import org.opensaml.core.config.InitializationService
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static org.apache.http.HttpStatus.*

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
        mockIdentityUserService(service)
        mockPrecedenceValidator(service)
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

    def "Reset phone pin verifies user level access throws NotAuthorizedException"() {
        when:
        def result = service.resetPhonePin(authToken, "userId", false).build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> { throw new NotAuthorizedException() }

        assert result.status == 401
    }

    def "Reset phone pin returns 403 for a racker impersonated token"() {
        given:
        BaseUser caller = entityFactory.createUser().with {
            it.uniqueId = "uniqueId"
            it.id = "userId"
            it
        }
        requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest() >> true

        when:
        def result = service.resetPhonePin(authToken, "userId", false).build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller

        assert result.status == 403
    }

    def "Reset phone pin returns 403 for default user"() {
        given:
        def caller = entityFactory.createUser()

        when:
        def result = service.resetPhonePin(authToken, "userId", false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false

        result.status == SC_FORBIDDEN
    }

    @Unroll
    def "Reset phone pin returns 403 for self update #callerType"() {
        given:
        def caller = entityFactory.createUser()

        when:
        def result = service.resetPhonePin(authToken, caller.id, false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType

        result.status == SC_FORBIDDEN

        where:
        callerType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "Reset phone pin returns 403 for impersonated token #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser()

        when:
        def result = service.resetPhonePin(authToken, user.id, false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> true

        result.status == SC_FORBIDDEN

        where:
        callerType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "Reset phone pin returns 404 for users outside the domain #callerType #domainId"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", domainId, "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.resetPhonePin(authToken, user.id, false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> false
        1 * identityUserService.checkAndGetUserById(user.id) >> user

        result.status == SC_NOT_FOUND

        where:
        callerType                          | domainId
        IdentityUserTypeEnum.USER_ADMIN     | null
        IdentityUserTypeEnum.USER_ADMIN     | "domainId2"
        IdentityUserTypeEnum.USER_MANAGER   | null
        IdentityUserTypeEnum.USER_MANAGER   | "domainId2"
    }

    @Unroll
    def "Reset phone pin returns 403 for users without precedence  #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def result = service.resetPhonePin(authToken, user.id, false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> false
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}

        result.status == SC_FORBIDDEN

        where:
        callerType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "Reset phone pin returns 204 for valid authorization #callerType; pinState: #pinState"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")

        User user = Mock()
        user.id >> "userId"
        user.domainId >> caller.domainId
        user.phonePin >> "123654"

        when:
        def result = service.resetPhonePin(authToken, user.id, false).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> false
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * user.phonePinState >> pinState

        1 * phonePinService.resetPhonePin(user)

        result.status == SC_NO_CONTENT

        where:
        [callerType, pinState] << [[IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER], [PhonePinStateEnum.INACTIVE, PhonePinStateEnum.ACTIVE]].combinations()
    }

    @Unroll
    def "Reset phone pin returns 204 with onlyIfMissing query param when pin is not set #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION").with {
            it.phonePin = null
            it
        }

        when:
        def result = service.resetPhonePin(authToken, user.id, true).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> false
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * phonePinService.resetPhonePin(user)

        result.status == SC_NO_CONTENT

        where:
        callerType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "Reset phone pin returns 409 with onlyIfMissing query param when pin is already set #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION").with {
            it.phonePin = "123456"
            it
        }

        when:
        def result = service.resetPhonePin(authToken, user.id, true).build()

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false
        1 * requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> false
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        0 * phonePinService.resetPhonePin(user)

        result.status == SC_CONFLICT

        where:
        callerType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    def "resetPhonePin: If user pin is locked, throws forbidden exception when trying to change when onlyMissing=false"() {
        given:
        def caller = entityFactory.createUser("user", "userId", "domainId", "REGION")

        User user = Mock()
        user.id >> "id"
        user.phonePin >> "123987"
        user.phonePinState >> PhonePinStateEnum.LOCKED

        when:
        def result = service.resetPhonePin(authToken, user.id, false).build()

        then: "Returns ok"
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> true
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        0 * phonePinService.resetPhonePin(_)

        and:
        result.status == HttpStatus.SC_FORBIDDEN
        def responseEntity = result.getEntity()
        responseEntity.code == HttpStatus.SC_FORBIDDEN
        responseEntity.message == "Error code: 'PP-004'; User's PIN is locked."
    }

    @Unroll
    def "resetPhonePin: When onlyMissing=true, throws conflict error when phone pin exists on user with PIN state: #pinState"() {
        given:
        def caller = entityFactory.createUser("user", "userId", "domainId", "REGION")

        User user = Mock()
        user.id >> "id"
        user.phonePin >> "123987"
        user.phonePinState >> pinState

        when:
        def result = service.resetPhonePin(authToken, user.id, true).build()

        then: "Returns ok"
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> true
        1 * identityUserService.checkAndGetEndUserById(user.id) >> user
        0 * phonePinService.resetPhonePin(_)

        and:
        result.status == HttpStatus.SC_CONFLICT
        def responseEntity = result.getEntity()
        responseEntity.code == HttpStatus.SC_CONFLICT
        responseEntity.message == "User already has a phone PIN"

        where:
        pinState << [PhonePinStateEnum.ACTIVE, PhonePinStateEnum.LOCKED]
    }
}
