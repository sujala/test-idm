package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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

    def "disabled UK global endpoints are not displayed in created user's service catalog"() {
        given:
        staticIdmConfiguration.setProperty("cloud.region", "UK")
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(defaultUser, tenant, MOSSO_ROLE_ID)

        defaultUser.defaultRegion = "LON"
        utils.updateUser(defaultUser)

        def endpointTemplate = utils.createEndpointTemplate(true, null, false, "compute", "LON")
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

        assert !foundEndpoint

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
        staticIdmConfiguration.reset()
    }

    def "disabled US global endpoints are not displayed in created user's service catalog"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def tenant = utils.createTenant()
        utils.addRoleToUserOnTenant(defaultUser, tenant, MOSSO_ROLE_ID)

        defaultUser.defaultRegion = "ORD"
        utils.updateUser(defaultUser)

        def endpointTemplate = utils.createEndpointTemplate(true, null, false)

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

        assert !foundEndpoint

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)
        utils.deleteEndpointTemplate(endpointTemplate)
    }

    @Unroll
    def "#userType manually assigning a global endpoint to user, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def mossoTenantId = domainId
        def nastTenantId = utils.getNastTenant(domainId)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        users = users.reverse()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", testUtils.getRandomUUID("http://public/"), "cloudServers", true, "ORD").with {
            it.global = true
            it
        }
        endpointTemplate = utils.createAndUpdateEndpointTemplate(endpointTemplate, endpointTemplateId)
        def nastEndpoint = endpointTemplate.publicURL + "/" + nastTenantId
        def mossoEndpoint = endpointTemplate.publicURL + "/" + mossoTenantId

        when: "auth as the user admin"
        def authResponse1 = utils.authenticate(userAdmin)

        then: "the user has the mosso endpoint due to it being a global mosso endpoint"
        authResponse1.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == mossoEndpoint}) == 1

        when: "manually assign this endpoint to the mosso tenant"
        def token
        switch(userType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break
        }
        def addEnpdointResponse = cloud20.addEndpoint(token, mossoTenantId, endpointTemplate, accept, request)
        def authResponse2 = utils.authenticate(userAdmin)

        then: "the mosso tenant still only has a single instance of that endpoint"
        addEnpdointResponse.status == 200
        authResponse2.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == mossoEndpoint}) == 1

        when: "manually assign this endpoint to the nast tenant"
        def addEnpdointResponse2 = cloud20.addEndpoint(token, nastTenantId, endpointTemplate, accept, request)
        def authResponse3 = utils.authenticate(userAdmin)

        then: "the nast and mosso tenants now have a single instance of the endpoint"
        addEnpdointResponse2.status == 200
        authResponse3.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == nastEndpoint}) == 1
        authResponse3.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == mossoEndpoint}) == 1

        cleanup:
        utils.deleteUsers(users)
        utils.deleteEndpointTemplate(endpointTemplate)

        where:
        userType                            | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.IDENTITY_ADMIN | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

}
