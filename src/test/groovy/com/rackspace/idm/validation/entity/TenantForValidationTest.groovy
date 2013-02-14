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
class TenantForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createTenant(id, name, displayName, description)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | id        | name      | displayName          | description
        0           | "id"      | "name"    | "displayName"        | "desc"
        1           | m("id")   | "name"    | "displayName"        | "desc"
        1           | "id"      | m("name") | "displayName"        | "desc"
        1           | "id"      | "name"    | m("displayName")     | "desc"
        0           | "id"      | "name"    | "displayName"        | m("desc")
        1           | "id"      | "name"    | "displayName"        | m(m("desc"))
    }
}
