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
class RoleForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createRole(id, name, tenantId, serviceId, description)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | id        | name      | tenantId      | serviceId     | description
        0           | "id"      | "name"    | "tenantId"    | "serviceId"   | "desc"
        1           | m("id")   | "name"    | "tenantId"    | "serviceId"   | "desc"
        1           | "id"      | m("name") | "tenantId"    | "serviceId"   | "desc"
        1           | "id"      | "name"    | m("tenantId") | "serviceId"   | "desc"
        1           | "id"      | "name"    | "tenantId"    | m("serviceId")| "desc"
        0           | "id"      | "name"    | "tenantId"    | "serviceId"   | m("desc")
        1           | "id"      | "name"    | "tenantId"    | "serviceId"   | m(m("desc"))
    }
}
