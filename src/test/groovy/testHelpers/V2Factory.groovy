package testHelpers

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl
import org.openstack.docs.identity.api.v2.*

import javax.xml.bind.JAXBElement

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
    private static DISPLAY = "displayName"
    private static EMAIL = "email@example.com"
    private static NAME = "name"
    private static DESCRIPTION = "description"
    private static V1Factory v1Factory = new V1Factory()

    def createAuthenticationRequest() {
        return createAuthenticationRequest("tenantId", "tenantName", null, null)
    }

    def createAuthenticationRequest(String tenantId, String tenantName, credential, any) {
        new AuthenticationRequest().with {
            it.tenantId = tenantId
            it.tenantName = tenantName
            it.credential = credential
            it.any = any
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

    def createRole() {
        return createRole(NAME, "serviceId", "tenantId")
    }

    def createRole(String name, String serviceId, String tenantId) {
        new Role().with {
            it.name = name
            it.serviceId = serviceId
            it.tenantId = tenantId
            return it
        }
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
        return createTenant(ID, NAME, DISPLAY, DESCRIPTION, true)
    }

    def createTenant(String id, String name, String displayName, String description, boolean enabled) {
        new Tenant().with {
            it.id = id ? id : ID
            it.description = description ? description : DESCRIPTION
            it.displayName = displayName ? displayName : DISPLAY
            it.name = name ? name : NAME
            it.enabled = enabled
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

    def createToken(String id) {
        new Token().with {
            it.id = id ? id : ID
            it.expires = new XMLGregorianCalendarImpl()
            it.tenant = new TenantForAuthenticateResponse()
            return it
        }
    }

    def createUser() {
        return createUser(ID, USERNAME, DISPLAY, EMAIL, true)
    }

    def createUser(String id, String username, String displayName, String email, boolean enabled) {
        new User().with {
            it.id = id ? id : ID
            it.username = username ? username : USERNAME
            it.displayName = displayName ? displayName : DISPLAY
            it.email = email ? email : EMAIL
            it.enabled
            return it
        }
    }

    def createUserForAuthenticateResponse() {
        return createUserForAuthenticateResponse(ID, NAME, null)
    }

    def createUserForAuthenticateResponse(String id, String name, RoleList roleList) {
        def RoleList list = roleList ? roleList : new RoleList()
        new UserForAuthenticateResponse().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
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

}
