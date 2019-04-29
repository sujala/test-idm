package com.rackspace.idm.validation

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.exception.ForbiddenException
import org.openstack.docs.identity.api.v2.User
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class Cloud20CreateUserValidatorTest extends RootServiceTest {

    Cloud20CreateUserValidator service

    def setup() {
        service = new Cloud20CreateUserValidator()

        mockValidator20(service)
        mockPrecedenceValidator(service)
        mockAuthorizationService(service)
    }

    def "validateCreateUserAndGetUserForDefaults: validate contactId's length is validated"() {
        given:
        def contactId = "contactId"
        def user = new User().with {
            it.id = "id"
            it.contactId = contactId
            it
        }
        def caller = entityFactory.createUser()


        when:
        service.validateCreateUserAndGetUserForDefaults(user, caller)

        then:
        1 * validator20.validateStringMaxLength("contactId", contactId, Validator20.MAX_LENGTH_64)
    }

    def "validateCreateUserAndGetUserForDefaults: user with the 'identity:phone-pin-admin' role can set a phone pin"() {
        given:
        def caller = entityFactory.createUser()
        def user = v2Factory.createUser().with {
            it.username = "userWithPin"
            it.phonePin = "246972"
            it
        }

        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> true

        when:
        service.validateCreateUserAndGetUserForDefaults(user, caller)

        then:
        1 * validator20.validatePhonePin(user.phonePin)
    }

    def "validateCreateUserAndGetUserForDefaults: user without the 'identity:phone-pin-admin' role cannot set phone pin"() {
        given:
        def caller = entityFactory.createUser()
        def user = v2Factory.createUser().with {
            it.username = "userWithPin"
            it.phonePin = "246972"
            it
        }

        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> false

        when:
        service.validateCreateUserAndGetUserForDefaults(user, caller)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Not authorized to set phone pin.")
    }
}
