package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import org.apache.commons.httpclient.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
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
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_TENANT_NAME_TO_BE_CHANGED_VIA_UPDATE_TENANT, useTenantNameFeatureFlag)
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
        assertAuthTenantNameAndId(authWithTokenData, mossoTenant, useTenantNameFeatureFlag)

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
        assertAuthTenantNameAndId(authWithTokenData2, otherTenant, useTenantNameFeatureFlag)

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

    @Unroll
    def "test create tenant with domain ID - accept: #acceptMediaType, request = #requestMediaType"() {
        given: "A domain and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def domain = utils.createDomainEntity()
        tenant.domainId = domain.id

        when: "Creating a new tenant"
        def response = cloud20.addTenant(adminToken, tenant, acceptMediaType, requestMediaType)

        then: "Assert tenant was created with given domain ID"
        response.status == 201
        if(acceptMediaType == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == domain.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == domain.id
        }

        cleanup:
        utils.deleteTenantById(tenantId)
        utils.deleteDomain(domain.id)

        where:
        acceptMediaType                 | requestMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "test create tenant with invalid domain ID - accept: #acceptMediaType, request = #requestMediaType"() {
        given: "A domain and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def invalidDomainId = testUtils.getRandomUUID("invalid")
        tenant.domainId = invalidDomainId

        when: "Creating a new tenant"
        def response = cloud20.addTenant(adminToken, tenant, acceptMediaType, requestMediaType)

        then: "Assert BadRequest"
        String expectedErrMsg = String.format(DefaultCloud20Service.DOMAIN_ID_NOT_FOUND_ERROR_MESSAGE, invalidDomainId)
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, expectedErrMsg)

        where:
        acceptMediaType                 | requestMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Assert domain Id is case insensitive on tenant creation  - accept: #acceptMediaType, request = #requestMediaType"() {
        given: "A domain and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def domain = utils.createDomainEntity()
        tenant.domainId = domain.id.toUpperCase()

        when: "Creating a new tenant"
        def response = cloud20.addTenant(adminToken, tenant, acceptMediaType, requestMediaType)

        then: "Assert tenant was created with given domain ID"
        response.status == 201
        if(acceptMediaType == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == domain.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == domain.id
        }

        where:
        acceptMediaType                 | requestMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Create tenant with disabled domain  - accept: #acceptMediaType, request = #requestMediaType"() {
        given: "A disabled domain and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def domainId = testUtils.getRandomUUID("domain")
        def domain = v2Factory.createDomain(domainId, domainId)
        domain.enabled = false
        utils.createDomain(domain)
        tenant.domainId = domain.id

        when: "Creating a new tenant"
        def response = cloud20.addTenant(adminToken, tenant, acceptMediaType, requestMediaType)

        then: "Assert tenant was created with given domain ID"
        response.status == 201
        if(acceptMediaType == MediaType.APPLICATION_XML_TYPE) {
            def tenantResponse = response.getEntity(Tenant).value
            assert tenantResponse.domainId == domain.id
        } else {
            def tenantResponse = new JsonSlurper().parseText(response.getEntity(String))
            assert tenantResponse['tenant']['RAX-AUTH:domainId'] == domain.id
        }

        where:
        acceptMediaType                 | requestMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Do not allow tenant name to be changed via update tenant - allowUpdate: #allowUpdate" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_TENANT_NAME_TO_BE_CHANGED_VIA_UPDATE_TENANT, allowUpdate)

        def tenantId = UUID.randomUUID().toString().replace("-", "")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def updateTenant = v2Factory.createTenant(tenantId, "name")

        when:
        def addTenant = cloud20.addTenant(utils.getServiceAdminToken(), tenant).getEntity(Tenant).value
        cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, updateTenant)
        def updatedTenant = cloud20.getTenant(utils.getServiceAdminToken(), tenantId).getEntity(Tenant).value
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        then:
        if (allowUpdate) {
            addTenant.name != updatedTenant.name
        } else {
            addTenant.name == updatedTenant.name
        }

        cleanup:
        reloadableConfiguration.reset()

        where:
        allowUpdate << [true, false]
    }

    @Unroll
    def "Create tenant with type can be retrieved and matches: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["type1", "Type1", "TYPE2"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)
        List<String> types = createdTenant.types.type

        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        then: "duplicates removed and stored as lowercase"
        types as Set == ["type1", "type2"] as Set

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "A maximum of 16 unique tenant types can be assigned to any given tenant: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["type1", "type2", "type3", "type4", "type5", "type6",
            "type7", "type8", "type9", "type10", "type11", "type12", "type13", "type14", "type15", "type16", "type17"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_CANNOT_EXCEED_MAXIMUM)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Tenant type can only contain alphanumeric characters: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, [type])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_MUST_BE_ALPHANUMERIC)

        where:
        requestContentType              | acceptContentType               | type
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | "type*"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "type()"
    }

    @Unroll
    def "Tenant type must possess a length > 0 and <= 15: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, [type])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_MUST_BE_CORRECT_SIZE)

        where:
        requestContentType              | acceptContentType               | type
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | ""
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "type567890123456"
    }

    @Unroll
    def "Update tenant with type can be retrieved and matches: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["type1", "Type1", "TYPE2"])
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, ["type3", "Type3", "TYPE4"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)
        def updateTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)

        then: "duplicates removed and stored as lowercase"
        updateTenant.types.type as Set == ["type3", "type4"] as Set
        createdTenant.types.type as Set == ["type3", "type4"] as Set

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update tenant with empty type deletes all tenant types: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["type1", "Type1", "TYPE2"])
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, [])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)
        def updateTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)

        then: "tenant types is empty"
        updateTenant.types == null
        createdTenant.types == null

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update tenant without type does not change tenant types: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["type1", "Type1", "TYPE2"])
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId)

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)
        def updateTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)

        then: "The values are not modified in update tenant"
        updateTenant.types.type as Set == ["type1", "type2"] as Set
        createdTenant.types.type as Set == ["type1", "type2"] as Set

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update type a maximum of 16 unique tenant types can be assigned to any given tenant: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, ["type1", "type2", "type3", "type4", "type5", "type6",
            "type7", "type8", "type9", "type10", "type11", "type12", "type13", "type14", "type15", "type16", "type17"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_CANNOT_EXCEED_MAXIMUM)

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update tenant type can only contain alphanumeric characters: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, [type])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_MUST_BE_ALPHANUMERIC)

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType               | type
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | "type*"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "type()"
    }

    @Unroll
    def "Update tenant type must possess a length > 0 and <= 15: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, [type])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_MUST_BE_CORRECT_SIZE)

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        where:
        requestContentType              | acceptContentType               | type
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | ""
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "type567890123456"
    }

    def getTenant(response) {
        def tenant = response.getEntity(Tenant)

        if (response.getType() == MediaType.APPLICATION_XML_TYPE) {
            tenant = tenant.value
        }
        return tenant
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
