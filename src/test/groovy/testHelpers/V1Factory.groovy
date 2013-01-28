package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspacecloud.docs.auth.api.v1.User
import com.unboundid.ldif.LDIFEntrySource
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Token

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
class V1Factory {

    private static ID = "id"
    private static NAME = "name"
    private static DESCRIPTION = "description"
    private static USERNAME = "username"
    private static DISPLAY = "displayName"
    private static EMAIL = "email@example.com"
    private static PASSWORD = "Password1"
    private static KEY = "key";
    private static MOSSOID = 10000123

    private static V2Factory v2Factory = new V2Factory()

    def createCapability() {
        return createCapability(ID, NAME)
    }

    def createCapability(String id, String name) {
        new Capability().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
            return it
        }
    }
    def createCapabilities() {
        return createCapabilities(null)
    }

    def createCapabilities(List<Capability> capabilityList) {
        def list = capabilityList ? capabilityList : [].asList()
        new Capabilities().with {
            it.getCapability().addAll(list)
            return it
        }
    }

    def createDomain() {
        return createDomain("id", "name", "description", true)
    }

    def createDomain(String id, String name, String description, boolean enabled) {
        new Domain().with {
            it.id = id
            it.name = name
            it.description = description
            it.enabled = enabled
            return it
        }
    }

    def createDomains() {
        return createDomains(null)
    }

    def createDomains(List<Domain> domainList) {
        def list = domainList ? domainList : [].asList()
        new Domains().with {
            it.getDomain().addAll(list)
            return it
        }
    }

    def createEndpointTemplate() {
        return createEndpointTemplate(1, NAME, true, true)
    }

    def createEndpointTemplate(int id, String name, boolean enabled, boolean global) {
        new EndpointTemplate().with {
            it.id = id
            it.name = name
            it.enabled = enabled
            it.global = global
            return it
        }
    }

    def createEndpointTemplateList() {
        return createEndpointTemplateList(null)
    }

    def createEndpointTemplateList(List<EndpointTemplate> templates) {
        def list = templates ? templates : [].asList()
        new EndpointTemplateList().with {
            it.getEndpointTemplate().addAll(list)
            return it
        }
    }

    def createImpersonationRequest(org.openstack.docs.identity.api.v2.User user) {
        new ImpersonationRequest().with {
            it.user = user
            return it
        }
    }

    def createImpersonationResponse() {
        return createImpersonationResponse(v2Factory.createToken())
    }

    def createImpersonationResponse(Token token) {
        new ImpersonationResponse().with {
            it.token = token ? token : v2Factory.createToken()
            return it
        }
    }

    def createPolicies() {
        return createPolicies(null)
    }

    def createPolicies(List<Policy> policyList) {
        def list = policyList ? policyList : [].asList()
        new Policies().with {
            it.policy = list
            return it
        }
    }

    def createPolicy() {
        return createPolicy(ID, "blob", DESCRIPTION, true, true)
    }

    def createPolicy(String id, String blob, String description, boolean enabled, boolean global) {
        new Policy().with {
            it.id = id ? id : ID
            it.blob = blob ? blob : "blob"
            it.description = description ? description : DESCRIPTION
            it.enabled = enabled
            it.global = global
            return it
        }
    }

    def createRole() {
        return createRole(NAME, "serviceId", "tenantid")
    }

    def createRole(String name, String serviceId, String tenantId) {
        new Role().with {
            it.name = name
            it.description = DESCRIPTION
            it.serviceId = serviceId
            it.tenantId = tenantId
            return it
        }
    }

    def createRsaCredentials() {
        return createRsaCredentials("username", "tokenKey")
    }

    def createRsaCredentials(String username, String tokenKey) {
        new RsaCredentials().with {
            it.username = username
            it.tokenKey = tokenKey
            return it
        }
    }

    def createSecretQA() {
        return createSecretQA("1", "answer", "question")
    }

    def createSecretQA(String id, String answer, String question) {
        new SecretQA().with {
            it.id = id ? id : "1"
            it.answer  = answer
            it.question = question
            return it
        }
    }
    def createService() {
        return createService(ID, NAME, DESCRIPTION)
    }

    def createService(String id, String name, String description) {
        new Service().with {
            it.id = id ? id : ID
            it.description = description ? description : DESCRIPTION
            it.name = name ? name : NAME
            return it
        }
    }

    def createServiceList() {
        return createServiceList(null)
    }

    def createServiceList(List<Service> serviceList) {
        def list = serviceList ? serviceList : [].asList()
        new ServiceList().with {
            it.getService().addAll(list)
            return it
        }
    }

    def createUserForCreate() {
        return createUserForCreate(USERNAME, PASSWORD, DISPLAY, EMAIL, true)
    }

    def createUserForCreate(String username, String password, String displayName, String email, boolean enabled) {
        new UserForCreate().with {
            it.username = username ? username : USERNAME
            it.password = password
            it.displayName = displayName ? displayName : DISPLAY
            it.email = email ? email : EMAIL
            it.enabled = enabled
            return it
        }
    }

    def createUser(){
        return createUser(USERNAME, KEY, MOSSOID)
    }

    def createUser(String id, String key, Integer mossoId){
        new User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.enabled = true
            return it
        }
    }
}
