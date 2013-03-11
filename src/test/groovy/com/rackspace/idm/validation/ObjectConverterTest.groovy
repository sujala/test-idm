package com.rackspace.idm.validation

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm
import com.rackspace.idm.validation.entity.AuthenticationRequestForValidation
import com.rackspace.idm.validation.entity.BaseUrlRefForValidation
import com.rackspace.idm.validation.entity.DefaultRegionServicesForValidation
import com.rackspace.idm.validation.entity.DomainForValidation
import com.rackspace.idm.validation.entity.EndpointTemplateForValidation
import com.rackspace.idm.validation.entity.GroupForValidation
import com.rackspace.idm.validation.entity.ImpersonationRequestForValidation
import com.rackspace.idm.validation.entity.PoliciesForValidation
import com.rackspace.idm.validation.entity.PolicyForValidation
import com.rackspace.idm.validation.entity.QuestionForValidation
import com.rackspace.idm.validation.entity.RegionForValidation
import com.rackspace.idm.validation.entity.RoleForValidation
import com.rackspace.idm.validation.entity.SecretQAForValidation
import com.rackspace.idm.validation.entity.ServiceForValidation
import com.rackspace.idm.validation.entity.StringForValidation
import com.rackspace.idm.validation.entity.TenantForValidation
import com.rackspace.idm.validation.entity.UserForValidation
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import com.rackspacecloud.docs.auth.api.v1.User
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.namespace.QName

import static com.rackspace.idm.RaxAuthConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/12/13
 * Time: 12:31 PM
 * To change this template use File | Settings | File Templates.
 */
class ObjectConverterTest extends RootServiceTest{
    @Shared ObjectConverter converter;

    def setupSpec(){
        converter = new ObjectConverter()
        converter.setup()
    }

    def "isConvertible returns true for Authentication Request"(){
        when:
        def result = converter.isConvertible(entity)

        then:
        result == expected

        where:
        expected    |   entity
        true        |   v2Factory.createAuthenticationRequest()
        false       |   new String()
    }

    def "Convert obect to the correct type"(){
        when:
        def result = converter.convert(entity)

        then:
        result.getClass().equals(expectedType)

        where:
        expectedType                           | entity
        AuthenticationRequestForValidation.class | v2Factory.createAuthenticationRequest()

    }

    def "Convert AuthenticationRequest to authenticationRequestForValidation: PasswordCredentials"(){
        when:
        def entity = v2Factory.createAuthenticationRequest()
        entity.credential = v2Factory.createJAXBPasswordCredentialsRequiredUsername("name","Password1")
        entity.token = v2Factory.createTokenForAuthenticationRequest()
        entity.otherAttributes.put(new QName("http://localhost"), "hello")
        AuthenticationRequestForValidation result = converter.convert(entity)

        then:
        result != null
        result.credential != null
        result.credential.value.password == "Password1"
        result.credential.value.username == "name"
        result.token.id == "id"
        result.tenantId == "tenantId"
        result.tenantName == "tenantName"
    }

    def "Convert AuthenticationRequest to authenticationRequestForValidation: ApiKeyCredentials"(){
        when:
        def entity = v2Factory.createAuthenticationRequest()
        entity.credential = v2Factory.createJAXBApiKeyCredentials("name","1234567890")
        AuthenticationRequestForValidation result = converter.convert(entity)

        then:
        result != null
        result.credential != null
        result.credential.value.apiKey == "1234567890"
        result.credential.value.username == "name"
    }

    def "Convert AuthenticationRequest to authenticationRequestForValidation: RsaCredentials"(){
        when:
        def entity = v2Factory.createAuthenticationRequest()
        entity.credential = v2Factory.createJAXBRsaCredentials("name","rsa")
        AuthenticationRequestForValidation result = converter.convert(entity)

        then:
        result != null
        result.credential != null
        result.credential.value.tokenKey == "rsa"
        result.credential.value.username == "name"
    }

    def "Convert Region to RegionForValidation"(){
        when:
        def entity = v1Factory.createRegion()
        RegionForValidation result = converter.convert(entity)

        then:
        result != null
        result.name == "name"
    }

    def "Convert Domain to DomainForValidation"(){
        when:
        def entity = v1Factory.createDomain()
        entity.description = "description"
        DomainForValidation result = converter.convert(entity)

        then:
        result != null
        result.name == "name"
        result.id == "id"
        result.description == "description"
    }

    def "Convert Role to RoleForValidation"(){
        when:
        def entity = v2Factory.createRole()
        entity.otherAttributes.put(QNAME_WEIGHT, "weight")
        entity.otherAttributes.put(QNAME_PROPAGATE, "propagate")
        entity.description = "description"
        entity.id = "id"
        RoleForValidation result = converter.convert(entity)

        then:
        result != null
        result.name == "name"
        result.id == "id"
        result.description == "description"
        result.tenantId == "tenantId"
        result.serviceId == "applicationId"
        result.otherAttributes.get(QNAME_WEIGHT) == "weight"
        result.otherAttributes.get(QNAME_PROPAGATE) == "propagate"
    }

    def "Convert Group to GroupForValidation"(){
        when:
        def entity = v1Factory.createGroup()
        GroupForValidation result = converter.convert(entity)

        then:
        result != null
        result.name == "name"
        result.id == "id"
        result.description == "description"
    }

    def "Convert Question to QuestionForValidation"(){
        when:
        def entity = v1Factory.createQuestion()
        QuestionForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "id"
        result.question == "question?"
    }

