package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.MOSSO_ROLE_ID
import static com.rackspace.idm.Constants.DEFAULT_PASSWORD


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

        def foundEndpoint = false

        when:
        AuthenticateResponse response = utils.authenticate(defaultUser)

        then:
        String tenantEndpoint = String.format("%s/%s", endpointTemplate.publicURL, tenant.id)
        for (List publicUrls : response.serviceCatalog.service.endpoint.publicURL) {
             if (publicUrls.contains(tenantEndpoint)) {
                 foundEndpoint = true
             }
        }

        assert foundEndpoint

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
    }

    def "endpoints created with tenantAlias = '' do not display tenant in the url"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(defaultUser, tenant, MOSSO_ROLE_ID)

        defaultUser.defaultRegion = "ORD"
        utils.updateUser(defaultUser)

        def endpointTemplate = utils.createEndpointTemplate(true, "")

        def foundEndpoint = false

        when:
        AuthenticateResponse response = utils.authenticate(defaultUser)

        then:
        for (List publicUrls : response.serviceCatalog.service.endpoint.publicURL) {
            if (publicUrls.contains(endpointTemplate.publicURL)) {
                foundEndpoint = true
            }
        }

        assert foundEndpoint

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
    }

    def "endpoints returned for disabled"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(defaultUser, tenant, MOSSO_ROLE_ID)

        defaultUser.defaultRegion = "ORD"
        utils.updateUser(defaultUser)

        def endpointTemplate = utils.createEndpointTemplate(true)

        def defaultUserToken = utils.getToken(defaultUser.username, DEFAULT_PASSWORD)
        defaultUser.enabled = false
        utils.updateUser(defaultUser)

        def foundEndpoint = false

        when:
        EndpointList response = utils.getEndpointsForToken(defaultUserToken)

        then:
        String tenantEndpoint = String.format("%s/%s", endpointTemplate.publicURL, tenant.id)
        if (response.endpoint.publicURL.contains(tenantEndpoint)) {
            foundEndpoint = true
        }

        assert foundEndpoint

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
    }

}
