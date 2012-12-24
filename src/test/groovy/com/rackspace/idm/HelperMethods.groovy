package com.rackspace.idm

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
class HelperMethods {

    JAXBObjectFactories objFactories = new JAXBObjectFactories()

    def createEntityApplication() {
        return createEntityApplication("clientId", "name", "description", true)
    }

    def createEntityApplication(String clientId, String name, String description, boolean enabled) {
        new Application().with {
            it.clientId = clientId ? clientId : "clientId"
            it.name = name ? name : "name"
            it.description = description ? description : "description"
            it.enabled = enabled
            it.uniqueId = "clientId=$it.clientId,ou=applications,o=rackspace"
        }
    }

    def createEntityAuthCredentials() {
        return createEntityAuthCredentials("clientId", "clientSecret", "username", "Password1", null, null)
    }

    def createEntityAuthCredentials(String clientId, String clientSecret, String username, String password, String grantType, OAuthGrantType oAuthGrantType) {
        new AuthCredentials().with {
            it.clientId = clientId ? clientId : "clientId"
            it.clientSecret = clientSecret ? clientSecret : "clientSecret"
            it.password = password ? password : "Password1"
            it.username = username ? username : "username"
            it.grantType = grantType ? grantType : "PASSWORD"
            it.OAuthGrantType = oAuthGrantType ? oAuthGrantType : OAuthGrantType.PASSWORD
        }
    }

    def createEntityCapabilities() {
        return createEntityCapabilities(null)
    }

    def createEntityCapabilities(List<Capability> capabilityList) {
        def list = capabilityList ? capabilityList : [].asList()
        new Capabilities().with {
            it.getCapability().addAll(list)
            return it
        }
    }

    def createEntityCapability() {
        def list = ["resource"].asList()
        return createEntityCapability("id", "name", "rsId", "action", "description", list)
    }

    def createEntityCapability(String id, String name, String rsId, String action, String description, List<String> resources) {
        def emptyList = [].asList()
        new Capability().with {
            it.id = id ? id : "id"
            it.name = name ? name : "name"
            it.rsId = rsId ? rsId : "rsId"
            it.action = action ? action : "action"
            it.description = description ? description : "description"
            it.resources = resources ? resources : emptyList
            return it
        }
    }

    def createEntityClientRole() {
        return createEntityClientRole("id", "name", 500, "clientId")
    }

    def createEntityClientRole(String id, String name, Integer weight, String clientId) {
        new ClientRole().with {
            it.id = id ? id : "id"
            it.name = name ? name : "name"
            it.rsWeight = weight ? weight : 500
            it.clientId = clientId ? clientId : "clientId"
            return it
        }
    }


    def createEntityCloudBaseUrl() {
        return createEntityCloudBaseUrl(true, true, "CloudServersOpenStack", "region")
    }

    def createEntityCloudBaseUrl(boolean v1Default, boolean enabled, String openstackType, String region) {
        new CloudBaseUrl().with {
            it.v1Default = v1Default
            it.enabled = enabled
            it.openstackType = openstackType ? openstackType : "CloudServersOpenStack"
            it.region = region ? region : "region"
            return it
        }
    }

    def createEntityCloudEndpoint() {
        return createEntityCloudEndpoint(1, "nastId", null)
    }

    def createEntityCloudEndpoint(Integer mossoId, String nastId, CloudBaseUrl baseUrl) {
        new CloudEndpoint().with {
            it.mossoId = mossoId ? mossoId : 1
            it.nastId = nastId ? nastId : "nastId"
            it.baseUrl = baseUrl ? baseUrl : createEntityCloudBaseUrl()
            return it
        }
    }

    def createEntityDomain() {
        return createEntityDomain(true, "domainid", "description")
    }

    def createEntityDomain(boolean enabled, String domainId, String description) {
        new Domain().with {
            it.enabled = enabled
            it.domainId = domainId ? domainId : "domainid"
            it.description = description ? description : "description"
            return it
        }
    }

    def createEntityUser() {
        return createEntityUser("username", "displayName", "id", "domainId", "example@email.com", "Password1", true)
    }

    def createEntityUser(String username, String displayName, String id, String domainId, String email, String password, boolean enabled) {
        new User().with {
            it.username = username ? username : "username"
            it.displayName = displayName ? displayName : "displayName"
            it.id = id ? id : "id"
            it.domainId = domainId ? domainId : "domainId"
            it.email = email ? email : "example@email.com"
            it.password = password ? password : "Password1"
            it.enabled = enabled
            it.uniqueId = "rsId=$it.id,ou=users,o=rackspace"
            return it
        }
    }


    def createTenantRole() {
        createTenantRole("roleRsId", "id", "name", "clientId", null)
    }

    def createTenantRole(String roleRsId, String id, String name, String clientId, String[] tenantIds) {
        new TenantRole().with {
            it.userId = id ? id : "id"
            it.clientId = clientId ? clientId : "clientId"
            it.name = name ? name : "name"
            it.roleRsId = roleRsId ? roleRsId : "roleRsId"
            it.tenantIds = tenantIds ? tenantIds : []
            return it
        }
    }

    def createEntityRegion() {
        return new Region()
    }

    def createEntityRegion(String name, String cloud, boolean isDefault, boolean isEnabled) {
        new Region().with {
            it.name = name ? name : "name"
            it.cloud = cloud ? cloud : "cloud"
            it.isDefault = isDefault
            it.isEnabled = isEnabled
        }
    }

    def createEntityQuestion() {
        return createEntityQuestion("id", "question")
    }

    def createEntityQuestion(String id, String question) {
        new Question().with {
            it.id = id ? id : "id"
            it.question = question ? question : "question"
            return it
        }
    }

    def createJAXBQuestion() {
        return createJAXBQuestion("id", "question")
    }

    def createJAXBQuestion(String id, String question) {
        objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion().with {
            it.id = id
            it.question = question
            return it
        }
    }

    /*
        SCOPEACCESS
    */
    def createClientScopeAccess() {
        return createClientScopeAccess("clientId", "tokenString", new Date())
    }

    def createClientScopeAccess(String clientId, String tokenString, Date tokenExp) {
        new ClientScopeAccess().with {
            it.clientId = clientId ? clientId : "clientId"
            it.accessTokenString = tokenString ? tokenString : "tokenString"
            it.accessTokenExp = tokenExp ? tokenExp : new Date()
            it.getLDAPEntry().DN = "accessToken=$it.accessTokenString,cn=TOKENS,clientId=$it.clientId"
            return it
        }
    }

    def createUserScopeAccess() {
        return createUserScopeAccess("tokenString", new Date(), "userRsId", "clientId")
    }

    def createUserScopeAccess(String tokenString, Date tokenExp, String userRsId, String clientId) {
        new UserScopeAccess().with {
            it.accessTokenString = tokenString ? tokenString : "tokenString"
            it.accessTokenExp = tokenExp ? tokenExp : new Date()
            it.userRsId = userRsId ? userRsId : "userRsId"
            it.clientId = clientId ? clientId : "clientId"
            it.getLDAPEntry().DN = "accessToken=$it.accessTokenString,cn=TOKENS,rsId=$it.userRsId,ou=users"
            return it
        }
    }
}
