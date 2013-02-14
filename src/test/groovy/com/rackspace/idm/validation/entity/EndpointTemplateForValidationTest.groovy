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
class EndpointTemplateForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createEndpointTemplate(type, name, region, publicURL, internalURL, adminURL, version)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | type      | name      | region        | publicURL     | internalURL   | adminURL  | version
        0           | "type"    | "name"    | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | m("type") | "name"    | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | m("name") | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | "name"    | m("region")   | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | "name"    | "region"      | mm("public")  | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | "name"    | "region"      | "public"      | mm("internal")| "admin"   | entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | "name"    | "region"      | "public"      | "internal"    | mm("admin")| entityFactoryForValidation.createVersionForService("id", "info", "list")
        1           | "type"    | "name"    | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService(m("id"), "info", "list")
        1           | "type"    | "name"    | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", mm("info"), "list")
        1           | "type"    | "name"    | "region"      | "public"      | "internal"    | "admin"   | entityFactoryForValidation.createVersionForService("id", "info", mm("list"))
    }
}
