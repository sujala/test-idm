package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.Tenant
import testHelpers.RootIntegrationTest

class TenantIntegrationTest extends RootIntegrationTest {

    def "create tenant limits tenant name to 64 characters"() {
        given:
        def tenantName63 = testUtils.getRandomUUIDOfLength("tenant", 63)
        def tenantName64 = testUtils.getRandomUUIDOfLength("tenant", 64)
        def tenantName65 = testUtils.getRandomUUIDOfLength("tenant", 65)
        def tenant63 = v2Factory.createTenant(tenantName63, tenantName63, true)
        def tenant64 = v2Factory.createTenant(tenantName64, tenantName64, true)
        def tenant65 = v2Factory.createTenant(tenantName65, tenantName65, true)

        when: "create tenant with name length < max name length"
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant63)

        then:
        response.status == 201
        def createdTenant63 = response.getEntity(Tenant).value
        createdTenant63.name == tenantName63

        when: "create tenant with name length == max name length"
        response = cloud20.addTenant(utils.getServiceAdminToken(), tenant64)

        then:
        response.status == 201
        def createdTenant64 = response.getEntity(Tenant).value
        createdTenant64.name == tenantName64

        when: "create tenant with name length > max name length"
        response = cloud20.addTenant(utils.getServiceAdminToken(), tenant65)

        then:
        response.status == 400

        cleanup:
        utils.deleteTenant(createdTenant63)
        utils.deleteTenant(createdTenant64)
    }

    def "update tenant limits tenant name to 64 characters"() {
        given:
        def tenant = utils.createTenant()
        def tenantName63 = testUtils.getRandomUUIDOfLength("tenant", 63)
        def tenantName64 = testUtils.getRandomUUIDOfLength("tenant", 64)
        def tenantName65 = testUtils.getRandomUUIDOfLength("tenant", 65)
        def tenant63 = v2Factory.createTenant(tenantName63, tenantName63, true)
        def tenant64 = v2Factory.createTenant(tenantName64, tenantName64, true)
        def tenant65 = v2Factory.createTenant(tenantName65, tenantName65, true)

        when: "update tenant with name length < max name length"
        def response = cloud20.updateTenant(utils.getServiceAdminToken(), tenant.id, tenant63)

        then:
        response.status == 200
        def createdTenant63 = response.getEntity(Tenant).value
        createdTenant63.name == tenantName63

        when: "update tenant with name length == max name length"
        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenant.id, tenant64)

        then:
        response.status == 200
        def createdTenant64 = response.getEntity(Tenant).value
        createdTenant64.name == tenantName64

        when: "update tenant with name length > max name length"
        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenant.id, tenant65)

        then:
        response.status == 400

        cleanup:
        utils.deleteTenant(tenant)
    }
}
