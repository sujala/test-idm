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
class PoliciesForValidationTest extends RootServiceTest{
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
        def entities = entityFactoryForValidation.createPolicies(algorithm, [entity].asList())
        def result = objectValidator.getViolationMessages(entities)

        then:
        result.size() == expected

        where:
        expected |id        | name      | type      | blob      | description   | algorithm
        0        |"id"      | "name"    | "type"    | "blob"    | "desc"        | "algo"
        1        |m("id")   | "name"    | "type"    | "blob"    | "desc"        | "algo"
        1        |"id"      | m("name") | "type"    | "blob"    | "desc"        | "algo"
        1        |"id"      | "name"    | m("type") | "blob"    | "desc"        | "algo"
        1        |"id"      | "name"    | "type"    | mmm("blob")| "desc"       | "algo"
        1        |"id"      | "name"    | "type"    | "blob"    | mm("desc")    | "algo"
        1        |"id"      | "name"    | "type"    | "blob"    | "desc"        | m("algo")
    }
}
