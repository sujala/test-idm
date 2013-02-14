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
class PolicyForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createPolicy(id, name, type, blob, description)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected |id        | name      | type      | blob      | description
        0        |"id"      | "name"    | "type"    | "blob"    | "desc"
        1        |m("id")   | "name"    | "type"    | "blob"    | "desc"
        1        |"id"      | m("name") | "type"    | "blob"    | "desc"
        1        |"id"      | "name"    | m("type") | "blob"    | "desc"
        1        |"id"      | "name"    | "type"    | mmm("blob")| "desc"
        1        |"id"      | "name"    | "type"    | "blob"    | mm("desc")
    }
}
