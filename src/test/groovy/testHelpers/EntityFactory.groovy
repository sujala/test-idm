package testHelpers

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.*
import com.unboundid.ldap.sdk.ReadOnlyEntry

import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/23/12
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
class EntityFactory {

    private static ID = "id"
    private static CLIENT = "clientId"
    private static NAME = "name"
    private static DESCRIPTION = "description"
    private static DISPLAY = "displayName"
    private static PASSWORD = "Password1"
    private static USERNAME = "username"
    private static EMAIL = "email@example.com"

    JAXBObjectFactories objFactories = new JAXBObjectFactories()

    def createApplication() {
        return createApplication(CLIENT, NAME, DESCRIPTION, true)
    }

    def createApplication(String clientId, String name, String description, boolean enabled) {
        new Application().with {
            it.clientId = clientId ? clientId : CLIENT
            it.name = name ? name : NAME
            it.description = description ? description : DESCRIPTION
            it.enabled = enabled
            it.uniqueId = "clientId=$it.clientId,ou=applications,o=rackspace"
            return it
        }
    }

    def createAuthCredentials() {
        return createAuthCredentials(CLIENT, "clientSecret", USERNAME, PASSWORD, null, null)
    }

    def createAuthCredentials(String clientId, String clientSecret, String username, String password, String grantType, OAuthGrantType oAuthGrantType) {
        new AuthCredentials().with {
            it.clientId = clientId ? clientId : CLIENT
            it.clientSecret = clientSecret ? clientSecret : "clientSecret"
            it.password = password ? password : PASSWORD
            it.username = username ? username : USERNAME
            it.grantType = grantType ? grantType : "PASSWORD"
            it.OAuthGrantType = oAuthGrantType ? oAuthGrantType : OAuthGrantType.PASSWORD
            return it
        }
    }

    def createUserAuthenticationResult() {
        return createUserAuthenticationResult(createUser(), true)
    }

    def createUserAuthenticationResult(User user, boolean authenticated) {
        new UserAuthenticationResult(user, authenticated).with {
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

    def createCapability() {
        def list = ["resource"].asList()
        return createCapability(ID, NAME, "rsId", "action", DESCRIPTION, list)
    }

    def createCapability(String id, String name, String rsId, String action, String description, List<String> resources) {
        def emptyList = [].asList()
        new Capability().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
            it.rsId = rsId ? rsId : "rsId"
            it.action = action ? action : "action"
            it.description = description ? description : DESCRIPTION
            it.resources = resources ? resources : emptyList
            return it
        }
    }

    def createClientRole() {
        return createClientRole(ID, NAME, 500, CLIENT)
    }

    def createClientRole(String id, String name, Integer weight, String clientId) {
        new ClientRole().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
            it.rsWeight = weight ? weight : 500
            it.clientId = clientId ? clientId : CLIENT
            return it
        }
    }


    def createCloudBaseUrl() {
        return createCloudBaseUrl(true, true, "CloudServersOpenStack", "region")
    }

    def createCloudBaseUrl(boolean v1Default, boolean enabled, String openstackType, String region) {
        new CloudBaseUrl().with {
            it.v1Default = v1Default
            it.enabled = enabled
            it.openstackType = openstackType ? openstackType : "CloudServersOpenStack"
            it.region = region ? region : "region"
            return it
        }
    }

    def createCloudEndpoint() {
        return createCloudEndpoint(1, "nastId", null)
    }

    def createCloudEndpoint(Integer mossoId, String nastId, CloudBaseUrl baseUrl) {
        new CloudEndpoint().with {
            it.mossoId = mossoId ? mossoId : 1
            it.nastId = nastId ? nastId : "nastId"
            it.baseUrl = baseUrl ? baseUrl : createCloudBaseUrl()
            return it
        }
    }

    def createDomain() {
        return createDomain(true, "domainId", DESCRIPTION)
    }

