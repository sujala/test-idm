package com.rackspace.idm.validation.entity

import com.rackspace.idm.validation.DefaultObjectValidator
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/3/13
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */
class CredentialTypeForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields passwordCred"(){
        when:
        def entity = entityFactoryForValidation.createPasswordCredentials(username, password)
        def result = objectValidator.getViolationMessages(entity.value)

        then:
        result.size() == expected

        where:
        expected    | username      | password
        0           | "username"    | "password"
        1           | m("username") | "password"
        1           | "username"    | m("password")
        2           | m("username") | m("password")
    }
}
