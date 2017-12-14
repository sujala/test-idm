package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AssignmentTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SourceTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.sun.jersey.api.client.ClientResponse
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class ListUserEffectiveRolesWithSourcesIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "Cloud Account: Receives effective domain roles on vanilla cloud account: media: #media"() {
        given:
        def iaToken = utils.getIdentityAdminToken()
        def userAdmin = utils.createCloudAccount(iaToken)

        // Figure out the tenants
        Tenants tenants = utils.listDomainTenants(userAdmin.domainId)
        def mossoTenant = tenants.tenant.find {it.id == userAdmin.domainId}
        assert mossoTenant != null
        def nastTenant = tenants.tenant.find {it.id != userAdmin.domainId}
        assert nastTenant != null

        when: "Get user's effective roles"
        ClientResponse response = cloud20.listUserEffectiveRolesWithSources(iaToken, userAdmin.id, media)
        response.status == HttpStatus.SC_OK
        RoleAssignments assignments = response.getEntity(RoleAssignments)

        then:
        assignments != null
        def tenantAssignments = assignments.tenantAssignments.tenantAssignment
        tenantAssignments.size() == 4

        and: "Has user-admin role on both mosso/nast tenants"
        def userAdminAssignment = tenantAssignments.find {it.onRoleName == IdentityUserTypeEnum.USER_ADMIN.roleName}
        userAdminAssignment != null
        userAdminAssignment.onRole == Constants.USER_ADMIN_ROLE_ID
        CollectionUtils.isEqualCollection(userAdminAssignment.forTenants, [mossoTenant.id, nastTenant.id])
        userAdminAssignment.sources.source.size() == 1

        and: "Source lists user-admin role appropriately"
        def userAdminSource = userAdminAssignment.sources.source[0]
        userAdminSource.sourceType == SourceTypeEnum.USER
        userAdminSource.sourceId == userAdmin.id
        userAdminSource.assignmentType == AssignmentTypeEnum.DOMAIN
        CollectionUtils.isEqualCollection(userAdminSource.forTenants, [mossoTenant.id, nastTenant.id])

        and: "Has compute:default role on mosso tenant"
        def computeAssignment = tenantAssignments.find {it.onRoleName == Constants.DEFAULT_COMPUTE_ROLE}
        computeAssignment != null
        computeAssignment.onRole == Constants.DEFAULT_COMPUTE_ROLE_ID
        CollectionUtils.isEqualCollection(computeAssignment.forTenants, [mossoTenant.id])
        computeAssignment.sources.source.size() == 1

        and: "Source lists compute:default role appropriately"
        def computeSource = computeAssignment.sources.source[0]
        computeSource.sourceType == SourceTypeEnum.USER
        computeSource.sourceId == userAdmin.id
        computeSource.assignmentType == AssignmentTypeEnum.TENANT
        CollectionUtils.isEqualCollection(computeSource.forTenants, [mossoTenant.id])

        and: "Has object-store:default role on nast tenant"
        def filesAssignment = tenantAssignments.find {it.onRoleName == Constants.DEFAULT_OBJECT_STORE_ROLE}
        filesAssignment != null
        filesAssignment.onRole == Constants.DEFAULT_OBJECT_STORE_ROLE_ID
        CollectionUtils.isEqualCollection(filesAssignment.forTenants, [nastTenant.id])
        filesAssignment.sources.source.size() == 1

        and: "Source lists object-store:default role appropriately"
        def nastSource = filesAssignment.sources.source[0]
        nastSource.sourceType == SourceTypeEnum.USER
        nastSource.sourceId == userAdmin.id
        nastSource.assignmentType == AssignmentTypeEnum.TENANT
        CollectionUtils.isEqualCollection(nastSource.forTenants, [nastTenant.id])

        and: "Has identity:tenant-access role on both mosso and nast tenant"
        def taAssignment = tenantAssignments.find {it.onRoleName == Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME}
        taAssignment != null
        taAssignment.onRole == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
        CollectionUtils.isEqualCollection(taAssignment.forTenants, [mossoTenant.id, nastTenant.id])
        taAssignment.sources.source.size() == 1

        and: "Source lists identity:tenant-access role appropriately"
        def taSource = taAssignment.sources.source[0]
        taSource.sourceType == SourceTypeEnum.SYSTEM
        taSource.sourceId == "IDENTITY"
        taSource.assignmentType == AssignmentTypeEnum.TENANT
        CollectionUtils.isEqualCollection(taSource.forTenants, [mossoTenant.id, nastTenant.id])

        where:
        media << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Generic Account w/ no tenant: An account with no tenant receives an empty array forTenants: #media"() {
        given:
        def iaToken = utils.getIdentityAdminToken()

        // Create account with non-integer string to ensure a cloud account is not created
        def domainId = RandomStringUtils.randomAlphanumeric(10)
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)

        // Verify domain doesn't have any tenants
        def response = cloud20.getDomainTenants(iaToken, userAdmin.domainId)
        assert response.status == HttpStatus.SC_NOT_FOUND

        when: "Get user's effective roles"
        response = cloud20.listUserEffectiveRolesWithSources(iaToken, userAdmin.id, media)
        response.status == HttpStatus.SC_OK
        RoleAssignments assignments = response.getEntity(RoleAssignments)

        then:
        assignments != null
        def tenantAssignments = assignments.tenantAssignments.tenantAssignment
        tenantAssignments.size() == 1

        and: "Has user-admin role on without tenants"
        def userAdminAssignment = tenantAssignments.find {it.onRoleName == IdentityUserTypeEnum.USER_ADMIN.roleName}
        userAdminAssignment != null
        userAdminAssignment.onRole == Constants.USER_ADMIN_ROLE_ID
        userAdminAssignment.forTenants != null
        userAdminAssignment.forTenants.size() == 0

        and: "Source lists user-admin role appropriately"
        userAdminAssignment.sources.source.size() == 1
        def userAdminSource = userAdminAssignment.sources.source[0]
        userAdminSource.sourceType == SourceTypeEnum.USER
        userAdminSource.assignmentType == AssignmentTypeEnum.DOMAIN
        userAdminSource.sourceId == userAdmin.id
        userAdminSource.forTenants != null
        userAdminSource.forTenants.size() == 0

        where:
        media << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    /**
     * This test uses the fact that the base CA image includes the 'identity:rcn-cloud' RCN role that is set to
     * apply to the 'cloud' tenant type and a cloud account will include a 'cloud' tenant and a 'files' tenant
     *
     * @return
     */
    @Unroll
    def "RCN Roles are applied to matching tenants within a domain: media: #media"() {
        given:
        def iaToken = utils.getIdentityAdminToken()

        // Create cloud account to include both files and cloud tenant creation
        def userAdmin = utils.createCloudAccount(iaToken)
        def domainId = userAdmin.domainId

        // Assign the user the identity:rcn-cloud role
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)

        when: "Get user's effective roles"
        def response = cloud20.listUserEffectiveRolesWithSources(iaToken, userAdmin.id, media)
        response.status == HttpStatus.SC_OK
        RoleAssignments assignments = response.getEntity(RoleAssignments)

        then:
        assignments != null
        def tenantAssignments = assignments.tenantAssignments.tenantAssignment
        //user-admin, object:store-default, compute:default, identity:tenant-access, identity:rcn-cloud
        tenantAssignments.size() == 5

        and: "Has rcn role on cloud tenant"
        def rcnAssignment = tenantAssignments.find {it.onRole == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID}
        rcnAssignment != null
        rcnAssignment.onRole == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        rcnAssignment.onRoleName == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_NAME
        rcnAssignment.forTenants != null
        rcnAssignment.forTenants.size() == 1
        rcnAssignment.forTenants[0] == domainId

        and: "Source lists user-admin role appropriately"
        rcnAssignment.sources.source.size() == 1
        def rcnSource = rcnAssignment.sources.source[0]
        rcnSource.sourceType == SourceTypeEnum.USER
        rcnSource.sourceId == userAdmin.id
        rcnSource.assignmentType == AssignmentTypeEnum.RCN
        rcnSource.forTenants != null
        rcnSource.forTenants.size() == 1
        rcnSource.forTenants[0] == domainId

        where:
        media << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    /**
     * This test uses the fact that the base CA image includes the 'identity:rcn-cloud' RCN role that is set to
     * apply to the 'cloud' tenant type and a cloud account will include a 'cloud' tenant and a 'files' tenant
     *
     * @return
     */
    @Unroll
    def "RCN Roles are applied to matching tenants across domains within same RCN: media: #media"() {
        given:
        def iaToken = utils.getIdentityAdminToken()

        def rcn = RandomStringUtils.randomAlphanumeric(10)

        // Create cloud account to include both files and cloud tenant creation
        def userAdmin = utils.createCloudAccount(iaToken)
        def domainId = userAdmin.domainId

        def userAdmin2 = utils.createCloudAccount(iaToken)
        def domainId2 = userAdmin2.domainId

        // switch domains to same RCN
        utils.domainRcnSwitch(domainId, rcn)
        utils.domainRcnSwitch(domainId2, rcn)

        // Assign the user the identity:rcn-cloud role
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)

        when: "Get user's effective roles"
        def response = cloud20.listUserEffectiveRolesWithSources(iaToken, userAdmin.id, media)
        response.status == HttpStatus.SC_OK
        RoleAssignments assignments = response.getEntity(RoleAssignments)

        then:
        assignments != null
        def tenantAssignments = assignments.tenantAssignments.tenantAssignment
        //user-admin, object:store-default, compute:default, identity:tenant-access, identity:rcn-cloud
        tenantAssignments.size() == 5

        and: "Has rcn role on cloud tenant"
        def rcnAssignment = tenantAssignments.find {it.onRole == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID}
        rcnAssignment != null
        rcnAssignment.onRole == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        rcnAssignment.onRoleName == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_NAME
        rcnAssignment.forTenants != null
        rcnAssignment.forTenants.size() == 2
        CollectionUtils.isEqualCollection(rcnAssignment.forTenants, [domainId, domainId2] as Set)

        and: "Source lists user-admin role appropriately"
        rcnAssignment.sources.source.size() == 1
        def rcnSource = rcnAssignment.sources.source[0]
        rcnSource.sourceType == SourceTypeEnum.USER
        rcnSource.sourceId == userAdmin.id
        rcnSource.assignmentType == AssignmentTypeEnum.RCN
        rcnSource.forTenants != null
        rcnSource.forTenants.size() == 2
        CollectionUtils.isEqualCollection(rcnSource.forTenants, [domainId, domainId2] as Set)

        where:
        media << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

}
