package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig

import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Endpoint
import org.openstack.docs.identity.api.v2.EndpointList
import org.openstack.docs.identity.api.v2.ServiceCatalog
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.DEFAULT_OBJECT_STORE_ROLE
import static com.rackspace.idm.Constants.MOSSO_ENDPOINT_TEMPLATE_ID
import static com.rackspace.idm.Constants.MOSSO_ENDPOINT_TEMPLATE_ID
import static com.rackspace.idm.Constants.MOSSO_ROLE_ID
import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC1_NAME
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_NAME


class Cloud20EndpointIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser, users
    @Shared def domainId

    @Autowired
    EndpointDao endpointDao;


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
        utils.deleteTenant(tenant)
        utils.deleteDomain(domainId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplate.id.toString())
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
        utils.deleteTenant(tenant)
        utils.deleteDomain(domainId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplate.id.toString())
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
        utils.deleteTenant(tenant)
        utils.deleteDomain(domainId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplate.id.toString())
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
        utils.deleteTenant(tenant)
        utils.deleteDomain(domainId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplate.id.toString())
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
        utils.deleteTenant(nastTenantId)
        utils.deleteTenant(mossoTenantId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)

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

    def "Regionless endpoints do not display 'DEFAULT' region"() {
        given:
        def pubUrl = "http://regionless.test"
        def endpointTemplate = utils.createEndpointTemplate(false, "", true, "compute", null, testUtils.getRandomIntegerString(), pubUrl)
        String endpointTemplateId = String.valueOf(endpointTemplate.id)

        //create a user and explicitly assign regionless endpoint to it.
        def domainId = utils.createDomain()
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addEndpointTemplateToTenant(userAdmin.domainId, Integer.parseInt(endpointTemplateId)) //rely on fact that domainId == mosso tenant id

        when: "get endpoint by id directly from the directory"
        def daoEndpoint = endpointDao.getBaseUrlById(endpointTemplateId)

        then: "region was set to the 'default' value"
        daoEndpoint.region == reloadableConfiguration.getString(IdentityConfig.ENDPOINT_REGIONID_DEFAULT)

        when: "get endpoint by id via service"
        def endpointById = utils.getEndpointTemplate(endpointTemplateId)

        then: "region is null"
        endpointById.region == null

        when: "authenticate as user"
        AuthenticateResponse ar = utils.authenticate(userAdmin)
        ServiceCatalog sc = ar.serviceCatalog

        then: "service catalog contains endpoint and region is null"
        def computeCatalog = sc.service.find() {it.name == "cloudServers"}
        computeCatalog != null
        def regionLessEndpoint = computeCatalog.endpoint.find() {it.publicURL.startsWith(pubUrl)}
        regionLessEndpoint != null
        regionLessEndpoint.region == null

        when: "get endpoints for the token"
        EndpointList tokenEndpoints = utils.getEndpointsForToken(ar.token.id)

        then: "endpoint returned without region"
        Endpoint foundEndpoint = tokenEndpoints.endpoint.find() {it.id == endpointTemplate.id}
        foundEndpoint != null
        foundEndpoint.region == null

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTenant(userAdmin.domainId)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)
    }

    def "explicitly assigning global endpoint to tenant does not cause endpoint to list twice"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def et = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD")
        def endpointTemplate = utils.createEndpointTemplate(et)
        endpointTemplate.enabled = true
        endpointTemplate.global = true
        utils.updateEndpointTemplate(endpointTemplate, endpointTemplate.id + "")
        def tenant = utils.createTenant()

        when: "list endpoints for tenant"
        def endpoints = (EndpointList) utils.listEndpointsForTenant(utils.getServiceAdminToken(), tenant.id)

        then: "only lists the global endpoint once"
        endpoints.endpoint.count({ it -> it.id == endpointTemplate.id }) == 1

        when: "explicitly assign the endpoint to the tenant"
        utils.addEndpointTemplateToTenant(tenant.id, endpointTemplate.id)
        endpoints = (EndpointList) utils.listEndpointsForTenant(utils.getServiceAdminToken(), tenant.id)

        then: "still only lists the endpoint once"
        endpoints.endpoint.count({ it -> it.id == endpointTemplate.id }) == 1

        cleanup:
        utils.deleteTenant(tenant)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)
    }

    def "listEndpointsForToken correctly lists endpoints for DA tokens"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        def userAdmin1 = utils.createCloudAccountWithRcn()
        def domain1 = utils.getDomain(userAdmin1.domainId)
        def userAdmin2 = utils.createCloudAccountWithRcn(utils.getIdentityAdminToken(), testUtils.getRandomInteger(), domain1.rackspaceCustomerNumber)
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdmin2 = utils.createIdentityAdmin()
        utils.domainRcnSwitch(identityAdmin.domainId, domain1.rackspaceCustomerNumber)
        utils.domainRcnSwitch(identityAdmin2.domainId, domain1.rackspaceCustomerNumber)
        def daDomain1UserAdmin2 = utils.createDelegationAgreementWithUserAsDelegate(utils.getToken(userAdmin1.username), userAdmin1.domainId, userAdmin2.id)
        def daDomain1UserAdmin1 = utils.createDelegationAgreementWithUserAsDelegate(utils.getToken(userAdmin1.username), userAdmin1.domainId, userAdmin1.id)
        def daDomain1IdentityAdmin2 = utils.createDelegationAgreementWithUserAsDelegate(utils.getToken(userAdmin1.username), userAdmin1.domainId, identityAdmin2.id)
        def daTokenDomain1UserAdmin2 = utils.getDelegationAgreementToken(userAdmin2.username, daDomain1UserAdmin2.id)
        def daTokenDomain1UserAdmin1 = utils.getDelegationAgreementToken(userAdmin1.username, daDomain1UserAdmin1.id)
        def daTokenDomain1IdentityAdmin2 = utils.getDelegationAgreementToken(identityAdmin2.username, daDomain1IdentityAdmin2.id)

        when: "list endpoints for DA token using same DA token"
        EndpointList endpoints = utils.listEndpointsForToken(daTokenDomain1UserAdmin1, daTokenDomain1UserAdmin1)

        then:
        endpoints.endpoint.find { e -> e.tenantId == userAdmin1.domainId } != null

        when: "list endpoints for user admin pw token using DA token"
        def response = cloud20.listEndpointsForToken(daTokenDomain1UserAdmin2, utils.getToken(userAdmin1.username))

        then:
        response.status == 403

        when: "list endpoints for DA token using user admin's pw token"
        response = cloud20.listEndpointsForToken(utils.getToken(userAdmin1.username), daTokenDomain1UserAdmin2)

        then:
        response.status == 403

        when: "list endpoints for DA token using same user's pw token when DA is for different domain"
        response = cloud20.listEndpointsForToken(utils.getToken(userAdmin2.username), daTokenDomain1UserAdmin2)

        then:
        response.status == 403

        when: "list endpoints for DA token using same user's pw token when DA is for same domain"
        response = cloud20.listEndpointsForToken(utils.getToken(userAdmin1.username), daTokenDomain1UserAdmin1)

        then:
        response.status == 403

        when: "list endpoints for DA token using Identity admin with DA for another identity admin"
        endpoints = utils.listEndpointsForToken(daTokenDomain1IdentityAdmin2, utils.getToken(identityAdmin.username))

        then:
        endpoints.endpoint.find { e -> e.tenantId == userAdmin1.domainId } != null
        endpoints.endpoint.find { e -> e.tenantId == identityAdmin2.domainId } == null

        when: "list endpoints for identity admin using DA token for another identity admin"
        response = cloud20.listEndpointsForToken(daTokenDomain1IdentityAdmin2, utils.getToken(identityAdmin.username))

        then:
        response.status == 403

        when: "list endpoints for DA token using Identity admin with DA for different domain"
        endpoints = utils.listEndpointsForToken(daTokenDomain1UserAdmin2, utils.getToken(identityAdmin.username))

        then:
        endpoints.endpoint.find { e -> e.tenantId == userAdmin1.domainId } != null
        endpoints.endpoint.find { e -> e.tenantId == userAdmin2.domainId } == null
    }


    @Unroll
    def "Verify endpoints for token don't include tenants excluded by whitelist: apply_rcn_roles: #apply_rcn_roles"() {
        String tenant_type_x = RandomStringUtils.randomAlphabetic(15).toLowerCase()
        TenantType tenantTypeX = v2Factory.createTenantType(tenant_type_x, "description")
        assert cloud20.addTenantType(utils.getServiceAdminToken(), tenantTypeX).status == HttpStatus.SC_CREATED

        def ua1 = utils.createGenericUserAdmin()

        // Create 2 tenants with the newly added tenant type in ua1 domain
        def tenantX1 = utils.createTenantInDomainWithTenantType(ua1.domainId, tenant_type_x)
        utils.addEndpointTemplateToTenant(tenantX1.id, Integer.parseInt(MOSSO_ENDPOINT_TEMPLATE_ID))
        def tenantX2 = utils.createTenantInDomainWithTenantType(ua1.domainId, tenant_type_x)
        utils.addEndpointTemplateToTenant(tenantX2.id, Integer.parseInt(MOSSO_ENDPOINT_TEMPLATE_ID))
        def tenantX3 = utils.createTenantInDomainWithTenantType(ua1.domainId, tenant_type_x)
        utils.addEndpointTemplateToTenant(tenantX3.id, Integer.parseInt(MOSSO_ENDPOINT_TEMPLATE_ID))

        def ua1Token = utils.getToken(ua1.username)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(v2Factory.createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas.tenantAssignment.add(v2Factory.createTenantAssignment(ROLE_RBAC1_ID, [tenantX2.id, tenantX3.id]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnUser(ua1, assignments, utils.getIdentityAdminToken())

        when: "Whitelist filter requires Role 1 only"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, ROLE_RBAC1_NAME)
        EndpointList endpointList = utils.listEndpointsForToken(ua1Token, ua1Token, apply_rcn_roles)

        then: "Endpoints returned for x2 and x3"
        endpointList.endpoint.find {it.tenantId == tenantX2.id} != null
        endpointList.endpoint.find {it.tenantId == tenantX3.id} != null

        and: "No endpoints returned on x1"
        endpointList.endpoint.find {it.tenantId == tenantX1.id} == null

        when: "Whitelist filter requires Role 2 only"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, ROLE_RBAC2_NAME)
        endpointList = utils.listEndpointsForToken(ua1Token, ua1Token, apply_rcn_roles)

        then: "Delegate gets endpoints on all tenants"
        endpointList.endpoint.find {it.tenantId == tenantX1.id} != null
        endpointList.endpoint.find {it.tenantId == tenantX2.id} != null
        endpointList.endpoint.find {it.tenantId == tenantX3.id} != null

        when: "Whitelist filter requires Role 2 only"
        reloadableConfiguration.setProperty(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + tenant_type_x, DEFAULT_OBJECT_STORE_ROLE)
        endpointList = utils.listEndpointsForToken(ua1Token, ua1Token, apply_rcn_roles)

        then: "Delegate gets endpoints on no tenants"
        endpointList.endpoint.find {it.tenantId == tenantX1.id} == null
        endpointList.endpoint.find {it.tenantId == tenantX2.id} == null
        endpointList.endpoint.find {it.tenantId == tenantX3.id} == null

        where:
        apply_rcn_roles << [true, false]
    }

    @Unroll
    def "Protect v2.0 Add/Update endpoint template w/ role - serviceAdmin is allowed when feature flag is set to #useRoleForEndpointManagement"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, useRoleForEndpointManagement)
        def serviceAdminToken = utils.getServiceAdminToken()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(serviceAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_CREATED

        when:
        response = cloud20.updateEndpointTemplate(serviceAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_OK

        when:
        response = cloud20.deleteEndpointTemplate(serviceAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        where:
        useRoleForEndpointManagement << [true , false]
    }

    @Unroll
    def "Protect v2.0 Add/Update endpoint template w/ role - userAdmin is not allowed when feature flag is set to #useRoleForEndpointManagement"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, useRoleForEndpointManagement)
        def domainId = utils.createDomain()
        def userAdmin
        (userAdmin) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(userAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when:
        response = cloud20.updateEndpointTemplate(userAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when:
        response = cloud20.deleteEndpointTemplate(userAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)

        where:
        useRoleForEndpointManagement << [true , false]
    }

    def "Protect v2.0 Add/Delete endpoint template w/ role - identityAdmin is allowed when feature flag is off"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, false)
        def identityAdminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(identityAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_CREATED

        when:
        response = cloud20.updateEndpointTemplate(identityAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when:
        response = cloud20.deleteEndpointTemplate(identityAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "Protect v2.0 Add/Update endpoint template w/ role - identityAdmin is not allowed when feature flag is on"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, true)
        def identityAdminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(identityAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when:
        response = cloud20.updateEndpointTemplate(identityAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when:
        response = cloud20.deleteEndpointTemplate(identityAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "Protect v2.0 Add/Update endpoint template w/ role - identityAdmin is allowed when feature flag is on and role is assigned"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, true)
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_RS_ENDPOINT_ADMIN_ROLE_ID)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(identityAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_CREATED

        when:
        response = cloud20.updateEndpointTemplate(identityAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_OK

        when:
        response = cloud20.deleteEndpointTemplate(identityAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        cleanup:
        utils.deleteUser(identityAdmin)
    }

    def "Protect v2.0 Add/Update endpoint template w/ role - userAdmin is allowed when feature flag is on and role is assigned"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USE_ROLE_FOR_ENDPOINT_MANAGEMENT_PROP, true)
        def domainId = utils.createDomain()
        def userAdmin
        (userAdmin) = utils.createUserAdmin(domainId)
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_RS_ENDPOINT_ADMIN_ROLE_ID)
        def userAdminToken = utils.getToken(userAdmin.username)
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplateToCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute")
        def endpointTemplateToUpdate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute").with {
            it.enabled = false
            it
        }

        when:
        def response = cloud20.addEndpointTemplate(userAdminToken, endpointTemplateToCreate)

        then:
        response.status == HttpStatus.SC_CREATED

        when:
        response = cloud20.updateEndpointTemplate(userAdminToken, endpointTemplateId, endpointTemplateToUpdate)

        then:
        response.status == HttpStatus.SC_OK

        when:
        response = cloud20.deleteEndpointTemplate(userAdminToken, endpointTemplateId)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
    }
}
