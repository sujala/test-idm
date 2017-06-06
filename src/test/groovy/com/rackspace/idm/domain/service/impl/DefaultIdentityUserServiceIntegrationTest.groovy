package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.domain.service.TenantService
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class DefaultIdentityUserServiceIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass())

    @Autowired DefaultIdentityUserService identityUserService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao saRepository

    @Autowired DefaultUserService userService

    @Autowired DomainService domainService

    @Autowired TenantService tenantService

    @Autowired
    ApplicationRoleDao applicationRoleDao

    def setup() {
        reloadableConfiguration.reset()
    }

    def "Retrieving service catalog info uses cached roles"() {
        given:

        // Without performant catalog, doesn't matter what cache role feature is set to
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        // Create user to test with
        def domainId = utils.createDomain()
        def user, users1
        (user, users1) = utils.createUserAdmin(domainId)
        def userEntity = userService.getUserById(user.id)

        def originalRole = utils.createRole()
        utils.addRoleToUser(user, originalRole.id)

        when: "Retrieve catalog"
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfo(userEntity)


        then: "Use has role"
        scInfo.userTenantRoles.find {it.name == originalRole.name} != null

        when: "Change role and auth again"
        ClientRole updatedRole = applicationRoleDao.getClientRole(originalRole.id)
        updatedRole.setName("update_" + RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(updatedRole)
        scInfo = identityUserService.getServiceCatalogInfo(userEntity)

        then:
        // The role name should be the old value as the client role was cached during initial auth
        assert scInfo.userTenantRoles.find {it.name == originalRole.name} != null
        assert scInfo.userTenantRoles.find {it.name == updatedRole.name} == null

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteRoleQuietly(originalRole)
    }

    /**
     * The goal of this test is:
     * <ol>
     *     <li>To verify global roles (those not assigned to a user on a tenant) are properly denormalized by getting
     * assigned to all tenants within the domain. For example, the identity user type role identity:user-admin</li>
     *     <li>To verify roles already assigned to a tenant are not assigned to other tenants
     * </ol>
     * @return
     */
    def "getServiceCatalogInfoApplyRcnRoles - Assigns non-RCN global roles to local tenants on cloud account"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        org.openstack.docs.identity.api.v2.User userAdmin = utils.createCloudAccount(utils.getIdentityAdminToken())
        def userEntity = userService.getUserById(userAdmin.id)
        def tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), userEntity.domainId).getEntity(Tenants).value
        def cloudTenant = tenants.tenant.find {
            it.id == userEntity.domainId
        }
        def filesTenant = tenants.tenant.find() {
            it.id != userEntity.domainId
        }

        when:
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity)

        then: "All roles are tenant based roles"
        scInfo.userTenantRoles.size() == 4 // user-admin, compute:default, object-store:default, tenant-access
        scInfo.userTenantRoles.each {
            assert it.getTenantIds().size() > 0
        }

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo.userTenantRoles, cloudTenant, filesTenant)
    }

    /**
     * This test verifies that an RCN role with specific tenant type will be applied to tenants within a user's own domain
     */
    def "getServiceCatalogInfoApplyRcnRoles - Assigns RCN roles to local tenants on cloud account"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        org.openstack.docs.identity.api.v2.User userAdmin = utils.createCloudAccount(utils.getIdentityAdminToken())
        def userEntity = userService.getUserById(userAdmin.id)
        def tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), userEntity.domainId).getEntity(Tenants).value
        def cloudTenant = tenants.tenant.find {
            it.id == userEntity.domainId
        }
        def filesTenant = tenants.tenant.find() {
            it.id != userEntity.domainId
        }

        //add rcn-cloud role to user to give user role on all cloud tenants
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)

        when:
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity)

        then: "All roles are tenant based roles"
        scInfo.userTenantRoles.each {
            assert it.getTenantIds().size() > 0
        }

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo.userTenantRoles, cloudTenant, filesTenant)

        and: "user gets role on cloud tenant only"
        def cloudRcnAssignment = scInfo.userTenantRoles.find {
            it.roleRsId == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        }
        assert cloudRcnAssignment != null
        assert cloudRcnAssignment.getTenantIds().size() == 1
        assert cloudRcnAssignment.getTenantIds().find() {it == cloudTenant.id} != null
    }

    /**
     * This test verifies that the "all" tenant type RCN role will be applied to all tenants within a user's own domain
     */
    def "getServiceCatalogInfoApplyRcnRoles - Assign multiple RCN roles to local tenants on cloud account"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        org.openstack.docs.identity.api.v2.User userAdmin = utils.createCloudAccount(utils.getIdentityAdminToken())
        def userEntity = userService.getUserById(userAdmin.id)
        def tenants = cloud20.getDomainTenants(utils.getIdentityAdminToken(), userEntity.domainId).getEntity(Tenants).value
        def cloudTenant = tenants.tenant.find {
            it.id == userEntity.domainId
        }
        def filesTenant = tenants.tenant.find() {
            it.id != userEntity.domainId
        }

        //add rcn-cloud role to user to give user role on all cloud tenants
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID)

        when:
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity)

        then: "All roles are tenant based roles"
        scInfo.userTenantRoles.each {
            assert it.getTenantIds().size() > 0
        }

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo.userTenantRoles, cloudTenant, filesTenant)

        and: "user gets 'cloud' rcn role on cloud tenant only"
        def cloudRcnAssignment = scInfo.userTenantRoles.find {
            it.roleRsId == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        }
        assert cloudRcnAssignment != null
        assert cloudRcnAssignment.getTenantIds().size() == 1
        assert cloudRcnAssignment.getTenantIds().find() {it == cloudTenant.id} != null

        and: "user gets 'all' rcn role on cloud and files tenant"
        def allRcnAssignment = scInfo.userTenantRoles.find {
            it.roleRsId == Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID
        }
        assert allRcnAssignment != null
        assert allRcnAssignment.getTenantIds().size() == 2
        assert allRcnAssignment.getTenantIds().find() {it == cloudTenant.id} != null
        assert allRcnAssignment.getTenantIds().find() {it == filesTenant.id} != null
    }

    /**
     * This tests verifies that RCN roles are applied across domains within the same RCN. It also verifies the inferred
     * logic for tenants that do not have a tenant type explicitly set
     */
    def "getServiceCatalogInfoApplyRcnRoles - Assigns RCN roles to all matching tenants within RCN"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        org.openstack.docs.identity.api.v2.User userAdmin1 = utils.createCloudAccount(utils.getIdentityAdminToken())
        org.openstack.docs.identity.api.v2.User userAdmin2 = utils.createCloudAccount(utils.getIdentityAdminToken())

        def userEntity1 = userService.getUserById(userAdmin1.id)
        def tenants1 = cloud20.getDomainTenants(utils.getIdentityAdminToken(), userAdmin1.domainId).getEntity(Tenants).value
        Tenant cloudTenant1 = tenants1.tenant.find {
            it.id == userAdmin1.domainId
        }
        Tenant filesTenant1 = tenants1.tenant.find() {
            it.id != userAdmin1.domainId
        }

        def userEntity2 = userService.getUserById(userAdmin2.id)
        def tenants2 = cloud20.getDomainTenants(utils.getIdentityAdminToken(), userAdmin2.domainId).getEntity(Tenants).value
        Tenant cloudTenant2 = tenants2.tenant.find {
            it.id == userAdmin2.domainId
        }
        Tenant filesTenant2 = tenants2.tenant.find() {
            it.id != userAdmin2.domainId
        }

        // Verify testing state.
        assert cloudTenant1.types.type.size == 1
        assert cloudTenant1.types.type[0] == Constants.TENANT_TYPE_CLOUD
        assert filesTenant1.types.type.size == 1
        assert filesTenant1.types.type[0] == Constants.TENANT_TYPE_FILES
        assert cloudTenant2.types.type.size == 1
        assert cloudTenant2.types.type[0] == Constants.TENANT_TYPE_CLOUD
        assert filesTenant2.types.type.size == 1
        assert filesTenant2.types.type[0] == Constants.TENANT_TYPE_FILES

        //update domains to have same RCN
        def rsRCN = RandomStringUtils.randomAlphanumeric(30)
        Domain domain1 = domainService.getDomain(userAdmin1.domainId)
        domain1.setRackspaceCustomerNumber(rsRCN)
        domainService.updateDomain(domain1)

        Domain domain2 = domainService.getDomain(userAdmin2.domainId)
        domain2.setRackspaceCustomerNumber(rsRCN)
        domainService.updateDomain(domain2)

        //add rcn-cloud role to user to give user role on all cloud tenants
        utils.addRoleToUser(userAdmin1, Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID)

        when: "auth without tenant types set on tenants"
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity1)

        then: "All roles are tenant based roles"
        scInfo.userTenantRoles.each {
            assert it.getTenantIds().size() > 0
        }

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo.userTenantRoles, cloudTenant1, filesTenant1)

        and: "rcn role granted on cloud tenant in both domain"
        def cloudRcnAssignment = scInfo.userTenantRoles.find {
            it.roleRsId == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        }
        assert cloudRcnAssignment != null
        assert cloudRcnAssignment.getTenantIds().size() == 2
        assert cloudRcnAssignment.getTenantIds().find() {it == cloudTenant1.id} != null
        assert cloudRcnAssignment.getTenantIds().find() {it == cloudTenant2.id} != null
        assert cloudRcnAssignment.getTenantIds().find() {it == filesTenant1.id} == null
        assert cloudRcnAssignment.getTenantIds().find() {it == filesTenant2.id} == null

        and: "rcn role not granted on files tenant in either domain"
        assert cloudRcnAssignment.getTenantIds().find() {it == filesTenant1.id} == null
        assert cloudRcnAssignment.getTenantIds().find() {it == filesTenant2.id} == null

        when: "assign explicit cloud tenant type to all tenants"
        setTenantType(cloudTenant1, GlobalConstants.TENANT_TYPE_CLOUD)
        setTenantType(cloudTenant2, GlobalConstants.TENANT_TYPE_CLOUD)
        setTenantType(filesTenant1, GlobalConstants.TENANT_TYPE_FILES)
        setTenantType(filesTenant2, GlobalConstants.TENANT_TYPE_FILES)
        ServiceCatalogInfo scInfo2 = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity1)

        then: "All roles are tenant based roles"
        scInfo2.userTenantRoles.each {
            assert it.getTenantIds().size() > 0
        }

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo2.userTenantRoles, cloudTenant1, filesTenant1)

        and: "standard cloud roles appropriately applied"
        assertApplyRcnStandardCloudAccount(scInfo.userTenantRoles, cloudTenant1, filesTenant1)

        and: "rcn role granted on cloud tenant in both domain"
        def cloudRcnAssignment2 = scInfo.userTenantRoles.find {
            it.roleRsId == Constants.IDENTITY_RCN_CLOUD_TENANT_ROLE_ID
        }
        assert cloudRcnAssignment2 != null
        assert cloudRcnAssignment2.getTenantIds().size() == 2
        assert cloudRcnAssignment2.getTenantIds().find() {it == cloudTenant1.id} != null
        assert cloudRcnAssignment2.getTenantIds().find() {it == cloudTenant2.id} != null
        assert cloudRcnAssignment2.getTenantIds().find() {it == filesTenant1.id} == null
        assert cloudRcnAssignment2.getTenantIds().find() {it == filesTenant2.id} == null

        and: "rcn role not granted on files tenant in either domain"
        assert cloudRcnAssignment2.getTenantIds().find() {it == filesTenant1.id} == null
        assert cloudRcnAssignment2.getTenantIds().find() {it == filesTenant2.id} == null
    }

    def setTenantType(Tenant tenant, String type) {
        Types types = new Types()
        types.type.add(type)
        tenant.setTypes(types)
        cloud20.updateTenant(utils.getIdentityAdminToken(), tenant.id, tenant)
    }

    /**
     * This shows an edge case. The result of applying RCN role logic to a user that belongs to a domain without any
     * tenants. When applied all roles are denormalized into tenant assigned roles, meaning no global roles will be
     * returned. In this case, since no tenants exist, the user will not have any roles - not even an identity user
     * classification role.
     *
     * @return
     */
    def "getServiceCatalogInfoApplyRcnRoles - User in domain with no tenants returns no roles"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin()
        def userEntity = userService.getUserById(userAdmin.id)

        when:
        ServiceCatalogInfo scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(userEntity)

        then: "no roles"
        scInfo.userTenantRoles.size() == 0 // no tenants == no roles

        and: "still has user type identified"
        scInfo.userTypeEnum == IdentityUserTypeEnum.USER_ADMIN
    }

    def void assertApplyRcnStandardCloudAccount(List<TenantRole> tenantRoles, cloudTenant, filesTenant) {
        // User has user-admin role on both tenants
        def userAdminAssignment = tenantRoles.find {
            it.name == IdentityUserTypeEnum.USER_ADMIN.roleName
        }
        assert userAdminAssignment != null
        assert userAdminAssignment.getTenantIds().size() == 2
        assert userAdminAssignment.getTenantIds().find() {it == cloudTenant.id} != null
        assert userAdminAssignment.getTenantIds().find() {it == filesTenant.id} != null

        // "User has compute:default role on only cloud tenant"
        def computeAssignment = tenantRoles.find {
            it.name == GlobalConstants.COMPUTE_DEFAULT_ROLE
        }
        assert computeAssignment != null
        assert computeAssignment.getTenantIds().size() == 1
        assert computeAssignment.getTenantIds().find() {it == cloudTenant.id} != null

        // "User has object-store:default role only on files tenant"
        def filesAssignment = tenantRoles.find {
            it.name == GlobalConstants.FILES_DEFAULT_ROLE
        }
        assert filesAssignment != null
        assert filesAssignment.getTenantIds().size() == 1
        assert filesAssignment.getTenantIds().find() {it == filesTenant.id} != null

        // User has tenant-access role on both tenants
        def accessRoleAssignment = tenantRoles.find {
            it.name == identityConfig.getReloadableConfig().getAutomaticallyAssignUserRoleOnDomainTenantsRoleName()
        }
        assert accessRoleAssignment != null
        assert accessRoleAssignment.getTenantIds().size() == 2
        assert accessRoleAssignment.getTenantIds().find() {it == cloudTenant.id} != null
        assert accessRoleAssignment.getTenantIds().find() {it == filesTenant.id} != null
    }
}
