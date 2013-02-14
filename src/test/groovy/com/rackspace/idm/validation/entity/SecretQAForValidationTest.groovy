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
class SecretQAForValidationTest extends RootServiceTest{
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup(){
        objectValidator.setup()
    }

    def "Validate fields"(){
        when:
        def entity = entityFactoryForValidation.createSecretQA(username, id, question, answer)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | id        | username      | question          | answer
        0           | "id"      | "username"    | "question"        | "answer"
        0           | "id"      | "username"    | m("question")     | "answer"
        1           | m("id")   | "username"    | "question"        | "answer"
        1           | "id"      | m("username") | "question"        | "answer"
        1           | "id"      | "username"    | m(m("question"))  | "answer"
        1           | "id"      | "username"    | "question"        | m("answer")
    }
}
