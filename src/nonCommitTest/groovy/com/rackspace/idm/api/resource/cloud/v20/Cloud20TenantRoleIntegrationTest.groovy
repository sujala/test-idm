package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import org.apache.http.HttpStatus
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.*
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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

    def "delete role for user on tenant that is assigned to two tenants leaves role assigned to other tenant"() {
        given:
        def role = utils.createRole(service)
        def tenantA = tenant
        def tenantB = utils.createTenant()
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id, role.id)
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantB.id, defaultUser.id, role.id)

        when: "delete role from user on TenantA"
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id, role.id)

        then: "role is still assigned to user on TenantB"
        response.status == 204
        def roles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenantB.id, defaultUser.id).getEntity(RoleList).value
        roles.role.size() == 1

        cleanup:
        utils.deleteTenant(tenantB)
        utils.deleteRole(role)
    }

    def "delete role for user on tenant that is only assigned to one tenant deletes the role from the user"() {
        given:
        def role = utils.createRole(service)
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        when:
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id, role.id)

        then:
        response.status == 204
        def roles = cloud20.listUserGlobalRoles(serviceAdminToken, defaultUser.id).getEntity(RoleList).value
        !roles.role.id.contains(role.id)
        def rolesOnTenant = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id).getEntity(RoleList).value
        !rolesOnTenant.role.id.contains(role.id)

        cleanup:
        utils.deleteRole(role)
    }

    def "delete role on tenant from user-admin deletes tenant on role for sub-users if two tenants assigned to role"() {
        given:
        def roleRequest = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }
        def role = cloud20.createRole(serviceAdminToken, roleRequest).getEntity(Role).value
        def tenantA = tenant
        def tenantB = utils.createTenant()
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantA.id, userAdmin.id, role.id)
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantB.id, userAdmin.id, role.id)

        when: "list roles for sub-user on tenantA"
        def subUserRoles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id).getEntity(RoleList).value

        then: "contains propagating role"
        subUserRoles.role.id.contains(role.id)

        when: "remove the tenantA from the role on user-admin"
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenantA.id, userAdmin.id, role.id)
        subUserRoles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id).getEntity(RoleList).value

        then: "removes the tenant from the role on the sub-user"
        response.status == 204
        !subUserRoles.role.id.contains(role.id)

        when: "list roles for sub-user on tenantB"
        subUserRoles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenantB.id, defaultUser.id).getEntity(RoleList).value

        then: "sub-user still has role for tenantB"
        subUserRoles.role.id.contains(role.id)

        cleanup:
        utils.deleteTenant(tenantB)
        utils.deleteRole(role)
    }

    def "delete role on tenant from user admin deletes role from tenant on sub-users if one tenant assigned to role"() {
        given:
        def roleRequest = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }
        def role = cloud20.createRole(serviceAdminToken, roleRequest).getEntity(Role).value


        when: "add propagating role to user admin"
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, userAdmin.id, role.id)

        then: "adds role to sub-users"
        def subUserRoles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id).getEntity(RoleList).value
        subUserRoles.role.id.contains(role.id)

        when: "remove the tenant from the role on user-admin"
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, userAdmin.id, role.id)
        subUserRoles = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenant.id, defaultUser.id).getEntity(RoleList).value
        def subUserGlobalRoles = cloud20.listUserGlobalRoles(serviceAdminToken, defaultUser.id).getEntity(RoleList).value

        then: "removes the tenant from the role on the sub-user"
        response.status == 204
        !subUserRoles.role.id.contains(role.id)
        !subUserGlobalRoles.role.id.contains(role.id)

        cleanup:
        utils.deleteRole(role)
    }

    def "delete role on user with invalid tenant ID does not delete the role on the user"() {
        given:
        def role = utils.createRole(service)
        def tenantA = tenant
        def tenantB = utils.createTenant()
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id, role.id)

        when: "try to delete the role with a tenant ID not associated with that role"
        def response = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenantB.id, defaultUser.id, role.id)

        then:
        response.status == 404

        when: "list the user roles on the correct tenant"
        def userRolesOnTenant = cloud20.listRolesForUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id).getEntity(RoleList).value

        then: "the user still has the role on that tenant"
        userRolesOnTenant.role.id.contains(role.id)

        cleanup:
        utils.deleteTenant(tenantA)
        utils.deleteTenant(tenantB)
        utils.deleteRole(role)
    }

    @Unroll
    def "test deleting identity user-type roles from a user"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "try to delete the identity:admin role from the user"
        def response = cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), Constants.IDENTITY_ADMIN_ROLE_ID, identityAdmin.id)

        then: "forbidden"
        response.status == 403
        assert response.getEntity(IdentityFault).value.message == DefaultCloud20Service.ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE

        when: "try to delete the identity:user-admin role from the user"
        response = cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), Constants.USER_ADMIN_ROLE_ID, userAdmin.id)

        then: "forbidden"
        response.status == 403
        assert response.getEntity(IdentityFault).value.message == DefaultCloud20Service.ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE

        when: "try to delete the identity:user-manage role from the user"
        response = cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), Constants.USER_MANAGE_ROLE_ID, userManage.id)

        then: "success"
        response.status == 204

        when: "try to delete the identity:default role from the user"
        response = cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), Constants.DEFAULT_USER_ROLE_ID, defaultUser.id)

        then: "forbidden"
        response.status == 403
        assert response.getEntity(IdentityFault).value.message == DefaultCloud20Service.ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userManage, userAdmin, identityAdmin].asList())
    }

    def "users without precedence to delete user-type role do not see user-type role error message"() {
        when:
        def response = cloud20.deleteApplicationRoleFromUser(utils.getIdentityAdminToken(), Constants.SERVICE_ADMIN_ROLE_ID, Constants.SERVICE_ADMIN_ID)

        then:
        response.status == 403
        response.getEntity(IdentityFault).value.message == DefaultAuthorizationService.NOT_AUTHORIZED_MSG
    }

    def "Allow deleting RCN roles on user"() {
        given: "A new user admin"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "Create new RCN role"
        def role = v2Factory.createRole(false, RoleAssignmentEnum.GLOBAL, RoleTypeEnum.RCN, ["*"]).with {
            it.serviceId = Constants.SERVERS_SERVICE_ID
            it.otherAttributes = null
            it
        }
        def createRoleResponse = cloud20.createRole(utils.getIdentityAdminToken(), role)
        def roleEntity = createRoleResponse.getEntity(Role).value

        then: "Assert new created RCN role"
        createRoleResponse.status == HttpStatus.SC_CREATED

        when: "Add global RCN role to user"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, roleEntity.id)

        then: "Assert role added to user"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "List user's global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, null, true)
        def globalRoles = userGlobalRolesResponse.getEntity(RoleList).value

        then: "Assert RCN role exist"
        globalRoles.role.find({it.id == roleEntity.id}) != null

        when: "Delete global RNC role"
        def deleteRoleFromUserResponse = cloud20.deleteApplicationRoleFromUser(utils.getIdentityAdminToken(), roleEntity.id, userAdmin.id)

        then:
        deleteRoleFromUserResponse.status == HttpStatus.SC_NO_CONTENT

        when:
        userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, null, false)
        globalRoles = userGlobalRolesResponse.getEntity(RoleList).value

        then:
        globalRoles.role.find({it.id == roleEntity.id}) == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteRole(roleEntity)
    }

    def "delete a role assigned to provisioned user"() {
        given:
        def role = utils.createRole(service)
        def tenantA = tenant
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenantA.id, defaultUser.id, role.id)

        when: "caller cannot delete a role assigned to provisioned user"
        def response = cloud20.deleteRole(serviceAdminToken, role.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        when: "delete role which is not assigned to any user"
        utils.deleteTenant(tenantA)
        def response1 = cloud20.deleteRole(serviceAdminToken, role.id)

        then:
        response1.status == 204
    }

    @Unroll
    def "attempt to add global role to user with an existing tenant role - mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        Role role = utils.createRole()
        Tenant tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(userAdmin, tenant, role.id)

        when: "add global role"
        def response = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, role.id, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultCloud20Service.ERROR_CANNOT_ADD_GLOBAL_ROLE_TO_USER_ERROR_MESSAGE)

        when: "delete tenant role and add global role"
        utils.deleteRoleFromUserOnTenant(userAdmin, tenant, role.id)
        response = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, role.id)

        then:
        response.getStatus() == HttpStatus.SC_OK

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
        utils.deleteTenantQuietly(tenant.id)
        utils.deleteRoleQuietly(role)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "attempt to add tenant role to user with an existing global role - mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        Role role = utils.createRole()
        utils.addRoleToUser(userAdmin, role.id)
        Tenant tenant = utils.createTenant()

        when: "add tenant role"
        def response = cloud20.addRoleToUserOnTenant(utils.getIdentityAdminToken(), tenant.id, userAdmin.id, role.id, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultCloud20Service.ERROR_CANNOT_ADD_ROLE_TO_USER_ON_TENANT_ERROR_MESSAGE)

        when: "delete global role and add tenant role"
        cloudFeedsMock.reset()
        utils.deleteRoleOnUser(userAdmin, role.id)
        response = cloud20.addRoleToUserOnTenant(utils.getIdentityAdminToken(), tenant.id, userAdmin.id, role.id, mediaType, mediaType)

        then:
        response.getStatus() == HttpStatus.SC_OK

        and: "verify that the events were posted"
        // Two event are expected: 1. deleteRole 2. addRole
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE.value()),
                VerificationTimes.exactly(2)
        )

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
        utils.deleteTenantQuietly(tenant.id)
        utils.deleteRoleQuietly(role)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}
