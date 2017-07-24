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
class BaseUrlRefListForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def baseUrlRef = entityFactoryForValidation.createBaseUrlRef(href)
        def entity = entityFactoryForValidation.createBaseUrlRefList([baseUrlRef].asList())
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | href
        0           | "href"
        1           | mm("href")
    }
}