    def createDomain(boolean enabled, String domainId, String description) {
        new Domain().with {
            it.enabled = enabled
            it.domainId = domainId ? domainId : "domainId"
            it.description = description ? description : DESCRIPTION
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

    def createOpenstackEndpoint() {
        return createOpenstackEndpoint(null)
    }

    def createOpenstackEndpoint(List<CloudBaseUrl> baseUrls) {
        def baseUrlList = baseUrls ? baseUrls : [].asList()
        new OpenstackEndpoint().with {
            it.tenantId = tenantId ? tenantId : "tenantId"
            it.baseUrls = baseUrlList
            it.tenantName = tenantName ? tenantName : "tenantName"
            return it
        }
    }

    def pattern(String name, String regex, String errMsg, String description){
        new Pattern().with {
            it.name = name
            it.regex = regex
            it.errMsg = errMsg
            it.description = description
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
        return createPolicy("blob", DESCRIPTION, true, true)
    }

    def createPolicy(String blob, String description, boolean enabled, boolean global) {
        new Policy().with {
            it.blob = blob ? blob : "blob"
            it.description = description ? description : DESCRIPTION
            it.enabled = enabled
            it.global = global
            return it
        }
    }

    def createQuestion() {
        return createQuestion(ID, "question")
    }

    def createQuestion(String id, String question) {
        new Question().with {
            it.id = id ? id : ID
            it.question = question ? question : "question"
            return it
        }
    }

    def createJAXBQuestion() {
        return createJAXBQuestion(ID, "question")
    }

    def createJAXBQuestion(String id, String question) {
        objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def createRacker() {
        new Racker().with {
            it.rackerId = rackerId
            return it
        }
    }

    def createRegion() {
        return createRegion(NAME, "cloud", true, true)
    }

    def createRegion(String name, String cloud, boolean isDefault, boolean isEnabled) {
        new Region().with {
            it.name = name ? name : NAME
            it.cloud = cloud ? cloud : "cloud"
            it.isDefault = isDefault
            it.isEnabled = isEnabled
            return it
        }
    }

    def createSecretQA() {
        return createSecretQA(ID, "question", "answer")
    }

    def createSecretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id ? id : ID
            it.question = question ? question : "question"
            it.answer = answer ? answer : "answer"
            return it
        }
    }

    def createSecretQAs() {
        return createSecretQAs(null)
    }

    def createSecretQAs(List<SecretQA> qaList) {
        def list = qaList ? qaList : [].asList()
        new SecretQAs().with {
            it.getSecretqa().addAll(list)
            return it
        }
    }

    def createTenant() {
        return createTenant(ID, NAME, DISPLAY, DESCRIPTION, true)
    }

    def createTenant(String id, String name, String displayName, String description, boolean enabled) {
        new Tenant().with {
            it.tenantId = id ? id : ID
            it.name = name ? name : NAME
            it.displayName = displayName ? displayName : DISPLAY
            it.description = description ? description : DESCRIPTION
            it.enabled = enabled
            return it
        }
    }

    def createTenantRole() {
        createTenantRole("roleRsId", ID, NAME, CLIENT, null)
    }

    def createTenantRole(String roleRsId, String userRsId, String name, String clientId, String[] tenantIds) {
        new TenantRole().with {
            it.userId = userRsId ? userRsId : ID
            it.clientId = clientId ? clientId : CLIENT
            it.name = name ? name : NAME
            it.roleRsId = roleRsId ? roleRsId : "roleRsId"
            it.tenantIds = tenantIds ? tenantIds : []
            return it
        }
    }

    def createUser() {
        return createUser(USERNAME, "displayName", ID, "domainId", EMAIL, PASSWORD, "region", true)
    }

    def createUser(String username, String displayName, String id, String domainId, String email, String password, String region, boolean enabled) {
        new User().with {
            it.username = username ? username : USERNAME
            it.displayName = displayName ? displayName : DISPLAY
            it.id = id ? id : ID
            it.domainId = domainId
            it.email = email ? email : EMAIL
            it.password = password
            it.enabled = enabled
            it.region = region ? region : "region"
            it.uniqueId = "rsId=$it.id,ou=users,o=rackspace"
            return it
        }
    }

    def createUsers() {
        return createUsers(null)
    }

    def createUsers(List<User> userList) {
        def list = userList ? userList : [].asList()
        new Users().with {
            it.setUsers(list)
            return it
        }
    }
}
