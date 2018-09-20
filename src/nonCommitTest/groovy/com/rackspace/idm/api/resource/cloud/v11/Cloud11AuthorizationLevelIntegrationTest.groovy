package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.domain.config.IdmProperty
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.v2.Role
import spock.lang.Shared
import testHelpers.Cloud20Utils
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

class Cloud11AuthorizationLevelIntegrationTest extends RootIntegrationTest {

    @Shared def serviceAdminToken

    def setupSpec() {
        Cloud20Utils cloud20Utils = new Cloud20Utils(cloud20)
        serviceAdminToken = cloud20Utils.getServiceAdminToken()
    }

    /**
     * This test depends on cache having a TTL of 0 seconds since updating a reloadable property. It tests the
     * "GET" services.
     * @return
     */
    def "Validate authorization level settings on v11 get service catalog service" () {
        given:
        def identityAdminUsername = "identityAdmin" + RandomStringUtils.randomAlphabetic(8)
        def identityAdmin = utils.createUser(serviceAdminToken, identityAdminUsername, DEFAULT_PASSWORD)
        def callerBasicAuth = cloud11.getBasicAuth(identityAdminUsername, DEFAULT_PASSWORD)

        def propertyName = "authorization.level.v1_1_get_user_service_catalog"
        def roleName = "identity:v1_1_get_user_service_catalog"
        Role role = getOrCreateRole(roleName)
        def nonExistUserName = "asfdnewriufv"

        when: "Service is unprotected"
        def response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "Caller is authorized"
        response.status == SC_NOT_FOUND

        when: "Set authorization level to 'LEGACY'"
        IdentityProperty prop = utils.createIdentityProperty(propertyName, "LEGACY")
        response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "Caller is authorized"
        response.status == SC_NOT_FOUND

        when: "Set authorization level to 'ROLE'"
        prop.setValue("ROLE")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "Caller is not authorized"
        response.status == SC_UNAUTHORIZED

        when: "Add role to user"
        utils.addRoleToUser(identityAdmin, role.id)
        response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "caller is now authorized"
        response.status == SC_NOT_FOUND

        when: "Set authorization level to 'FORBIDDEN'"
        prop.setValue("FORBIDDEN")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "Caller is not authorized"
        response.status == SC_UNAUTHORIZED

        when: "Set authorization level to 'invalidValue'"
        prop.setValue("invalidValue")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.getServiceCatalog(nonExistUserName, callerBasicAuth)

        then: "Caller is authorized"
        // Uses "LEGACY" logic
        response.status == SC_NOT_FOUND

        cleanup:
        deletePropIfExists(propertyName)
        utils.deleteUserQuietly(identityAdmin)
        utils.deleteRoleQuietly(role)
    }

    /**
     * This test depends on cache having a TTL of 0 seconds since updating a reloadable property. It tests the
     * "POST" services which throw a different exception.
     * @return
     */
    def "Validate authorization level settings on v11 " () {
        given:
        def identityAdminUsername = "identityAdmin" + RandomStringUtils.randomAlphabetic(8)
        def identityAdmin = utils.createUser(serviceAdminToken, identityAdminUsername, DEFAULT_PASSWORD)
        def callerBasicAuth = cloud11.getBasicAuth(identityAdminUsername, DEFAULT_PASSWORD)

        def propertyName = "authorization.level.v1_1_add_endpoint_template"
        def roleName = "identity:v1_1_add_endpoint_template"
        Role role = getOrCreateRole(roleName)

        // Create a baseUrl to try to create. This is invalid so will ultimately fail if caller is authorized to add
        // base url. This is just testing the authorization aspect so don't actually want to create a baseUrl.
        BaseURL baseUrl = new BaseURL().with {
            it.id = 435132
            it.serviceName = "hi"
            it.publicURL = "hi"
            it
        }

        when: "Service is unprotected"
        def response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "Caller is authorized"
        response.status == SC_NOT_FOUND // Service isn't found

        when: "Set authorization level to 'LEGACY'"
        IdentityProperty prop = utils.createIdentityProperty(propertyName, "LEGACY")
        response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "Caller is authorized"
        response.status == SC_NOT_FOUND

        when: "Set authorization level to 'ROLE'"
        prop.setValue("ROLE")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "Caller is not authorized"
        response.status == SC_METHOD_NOT_ALLOWED

        when: "Add role to user"
        utils.addRoleToUser(identityAdmin, role.id)
        response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "caller is now authorized"
        response.status == SC_NOT_FOUND

        when: "Set authorization level to 'FORBIDDEN'"
        prop.setValue("FORBIDDEN")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "Caller is not authorized"
        response.status == SC_METHOD_NOT_ALLOWED

        when: "Set authorization level to 'invalidValue'"
        prop.setValue("invalidValue")
        utils.updateIdentityProperty(prop.getId(), prop)
        response = cloud11.addBaseUrl(baseUrl, callerBasicAuth)

        then: "Caller is authorized"
        // Uses "LEGACY" logic
        response.status == SC_NOT_FOUND

        cleanup:
        deletePropIfExists(propertyName)
        utils.deleteUserQuietly(identityAdmin)
        utils.deleteRoleQuietly(role)
    }

    def deletePropIfExists(def propName) {
        try {
            IdmProperty prop = utils.getIdentityPropertyByName(propName)
            if (prop != null) {
                utils.deleteIdentityProperty(prop.getId())
            }
        } catch (Exception ex) {
            // eat
        }
    }

    def getOrCreateRole(String roleName) {
        Role role = utils.getRoleByName(roleName)
        if (role == null) {
            Role newRole = v2Factory.createRole(roleName).with {
                it.administratorRole = IdentityUserTypeEnum.SERVICE_ADMIN.getRoleName()
                it.assignment = RoleAssignmentEnum.GLOBAL
                it.name = roleName
                it.roleType = RoleTypeEnum.STANDARD
                it
            }
            role = utils.createRole(newRole)
        }
        return role
    }
}
