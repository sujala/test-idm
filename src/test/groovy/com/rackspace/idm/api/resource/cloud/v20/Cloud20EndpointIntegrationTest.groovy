package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

class Cloud20EndpointIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    def "global endpoints are assigned to users"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(defaultUser, tenant, MOSSO_ROLE_ID)

        defaultUser.defaultRegion = "ORD"
        utils.updateUser(defaultUser)

        def endpointTemplate = utils.createEndpointTemplate(true)

        when:
        AuthenticateResponse response = utils.authenticate(defaultUser)

        then:
        String tenantEndpoint = String.format("%s/%s", endpointTemplate.publicURL, tenant.id)
        assert response.serviceCatalog.service.endpoint.publicURL.get(0).contains(tenantEndpoint)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
    }
}
