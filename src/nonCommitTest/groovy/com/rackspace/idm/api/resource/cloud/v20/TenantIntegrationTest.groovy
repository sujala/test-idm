package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
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
        def tenants = getTenantsFromResponse(response)
        assert tenants.tenant.find { it.domainId == domainId } != null

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
    def "Do not allow tenant name to be changed via update tenant" () {
        given:
        def tenantId = UUID.randomUUID().toString().replace("-", "")
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def updateTenant = v2Factory.createTenant(tenantId, "name")

        when:
        def addTenant = cloud20.addTenant(utils.getServiceAdminToken(), tenant).getEntity(Tenant).value
        cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, updateTenant)
        def updatedTenant = cloud20.getTenant(utils.getServiceAdminToken(), tenantId).getEntity(Tenant).value
        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        then:
        addTenant.name == updatedTenant.name

        cleanup:
        reloadableConfiguration.reset()
    }

    @Unroll
    def "Create tenant with type can be retrieved and matches: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "Cloud", "FILES"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)
        List<String> types = createdTenant.types.type

        cloud20.deleteTenant(utils.getServiceAdminToken(), addTenant.id)

        then: "duplicates removed and stored as lowercase"
        types as Set == ["cloud", "files"] as Set

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
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "files", "faws", "rcn", "type5", "type6",
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

    def "Update tenant with type can be retrieved and matches: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "Cloud", "FILES"])
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, ["faws", "Faws", "RCN"])

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)
        def updateTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)

        then: "duplicates removed and stored as lowercase"
        updateTenant.types.type as Set == ["faws", "rcn"] as Set
        createdTenant.types.type as Set == ["faws", "rcn"] as Set

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
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "Cloud", "FILES"])
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
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "Cloud", "FILES"])
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId)

        when:
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def addTenant = getTenant(response)

        response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)
        def updateTenant = getTenant(response)

        response = cloud20.getTenantByName(utils.getServiceAdminToken(), tenantId, acceptContentType)
        def createdTenant = getTenant(response)

        then: "The values are not modified in update tenant"
        updateTenant.types.type as Set == ["cloud", "files"] as Set
        createdTenant.types.type as Set == ["cloud", "files"] as Set

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
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, ["cloud", "files", "faws", "rcn", "type5", "type6",
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
    def "Get Tenant By ID/NAME returns the tenant types in the response - accept = #acceptContentType" () {
        given:
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud"])

        when: "Create new tenant"
        def createTenantResponse = cloud20.addTenant(adminToken, tenant)

        then: "Assert created tenant"
        createTenantResponse.status == 201

        when: "Get tenant by id"
        def getTenantResponse = cloud20.getTenant(adminToken, tenantId, acceptContentType)

        then: "Assert types on tenant"
        assert getTenantResponse.status == 200
        def tenantEntity = getTenant(getTenantResponse)
        assert tenantEntity.types.type.size == 1
        assert tenantEntity.types.type[0] == "cloud"

        when: "Get tenant by name"
        def getTenantByNameResponse = cloud20.getTenantByName(adminToken, tenantId, acceptContentType)

        then: "Assert types on tenant"
        assert getTenantByNameResponse.status == 200
        def tenantNameEntity = getTenant(getTenantByNameResponse)
        assert tenantNameEntity.types.type.size == 1
        assert tenantNameEntity.types.type[0] == "cloud"

        cleanup:
        utils.deleteTenant(tenant)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert get Tenant By ID/NAME does not return tenant types for empty list - accept = #acceptContentType, type = #type" () {
        given:
        def adminToken = utils.getIdentityAdminToken()
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId, type)

        when: "Create new tenant"
        def createTenantResponse = cloud20.addTenant(adminToken, tenant)

        then: "Assert created tenant"
        createTenantResponse.status == 201

        when: "Get tenant by id"
        def getTenantResponse = cloud20.getTenant(adminToken, tenantId, acceptContentType)

        then: "Assert null value for types"
        getTenantResponse.status == 200
        def tenantEntity = getTenant(getTenantResponse)
        assert tenantEntity.types == null

        when: "Get tenant by name"
        def getTenantByNameResponse = cloud20.getTenant(adminToken, tenantId, acceptContentType)

        then: "Assert null value for types"
        getTenantResponse.status == 200
        def tenantNameEntity = getTenant(getTenantByNameResponse)
        assert tenantNameEntity.types == null

        cleanup:
        utils.deleteTenant(tenant)

        where:
        acceptContentType               | type | _
        MediaType.APPLICATION_XML_TYPE  | []   | _
        MediaType.APPLICATION_JSON_TYPE | []   | _
        MediaType.APPLICATION_XML_TYPE  | null | _
        MediaType.APPLICATION_JSON_TYPE | null | _
    }

    @Unroll
    def "Assert get List Tenants returns tenant types - accept = #acceptContentType" () {
        given: "A new user and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["cloud"]))
        utils.addRoleToUserOnTenant(user, tenant)
        def userToken = utils.getToken(username)

        when: "List tenants"
        def listTenantResponse = cloud20.listTenants(userToken, acceptContentType)

        then: "Assert correct values for types"
        assert listTenantResponse.status == 200
        def tenantsEntity = getTenantsFromResponse(listTenantResponse)
        assert tenantsEntity.tenant.size == 1
        assert tenantsEntity.tenant[0].types.type.size == 1
        assert tenantsEntity.tenant[0].types.type[0] == "cloud"

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert get List Tenants does not return tenant types for empty list - accept = #acceptContentType" () {
        given: "A new user and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, type))
        utils.addRoleToUserOnTenant(user, tenant)
        def userToken = utils.getToken(username)

        when: "List tenants"
        def listTenantResponse = cloud20.listTenants(userToken, acceptContentType)

        then: "Assert null values for types"
        assert listTenantResponse.status == 200
        def tenantsEntity = getTenantsFromResponse(listTenantResponse)
        tenantsEntity.tenant[0].types == null

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)

        where:
        acceptContentType               | type
        MediaType.APPLICATION_XML_TYPE  | []
        MediaType.APPLICATION_JSON_TYPE | []
        MediaType.APPLICATION_XML_TYPE  | null
        MediaType.APPLICATION_JSON_TYPE | null
    }

    @Unroll
    def "Assert get domain tenants returns tenant types - accept = #acceptContentType" () {
        given: "A new user and tenant"
        def adminToken = utils.getIdentityAdminToken()
        def domainId = testUtils.getRandomUUID("domainId")
        utils.createDomainEntity(domainId)
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["cloud"]))
        utils.addTenantToDomain(domainId, tenantId)

        when: "List tenants"
        def domainTenantsResponse = cloud20.getDomainTenants(adminToken, domainId, true, acceptContentType)

        then: "Assert correct value for types"
        assert domainTenantsResponse.status == 200
        def tenantsEntity = getTenantsFromResponse(domainTenantsResponse)
        assert tenantsEntity.tenant.size == 1
        assert tenantsEntity.tenant[0].types.type.size == 1
        assert tenantsEntity.tenant[0].types.type[0] == "cloud"

        cleanup:
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    /**
     * This tests the implicit assignment of identity:tenant-access role to all tenants within a user's domain
     *
     * @return
     */
    def "List Tenants: Automatically returns all tenants within user's domain" () {
        given: "A new user and 2 tenants"
        reloadableConfiguration.setProperty(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, "identity:tenant-access")
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def tenantId1 = testUtils.getRandomUUID("tenant")
        def tenantId2 = testUtils.getRandomUUID("tenant")

        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["cloud"]).with {
            it.domainId = domainId
            it
        })
        def tenant2 = utils.createTenant(v2Factory.createTenant(tenantId2, tenantId2, ["files"]).with {
            it.domainId = domainId
            it
        })

        def userToken = utils.getToken(username)

        when: "List tenants w/ feature enabled"
        def listTenantResponse2 = cloud20.listTenants(userToken)

        then: "Have role auto assigned on tenants"
        assert listTenantResponse2.status == 200
        def tenantsEntity2 = getTenantsFromResponse(listTenantResponse2)
        assert tenantsEntity2.tenant.size == 2

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant1)
        utils.deleteTenant(tenant2)
    }

    /**
     * This tests the implicit assignment of identity:tenant-access role to all tenants within a user's domain excludes
     * when user is assigned the same domain as the default tenant domain. This test assumes the default domain exists.
     *
     * @return
     */
    def "List Tenants: Automatic assignment of tenant access ignores tenants associated with default domain" () {
        given: "A new user and 2 tenants"
        reloadableConfiguration.setProperty(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, "identity:tenant-access")
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId()
        def userDomainId = getRandomUUID()
        def user = utils.createUser(adminToken, username, userDomainId)
        def tenantId1 = testUtils.getRandomUUID("tenant")

        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["cloud"]).with {
            it.domainId = domainId
            it
        })

        def userToken = utils.getToken(username)

        when: "List tenants"
        def listTenantResponse2 = cloud20.listTenants(userToken)

        then: "Do not have role auto assigned on tenants"
        assert listTenantResponse2.status == 200
        def tenantsEntity2 = getTenantsFromResponse(listTenantResponse2)
        assert tenantsEntity2.tenant.size == 0

        cleanup:
        utils.deleteUser(user)
        utils.deleteTenant(tenant1)
    }

    @Unroll
    def "Any tenant types specified in 'Create Tenant' service must match an existing tenant type: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId, ["cloud", "doesnotexist"])

        when: "If any provided tenant type does not match"
        def response = cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)

        then: "a 400 must be returned"
        String errMsg = String.format(Validator20.ERROR_TENANT_TYPE_WAS_NOT_FOUND, "doesnotexist");
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, errMsg)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }


    @Unroll
    def "Any tenant types specified in 'Update Tenant' service must match an existing tenant type: request=#requestContentType, accept=#acceptContentType" () {
        given:
        def random = UUID.randomUUID().toString().replace("-", "")
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def tenantToUpdate = v2Factory.createTenant(tenantId, tenantId, ["cloud", "doesnotexist"])

        when: "If any provided tenant type does not match"
        cloud20.addTenant(utils.getServiceAdminToken(), tenant, acceptContentType, requestContentType)
        def response = cloud20.updateTenant(utils.getServiceAdminToken(), tenantId, tenantToUpdate, acceptContentType, requestContentType)

        then: "a 400 must be returned"
        String errMsg = String.format(Validator20.ERROR_TENANT_TYPE_WAS_NOT_FOUND, "doesnotexist");
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, errMsg)

        cleanup:
        cloud20.deleteTenant(utils.getServiceAdminToken(), tenantId)

        where:
        requestContentType              | acceptContentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "test that the type passed in the create request overrides the prefix value - tenantPrefix = #tenantTypePrefix"() {
        given:
        def tenantType = utils.createTenantType()
        def tenantName;
        switch (tenantTypePrefix) {
            case Constants.TENANT_TYPE_CLOUD :
                tenantName = "" + testUtils.getRandomInteger()
                break;
            case Constants.TENANT_TYPE_FILES :
                tenantName = identityConfig.getStaticConfig().getNastTenantPrefix() + RandomStringUtils.randomAlphabetic(8)
                break;
            case Constants.TENANT_TYPE_MANAGED_HOSTING :
                tenantName = GlobalConstants.MANAGED_HOSTING_TENANT_PREFIX + RandomStringUtils.randomAlphabetic(8)
                break;
            case [Constants.TENANT_TYPE_FAWS, Constants.TENANT_TYPE_RCN] :
                tenantName = tenantTypePrefix + ':' + RandomStringUtils.randomAlphabetic(8)
                break;
        }
        def tenant = v2Factory.createTenant(tenantName, tenantName, [tenantType.name])

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        createdTenant.types.type.size == 1
        createdTenant.types.type[0] == tenantType.name

        cleanup:
        utils.deleteTenant(createdTenant)
        utils.deleteTenantType(tenantType.name)

        where:
        tenantTypePrefix                        | _
        Constants.TENANT_TYPE_CLOUD             | _
        Constants.TENANT_TYPE_FILES             | _
        Constants.TENANT_TYPE_MANAGED_HOSTING   | _
        Constants.TENANT_TYPE_FAWS              | _
        Constants.TENANT_TYPE_RCN               | _
    }

    @Unroll
    def "test that the appropriate tenant type is applied using the tenant name prefix, when no tenant type is provided - tenantTypePrefix = #tenantTypePrefix"() {
        given:
        def tenantName;
        switch (tenantTypePrefix) {
            case Constants.TENANT_TYPE_CLOUD :
                tenantName = "" + testUtils.getRandomInteger()
                break;
            case Constants.TENANT_TYPE_FILES :
                tenantName = identityConfig.getStaticConfig().getNastTenantPrefix() + RandomStringUtils.randomAlphabetic(8)
                break;
            case Constants.TENANT_TYPE_MANAGED_HOSTING :
                tenantName = GlobalConstants.MANAGED_HOSTING_TENANT_PREFIX + RandomStringUtils.randomAlphabetic(8)
                break;
            case [Constants.TENANT_TYPE_FAWS, Constants.TENANT_TYPE_RCN] :
                tenantName = tenantTypePrefix + ':' + RandomStringUtils.randomAlphabetic(8)
                break;
        }
        def tenant = v2Factory.createTenant(tenantName, tenantName)

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        createdTenant.types.type.size == 1
        createdTenant.types.type[0] == tenantTypePrefix

        cleanup:
        utils.deleteTenant(createdTenant)

        where:
        tenantTypePrefix                        | _
        Constants.TENANT_TYPE_CLOUD             | _
        Constants.TENANT_TYPE_FILES             | _
        Constants.TENANT_TYPE_MANAGED_HOSTING   | _
        Constants.TENANT_TYPE_FAWS              | _
        Constants.TENANT_TYPE_RCN               | _
    }

    @Unroll
    def "test scenarios where tenant name is different values of integer - tenantName = #tenantName"() {
        given:
        def tenant = v2Factory.createTenant(tenantName, tenantName)

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        if ((Long.parseLong(tenantName) <= Integer.MAX_VALUE) &&
                (Long.parseLong(tenantName) >= Integer.MIN_VALUE)) {
            assert createdTenant.types.type.size == 1
            assert createdTenant.types.type[0] == Constants.TENANT_TYPE_CLOUD
        } else {
            assert createdTenant.types == null
        }

        cleanup:
        utils.deleteTenant(createdTenant)

        where:
        tenantName                              | _
        "1"                                     | _
        "0"                                     | _
        "-1"                                    | _
        "" + Integer.MAX_VALUE                  | _
        "" + (((long)Integer.MAX_VALUE) + 1)    | _
        "" + Integer.MIN_VALUE                  | _
        "" + (((long)Integer.MIN_VALUE) - 1)    | _
    }

    def "Verify tenant names with multiple prefixes (eg faws:rcn:random_name) & no tenant types"() {
        given:
        def tenantName = Constants.TENANT_TYPE_FAWS + ":" + Constants.TENANT_TYPE_RCN + ":" + RandomStringUtils.randomAlphabetic(8)
        def tenant = v2Factory.createTenant(tenantName, tenantName)

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        createdTenant.types.type.size == 1
        createdTenant.types.type[0] == Constants.TENANT_TYPE_FAWS

        cleanup:
        utils.deleteTenant(createdTenant)
    }

    def "test where tenant name contains a non-ASCII prefix (the same test as testing if the tenant type does not exist)"() {
        given:
        def tenantName = "©˚©√†˙∂®©®≈ƒ√˙∫∆˚∆˜∆˚˜˚" + ":" + RandomStringUtils.randomAlphabetic(8)
        def tenant = v2Factory.createTenant(tenantName, tenantName)

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        createdTenant.types == null

        cleanup:
        utils.deleteTenant(createdTenant)
    }

    @Unroll
    def "test assigning default tenant type on create feature flag - #featureTurnedOn = #featureTurnedOn"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, featureTurnedOn)
        def tenantType = utils.createTenantType()
        def tenantName = "$tenantType.name:" + testUtils.getRandomInteger()
        def tenant = v2Factory.createTenant(tenantName, tenantName)

        when:
        def result = cloud20.addTenant(utils.getIdentityAdminToken(), tenant)

        then:
        result.status == 201
        def createdTenant = result.getEntity(Tenant).value
        if (featureTurnedOn) {
            assert createdTenant.types.type.size == 1
            assert createdTenant.types.type[0] == tenantType.name
        } else {
            assert createdTenant.types == null
        }

        cleanup:
        utils.deleteTenant(createdTenant)
        utils.deleteTenantType(tenantType.name)

        where:
        featureTurnedOn | _
        true            | _
        false           | _
    }

    @Unroll
    def "Test list tenants with query param 'apply_rcn_roles' - apply_rcn_roles=#applyRcnRoles, accpet=#accept" () {
        given: "Two new users"
        def user1 = utils.createCloudAccount(utils.identityAdminToken)
        def user2 = utils.createCloudAccount(utils.identityAdminToken)
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(user1.domainId, rcn)
        utils.domainRcnSwitch(user2.domainId, rcn)

        when: "Listing tenants for user1 prior to adding RCN role"
        def user1Token = utils.getToken(user1.username)
        def listTenantResponse = cloud20.listTenants(user1Token, applyRcnRoles, accept)
        def user1Tenants = getTenantsFromResponse(listTenantResponse)

        then: "Assert user1 tenants are return whether or not RCN roles are applied"
        user1Tenants.tenant.find({it.id == user1.domainId}) != null
        user1Tenants.tenant.find({it.id == utils.getNastTenant(user1.domainId)}) != null
        user1Tenants.tenant.size == 2

        when: "Add global 'rcn-all' role to user1"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), user1.id, Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID)

        then: "Assert role added to user1"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "Listing tenants for user1"
        listTenantResponse = cloud20.listTenants(user1Token, applyRcnRoles, accept)
        user1Tenants = getTenantsFromResponse(listTenantResponse)

        then:
        // When 'apply_rcn_roles=true` all tenant from user2 are added to the list of user1 tenants
        if (applyRcnRoles instanceof String) {
            applyRcnRoles = Boolean.parseBoolean(applyRcnRoles)
        }
        if (applyRcnRoles) {
            assert user1Tenants.tenant.find({it.id == user2.domainId}) != null
            assert user1Tenants.tenant.find({it.id == utils.getNastTenant(user2.domainId)}) != null
            assert user1Tenants.tenant.size == 4
        } else{
            assert user1Tenants.tenant.find({it.id == user2.domainId}) == null
            assert user1Tenants.tenant.find({it.id == utils.getNastTenant(user2.domainId)}) == null
            assert user1Tenants.tenant.size == 2
        }
        user1Tenants.tenant.find({it.id == user1.domainId}) != null
        user1Tenants.tenant.find({it.id == utils.getNastTenant(user1.domainId)}) != null

        cleanup:
        utils.deleteUser(user1)
        utils.deleteUser(user2)
        utils.deleteDomain(user1.domainId)
        utils.deleteDomain(user2.domainId)

        where:
        applyRcnRoles | accept
        true          | MediaType.APPLICATION_XML_TYPE
        true          | MediaType.APPLICATION_JSON_TYPE
        "TRUE"        | MediaType.APPLICATION_XML_TYPE
        "TrUe"        | MediaType.APPLICATION_XML_TYPE
        false         | MediaType.APPLICATION_XML_TYPE
        false         | MediaType.APPLICATION_JSON_TYPE
        "FALSE"       | MediaType.APPLICATION_XML_TYPE
        "FaLSe"       | MediaType.APPLICATION_XML_TYPE
        "invalid"     | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Assert no duplicate on list tenants with query param 'apply_rcn_roles=true' - accpet=#accept" () {
        given: "Two new users"
        def user1 = utils.createCloudAccount(utils.identityAdminToken)
        def user2 = utils.createCloudAccount(utils.identityAdminToken)
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(user1.domainId, rcn)
        utils.domainRcnSwitch(user2.domainId, rcn)

        when: "Add global 'rcn-all' role to user1"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), user1.id, Constants.IDENTITY_RCN_ALL_TENANT_ROLE_ID)

        then: "Assert role added to user1"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "Add new tenant role on both users"
        def tenant = utils.createTenant()
        def role = utils.createRole()
        utils.addRoleToUserOnTenant(user1, tenant, role.id)
        utils.addRoleToUserOnTenant(user2, tenant, role.id)

        def user1Token = utils.getToken(user1.username)
        def user2Token = utils.getToken(user2.username)

        def user1ListTenantsResponse = cloud20.listTenants(user1Token, false, accept)
        def user2ListTenantsResponse = cloud20.listTenants(user2Token, false, accept)

        def user1Tenants = getTenantsFromResponse(user1ListTenantsResponse)
        def user2Tenants = getTenantsFromResponse(user2ListTenantsResponse)

        then: "Assert tenants on users"
        user1Tenants.tenant.find({it.id == tenant.id}) != null
        user2Tenants.tenant.find({it.id == tenant.id}) != null

        when: "Listing tenants for user1 with 'apply_rcn_roles=true'"
        user1ListTenantsResponse = cloud20.listTenants(user1Token, true, accept)
        user1Tenants = getTenantsFromResponse(user1ListTenantsResponse)

        then: "Assert tenant is not duplicated"
        user1Tenants.tenant.find({it.id == user2.domainId}) != null
        user1Tenants.tenant.find({it.id == utils.getNastTenant(user2.domainId)}) != null
        user1Tenants.tenant.find({it.id == user1.domainId}) != null
        user1Tenants.tenant.find({it.id == utils.getNastTenant(user1.domainId)}) != null
        user1Tenants.tenant.find({it.id == tenant.id}) != null
        assert user1Tenants.tenant.size == 5

        cleanup:
        utils.deleteUser(user1)
        utils.deleteUser(user2)
        utils.deleteDomain(user1.domainId)
        utils.deleteDomain(user2.domainId)
        utils.deleteTenant(tenant)
        utils.deleteRole(role)

        where:
        accept                          | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def getTenant(response) {
        def tenant = response.getEntity(Tenant)

        if (response.getType() == MediaType.APPLICATION_XML_TYPE) {
            tenant = tenant.value
        }
        return tenant
    }

    def getTenantsFromResponse(response) {
        def tenants = response.getEntity(Tenants)

        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            tenants = tenants.value
        }
        return tenants
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
