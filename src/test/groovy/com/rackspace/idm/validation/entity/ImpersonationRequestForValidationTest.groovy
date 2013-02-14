package com.rackspace.idm.validation.entity

import com.rackspace.idm.validation.DefaultObjectValidator
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/14/13
 * Time: 10:51 AM
 * To change this template use File | Settings | File Templates.
 */
class ImpersonationRequestForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def user = entityFactoryForValidation.createUser(id, username, email, displayName, password)
        def entity = entityFactoryForValidation.createImpersonationRequest(user)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    |  id       | username      | email     | displayName       | password
        0           | "id"      | "username"    | "email"   | "displayName"     | "password"
        1           | m("id")   | "username"    | "email"   | "displayName"     | "password"
        1           | "id"      | m("username") | "email"   | "displayName"     | "password"
        1           | "id"      | "username"    | m("email")| "displayName"     | "password"
        1           | "id"      | "username"    | "email"   | m("displayName")  | "password"
        1           | "id"      | "username"    | "email"   | "displayName"     | m("password")
    }
}
