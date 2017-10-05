package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class ListUserGroupRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    @Shared UserGroup sharedUserGroup

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }

        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        sharedUserGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = Constants.ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = Constants.ROLE_RBAC2_ID
                            ta.forTenants.add(sharedUserAdminCloudTenant.id)
                            ta
                    })
                    tas
            }
            it
        }
        def grantResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, sharedUserGroup, assignments)
        assert grantResponse.status == HttpStatus.SC_OK
    }

    @Unroll
    def "Retrieve roles on user group w/o pagination requested; mediaType = #mediaType"() {
        when:
        def listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, sharedUserGroup, null, mediaType)

        then:
        listResponse.status == HttpStatus.SC_OK
        RoleAssignments retrievedEntity = listResponse.getEntity(RoleAssignments)

        and: "Retrieves both roles"
        retrievedEntity.tenantAssignments != null
        def rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == "*"

        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC2_ID}
        rbac2Assignment != null
        rbac2Assignment.forTenants.size() == 1
        rbac2Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        and:
        StringUtils.isBlank(listResponse.headers.getFirst(HttpHeaders.LINK))

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Retrieve roles on user group with pagination requested; mediaType = #mediaType"() {
        when: "Get first page"
        def searchParams = new UserGroupRoleSearchParams(new PaginationParams(0, 1))
        def listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, sharedUserGroup, searchParams, mediaType)

        then: "returns one result"
        listResponse.status == HttpStatus.SC_OK
        RoleAssignments firstPage = listResponse.getEntity(RoleAssignments)
        firstPage.tenantAssignments != null
        firstPage.tenantAssignments.tenantAssignment.size() == 1

        and:
        StringUtils.isNotBlank(listResponse.headers.getFirst("Link"))

        when: "Get second page"
        searchParams = new UserGroupRoleSearchParams(new PaginationParams(1 ,1))
        def listResponse2 = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, sharedUserGroup, searchParams, mediaType)

        then: "returns one result"
        listResponse2.status == HttpStatus.SC_OK
        RoleAssignments secondPage = listResponse2.getEntity(RoleAssignments)
        secondPage.tenantAssignments != null
        secondPage.tenantAssignments.tenantAssignment.size() == 1

        and:
        StringUtils.isNotBlank(listResponse2.headers.getFirst("Link"))

        when: "combine both pages"
        def retrievedRoleAssignments = new ArrayList<TenantAssignment>(2)
        retrievedRoleAssignments.addAll(firstPage.tenantAssignments.tenantAssignment)
        retrievedRoleAssignments.addAll(secondPage.tenantAssignments.tenantAssignment)

        then:
        def rbac1Assignment = retrievedRoleAssignments.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == "*"

        def rbac2Assignment = retrievedRoleAssignments.find {it.onRole == Constants.ROLE_RBAC2_ID}
        rbac2Assignment != null
        rbac2Assignment.forTenants.size() == 1
        rbac2Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Missing domain returns 404; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = UUID.randomUUID()
            it.id = UUID.randomUUID()
            it
        }

        when:
        def response = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, group, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND
                , String.format("Domain with ID %s not found.", group.domainId))

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}
