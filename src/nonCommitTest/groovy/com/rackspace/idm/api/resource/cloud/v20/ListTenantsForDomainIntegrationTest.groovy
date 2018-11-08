package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.Tenants
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class ListTenantsForDomainIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "List tenants for domain with enabled query parameter - getOnlyUseTenantDomainPointers = #feature"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ONLY_USE_TENANT_DOMAIN_POINTERS_PROP, feature)
        def domain = utils.createDomainEntity()

        // Create an enabled tenant
        def tenant = utils.createTenantInDomain(domain.id)

        // Create a disabled tenant
        def disabledTenant = utils.createTenantInDomain(domain.id)
        utils.updateTenant(disabledTenant.id, false)

        when: "searching enabled tenants"
        def response = cloud20.getDomainTenants(utils.getServiceAdminToken(), domain.id, true)
        def tenants = response.getEntity(Tenants).value

        then:
        response.status == HttpStatus.SC_OK

        tenants.tenant.size() == 1
        tenants.tenant.find { it.id == tenant.id } != null

        when: "searching disabled tenants"
        response = cloud20.getDomainTenants(utils.getServiceAdminToken(), domain.id, false)
        tenants = response.getEntity(Tenants).value

        then:
        response.status == HttpStatus.SC_OK

        tenants.tenant.size() == 1
        tenants.tenant.find { it.id == disabledTenant.id } != null

        when: "searching all tenants"
        response = cloud20.getDomainTenants(utils.getServiceAdminToken(), domain.id, null)
        tenants = response.getEntity(Tenants).value

        then:
        response.status == HttpStatus.SC_OK

        tenants.tenant.size() == 2
        tenants.tenant.find { it.id == tenant.id } != null
        tenants.tenant.find { it.id == disabledTenant.id } != null

        cleanup:
        utils.deleteTenantQuietly(tenant)
        utils.deleteTenantQuietly(disabledTenant)
        utils.deleteTestDomainQuietly(domain)

        where:
        feature << [true, false]
    }

    @Unroll
    def "List tenants for domain with invalid enabled query parameter - getOnlyUseTenantDomainPointers = #feature, enabled = #enabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ONLY_USE_TENANT_DOMAIN_POINTERS_PROP, feature)
        def domain = utils.createDomainEntity()

        // Create an enabled tenant
        def tenant = utils.createTenantInDomain(domain.id)

        // Create a disabled tenant
        def disabledTenant = utils.createTenantInDomain(domain.id)
        utils.updateTenant(disabledTenant.id, false)

        when: "get domain tenants"
        def response = cloud20.getDomainTenants(utils.getServiceAdminToken(), domain.id, enabled)
        def tenants = response.getEntity(Tenants).value

        then:
        response.status == HttpStatus.SC_OK

        tenants.tenant.size() == 2
        tenants.tenant.find { it.id == tenant.id } != null
        tenants.tenant.find { it.id == disabledTenant.id } != null

        cleanup:
        utils.deleteTenantQuietly(tenant)
        utils.deleteTenantQuietly(disabledTenant)
        utils.deleteTestDomainQuietly(domain)

        where:
        feature  | enabled
        true     | "0"
        true     | "1"
        true     | "bad"
        true     | "!@#"
        false    | "0"
        false    | "1"
        false    | "bad"
        false    | "!@#"
    }
}
