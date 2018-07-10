package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_OK

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

        // Create a tenant with for the newly added tenant type
        def whiteListTenantName = tenant_type_x + ":" + sharedUserAdmin.domainId
        def whitelisttenant =  v2Factory.createTenant(whiteListTenantName, whiteListTenantName, [tenant_type_x]).with {it.domainId = sharedUserAdmin.domainId; it}
        def response = cloud20.addTenant(sharedIdentityAdminToken, whitelisttenant)
        assert response.status == HttpStatus.SC_CREATED
        tenantX1 = response.getEntity(Tenant).value

        // Create a tenant with for the newly added tenant type
        whiteListTenantName = tenant_type_x + ":" + sharedUserAdmin.domainId + "_2"
        whitelisttenant =  v2Factory.createTenant(whiteListTenantName, whiteListTenantName, [tenant_type_x]).with {it.domainId = sharedUserAdmin.domainId; it}
        response = cloud20.addTenant(sharedIdentityAdminToken, whitelisttenant)
        assert response.status == HttpStatus.SC_CREATED
        tenantX2 = response.getEntity(Tenant).value

        // Add an endpoint to the tenantX
        def addEndpointToTenantResponse = cloud20.addEndpoint(sharedIdentityAdminToken, tenantX1.id, v1Factory.createEndpointTemplate(Constants.MOSSO_ENDPOINT_TEMPLATE_ID, "unused"))
        assert addEndpointToTenantResponse.status == SC_OK
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

        and: "withour rcn doesn't include tenant"
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
}
