package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse

import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

class TenantTypeRoleWhitelistFilterIntegrationTest extends RootIntegrationTest {

    @Shared def sharedServiceAdminToken
    @Shared def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared def sharedUserAdminToken

    @Shared User sharedSubUser
    @Shared def sharedSubUserToken

    @Shared EndpointTemplate endpointTemplate

    @Autowired
    IdentityUserService identityUserService

    @Shared String tenant_type_x = RandomStringUtils.randomAlphabetic(15).toLowerCase()

    @Shared Tenant tenantX1
    @Shared Tenant tenantX2

    static String TENANT_TYPE = "dumb_tenant_type"
    static String WHITE_LIST_FILTER_PROPERTY = IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + TENANT_TYPE

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a cloud account
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser = cloud20.createSubUser(sharedUserAdminToken)
        authResponse = cloud20.authenticatePassword(sharedSubUser.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUserToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        TenantType tenantTypeX = v2Factory.createTenantType(tenant_type_x, "description")
        assert cloud20.addTenantType(sharedServiceAdminToken, tenantTypeX).status == HttpStatus.SC_CREATED

        // Create a tenant with the newly added tenant type
        def whiteListTenantName = tenant_type_x + ":" + sharedUserAdmin.domainId
        def whitelisttenant =  v2Factory.createTenant(whiteListTenantName, whiteListTenantName, [tenant_type_x]).with {it.domainId = sharedUserAdmin.domainId; it}
        def response = cloud20.addTenant(sharedIdentityAdminToken, whitelisttenant)
        assert response.status == HttpStatus.SC_CREATED
        tenantX1 = response.getEntity(Tenant).value

        // Create a tenant with the newly added tenant type
        whiteListTenantName = tenant_type_x + ":" + sharedUserAdmin.domainId + "_2"
        whitelisttenant =  v2Factory.createTenant(whiteListTenantName, whiteListTenantName, [tenant_type_x]).with {it.domainId = sharedUserAdmin.domainId; it}
        response = cloud20.addTenant(sharedIdentityAdminToken, whitelisttenant)
        assert response.status == HttpStatus.SC_CREATED
        tenantX2 = response.getEntity(Tenant).value

        // Add an endpoint to the tenantX
        def addEndpointToTenantResponse = cloud20.addEndpoint(sharedIdentityAdminToken, tenantX1.id, v1Factory.createEndpointTemplate(Constants.MOSSO_ENDPOINT_TEMPLATE_ID, "unused"))
        assert addEndpointToTenantResponse.status == SC_OK

        def tenantType = new TenantType().with {
            it.name = TENANT_TYPE
            it.description = "description"
            it
        }
        cloud20.addTenantType(sharedServiceAdminToken, tenantType)
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "Auth: Roles on a tenant with a tenant type without a whitelist are always returned"() {
        when: "Tenant doesn't have a whitelist"
        def response = utils.authenticate(sharedUserAdmin, Constants.DEFAULT_PASSWORD, "true")

        then:
        response.user.roles.role.find {it.tenantId == tenantX1.id} != null
        response.user.roles.role.find {it.tenantId == tenantX2.id} != null
    }

    def "Auth: Roles on a tenant with a whitelisted tenant type are returned when user is assigned a whitelist role at the domain level"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        when: "User doesn't have whitelisted role"
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "Neither of the tenants of whitelisted tenant type are returned"
        responseRcn.user.roles.role.find {it.tenantId == tenantX1.id} == null
        responseRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null
        responseNoRcn.user.roles.role.find {it.tenantId == tenantX1.id} == null
        responseNoRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null

        when: "User assigned whitelisted role on domain"
        utils.addRoleToUser(mySubUser, Constants.ROLE_RBAC1_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "Both of the tenants of whitelisted tenant type are returned (and all domain roles) when apply rcn"
        responseRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 3 //default, tenant access, rbac1
        responseRcn.user.roles.role.find {it.tenantId == tenantX1.id && it.id == Constants.ROLE_RBAC1_ID} != null
        responseRcn.user.roles.role.findAll {it.tenantId == tenantX2.id}.size() == 3 //default, tenant access, rbac1
        responseRcn.user.roles.role.find {it.tenantId == tenantX2.id && it.id == Constants.ROLE_RBAC1_ID} != null

        and: "Only the tenant:access role is returned on tenant when not applying rcn roles"
        responseNoRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 1
        responseNoRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.get(0).id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
        responseNoRcn.user.roles.role.findAll {it.tenantId == tenantX2.id}.size() == 1
        responseNoRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.get(0).id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
    }

    def "Auth: Roles on a tenant with a whitelisted tenant type are returned when user is assigned a whitelist role at the tenant level"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        when: "User doesn't have whitelisted role"
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "with rcn doesn't include tenant"
        responseRcn.user.roles.role.find {it.tenantId == tenantX1.id} == null
        responseRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "without rcn doesn't include tenant"
        responseNoRcn.user.roles.role.find {it.tenantId == tenantX1.id} == null
        responseNoRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "with RCN has domain roles assigned to tenant and tenant assigned role only on tenant assigned."
        responseRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 3 //default, tenant access, rbac1
        responseRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "without RCN only has role assigned to tenant and tenant access"
        responseNoRcn.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 2 //tenant access, rbac1
        responseNoRcn.user.roles.role.find {it.tenantId == tenantX2.id} == null
    }

