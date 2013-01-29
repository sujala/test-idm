package testHelpers

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.*
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/23/12
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
class EntityFactory extends Specification {

    private static ID = "id"
    private static CLIENT = "clientId"
    private static NAME = "name"
    private static PASSWORD = "Password1"
    private static USERNAME = "username"

    private static objFactories = new JAXBObjectFactories()

    def createApplication() {
        return createApplication(CLIENT, NAME)
    }

    def createApplication(String clientId, String name) {
        def id = clientId ? clientId : CLIENT
        new Application().with {
            it.clientId = clientId
            it.name = name
            it.enabled = true
            it.uniqueId = "clientId=$id,ou=applications,o=rackspace"
            return it
        }
    }

    def createAuthCredentials() {
        return createAuthCredentials(CLIENT, "clientSecret", USERNAME, PASSWORD, null)
    }

    def createAuthCredentials(String clientId, String clientSecret, String username, String password, String grantType) {
        new AuthCredentials().with {
            it.clientId = clientId
            it.clientSecret = clientSecret
            it.password = password
            it.username = username
            it.grantType = "password"
            it.OAuthGrantType = OAuthGrantType.PASSWORD
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
        return createCapability(ID, NAME)
    }

    def createCapability(String id, String name) {
        new Capability().with {
            it.id = id
            it.name = name
            it.resources = [].asList()
            return it
        }
    }

    def createClientRole() {
        return createClientRole(NAME)
    }

    def createClientRole(String name) {
        new ClientRole().with {
            it.id = ID
            it.name = name ? name : NAME
            it.rsWeight = 500
            return it
        }
    }


    def createCloudBaseUrl() {
        return createCloudBaseUrl("CloudServersOpenStack", "region")
    }

    def createCloudBaseUrl(String openstackType, String region) {
        new CloudBaseUrl().with {
            it.v1Default = true
            it.enabled = true
            it.openstackType = openstackType
            it.region = region
            return it
        }
    }

    def createCloudEndpoint() {
        return createCloudEndpoint(1, "nastId")
    }

    def createCloudEndpoint(Integer mossoId, String nastId) {
        new CloudEndpoint().with {
            it.mossoId = mossoId ? mossoId : 1
            it.nastId = nastId ? nastId : "nastId"
            it.baseUrl = createCloudBaseUrl()
            return it
        }
    }

    def createDomain() {
        return createDomain("domainId")
    }

    def createDomain(String domainId) {
        new Domain().with {
            it.domainId = domainId
            it.enabled = true
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
        return createOpenstackEndpoint("tenantId", "tenantName")
    }

    def createOpenstackEndpoint(String tenantId, String tenantName) {
        new OpenstackEndpoint().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.baseUrls = [].asList()
            return it
        }
    }

    def createDefinedPermission() {
        return createDefinedPermission("title", "type", "value")
    }

    def createDefinedPermission(String title, String type, String value) {
        new DefinedPermission().with {
            it.title = title
            it.permissionType = type
            it.value = value
            it.enabled = true
            return it
        }
    }

    def createPermission() {
        return createPermission("clientId", "customerId", "permissionId")
    }

    def createPermission(String clientId, String customerId, String permissionId) {
        new Permission().with {
            it.clientId = clientId
            it.permissionId = permissionId
            it.customerId = customerId
            return it
        }
    }

    def createPattern(String name, String regex) {
        new Pattern().with {
            it.name = name
            it.regex = regex
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
        return createPolicy("blob")
    }

    def createPolicy(String blob) {
        new Policy().with {
            it.blob = blob
            it.enabled = true
            it.global = true
            return it
        }
    }

    def createQuestion() {
        return createQuestion(ID, "question")
    }

    def createQuestion(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
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
        return createRegion(NAME, "cloud")
    }

    def createRegion(String name, String cloud) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = true
            it.isEnabled = true
            return it
        }
    }

    def createSecretQA() {
        return createSecretQA(ID, "question", "answer")
    }

    def createSecretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
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
        return createTenant(ID, NAME)
    }

    def createTenant(String id, String name) {
        new Tenant().with {
            it.tenantId = id
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createTenantRole() {
        createTenantRole(NAME)
    }

    def createTenantRole(String name) {
        new TenantRole().with {
            it.name = name
            it.tenantIds = []
            return it
        }
    }

    def buildUser() {
        return new UserBuilder()
    }

    def createUser() {
        return createUser("username", "id", "domainId", "region")
    }

    def createUser(String username, String userId, String domainId, String region) {
        def id = userId ? userId : "id"
        new User().with {
            it.username = username
            it.id = id
            it.domainId = domainId
            it.region = region
            it.uniqueId = "rsId=$id,ou=users,o=rackspace"
            it.enabled = true
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
