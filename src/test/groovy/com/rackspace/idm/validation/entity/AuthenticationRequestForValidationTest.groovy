package com.rackspace.idm.validation.entity;

import com.rackspace.idm.aspect.ValidateAspect
import com.rackspace.idm.validation.DefaultObjectValidator;
import com.rackspace.idm.validation.ObjectConverter
import spock.lang.Shared;
import testHelpers.RootServiceTest

class AuthenticationRequestForValidationTest extends RootServiceTest {
    @Shared
    DefaultObjectValidator objectValidator

    def setupSpec(){
        objectValidator = new DefaultObjectValidator()
    }

    def setup() {
        objectValidator.setup()
    }

    def "validate fields for AuthenticationRequestForValidation"() {
        when:
        def entity = entityFactoryForValidation.createAuthenticationRequest(tenantId, tenantName, id, credential)
        def result = objectValidator.getViolationMessages(entity)

        then:
        result.size() == expected

        where:
        expected    | id        | tenantId          | tenantName        | credential
        0           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createPasswordCredentials("username", "Password1")
        1           | m("id")   | "tenantId"        | "tenantName"      | entityFactoryForValidation.createPasswordCredentials("username", "Password1")
        1           | "id"      | m("tenantId")     | "tenantName"      | entityFactoryForValidation.createPasswordCredentials("username", "Password1")
        1           | "id"      | "tenantId"        | m("tenantName")   | entityFactoryForValidation.createPasswordCredentials("username", "Password1")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createPasswordCredentials(m("username"), "Password1")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createPasswordCredentials("username", m("Password1"))
        5           | m("id")   | m("tenantId")     | m("tenantName")   | entityFactoryForValidation.createPasswordCredentials(m("username"), m("Password1"))

        0           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createApiKeyCredentials("username", "123456790")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createApiKeyCredentials(m("username"), "123456790")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createApiKeyCredentials("username", m("123456790"))

        0           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createRsaCredentials("username", "tokenKey")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createRsaCredentials(m("username"), "tokenKey")
        1           | "id"      | "tenantId"        | "tenantName"      | entityFactoryForValidation.createRsaCredentials("username", m("tokenKey"))
    }
}