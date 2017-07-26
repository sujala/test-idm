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
class QuestonForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createQuestion(id, question)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | id        | question
        0           | "id"      | "question"
        1           | m("id")   | "question"
        0           | "id"      | m("question")
        1           | "id"      | m(m("question"))
    }
}
