package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices
import com.rackspace.idm.validation.entity.AuthenticationRequestForValidation
import com.rackspace.idm.validation.entity.BaseUrlRefForValidation
import com.rackspace.idm.validation.entity.BaseUrlRefListForValidation
import com.rackspace.idm.validation.entity.CredentialTypeForValidation
import com.rackspace.idm.validation.entity.DefaultRegionServicesForValidation
import com.rackspace.idm.validation.entity.DomainForValidation
import com.rackspace.idm.validation.entity.EndpointTemplateForValidation
import com.rackspace.idm.validation.entity.GroupForValidation
import com.rackspace.idm.validation.entity.ImpersonationRequestForValidation
import com.rackspace.idm.validation.entity.JAXBElementCredentialTypeForValidation
import com.rackspace.idm.validation.entity.PoliciesForValidation
import com.rackspace.idm.validation.entity.PolicyForValidation
import com.rackspace.idm.validation.entity.QuestionForValidation
import com.rackspace.idm.validation.entity.QuestonForValidationTest
import com.rackspace.idm.validation.entity.RegionForValidation
import com.rackspace.idm.validation.entity.RoleForValidation
import com.rackspace.idm.validation.entity.SecretQAForValidation
import com.rackspace.idm.validation.entity.ServiceForValidation
import com.rackspace.idm.validation.entity.StringForValidation
import com.rackspace.idm.validation.entity.TenantForValidation
import com.rackspace.idm.validation.entity.TokenForAuthenticationRequestForValidation
import com.rackspace.idm.validation.entity.UserForValidation
import com.rackspace.idm.validation.entity.VersionForServiceForValidation
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/14/13
 * Time: 9:45 AM
 * To change this template use File | Settings | File Templates.
 */
class EntityFactoryForValidation extends Specification{


    def createAuthenticationRequest(String tenantId, String tenantName, String id, credential){
        new AuthenticationRequestForValidation().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.token = new TokenForAuthenticationRequestForValidation().with {
                it.id = id
                return it
            }
            it.credential = credential
            return it
        }
    }

    def createPasswordCredentials(String username, String password){
        return createCredentialType(username, password, null, null)
    }

    def createApiKeyCredentials(String username, String apiKey){
        return createCredentialType(username, null, null, apiKey)
    }

    def createRsaCredentials(String username, String tokenKey){
        return createCredentialType(username, null, tokenKey, null)
    }

    def createCredentialType(String username, String password, String tokenKey, String apiKey){
        def cred = new CredentialTypeForValidation().with {
            it.username = username
            it.password = password
            it.tokenKey = tokenKey
            it.apiKey = apiKey
            return it
        }
        return new JAXBElementCredentialTypeForValidation(cred)
    }

    def createRegion(String name) {
        new RegionForValidation().with {
            it.name = name
            return it
        }
    }

    def createDomain(String id, String name, String description) {
        new DomainForValidation().with {
            it.id = id
            it.name = name
            it.description = description
            return it
        }
    }

    def createRole(String id, String name, String tenantId, String serviceId, String description) {
        new RoleForValidation().with {
            it.id = id
            it.name = name
            it.tenantId = tenantId
            it.serviceId = serviceId
            it.description = description
            return it
        }
    }

    def createGroup(String id, String name, String description) {
        new GroupForValidation().with {
            it.id = id
            it.name = name
            it.description = description
            return it
        }
    }

    def createQuestion(String id, String question) {
        new QuestionForValidation().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def createSecretQA(String username, String id, String question, String answer) {
        new SecretQAForValidation().with {
            it.username = username
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }

    def createSecretQA_Keystone(String username, String id, String question, String answer) {
        new SecretQAForValidation().with {
            it.username = username
            it.id = id
            it.question = question
            it.answer = answer
            return it
        }
    }

    def createService(String id, String name, String type, String description) {
        new ServiceForValidation().with {
            it.id = id
            it.name = name
            it.type = type
            it.description = description
            return it
        }
    }

    def createTenant(String id, String name, String displayName, String description) {
        new TenantForValidation().with {
            it.id = id
            it.name = name
            it.displayName = displayName
            it.description = description
            return it

        }
    }

    def createEndpointTemplate(String type, String name, String region, String publicURL,
            String internalURL, String adminURL, version) {
        new EndpointTemplateForValidation().with {
            it.type= type
            it.name = name
            it.region = region
            it.publicURL = publicURL
            it.internalURL = internalURL
            it.adminURL = adminURL
            it.version = version
            return it
        }
    }

    def createVersionForService(String id, String info, String list) {
        new VersionForServiceForValidation().with {
            it.id = id
            it.info = info
            it.list = list
            return it
        }
    }

    def createPolicy(String id, String name, String type, String blob, String description) {
        new PolicyForValidation().with {
            it.description = description
            it.id = id
            it.name = name
            it.type = type
            it.blob = blob
            return it
        }
    }

    def createPolicies(String algorithm, policy) {
        new PoliciesForValidation().with {
            it.algorithm = algorithm
            it.policy = policy
            return it
        }
    }

    def createDefaultRegionServices(serviceName) {
        new DefaultRegionServicesForValidation().with {
            it.serviceName = serviceName
            return it
        }
    }

    def createString(String value) {
        new StringForValidation().with {
            it.value = value
            return it
        }
    }

    def createUser(String id, String username, String email, String displayName, String password) {
        return createUser(id, username, email, displayName, password, null, null, null);
    }

    def createUser(String id, String username, String email, String displayName, String password,
                   String nastId, String key, baseURLRefs) {
        new UserForValidation().with {
            it.id = id
            it.username = username
            it.email = email
            it.displayName = displayName
            it.password = password
            it.nastId = nastId
            it.key = key
            it.baseURLRefs = baseURLRefs
            return it
        }
    }

    def createImpersonationRequest(user) {
        new ImpersonationRequestForValidation().with {
            it.user = user
            return it
        }
    }

    def createBaseUrlRef(String href) {
        new BaseUrlRefForValidation().with {
            it.href = href
            return it
        }
    }

    def createBaseUrlRefList(baseUrlRef) {
        new BaseUrlRefListForValidation().with {
            it.baseURLRef = baseUrlRef
            return it
        }
    }
}
