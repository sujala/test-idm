package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.DomainService
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class TenantIntegrationTest extends RootIntegrationTest {

    @Autowired
    DomainService domainService

    @Autowired
    IdentityConfig identityConfig

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

    def "delete tenant deletes the tenantId off of the domain"() {
        given:
        def tenant = utils.createTenant()
        def domain = utils.createDomain(v2Factory.createDomain(utils.createDomain(), testUtils.getRandomUUID("domainName")))

        when: "add the tenant to the domain"
        utils.addTenantToDomain(domain.id, tenant.id)
        def domainEnttiy = domainService.getDomain(domain.id)

        then: "the tenant ID is on the domain"
        domainEnttiy.tenantIds.find { it == tenant.id } == tenant.id

        when: "delete the tenant"
        utils.deleteTenant(tenant)
        domainEnttiy = domainService.getDomain(domain.id)

        then: "the tenant ID was deleted off of the domain"
        domainEnttiy.tenantIds.find { it == tenant.id } == null

        cleanup:
        utils.deleteDomain(domain.id)
    }

    @Unroll
    def "test get tenant by ID returns domain ID - accept: #accept"() {
        given:
        def domain = utils.createDomainEntity()
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)

        when:
        def response = cloud20.getTenant(utils.getServiceAdminToken(), tenant.id, accept)

        then:
        response.status == 200
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == domain.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == domain.id
        }

        cleanup:
        utils.deleteTenant(tenant)
        utils.deleteDomain(domain.id)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test list tenants for domain returns domain ID on tenants - accept: #accept"() {
        given:
        def domain = utils.createDomainEntity()
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)

        when:
        def response = cloud20.getDomainTenants(utils.getServiceAdminToken(), domain.id, true, accept)

        then:
        response.status == 200
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def tenantsResponse = response.getEntity(Tenants).value
            assert tenantsResponse.tenant.find { it.domainId == domain.id } != null
        } else {
            def tenantsResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantsResponse['tenants'].find { it['RAX-AUTH:domainId'] == domain.id } != null
        }

        cleanup:
        utils.deleteTenant(tenant)
        utils.deleteDomain(domain.id)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test list tenants returns domain ID on tenants - accept: #accept"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def response = cloud20.listTenants(utils.getToken(userAdmin.username), accept)

        then:
        response.status == 200
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def tenantsResponse = response.getEntity(Tenants).value
            assert tenantsResponse.tenant.find { it.domainId == domainId } != null
        } else {
            def tenantsResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantsResponse['tenants'].find { it['RAX-AUTH:domainId'] == domainId } != null
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test create tenant returns domain ID - accept: #accept, request = #request"() {
        when:
        def tenant = v2Factory.createTenant(testUtils.getRandomIntegerString(), testUtils.getRandomUUID("tenant"))
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, accept, request)

        then:
        response.status == 201
        def tenantId
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == identityConfig.getReloadableConfig().getTenantDefaultDomainId()
            tenantId = tenantResponse.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == identityConfig.getReloadableConfig().getTenantDefaultDomainId()
            tenantId = tenantResponse['tenant']['id']
        }

        cleanup:
        utils.deleteTenantById(tenantId)

        where:
        accept | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "test update tenant returns domain ID - accept: #accept, request = #request"() {
        when:
        def domain = utils.createDomainEntity()
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)
        tenant.domainId = null //set domainId to null b/c you cannot set that through the API
        def response = cloud20.updateTenant(utils.getServiceAdminToken(), tenant.id, tenant, accept, request)

        then:
        response.status == 200
        def tenantId
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == domain.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == domain.id
        }

        cleanup:
        utils.deleteTenant(tenant)
        utils.deleteDomain(domain.id)

        where:
        accept | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "updated tenant name is reflected in authenticate and validate token responses, useTenantNameFeatureFlag = #useTenantNameFeatureFlag"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.USE_TENANT_NAME_FOR_AUTH_AND_VALIDATE_RESPONSE_PROP, useTenantNameFeatureFlag)
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdminWithTenants()
        def userAdminToken = utils.getToken(userAdmin.username)
        def mossoTenantId = userAdmin.domainId
        def mossoTenant = utils.updateTenant(mossoTenantId, true, testUtils.getRandomUUID())
        def otherTenantId = testUtils.getRandomInteger()
        def otherTenant = utils.createTenant(otherTenantId)
        otherTenant = utils.updateTenant(otherTenant.id, true, testUtils.getRandomUUID())
        def role = utils.createRole()
        utils.addRoleToUserOnTenant(userAdmin, otherTenant, role.id)
        utils.addApiKeyToUser(userAdmin)

        when: "auth and validate w/ mosso tenant"
        def authWithPasswordResponse = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def authWithApiKeyResponse = cloud20.authenticateApiKey(userAdmin.username, Constants.DEFAULT_API_KEY)
        def authWithTokenResponse = cloud20.authenticateTokenAndTenant(userAdminToken, mossoTenantId)
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), userAdminToken)

        then: "assert on auth responses"
        def authWithPasswordData = authWithPasswordResponse.getEntity(AuthenticateResponse).value
        def authWithApiKeyData = authWithApiKeyResponse.getEntity(AuthenticateResponse).value
        def authWithTokenData = authWithTokenResponse.getEntity(AuthenticateResponse).value
        assertAuthTenantNameAndId(authWithPasswordData, mossoTenant, useTenantNameFeatureFlag)
        assertAuthTenantNameAndId(authWithApiKeyData, mossoTenant, useTenantNameFeatureFlag)
        //NOTE: existing logic for auth w/ token and tenant always returned the correct tenant name in the response
        assertAuthTenantNameAndId(authWithTokenData, mossoTenant, true)

        and: "assert on validate response"
        validateResponse.status == 200
        AuthenticateResponse validateData = validateResponse.getEntity(AuthenticateResponse).value
        assertAuthTenantNameAndId(validateData, mossoTenant, useTenantNameFeatureFlag)

        when: "delete the mosso tenant off the user to fall back to 'numeric tenant ID logic' and auth and validate w/ nast tenant"
        utils.deleteRoleFromUserOnTenant(userAdmin, mossoTenant, Constants.MOSSO_ROLE_ID)
        def authWithPasswordResponse2 = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def authWithApiKeyResponse2 = cloud20.authenticateApiKey(userAdmin.username, Constants.DEFAULT_API_KEY)
        def authWithTokenResponse2 = cloud20.authenticateTokenAndTenant(userAdminToken, otherTenantId)
        def validateResponse2 = cloud20.validateToken(utils.getServiceAdminToken(), userAdminToken)

        then: "assert on auth responses"
        def authWithPasswordData2 = authWithPasswordResponse2.getEntity(AuthenticateResponse).value
        def authWithApiKeyData2 = authWithApiKeyResponse2.getEntity(AuthenticateResponse).value
        def authWithTokenData2 = authWithTokenResponse2.getEntity(AuthenticateResponse).value
        assertAuthTenantNameAndId(authWithPasswordData2, otherTenant, useTenantNameFeatureFlag)
        assertAuthTenantNameAndId(authWithApiKeyData2, otherTenant, useTenantNameFeatureFlag)
        //NOTE: existing logic for auth w/ token and tenant always returned the correct tenant name in the response
        assertAuthTenantNameAndId(authWithTokenData2, otherTenant, true)

        and: "assert on validate response"
        validateResponse.status == 200
        AuthenticateResponse validateData2 = validateResponse2.getEntity(AuthenticateResponse).value
        assertAuthTenantNameAndId(validateData2, otherTenant, useTenantNameFeatureFlag)

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUsers(users)
        utils.deleteRole(role)
        utils.deleteTenant(mossoTenant)
        utils.deleteTenant(otherTenant)

        where:
        useTenantNameFeatureFlag | _
        true                     | _
        false                    | _
    }

    def assertAuthTenantNameAndId(AuthenticateResponse authData, tenant, boolean nameAndIdShouldNotMatch) {
        if (nameAndIdShouldNotMatch) {
            assert authData.token.tenant.name == tenant.name
            assert authData.token.tenant.id == tenant.id
            assert authData.token.tenant.id != authData.token.tenant.name
        } else {
            assert authData.token.tenant.name == tenant.id
            assert authData.token.tenant.id == tenant.id
            assert authData.token.tenant.id == authData.token.tenant.name
        }
        true
    }

}
