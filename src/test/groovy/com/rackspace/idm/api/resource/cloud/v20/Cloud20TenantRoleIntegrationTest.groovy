package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import spock.lang.Shared
import testHelpers.RootIntegrationTest


class Cloud20TenantRoleIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId
    @Shared def serviceAdminToken
    @Shared def userAdminToken
    @Shared def identityAdminToken
    @Shared def tenant
    @Shared def service

    def setup() {
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        serviceAdminToken = utils.getServiceAdminToken()
        userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        identityAdminToken = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        tenant = utils.createTenant()
        service = utils.createService()
    }

    def cleanup() {
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteService(service)
    }

    def "deleteRoleFromUserOnTenant returns 204 when trying to delete a tenant role that exists on user"() {
        given:
        def role = utils.createRole(service)
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        when:
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteRole(role)
    }

    def "deleteRoleFromUserOnTenant returns 404 when trying to delete a tenant role that does not exist on user"() {
        given:
        def role = utils.createRole(service)

        when:
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteRole(role)
    }

    def "delete role for user on tenant that does not exist returns error message about role not being found for the given user"() {
        given:
        def role = utils.createRole(service)

        when:
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        then:
        response.status == 404
        def message = response.getEntity(String).toLowerCase()
        message.contains("user")
        message.contains(defaultUser.id)
        message.contains("role")
        message.contains(role.id)

        cleanup:
        utils.deleteRole(role)
    }

}
