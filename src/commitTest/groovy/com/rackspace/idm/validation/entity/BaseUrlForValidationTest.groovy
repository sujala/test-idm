package com.rackspace.idm.validation.entity

import com.rackspace.idm.validation.DefaultObjectValidator
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/2/13
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
class BaseUrlForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"() {
       when:
        def entity = entityFactoryForValidation.createBaseUrl(serviceName, region, adminUrl, publicUrl, internalUrl)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | serviceName | region      | adminUrl      | publicUrl   | internalUrl
        1           | m("service")| "region"    | "adminUrl"    | "public"    | "internal"
        1           | "service"   | m("region") | "adminUrl"    | "public"    | "internal"
        1           | "service"   | "region"    | m("adminUrl") | "public"    | "internal"
        1           | "service"   | "region"    | "adminUrl"    | m("public") | "internal"
        1           | "service"   | "region"    | "adminUrl"    | "public"    | m("internal")
        2           | "service"   | m("region") | "adminUrl"    | m("public") | "internal"
    }
}
