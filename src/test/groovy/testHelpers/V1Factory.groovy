package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.unboundid.ldif.LDIFEntrySource
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList

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
            it.id = id ? id : ID
            it.name = name ? name : NAME
            it.description = description ? description : DESCRIPTION
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
        return createEndpointTemplate(ID, NAME, true, true)
    }

    def createEndpointTemplate(String id, String name, boolean enabled, boolean global) {
        new EndpointTemplate().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
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

    def createImpersonationResponse() {
        return createImpersonationResponse("token")
    }

    def createImpersonationResponse(String token) {
        new ImpersonationResponse().with {
            it.token = token ? token : "token"
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
        return createRole(ID, NAME, DESCRIPTION, null)
    }

    def createRole(String id, String name, String description, String tenantId) {
        new Role().with {
            it.id = id ? id : ID
            it.name = name ? name : NAME
            it.description = description ? description : DESCRIPTION
            it.serviceId = "serviceId"
            it.tenantId = tenantId ? tenantId : "tenantId"
            return it
        }
    }

    def createRoleList() {
        return createRoleList(null)
    }

    def createRoleList(List<Role> roleList) {
        def list = createRoleList() ? roleList : [].asList()
        new RoleList().with {
            it.getRole().addAll(list)
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
            it.password = password ? password : PASSWORD
            it.displayName = displayName ? displayName : DISPLAY
            it.email = email ? email : EMAIL
            it.enabled = enabled
            return it
        }
    }
}
