package com.rackspace.idm.api.resource.tenant

import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*


class FoundationTenantIntegrationTest extends RootIntegrationTest{


    def "Create/Get tenant" () {
        when:
        def authData = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def tenant = foundationUtils.createTenant(authData.accessToken.id)
        def getTenant = foundationUtils.getTenant(authData.accessToken.id, tenant.id)

        then:
        authData != null
        tenant != null
        getTenant != null
        tenant.displayName == getTenant.displayName

        cleanup:
        utils.deleteTenant(getTenant.id)
    }
}
