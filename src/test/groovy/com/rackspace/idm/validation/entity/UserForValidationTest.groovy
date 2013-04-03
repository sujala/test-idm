package com.rackspace.idm.validation.entity

import com.rackspace.idm.validation.DefaultObjectValidator
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.namespace.QName

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/14/13
 * Time: 10:51 AM
 * To change this template use File | Settings | File Templates.
 */
class UserForValidationTest extends RootServiceTest{
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
        def baseUrlRefList = entityFactoryForValidation.createBaseUrlRefList([baseUrlRef].asList())
        def entity = entityFactoryForValidation.createUser(id, username, email, displayName, password, nastId, key, baseUrlRefList)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    |  id       | username      | email     | displayName       | password      | key       | nastId        | href
        0           | "id"      | "username"    | "email"   | "displayName"     | "password"    | "key"     | "nastId"      | "href"
        1           | m("id")   | "username"    | "email"   | "displayName"     | "password"    | "key"     | "nastId"      | "href"
        1           | "id"      | m("username") | "email"   | "displayName"     | "password"    | "key"     | "nastId"      | "href"
        1           | "id"      | "username"    | m("email")| "displayName"     | "password"    | "key"     | "nastId"      | "href"
        1           | "id"      | "username"    | "email"   | m("displayName")  | "password"    | "key"     | "nastId"      | "href"
        1           | "id"      | "username"    | "email"   | "displayName"     | m("password") | "key"     | "nastId"      | "href"
        1           | "id"      | "username"    | "email"   | "displayName"     | "password"    | m("key")  | "nastId"      | "href"
        1           | "id"      | "username"    | "email"   | "displayName"     | "password"    | "key"     | m("nastId")   | "href"
        1           | "id"      | "username"    | "email"   | "displayName"     | "password"    | "key"     | "nastId"      | mm("href")
    }

    def "Validate other attr fields"(){
        when:
        def entity = entityFactoryForValidation.createUser(id, username, email, displayName, password, nastId, key, null)
        entity.otherAttributes.put(new QName(""), String.valueOf(attr))
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    |  id       | username      | email     | displayName       | password      | key       | nastId        | href      | attr
        2           | "id"      | m("username") | "email"   | "displayName"     | "password"    | "key"     | "nastId"      | "href"    | m("attr")
    }


}
