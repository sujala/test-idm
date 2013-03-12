package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl
import org.openstack.docs.identity.api.v2.*

import javax.xml.namespace.QName

import static com.rackspace.idm.RaxAuthConstants.*

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
class V2Factory {

    private static ID = "id"
    private static USERNAME = "username"
    private static NAME = "name"
    private static objFactory = new org.openstack.docs.identity.api.v2.ObjectFactory()

    def createAuthenticationRequest() {
        return createAuthenticationRequest("tenantId", "tenantName", null)
    }

    def createAuthenticationRequest(String tenantId, String tenantName, Object credential) {
        new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.credential = credential
            return it
        }
    }

    def createAuthenticationRequest(String tokenId, String tenantId, String tenantName) {
        def token = new TokenForAuthenticationRequest().with {
            it.id = tokenId
            return it
        }

        return new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.token = token
            return it
        }

    }

    def createAuthenticateResponse() {
        return createAuthenticateResponse(createToken(), null, null)
    }

    def createAuthenticateResponse(Token token, ServiceCatalog serviceCatalog, UserForAuthenticateResponse user) {
        new AuthenticateResponse().with {
            it.token = token ? token : createToken()
            it.serviceCatalog = serviceCatalog ? serviceCatalog : new ServiceCatalog()
            it.user = user ? user : new UserForAuthenticateResponse()
            return it
        }
    }

    def createEndpoint() {
        return createEndpoint(1, "tenantId", NAME, "region")
    }

    def createEndpoint(int id, String tenantId, String name, String region) {
        new Endpoint().with {
            it.id = id
            it.tenantId = tenantId
            it.name = name
            it.region = region
            return it
        }
    }

    def createEndpointList() {
        return createEndpointList(null)
    }

    def createEndpointList(List<Endpoint> endpoints) {
        def list = endpoints ? endpoints : [].asList()
        new EndpointList().with {
            it.getEndpoint().addAll(list)
            return it
        }
    }

    def createPasswordCredentialsRequiredUsername() {
        return createPasswordCredentialsRequiredUsername("username", "Password1")
    }

    def createPasswordCredentialsRequiredUsername(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def createJAXBPasswordCredentialsRequiredUsername(String username, String password) {
        def credential = createPasswordCredentialsRequiredUsername(username, password)
        return objFactory.createPasswordCredentials(credential)
    }


    def createJAXBApiKeyCredentials(String username, String apiKey){
        def credential = createApiKeyCredentials(username, apiKey)
        return objFactory.createCredential(credential)
    }

    def createApiKeyCredentials(String username, String apiKey){
        return new ApiKeyCredentials().with {
            it.apiKey = apiKey
            it.username = username
            return it
        }
    }

    def createJAXBRsaCredentials(String username, String tokenKey){
        def credential = createRsaCredentials(username, tokenKey)
        return objFactory.createCredential(credential)
    }

    def createRsaCredentials(String username, String tokenKey){
        return new RsaCredentials().with {
            it.tokenKey = tokenKey
            it.username = username
            return it
        }
    }

    def createRole() {
        return createRole(NAME, "applicationId", "tenantId")
    }

    def createRole(String name, String serviceId, String tenantId) {
        new Role().with {
            it.name = name
            it.serviceId = serviceId
            it.tenantId = tenantId
            return it
        }
    }

    def createRole(propagate, weight) {
        def other = createOtherMap(propagate, weight)
        return new Role().with {
            it.name = "name"
            it.description = "desc"
            it.otherAttributes = other
            return it
        }
    }

    def createOtherMap(propagate, weight) {
        def map = new HashMap<QName, Object>()
        if (propagate != null) {
            map.put(QNAME_PROPAGATE, Boolean.toString(propagate))
        }
        if (weight != null) {
            map.put(QNAME_WEIGHT, Integer.toString(weight))
        }
        return map
    }

    def createRoleList() {
        return createRoleList(null)
    }

    def createRoleList(List<Role> roleList) {
        def list = roleList ? roleList : [].asList()
        new RoleList().with {
            it.getRole().addAll(list)
            return it
        }
    }

    def createServiceCatalog() {
        return createServiceCatalog(null)
    }

    def createServiceCatalog(List<ServiceForCatalog> services) {
        def list = services ? services : [].asList()
        new ServiceCatalog().with {
            it.getService().addAll(list)
        }
    }
    def createTenant() {
        return createTenant(ID, NAME)
    }

    def createTenant(String id, String name) {
        new Tenant().with {
            it.id = id
            it.name = name
            it.enabled = true
            return it
        }
    }

    def createTenants() {
        return createTenants(null)
    }

    def createTenants(List<Tenant> tenantList) {
        def list = tenantList ? tenantList : [].asList
        new Tenants().with {
            it.getTenant().addAll(list)
            return it
        }
    }

    def createToken() {
        return createToken(ID)
    }

    def createTokenForAuthenticationRequest(){
        return createTokenForAuthenticationRequest(ID)
    }

    def createTokenForAuthenticationRequest(id){
        return new TokenForAuthenticationRequest().with {
            it.id = id
            return it
        }
    }

    def createToken(String id) {
        new Token().with {
            it.id = id ? id : ID
            it.expires = new XMLGregorianCalendarImpl()
            it.tenant = new TenantForAuthenticateResponse()
            return it
        }
    }

    def createUser() {
        return createUser(ID, USERNAME)
    }

    def createUser(String id, String username) {
        new User().with {
            it.id = id
            it.username = username
            it.enabled = true
            return it
        }
    }

    def createUserForAuthenticateResponse() {
        return createUserForAuthenticateResponse(ID, NAME, null)
    }

    def createUserForAuthenticateResponse(String id, String name, RoleList roleList) {
        def RoleList list = roleList ? roleList : new RoleList()
        new UserForAuthenticateResponse().with {
            it.id = id
            it.name = name
            it.roles = list
            return it
        }
    }

    def createUserList() {
        return createUserList(null)
    }

    def createUserList(List<User> userList) {
        def list = userList ? userList : [].asList()
        new UserList().with {
            it.getUser().addAll(list)
            return it
        }
    }

    def createVersionForService(){
        return createVersionForService(1,"info","list")
    }

    def createVersionForService(int id, String info, String list){
        new VersionForService().with {
            it.id = id
            it.info = info
            it.list = list
            return it
        }
    }
}