    def "Auth: Service catalog endpoints on a tenant without a whitelisted tenant type are returned"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, "")
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        when: "User doesn't have any explicit role on tenant"
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user still receives endpoints for tenant in service catalog (has tenant access role)"
        responseRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
    }

    def "Auth: Service catalog endpoints on a tenant with a whitelisted tenant type are returned when user is assigned a whitelisted role at the domain level"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        when: "User doesn't have whitelisted role"
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user does not have any endpoints for tenant in service catalog"
        responseRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null

        when: "User assigned non-whitelisted role on domain"
        utils.addRoleToUser(mySubUser, Constants.ROLE_RBAC2_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user still does not have any endpoints for tenant in service catalog"
        responseRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null

        when: "User assigned whitelisted role on domain"
        utils.addRoleToUser(mySubUser, Constants.ROLE_RBAC1_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user has endpoint on tenant in service catalog"
        responseRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
    }

    def "Auth: Service catalog endpoints on a tenant with a whitelisted tenant type are returned when user is assigned a whitelisted role at the tenant level"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        when: "User doesn't have whitelisted role"
        def response = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user does not have any endpoints for tenant in service catalog"
        response.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null

        when: "User assigned non-whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC2_ID)
        response = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user does not have any endpoints for tenant in service catalog"
        response.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} == null

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        response = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        then: "user has endpoint on tenant in service catalog"
        response.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
        responseNoRcn.serviceCatalog.service.find {it.endpoint.find {e -> e.tenantId == tenantX1.id}} != null
    }

    def "Auth w/ Token + Tenant: Roles on a tenant with a whitelisted tenant type are returned when user is assigned a whitelist role at the tenant level"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)

        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)
        def initialToken = utils.getToken(mySubUser.username, Constants.DEFAULT_PASSWORD)

        when: "User doesn't have whitelisted role"
        def responseRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "true")
        def responseNoRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "false")

        then: "Can't authenticate under tenant"
        responseRcn.status == SC_UNAUTHORIZED
        responseNoRcn.status == SC_UNAUTHORIZED

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        responseRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "true")
        responseNoRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "false")

        then: "Can authenticate under tenant"
        responseRcn.status == SC_OK
        responseNoRcn.status == SC_OK

        when: "Change whitelist so user doesn't have access to tenant any more"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, "non-existant-role")
        responseRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "true")
        responseNoRcn = cloud20.authenticateTokenAndTenant(initialToken, tenantX1.id, "false")

        then: "Can't authenticate under tenant"
        responseRcn.status == SC_UNAUTHORIZED
        responseNoRcn.status == SC_UNAUTHORIZED
    }

    @Unroll
    def "List tenants for user: Tenants of a whitelisted tenant type are returned when user is assigned a whitelist role at the tenant level: applyRcn: #applyRcn"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)

        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)
        def initialToken = utils.getToken(mySubUser.username, Constants.DEFAULT_PASSWORD)

        when: "User doesn't have whitelisted role"
        Tenants tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "Whitelisted tenants not returned"
        tenants.tenant.find {it.id == tenantX1.id} == null
        tenants.tenant.find {it.id == tenantX2.id} == null

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "Tenant on which role is assigned is returned"
        tenants.tenant.find {it.id == tenantX1.id} != null
        tenants.tenant.find {it.id == tenantX2.id} == null

        when: "Change whitelist so user doesn't have access to tenant any more"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, "non-existant-role")
        tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "No longer see whitelisted tenants"
        tenants.tenant.find {it.id == tenantX1.id} == null
        tenants.tenant.find {it.id == tenantX2.id} == null

        where:
        applyRcn << [true, false]
    }

    @Unroll
    def "List tenants for user: Tenants of a whitelisted tenant type are returned when user is assigned a whitelist role at the domain level: applyRcn: #applyRcn"() {
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)

        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)
        def initialToken = utils.getToken(mySubUser.username, Constants.DEFAULT_PASSWORD)

        when: "User doesn't have whitelisted role"
        Tenants tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "Whitelisted tenants not returned"
        tenants.tenant.find {it.id == tenantX1.id} == null
        tenants.tenant.find {it.id == tenantX2.id} == null

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUser(mySubUser, Constants.ROLE_RBAC1_ID)
        tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "Tenant on which role is assigned is returned"
        tenants.tenant.find {it.id == tenantX1.id} != null
        tenants.tenant.find {it.id == tenantX2.id} != null

        when: "Change whitelist so user doesn't have access to tenant any more"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, "non-existant-role")
        tenants = utils.listTenantsForToken(initialToken, applyRcn)

        then: "No longer see whitelisted tenants"
        tenants.tenant.find {it.id == tenantX1.id} == null
        tenants.tenant.find {it.id == tenantX2.id} == null

        where:
        applyRcn << [true, false]
    }

    def "Validate token: Roles on a tenant with a whitelisted tenant type are returned when user is assigned a whitelist role at the tenant level"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        // Get tokens
        def tokenRcn = responseRcn.token.id
        def tokenNoRcn = responseNoRcn.token.id

        when: "validate token"
        def validateRcnResponse = utils.validateTokenApplyRcnRoles(tokenRcn, "true")
        def validateNoRcnResponse = utils.validateTokenApplyRcnRoles(tokenNoRcn,"false")
        def validateBelongsToResponse = cloud20.validateTokenApplyRcnRoles(utils.getServiceAdminToken(), tokenNoRcn,"false", tenantX1.id)

        then: "with rcn doesn't include tenant"
        validateRcnResponse.user.roles.role.find {it.tenantId == tenantX1.id} == null
        validateRcnResponse.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "without rcn doesn't include tenant"
        validateNoRcnResponse.user.roles.role.find {it.tenantId == tenantX1.id} == null
        validateNoRcnResponse.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "assert 404 Not Found - belongsTo"
        validateBelongsToResponse.status == SC_NOT_FOUND

        and: "verify roles"
        validateRcnResponse.user.roles.role.size() == responseRcn.user.roles.role.size()
        validateNoRcnResponse.user.roles.role.size() == responseNoRcn.user.roles.role.size()

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        utils.addRoleToUserOnTenantId(mySubUser, tenantX2.id, Constants.ROLE_RBAC2_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")
        tokenRcn = responseRcn.token.id
        tokenNoRcn = responseNoRcn.token.id
        validateRcnResponse = utils.validateTokenApplyRcnRoles(tokenRcn)
        validateNoRcnResponse = utils.validateTokenApplyRcnRoles(tokenNoRcn,"false")
        validateBelongsToResponse = utils.validateTokenApplyRcnRoles(tokenNoRcn,"false", tenantX1.id)

        then: "with RCN has domain roles assigned to tenant and tenant assigned role only on tenant assigned."
        validateRcnResponse.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 3 //default, tenant access, rbac1
        validateRcnResponse.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "without RCN only has role assigned to tenant and tenant access"
        validateNoRcnResponse.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 2 //tenant access, rbac1
        validateNoRcnResponse.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "without RCN only has role assigned to tenant and tenant access - belongsTo"
        validateBelongsToResponse.user.roles.role.findAll {it.tenantId == tenantX1.id}.size() == 2 //tenant access, rbac1
        validateBelongsToResponse.user.roles.role.find {it.tenantId == tenantX2.id} == null

        and: "verify roles"
        validateRcnResponse.user.roles.role.size() == responseRcn.user.roles.role.size()
        validateNoRcnResponse.user.roles.role.size() == responseNoRcn.user.roles.role.size()
        validateBelongsToResponse.user.roles.role.size() == responseNoRcn.user.roles.role.size()
    }

    def "Include all roles when tenant type is not specified on whitelist"() {
        given:
        String tenantTypeName = RandomStringUtils.randomAlphabetic(15).toLowerCase()
        TenantType tenantType = v2Factory.createTenantType(tenantTypeName, "description")
        assert cloud20.addTenantType(sharedServiceAdminToken, tenantType).status == HttpStatus.SC_CREATED

        // Create a tenant with the newly added tenant type
        def tenantName = tenantTypeName + ":" + sharedUserAdmin.domainId
        def tenant = v2Factory.createTenant(tenantName, tenantName, [tenantTypeName]).with {it.domainId = sharedUserAdmin.domainId; it}
        def response = cloud20.addTenant(sharedIdentityAdminToken, tenant)
        assert response.status == HttpStatus.SC_CREATED
        def tenantEntity = response.getEntity(Tenant).value

        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)

        // add tenant role
        utils.addRoleToUserOnTenantId(mySubUser, tenantEntity.id, Constants.ROLE_RBAC1_ID)

        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        // Get tokens
        def tokenRcn = responseRcn.token.id
        def tokenNoRcn = responseNoRcn.token.id

        when: "validate token"
        def validateRcnResponse = utils.validateTokenApplyRcnRoles(tokenRcn, "true")
        def validateNoRcnResponse = utils.validateTokenApplyRcnRoles(tokenNoRcn,"false")
        def validateBelongsToResponse = utils.validateTokenApplyRcnRoles(tokenNoRcn,"false", tenant.id)

        then: "with RCN has domain roles assigned to tenant and tenant assigned role only on tenant assigned."
        validateRcnResponse.user.roles.role.findAll {it.tenantId == tenant.id}.size() == 3 //default, tenant access, rbac1

        and: "without RCN only has role assigned to tenant and tenant access"
        validateNoRcnResponse.user.roles.role.findAll {it.tenantId == tenant.id}.size() == 2 //tenant access, rbac1

        and: "without RCN only has role assigned to tenant and tenant access - belongsTo"
        validateBelongsToResponse.user.roles.role.findAll {it.tenantId == tenant.id}.size() == 2 //tenant access, rbac1

        and: "verify roles"
        validateRcnResponse.user.roles.role.size() == responseRcn.user.roles.role.size()
        validateNoRcnResponse.user.roles.role.size() == responseNoRcn.user.roles.role.size()
        validateBelongsToResponse.user.roles.role.size() == responseNoRcn.user.roles.role.size()

        when: "check token"
        def checkTokenRcnResponse = cloud20.checkTokenApplyRcnRoles(sharedServiceAdminToken, tokenRcn, "true")
        def checkTokenNoRcnResponse = cloud20.checkTokenApplyRcnRoles(sharedServiceAdminToken, tokenNoRcn,"false")
        def checkTokenBelongsToResponse = cloud20.checkTokenApplyRcnRoles(sharedServiceAdminToken, tokenNoRcn,"false", tenant.id)

        then: "asset 200 OK"
        checkTokenRcnResponse.status == SC_OK
        checkTokenNoRcnResponse.status == SC_OK
        checkTokenBelongsToResponse.status == SC_OK
    }

    def "Check token: Roles on a tenant with a whitelisted tenant type are returned when user is assigned a whitelist role at the tenant level"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, Constants.ROLE_RBAC1_NAME)
        def mySubUser = cloud20.createSubUser(sharedUserAdminToken)
        def responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        def responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")

        // Get tokens
        def serviceAdminToken = utils.getServiceAdminToken()
        def tokenRcn = responseRcn.token.id
        def tokenNoRcn = responseNoRcn.token.id

        when: "validate token"
        def checkTokenRcnResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenRcn, "true")
        def checkTokenNoRcnResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false")
        def checkTokenBelongsToResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false", tenantX1.id)
        def checkTokenBelongsTo2Response = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false", tenantX2.id)

        then: "assert 200 OK "
        checkTokenRcnResponse.status == SC_OK
        checkTokenNoRcnResponse.status == SC_OK

        and: "assert 404 Not Found - belongsTo"
        checkTokenBelongsToResponse.status == SC_NOT_FOUND
        checkTokenBelongsTo2Response.status == SC_NOT_FOUND

        when: "User assigned whitelisted role on tenant"
        utils.addRoleToUserOnTenantId(mySubUser, tenantX1.id, Constants.ROLE_RBAC1_ID)
        utils.addRoleToUserOnTenantId(mySubUser, tenantX2.id, Constants.ROLE_RBAC2_ID)
        responseRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "true")
        responseNoRcn = utils.authenticate(mySubUser, Constants.DEFAULT_PASSWORD, "false")
        tokenRcn = responseRcn.token.id
        tokenNoRcn = responseNoRcn.token.id
        checkTokenRcnResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenRcn, "true")
        checkTokenNoRcnResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false")
        checkTokenBelongsToResponse = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false", tenantX1.id)
        checkTokenBelongsTo2Response = cloud20.checkTokenApplyRcnRoles(serviceAdminToken, tokenNoRcn,"false", tenantX2.id)

        then: "asset 200 OK"
        checkTokenRcnResponse.status == SC_OK
        checkTokenNoRcnResponse.status == SC_OK
        checkTokenBelongsToResponse.status == SC_OK

        and: "assert 404 Not Found - belongsTo"
        checkTokenBelongsTo2Response.status == SC_NOT_FOUND // tenantX2 is hidden
    }

    def "Tenant type whitelist filter for roles assigned at a domain level"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        utils.addTenantToDomain(domainId, tenant.id)

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Tenant type whitelist filter for roles assigned at a tenant level"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        utils.addRoleToUserOnTenantId(userAdmin, tenant.id, Constants.ROLE_RBAC1_ID)

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Tenant type whitelist filter for roles granted to user by tenantName"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        def userGroup = utils.createUserGroup(domainId)
        utils.addTenantToDomain(domainId, tenant.id)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.grantRoleAssignmentsOnUser(userAdmin, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenantName]))

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Tenant type whitelist filter for roles granted to user by '*' tenant"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        def userGroup = utils.createUserGroup(domainId)
        utils.addTenantToDomain(domainId, tenant.id)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.grantRoleAssignmentsOnUser(userAdmin, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, ['*']))

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Tenant type whitelist filter for roles granted to user group by tenantName"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        def userGroup = utils.createUserGroup(domainId)
        utils.addTenantToDomain(domainId, tenant.id)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenantName]))

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Tenant type whitelist filter for roles granted to user group by '*' tenant"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        def userGroup = utils.createUserGroup(domainId)
        utils.addTenantToDomain(domainId, tenant.id)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, ['*']))

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.clearProperty(WHITE_LIST_FILTER_PROPERTY)
        def response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment = response.getEntity(RoleAssignments)
        roleAssignment.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type has the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "Verify the role is returned with the appropriate tenants"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment2 = response.getEntity(RoleAssignments)
        roleAssignment2.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() > 0

        when: "Tenant type does not have the role in the whitelist filter"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        response = cloud20.listUserEffectiveRolesWithSources(sharedServiceAdminToken, userAdmin.id)

        then: "role with tenant type matching white list is not returned"
        response.status == HttpServletResponse.SC_OK
        RoleAssignments roleAssignment3 = response.getEntity(RoleAssignments)
        roleAssignment3.getTenantAssignments().tenantAssignment.findAll { it.forTenants.contains(tenant.name)}.size() ==  0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }
}