    def "Convert SecretQA(rax-auth) to SecretQAForValidation"(){
        when:
        def entity = v1Factory.createSecretQA()
        SecretQAForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "1"
        result.question == "question"
        result.answer == "answer"
    }

    def "Convert SecretQA(rax-ksqa) to SecretQAForValidation"(){
        when:
        def entity = v1Factory.createSecretQA_Keystone()
        SecretQAForValidation result = converter.convert(entity)

        then:
        result != null
        result.username == "username"
        result.question == "question"
        result.answer == "answer"
    }

    def "Convert Service to ServiceForValidation"(){
        when:
        def entity = v1Factory.createService()
        entity.type = "type"
        entity.description = "description"
        ServiceForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "id"
        result.name == "name"
        result.type == "type"
        result.description == "description"
    }

    def "Convert Tenant to TenantForValidation"(){
        when:
        def entity = v2Factory.createTenant()
        entity.displayName = "name"
        entity.description = "description"
        TenantForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "id"
        result.name == "name"
        result.displayName == "name"
        result.description == "description"
    }

    def "Convert EndpointTemplate to EndpointTemplateForValidation"(){
        when:
        def entity = v1Factory.createEndpointTemplate()
        entity.type = "type"
        entity.region = "region"
        entity.publicURL = "publicUrl"
        entity.internalURL = "internalUrl"
        entity.adminURL = "adminUrl"
        def versionForService = v2Factory.createVersionForService()
        entity.version = versionForService
        EndpointTemplateForValidation result = converter.convert(entity)

        then:
        result != null
        result.type == "type"
        result.region == "region"
        result.publicURL == "publicUrl"
        result.internalURL == "internalUrl"
        result.adminURL == "adminUrl"
        result.name == "name"
        result.version != null
        result.version.info == "info"
        result.version.list == "list"
    }

    def "Convert Policy to PolicyForValidation"(){
        when:
        def entity = v1Factory.createPolicy()
        entity.name = "name"
        entity.type = "type"
        entity.description = "description"
        PolicyForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "id"
        result.name == "name"
        result.description == "description"
        result.type == "type"
        result.blob == "blob"
    }

    def "Convert Policies to PoliciesForValidation"(){
        when:
        def entity = v1Factory.createPolicy()
        entity.name = "name"
        entity.type = "type"
        entity.description = "description"
        def policies = v1Factory.createPolicies([entity].asList())
        policies.algorithm = PolicyAlgorithm.IF_TRUE_ALLOW
        PoliciesForValidation result = converter.convert(policies)

        then:
        result != null
        result.policy != null
        result.algorithm ==  "IF_TRUE_ALLOW"
        result.policy.get(0).id == "id"
        result.policy.get(0).name == "name"
        result.policy.get(0).description == "description"
        result.policy.get(0).type == "type"
        result.policy.get(0).blob == "blob"
    }

    def "Convert DefaultRegionServices to DefaultRegionServicesForValidation"(){
        when:
        def entity = v1Factory.createDefaultRegionServices(["name"].asList())
        DefaultRegionServicesForValidation result = converter.convert(entity)

        then:
        result != null
        def item = result.getServiceNames().get(0)
        item instanceof StringForValidation
        item.value.toString() == "name"
    }

    def "Convert User to UserForValidation"(){
        when:
        def entity = v2Factory.createUser()
        entity.email = "email"
        entity.displayName = "name"
        UserForValidation result = converter.convert(entity)

        then:
        result != null
        result.displayName == "name"
        result.email == "email"
        result.id == "id"
        result.username == "username"
    }

    def "Convert UserForCreate to UserForValidation"(){
        when:
        def entity = new UserForCreate().with {
            it.password == "Password1"
            return it
        }
        entity.email = "email"
        entity.displayName = "name"
        entity.password = "Password1"
        entity.username = "username"
        entity.id = "id"
        UserForValidation result = converter.convert(entity)

        then:
        result != null
        result.displayName == "name"
        result.email == "email"
        result.id == "id"
        result.username == "username"
        result.password == "Password1"
    }

    def "Convert ImpersonationRequest to ImpersonationRequestForValidation"(){
        when:
        def entity = v2Factory.createUser()
        entity.email = "email"
        entity.displayName = "name"
        def impersonationRequest = new ImpersonationRequest().with {
            it.user = entity
            return it
        }
        ImpersonationRequestForValidation result = converter.convert(impersonationRequest)

        then:
        result != null
        result.user != null
        result.user.displayName == "name"
        result.user.email == "email"
        result.user.id == "id"
        result.user.username == "username"
    }

    def "Convert BaseURLRef to BaseURLRefForValidation"(){
        when:
        def entity = v1Factory.createBaseUrlRef()
        BaseUrlRefForValidation result = converter.convert(entity)

        then:
        result != null
        result.href == "href"
    }

    def "Convert User(v1.1) to UserForValidation"(){
        when:
        def entity = new User().with {
            it.id = "id"
            it.nastId = "nast"
            it.key = "key"
            return it
        }
        def baseUrlRef = new BaseURLRef().with {
            it.href = "href"
            return it
        }
        def baseUrlRefList = new BaseURLRefList().with {
            it.baseURLRef = [baseUrlRef].asList()
            return it
        }
        entity.baseURLRefs = baseUrlRefList

        UserForValidation result = converter.convert(entity)

        then:
        result != null
        result.id == "id"
        result.key == "key"
        result.nastId == "nast"
        result.baseURLRefs.baseURLRef.get(0).href == "href"
    }
}
