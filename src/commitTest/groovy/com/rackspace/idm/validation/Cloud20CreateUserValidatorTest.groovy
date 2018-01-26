package com.rackspace.idm.validation

import org.openstack.docs.identity.api.v2.User
import testHelpers.RootServiceTest

class Cloud20CreateUserValidatorTest extends RootServiceTest {

    Cloud20CreateUserValidator cloud20CreateUserValidator

    def setup() {
        cloud20CreateUserValidator = new Cloud20CreateUserValidator()

        mockValidator20(cloud20CreateUserValidator)
        mockPrecedenceValidator(cloud20CreateUserValidator)
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
        cloud20CreateUserValidator.validateCreateUserAndGetUserForDefaults(user, caller)

        then:
        1 * validator20.validateStringMaxLength("contactId", contactId, Validator20.MAX_LENGTH_64)
    }
}
