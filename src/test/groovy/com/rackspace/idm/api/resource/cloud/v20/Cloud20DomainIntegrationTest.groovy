package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.junit.IgnoreByRepositoryProfile

import javax.ws.rs.core.HttpHeaders
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriInfo
import javax.xml.datatype.DatatypeFactory
import java.time.Duration

import static com.rackspace.idm.Constants.*
import static javax.servlet.http.HttpServletResponse.*
import static org.mockito.Mockito.mock
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponseWithErrorCode
import static testHelpers.IdmAssert.assertRackspaceCommonFaultResponse

class Cloud20DomainIntegrationTest extends RootIntegrationTest {

    @Autowired DomainService domainService;
    @Autowired Cloud20Service cloud20Service;
    @Autowired UserService userService;
    @Autowired TenantService tenantService;

    @Autowired
    private IdentityConfig identityConfig

    @Autowired UserDao userDao;

    def setup() {
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    def "list accessible domains for user admin returns the user's domain"() {
        given:
        def domain = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domain)

        when:
        def response = cloud20.getAccessibleDomains(utils.getToken(userAdmin.username))

        then:
        response.status == 200
        def domains = response.getEntity(Domains)
        domains.domain.size() == 1
        domains.domain[0].id == domain

        cleanup:
        utils.deleteUsers(users)
    }

    @Unroll
    def "list accessible domains returns domain session timeout: accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def domain = utils.getDomain(domainId)

        when: "list accessible domains w/o a session timeout set on the domain"
        def response = cloud20.getAccessibleDomains(userAdminToken, null, null, accept)

        then: "returns the default session timeout"
        response.status == 200
        assertSessionInactivityTimeoutForSingleDomainList(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "list accessible domains w/ a session timeout set on the domain"
        def domainDuration = DatatypeFactory.newInstance().newDuration(
                identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().plusHours(3).toString());
        domain.sessionInactivityTimeout = domainDuration
        utils.updateDomain(domain.id, domain)
        response = cloud20.getAccessibleDomains(userAdminToken, null, null, accept)

        then: "returns the session timeout set on the domain"
        assertSessionInactivityTimeoutForSingleDomainList(response, accept, domain.sessionInactivityTimeout.toString())

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        accept                          | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "list accessible domains for user returns domain session timeout: accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def domain = utils.getDomain(domainId)

        when: "list accessible domains w/o a session timeout set on the domain"
        def response = cloud20.getAccessibleDomainsForUser(userAdminToken, userAdmin.id, accept)

        then: "returns the default session timeout"
        response.status == 200
        assertSessionInactivityTimeoutForSingleDomainList(response, accept, identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString())

        when: "list accessible domains w/ a session timeout set on the domain"
        def domainDuration = DatatypeFactory.newInstance().newDuration(
                identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().plusHours(3).toString());
        domain.sessionInactivityTimeout = domainDuration
        utils.updateDomain(domain.id, domain)
        response = cloud20.getAccessibleDomainsForUser(userAdminToken, userAdmin.id, accept)

        then: "returns the session timeout set on the domain"
        assertSessionInactivityTimeoutForSingleDomainList(response, accept, domain.sessionInactivityTimeout.toString())

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        accept                          | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert list accessible domains returns rackspaceCustomerNumber if present - rcn = #rackspaceCustomerNumber, content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)

        when: "Create domain"
        domain.rackspaceCustomerNumber = rackspaceCustomerNumber
        def response = cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        then:
        response.status == SC_CREATED

        when: "list accessible domains"
        def getAccessibleDomainsResponse = cloud20.getAccessibleDomains(utils.identityAdminToken, null, null, accept)
        def testDomain = getDomainFromAccessibleDomainListById(getAccessibleDomainsResponse, domainId)

        then:
        testDomain.id == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  testDomain.rackspaceCustomerNumber == null
        } else {
            assert  testDomain.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        cleanup:
        utils.deleteDomain(domainId)

        where:
        rackspaceCustomerNumber | accept                          | content
        "RNC-123-123-123"       | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        "RNC-123-123-123"       | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        ""                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Assert list accessible domains for user returns rackspaceCustomerNumber if present - rcn = #rackspaceCustomerNumber, content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when: "Update domain"
        def domain = v1Factory.createDomain(domainId, domainId)
        domain.rackspaceCustomerNumber = rackspaceCustomerNumber
        def response = cloud20.updateDomain(utils.identityAdminToken, domainId, domain, accept, content)

        then:
        response.status == SC_OK

        when: "list accessible domains for user using admin token"
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def getAccessibleDomainsForUserResponse = cloud20.getAccessibleDomainsForUser(identityAdminToken, userAdmin.id, accept)
        def domains = getAccessibleDomainsForUserResponse.getEntity(Domains)
        def testDomain = domains.domain.find({it.id == domainId})

        then:
        testDomain.id == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  testDomain.rackspaceCustomerNumber == null
        } else {
            assert  testDomain.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        when: "list accessible domains for user admin"
        def userAdminToken = utils.getToken(userAdmin.username)
        getAccessibleDomainsForUserResponse = cloud20.getAccessibleDomainsForUser(userAdminToken, userAdmin.id, accept)
        domains = getAccessibleDomainsForUserResponse.getEntity(Domains)
        testDomain = domains.domain.find({it.id == domainId})

        then:
        testDomain.id == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  testDomain.rackspaceCustomerNumber == null
        } else {
            assert  testDomain.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        when: "list accessible domains for manage user"
        def userManageToken = utils.getToken(userManage.username)
        getAccessibleDomainsForUserResponse = cloud20.getAccessibleDomainsForUser(userManageToken, userManage.id, accept)
        domains = getAccessibleDomainsForUserResponse.getEntity(Domains)
        testDomain = domains.domain.find({it.id == domainId})

        then:
        testDomain.id == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  testDomain.rackspaceCustomerNumber == null
        } else {
            assert  testDomain.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        when: "list accessible domains for default user"
        def defaultUserToken = utils.getToken(defaultUser.username)
        getAccessibleDomainsForUserResponse = cloud20.getAccessibleDomainsForUser(defaultUserToken, defaultUser.id, accept)
        domains = getAccessibleDomainsForUserResponse.getEntity(Domains)
        testDomain = domains.domain.find({it.id == domainId})

        then:
        testDomain.id == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  testDomain.rackspaceCustomerNumber == null
        } else {
            assert  testDomain.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        rackspaceCustomerNumber | accept                          | content
        "RNC-123-123-123"       | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        "RNC-123-123-123"       | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        ""                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "Test if 'cloud20Service.addTenant(...)' adds default 'domainId' to the tenant and updates domain to point to tenant"() {
        given:
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();

        User user = utils.createUser(utils.getServiceAdminToken(), testUtils.getRandomUUID("identityAdmin"), testUtils.getRandomUUID("domain"));
        def token = utils.getToken(user.username)
        def tenantName = testUtils.getRandomUUID("tenant")
        assert user.domainId != null

        //must be different or this test doesn't test that the default domain is used instead of the caller's domain
        assert user.domainId != defaultDomainId

        when:
        def tenant = v2Factory.createTenant(tenantName, tenantName)
        cloud20Service.addTenant(mock(HttpHeaders), mock(UriInfo), token, tenant)
        def result = tenantService.getTenantByName(tenantName)
        def defaultDomain = domainService.getDomain(defaultDomainId)

        then:
        result.domainId == defaultDomainId
        Arrays.asList(defaultDomain.tenantIds).contains(result.tenantId)

        cleanup:
        try { tenantService.deleteTenant(tenantName) } catch (Exception e) {}
        try { userDao.deleteUser(user) } catch (Exception e) {}
        try { domainService.deleteDomain(domainId) } catch (Exception e) {}
    }

    def "Domain names are unique"() {
        given:
        def domainId1 = utils.createDomain()
        def domainName1 = testUtils.getRandomUUID(domainId1)
        def domainId2 = utils.createDomain()
        def domainName2 = testUtils.getRandomUUID(domainId2)
        utils.createDomain(v2Factory.createDomain(domainId1, domainName1))

        when: "try to create domain 2 with the name of domain 1"
        def domainData = v2Factory.createDomain(domainId2, domainName1)
        def createResponse = cloud20.addDomain(utils.getServiceAdminToken(), domainData)

        then: "409"
        createResponse.status == 409

        when: "now create domain 2 and then update the domain to have name of domain 1"
        domainData = v2Factory.createDomain(domainId2, domainName2)
        cloud20.addDomain(utils.getServiceAdminToken(), domainData)
        domainData = v2Factory.createDomain(domainId2, domainName1)
        def updateResponse = cloud20.updateDomain(utils.getServiceAdminToken(), domainId2, domainData)

        then: "409"
        updateResponse.status == 409

        cleanup:
        utils.deleteDomain(domainId1)
        utils.deleteDomain(domainId2)
    }

    def "Test if 'domainService.addTenantToDomain(...)' adds 'domainId' to the tenant"() {
        given:
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)
        def tenant = utils.createTenant() //will associate tenant to default domain
        def tenantEntity = tenantService.getTenantByName(tenant.name)

        //assert initial state is as expected
        assertDomainContainsTenant(defaultDomainId, tenant.id)
        assertDomainDoesNotContainTenant(domainId, tenant.id)

        when:
        domainService.addTenantToDomain(tenant.id, domainId)
        def result = tenantService.getTenantByName(tenant.name)

        then:
        result.domainId == domainId
        assertDomainContainsTenant(domainId, tenant.id)
        assertDomainDoesNotContainTenant(defaultDomainId, tenant.id)

        cleanup:
        utils.deleteTenant(tenant)
        domainService.deleteDomain(domainId)
    }

    @Unroll
    def "Test 'domainService.addTenantToDomain(...)' adding tenant to same domain it belongs to is no-op for #testDescription"() {
        given:
        //create tenant
        def tenant = utils.createTenant() //will associate tenant to default domain

        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
        def domainToUse = defaultDomainId
        //if not using default domain, create a test domain and associate tenant with that domain
        if (!testWithDefaultDomain) {
            domainToUse = utils.createDomain()
            domainService.createNewDomain(domainToUse)
            domainService.addTenantToDomain(tenant.id, domainToUse)
        }

        when: "add the tenant to domain that it already points to"
        domainService.addTenantToDomain(tenant.id, domainToUse)
        def result = tenantService.getTenantByName(tenant.name)

        then: "tenant still points to domain"
        result.domainId == domainToUse

        and: "domain still points to this tenant"
        assertDomainContainsTenant(domainToUse, tenant.id)

        cleanup:
        utils.deleteTenant(tenant)
        if (!testWithDefaultDomain) {
            domainService.deleteDomain(domainToUse)
        }

        where:
        testWithDefaultDomain | testDescription
        Boolean.FALSE | "non-default domain"
        Boolean.TRUE | "default domain"
    }

    def "Test if 'domainService.removeTenantFromDomain(...)' sets tenant to default domain"() {
        given:
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)
        def tenant = utils.createTenant() //will associate tenant to default domain

        //add tenant to specified domain
        domainService.addTenantToDomain(tenant.id, domainId)

        //assert initial state is as expected w/ tenant pointing to new domain
        assertDomainContainsTenant(domainId, tenant.id)
        assertDomainDoesNotContainTenant(defaultDomainId, tenant.id)

        when: "remove tenant"
        domainService.removeTenantFromDomain(tenant.id, domainId)
        def result = tenantService.getTenantByName(tenant.name)

        then:
        result.domainId == defaultDomainId
        assertDomainContainsTenant(defaultDomainId, tenant.id)
        assertDomainDoesNotContainTenant(domainId, tenant.id)

        cleanup:
        utils.deleteTenant(tenant)
        domainService.deleteDomain(domainId)
    }

    def "Test 'domainService.removeTenantFromDomain(...)' removing tenant from default domain not allowed"() {
        given:
        def tenant = utils.createTenant() //will associate tenant to default domain
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();

        when: "remove tenant from default domain"
        domainService.removeTenantFromDomain(tenant.id, defaultDomainId)

        then: "get BadRequest exception"
        thrown(BadRequestException)
    }

    /**
     * Ignore for SQL because dual pointers are NOT maintained in MariaDB. Instead the TENANT pointer to the domain is
     * what is maintained. The domain's pointers to tenants are ignored.
     *
     * @return
     */
    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "Test 'domainService.removeTenantFromDomain(...)' and ''domainService.addTenantToDomain(...)' when pointers are off"() {
        given:
        def tenant = utils.createTenant() //will associate tenant to default domain
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();

        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)

        //add tenant to specified domain using backend so default domain still points to tenant, and new domain does not (invalid state)
        def tenantEntity = tenantService.getTenantByName(tenant.name)
        tenantEntity.setDomainId(domainId)
        tenantService.updateTenant(tenantEntity)

        //test initial state
        assertDomainContainsTenant(defaultDomainId, tenant.id)

        when: "remove tenant from default domain when tenant points to different domain"
        domainService.removeTenantFromDomain(tenant.id, defaultDomainId)

        then: "do not get IllegalArgument exception"
        notThrown(BadRequestException)

        and: "default domain no longer points to tenant"
        assertDomainDoesNotContainTenant(defaultDomainId, tenant.id)

        and: "actual domain does not point to tenant either since hacked data"
        assertDomainDoesNotContainTenant(domainId, tenant.id)

        when: "add tenant to domain"
        domainService.addTenantToDomain(tenant.id, domainId)

        then: "domain now points to tenant"
        assertDomainContainsTenant(domainId, tenant.id)

        cleanup:
        utils.deleteTenant(tenant)
        domainService.deleteDomain(domainId)
    }

    def "Delete Domain - require disabled when prop set"() {
        given:
        def enabledDomainId = utils.createDomain()
        def disabledDomainId = utils.createDomain()
        utils.createDomain(v2Factory.createDomain(enabledDomainId, enabledDomainId).with {
            it.enabled = true
            it
        })
        utils.createDomain(v2Factory.createDomain(disabledDomainId, disabledDomainId).with {
            it.enabled = false
            it
        })
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP, true)

        when: "delete an enabled domain when prop set to true"
        def response = cloud20.deleteDomain(utils.getServiceAdminToken(), enabledDomainId)

        then: "can't delete"
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, GlobalConstants.ERROR_MSG_DELETE_ENABLED_DOMAIN)

        when: "delete a disabled domain when prop set to true"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP, false)
        response = cloud20.deleteDomain(utils.getServiceAdminToken(), disabledDomainId)

        then: "can delete"
        response.status == HttpStatus.SC_NO_CONTENT

        when: "delete an enabled domain when prop set to false"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_DELETE_DOMAIN_RULE_MUST_BE_DISABLED_PROP, false)
        response = cloud20.deleteDomain(utils.getServiceAdminToken(), enabledDomainId)

        then: "can delete"
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "Delete Domain - Fail if default"() {
        given:
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();

        when: "delete default domain"
        def response = cloud20.deleteDomain(utils.getServiceAdminToken(), defaultDomainId)

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, GlobalConstants.ERROR_MSG_DELETE_DEFAULT_DOMAIN)
    }

    def "Delete Domain - reassigns tenants"() {
        given:
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)
        def tenant = utils.createTenant() //will associate tenant to default domain
        def tenantEntity = tenantService.getTenantByName(tenant.name)

        utils.addTenantToDomain(domainId, tenant.id)
        utils.disableDomain(domainId)

        when: "delete a disabled domain with associated tenant"
        def response = cloud20.deleteDomain(utils.getServiceAdminToken(), domainId)

        then: "tenant is reassigned to default domain and domain is deleted"
        response.status == HttpStatus.SC_NO_CONTENT
        cloud20.getDomain(utils.getServiceAdminToken(), domainId).status == HttpStatus.SC_NOT_FOUND
        assertDomainContainsTenant(defaultDomainId, tenant.id)
    }

    @Unroll
    def "test getDomain security - accept == #accept"() {
        given:
        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin1, userAdmin1, userManage1, defaultUser1
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin1, userAdmin1, userManage1, defaultUser1) = utils.createUsers(domainId1)
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)
        def users1 = [defaultUser1, userManage1, userAdmin1, identityAdmin1]
        def users2 = [defaultUser2, userManage2, userAdmin2, identityAdmin2]

        when: "service admin calls getDomain"
        def response = cloud20.getDomain(utils.getServiceAdminToken(), domainId1, accept)

        then: "the request is successful"
        response.status == 200
        protectedDomainAttributesVisible(response, accept)

        when: "identity admin calls getDomain"
        response = cloud20.getDomain(utils.getIdentityAdminToken(), domainId1, accept)

        then: "the request is successful"
        response.status == 200
        protectedDomainAttributesVisible(response, accept)

        when: "user admin calls getDomain on their domain"
        def token = utils.getToken(userAdmin1.username)
        response = cloud20.getDomain(token, domainId1, accept)

        then: "the request is successful"
        response.status == 200
        !protectedDomainAttributesVisible(response, accept)

        when: "user admin calls getDomain on other domain"
        response = cloud20.getDomain(token, domainId2, accept)

        then: "the request is an error (403)"
        response.status == 403

        when: "user manage calls getDomain"
        token = utils.getToken(userManage1.username)
        response = cloud20.getDomain(token, domainId1, accept)

        then: "the request is successful"
        response.status == 200
        !protectedDomainAttributesVisible(response, accept)

        when: "user manage calls getDomain on other domain"
        response = cloud20.getDomain(token, domainId2, accept)

        then: "the request is an error (403)"
        response.status == 403

        when: "default user calls getDomain"
        token = utils.getToken(defaultUser1.username)
        response = cloud20.getDomain(token, domainId1, accept)

        then: "the request is successful"
        response.status == 200
        !protectedDomainAttributesVisible(response, accept)

        when: "default user calls getDomain on other domain"
        response = cloud20.getDomain(token, domainId2, accept)

        then: "the request is an error (403)"
        response.status == 403

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def protectedDomainAttributesVisible(response, contentType) {
        def nameVisible
        def descriptionVisible
        if(contentType == MediaType.APPLICATION_XML_TYPE) {
            def domain = response.getEntity(Domain)
            nameVisible = domain.name != null
            descriptionVisible = domain.description != null
        } else {
            def domain = new JsonSlurper().parseText(response.getEntity(String))['RAX-AUTH:domain']
            nameVisible = domain.keySet().contains('name')
            descriptionVisible = domain.keySet().contains('description')
        }
        return nameVisible || descriptionVisible
    }

    def "can only update domain of service admin if caller is service admin"() {
        given:
        def domainId1 = testUtils.getRandomUUID()
        def domainId2 = testUtils.getRandomUUID()
        Domain domain1 = v2Factory.createDomain(domainId1, testUtils.getRandomUUID())
        Domain domain2 = v2Factory.createDomain(domainId2, testUtils.getRandomUUID())
        utils.createDomain(domain1)
        utils.createDomain(domain2)
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id

        when: "update domain as service admin"
        def response = cloud20.addUserToDomain(utils.getToken(SERVICE_ADMIN_2_USERNAME, SERVICE_ADMIN_2_PASSWORD), serviceAdminId, domainId1)

        then: "not authorized"
        response.status == 204

        when: "update domain as identity admin"
        response = cloud20.addUserToDomain(utils.getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD), serviceAdminId, domainId2)

        then: "not authorized"
        response.status == 403

        cleanup:
        removeDomainFromUser(SERVICE_ADMIN_USERNAME)
    }

    def "can only update domain of identity admin if service admin"() {
        given:
        def domainId1 = testUtils.getRandomUUID()
        def domainId2 = testUtils.getRandomUUID()
        Domain domain1 = v2Factory.createDomain(domainId1, testUtils.getRandomUUID())
        Domain domain2 = v2Factory.createDomain(domainId2, testUtils.getRandomUUID())
        utils.createDomain(domain1)
        utils.createDomain(domain2)
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id

        when: "update domain as service admin"
        def response = cloud20.addUserToDomain(utils.getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD), serviceAdminId, domainId2)

        then: "not authorized"
        response.status == 204

        when: "update domain as identity admin"
        response = cloud20.addUserToDomain(utils.getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD), serviceAdminId, domainId2)

        then: "not authorized"
        response.status == 403

        cleanup:
        removeDomainFromUser(IDENTITY_ADMIN_USERNAME)
        utils.deleteTestDomainQuietly(domainId1)
    }

    def "identity admin can only update domain of user admins and sub-users"() {
        given:
        def domainId = testUtils.getRandomUUID()
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id
        def identityAdminId = userService.checkAndGetUserByName(IDENTITY_ADMIN_USERNAME).id
        def domainData = v2Factory.createDomain(domainId, testUtils.getRandomUUID("domain"))
        def identityAdminToken = utils.getIdentityAdminToken()
        utils.createDomain(domainData)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def subUser = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("subuser"), domainId)

        when: "update domain of service admin"
        def response = cloud20.addUserToDomain(identityAdminToken, serviceAdminId, domainId)

        then: "error"
        response.status == 403

        when: "update domain of identity admin"
        response = cloud20.addUserToDomain(identityAdminToken, identityAdminId, domainId)

        then: "error"
        response.status == 403

        when: "update domain of user admin"
        response = cloud20.addUserToDomain(identityAdminToken, userAdmin.id, domainId)

        then: "success"
        response.status == 204

        when: "update domain of sub-user"
        response = cloud20.addUserToDomain(identityAdminToken, subUser.id, domainId)

        then: "success"
        response.status == 204

        cleanup:
        utils.deleteUser(subUser)
        utils.deleteUsers(users)
    }

    def "an identity admin and service admin can be added to the same domain as a user admin"() {
        given:
        def domainId = testUtils.getRandomUUID()
        def userAdminUsername = testUtils.getRandomUUID("userAdmin")
        def userAdmin = utils.createUser(utils.getIdentityAdminToken(), userAdminUsername, domainId)
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id
        def identityAdminId = userService.checkAndGetUserByName(IDENTITY_ADMIN_USERNAME).id

        when: "add service admin to user admins domain"
        def response = cloud20.addUserToDomain(utils.getServiceAdminToken(), serviceAdminId, domainId)

        then: "success"
        response.status == 204

        when: "add identity admin to user admins domain"
        response = cloud20.addUserToDomain(utils.getServiceAdminToken(), identityAdminId, domainId)

        then: "success"
        response.status == 204

        cleanup:
        removeDomainFromUser(SERVICE_ADMIN_USERNAME)
        removeDomainFromUser(IDENTITY_ADMIN_USERNAME)
        utils.deleteUser(userAdmin)
    }

    def "an identity admin can update a domain that they belong to as long as it does not contain a service admin"() {
        given:
        def domainId = utils.createDomain()
        def identityAdminUsername = testUtils.getRandomUUID("identityAdmin")
        utils.createUser(utils.getServiceAdminToken(), identityAdminUsername, domainId)
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id
        def identityAdminToken = utils.getToken(identityAdminUsername)

        when: "update the domain using the identity admin"
        def domain = v2Factory.createDomain(domainId, testUtils.getRandomUUID())
        def response = cloud20.updateDomain(identityAdminToken, domain.id, domain)

        then: "success"
        response.status == 200

        when: "add a service admin to the domain and try to update the domain again"
        utils.addUserToDomain(serviceAdminId, domainId)
        domain = v2Factory.createDomain(domainId, testUtils.getRandomUUID())
        def response2 = cloud20.updateDomain(identityAdminToken, domain.id, domain)

        then: "403"
        response2.status == 403

        cleanup:
        removeDomainFromUser(SERVICE_ADMIN_USERNAME)
        removeDomainFromUser(IDENTITY_ADMIN_USERNAME)
    }

    def "an identity admin cannot update a domain containing another identity admin if they do not belong to the domain"() {
        given:
        def domainId = utils.createDomain()
        def identityAdminUsername = testUtils.getRandomUUID("identityAdmin")
        utils.createUser(utils.getServiceAdminToken(), identityAdminUsername, domainId)
        def identityAdminId = userService.checkAndGetUserByName(IDENTITY_ADMIN_USERNAME).id

        when: "try to update the domain"
        def domain = v2Factory.createDomain(domainId, testUtils.getRandomUUID())
        def response = cloud20.updateDomain(utils.getIdentityAdminToken(), domain.id, domain)

        then: "error"
        response.status == 403

        when: "add the identity admin to the domain and update again"
        utils.addUserToDomain(identityAdminId, domainId)
        domain = v2Factory.createDomain(domainId, testUtils.getRandomUUID())
        response = cloud20.updateDomain(utils.getIdentityAdminToken(), domain.id, domain)

        then: "success"
        response.status == 200

        cleanup:
        removeDomainFromUser(IDENTITY_ADMIN_USERNAME)
    }

    def "a service admin can update a domain containing other service admins"() {
        given:
        def domainId = utils.createDomain()
        def domainData = v2Factory.createDomain(domainId, testUtils.getRandomUUID("domain"))
        utils.createDomain(domainData)
        def serviceAdminId = userService.checkAndGetUserByName(SERVICE_ADMIN_USERNAME).id
        utils.addUserToDomain(serviceAdminId, domainId)

        when: "other service admin tries to update the domain"
        domainData = v2Factory.createDomain(domainId, testUtils.getRandomUUID("domain"))
        def response = cloud20.updateDomain(utils.getToken(SERVICE_ADMIN_2_USERNAME, SERVICE_ADMIN_2_PASSWORD), domainId, domainData)

        then: "success"
        response.status == 200

        cleanup:
        removeDomainFromUser(SERVICE_ADMIN_USERNAME)
        removeDomainFromUser(SERVICE_ADMIN_2_USERNAME)
    }

    def "update domain does not require domain ID to be within the body of the request"() {
        given:
        def domainId = utils.createDomain()
        def domainData = v2Factory.createDomain(domainId, testUtils.getRandomUUID("domain"))
        utils.createDomain(domainData)
        def domain = v2Factory.createDomain(domainId, testUtils.getRandomUUID()).with {
            it.id = null
            it
        }

        when: "update the domain without the domain ID in the body"
        def response = cloud20.updateDomain(utils.getIdentityAdminToken(), domainId, domain)

        then: "success"
        response.status == 200
    }

    def "Add tenant to disabled domain"() {
        given: "Disabled domain and tenant"
        def tenant = utils.createTenant()
        def domainId = testUtils.getRandomUUID('domain')
        def domainForCreate = v2Factory.createDomain(domainId, domainId)
        domainForCreate.enabled = false
        def domain = utils.createDomain(domainForCreate)

        when: "Add tenant to domain"
        def response = cloud20.addTenantToDomain(utils.getIdentityAdminToken(), domain.id, tenant.id)

        then: "Assert tenant was added to domain"
        response.status == 204

        cleanup:
        utils.deleteTenant(tenant)
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "Create domain with sessionInactivityTimeout - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def sessionInactivityTimeout = DatatypeFactory.newInstance().newDuration("PT24H")
        def domain = v1Factory.createDomain(domainId, domainId).with {
            it.sessionInactivityTimeout = sessionInactivityTimeout
            it
        }

        when:
        def domainEntity = cloud20.addDomain(utils.identityAdminToken, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.sessionInactivityTimeout == sessionInactivityTimeout

        cleanup:
        utils.deleteDomain(domainId)

        where:
        accept                          | content
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Create and get domain with rackspaceCustomerNumber - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)

        when: "Create domain"
        domain.rackspaceCustomerNumber = rackspaceCustomerNumber
        def domainEntity = cloud20.addDomain(utils.identityAdminToken, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  domainEntity.rackspaceCustomerNumber == null
        } else {
            assert  domainEntity.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        when: "Get domain"
        def getDomainEntity = cloud20.getDomain(utils.identityAdminToken, domainId, accept).getEntity(Domain)

        then:
        getDomainEntity.id == domainId
        getDomainEntity.name == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  getDomainEntity.rackspaceCustomerNumber == null
        } else {
            assert  getDomainEntity.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        cleanup:
        utils.deleteDomain(domainId)

        where:
        rackspaceCustomerNumber | accept                          | content
        "RNC-123-123-123"       | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        "RNC-123-123-123"       | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        ""                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update and get domain with rackspaceCustomerNumber - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)
        def rcn = testUtils.getRandomUUIDOfLength("RCN", 10)
        domain.rackspaceCustomerNumber = rcn
        cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        when: "Update domain"
        domain.rackspaceCustomerNumber = rackspaceCustomerNumber
        def domainEntity = cloud20.updateDomain(utils.identityAdminToken, domainId, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  domainEntity.rackspaceCustomerNumber == rcn
        } else {
            assert  domainEntity.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        when: "Get domain"
        def getDomainEntity = cloud20.getDomain(utils.identityAdminToken, domainId, accept).getEntity(Domain)

        then:
        getDomainEntity.id == domainId
        getDomainEntity.name == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  getDomainEntity.rackspaceCustomerNumber == rcn
        } else {
            assert  getDomainEntity.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        cleanup:
        utils.deleteDomain(domainId)

        where:
        rackspaceCustomerNumber | accept                          | content
        "RNC-123-123-123"       | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        "RNC-123-123-123"       | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        ""                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update domain with rackspaceCustomerNumber for domain with no rackspaceCustomerNumber - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)
        cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        when: "Update domain"
        domain.rackspaceCustomerNumber = rackspaceCustomerNumber
        def domainEntity = cloud20.updateDomain(utils.identityAdminToken, domainId, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        if (StringUtils.isBlank(rackspaceCustomerNumber)) {
            assert  domainEntity.rackspaceCustomerNumber == null
        } else {
            assert  domainEntity.rackspaceCustomerNumber == rackspaceCustomerNumber
        }

        cleanup:
        utils.deleteDomain(domainId)

        where:
        rackspaceCustomerNumber | accept                          | content
        "RNC-123-123-123"       | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        "RNC-123-123-123"       | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        ""                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Assert existing rackspaceCustomerNumber is not changed if attribute is not provided - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)
        def rcn = testUtils.getRandomUUIDOfLength("RCN", 10)
        domain.rackspaceCustomerNumber = rcn
        cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        when: "Update domain with null RCN"
        domain.rackspaceCustomerNumber = null
        def domainEntity = cloud20.updateDomain(utils.identityAdminToken, domainId, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.rackspaceCustomerNumber == rcn

        cleanup:
        utils.deleteDomain(domainId)

        where:
        accept                          | content
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Assert default value for sessionInactivityTimeout on domain creation - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v1Factory.createDomain(domainId, domainId)

        when:
        def domainEntity = cloud20.addDomain(utils.identityAdminToken, domain, accept, content).getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.sessionInactivityTimeout.toString() == identityConfig.reloadableConfig.domainDefaultSessionInactivityTimeout.toString()

        cleanup:
        utils.deleteDomain(domainId)

        where:
        accept                          | content
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Invalid cases for domain creation - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)

        DatatypeFactory factory = DatatypeFactory.newInstance();

        when: "Domain's sessionInactivityTimeout exceeding max duration"
        domain.sessionInactivityTimeout = factory.newDuration("PT25H")
        def createDomainResponse = cloud20.addDomain(utils.identityAdminToken, domain, accept, content)
        Duration maxDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMaxDuration()
        Duration minDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMinDuration()
        String errMsg = String.format(Validator20.SESSION_INACTIVITY_TIMEOUT_RANGE_ERROR_MESSAGE, minDuration.getSeconds(), maxDuration.getSeconds())

        then:
        assertOpenStackV2FaultResponse(createDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)

        when: "Domain's sessionInactivityTimeout less than min duration"
        domain.sessionInactivityTimeout = factory.newDuration("PT4M")
        createDomainResponse = cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        then:
        assertOpenStackV2FaultResponse(createDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)

        when: "Domain's name is null"
        domain.sessionInactivityTimeout = factory.newDuration("PT24H")
        domain.name = null
        createDomainResponse = cloud20.addDomain(utils.identityAdminToken, domain, accept, content)
        errMsg = String.format(Validator20.REQUIRED_ATTR_MESSAGE, "name")

        then:
        assertOpenStackV2FaultResponse(createDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)

        when: "Domain's rackspaceCustomerNumber is longer than 32 characters"
        domain = v2Factory.createDomain(domainId, domainId)
        domain.rackspaceCustomerNumber = testUtils.getRandomUUIDOfLength("RCN",33)
        createDomainResponse = cloud20.addDomain(utils.identityAdminToken, domain, accept, content)

        then:
        assertOpenStackV2FaultResponseWithErrorCode(createDomainResponse, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        where:
        accept                          | content
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update domain to set sessionInactivityTimeout - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when: "Updating sessionInactivityTimeout on domain using identity-admin token"
        def updateDomain = new Domain()
        DatatypeFactory factory = DatatypeFactory.newInstance();
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT24H")
        def updateDomainResponse = cloud20.updateDomain(utils.getIdentityAdminToken(), domainId, updateDomain, accept, content)
        def domainEntity = updateDomainResponse.getEntity(Domain)

        then:
        updateDomainResponse.status == SC_OK
        domainEntity.id == domainId
        domainEntity.sessionInactivityTimeout.toString() == "PT24H"

        when: "Updating sessionInactivityTimeout on domain using user-admin token"
        def userAdminToken = utils.getToken(userAdmin.username)
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT23H")
        updateDomainResponse = cloud20.updateDomain(userAdminToken, domainId, updateDomain, accept, content)
        domainEntity = updateDomainResponse.getEntity(Domain)

        then:
        updateDomainResponse.status == SC_OK
        domainEntity.id == domainId
        domainEntity.sessionInactivityTimeout.toString() == "PT23H"

        when: "Updating sessionInactivityTimeout on domain using user-manager token"
        def userManageToken = utils.getToken(userManage.username)
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT22H")
        updateDomainResponse = cloud20.updateDomain(userManageToken, domainId, updateDomain, accept, content)
        domainEntity = updateDomainResponse.getEntity(Domain)

        then:
        updateDomainResponse.status == SC_OK
        domainEntity.id == domainId
        domainEntity.sessionInactivityTimeout.toString() == "PT22H"

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        accept                          | content
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Invalid cases for updating domain - v2 Exceptions = #flag, content-type = #content, accept = #accept"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_FORCE_STANDARD_V2_EXCEPTIONS_FOR_END_USER_SERVICES_PROP, flag)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        def domainId2 = utils.createDomain()
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)
        def users2 = [defaultUser2, userManage2, userAdmin2, identityAdmin2]

        def updateDomain = new Domain()
        DatatypeFactory factory = DatatypeFactory.newInstance();
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT15M")

        when: "Updating sessionInactivityTimeout on domain using user-admin token from a different domain"
        def userAdminToken = utils.getToken(userAdmin.username)
        def updateDomainResponse = cloud20.updateDomain(userAdminToken, domainId2, updateDomain, accept, content)

        then:
        updateDomainResponse.status == SC_FORBIDDEN

        when: "Updating sessionInactivityTimeout on domain using user-manage token from a different domain"
        def userManageToken = utils.getToken(userManage.username)
        updateDomainResponse = cloud20.updateDomain(userManageToken, domainId2, updateDomain, accept, content)

        then:
        updateDomainResponse.status == SC_FORBIDDEN

        when: "Updating sessionInactivityTimeout exceeding max duration"
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT25H")
        updateDomainResponse = cloud20.updateDomain(utils.identityAdminToken, domainId, updateDomain, accept, content)
        Duration maxDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMaxDuration()
        Duration minDuration = identityConfig.getReloadableConfig().getSessionInactivityTimeoutMinDuration()
        String errMsg = String.format(Validator20.SESSION_INACTIVITY_TIMEOUT_RANGE_ERROR_MESSAGE, minDuration.getSeconds(), maxDuration.getSeconds())

        then:
        if(flag) {
            assertOpenStackV2FaultResponse(updateDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)
        } else{
            assertRackspaceCommonFaultResponse(updateDomainResponse, com.rackspace.api.common.fault.v1.BadRequestFault, SC_BAD_REQUEST, errMsg)
        }

        when: "Updating sessionInactivityTimeout less than min duration"
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT4M")
        updateDomainResponse = cloud20.updateDomain(utils.identityAdminToken, domainId, updateDomain, accept, content)

        then:
        if(flag) {
            assertOpenStackV2FaultResponse(updateDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)
        } else{
            assertRackspaceCommonFaultResponse(updateDomainResponse, com.rackspace.api.common.fault.v1.BadRequestFault, SC_BAD_REQUEST, errMsg)
        }

        when: "Updating with incorrect domain id"
        updateDomain.sessionInactivityTimeout = factory.newDuration("PT24H")
        updateDomain.id = "id"
        updateDomainResponse = cloud20.updateDomain(utils.identityAdminToken, domainId, updateDomain, accept, content)
        errMsg = "Domain Id does not match."

        then:
        if(flag) {
            assertOpenStackV2FaultResponse(updateDomainResponse, BadRequestFault, SC_BAD_REQUEST, errMsg)
        } else{
            assertRackspaceCommonFaultResponse(updateDomainResponse, com.rackspace.api.common.fault.v1.BadRequestFault, SC_BAD_REQUEST, errMsg)
        }

        when: "Updating rackspaceCustomerNumber longer than 32 characters"
        updateDomain = v2Factory.createDomain(domainId, domainId)
        updateDomain.rackspaceCustomerNumber = testUtils.getRandomUUIDOfLength("RCN",33)
        updateDomainResponse = cloud20.addDomain(utils.identityAdminToken, updateDomain, accept, content)

        then:
        assertOpenStackV2FaultResponseWithErrorCode(updateDomainResponse, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)

        where:
        flag  | accept                          | content
        true  | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        true  | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        false | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        false | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Verify that user-admin or user-manage cannot update attributes on domain - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def rcn = "RCN-123-123-123"
        def description = "description"
        def domain = v2Factory.createDomain(domainId, domainId, true, description, null, rcn)
        cloud20.updateDomain(utils.getIdentityAdminToken(), domainId, domain)

        when: "Attempt to update attribute on a domain using a user-admin token"
        def userAdminToken = utils.getToken(userAdmin.username)
        def updateDomainResponse = cloud20.updateDomain(userAdminToken, domainId, updateDomain, accept, content)
        def domainEntity = updateDomainResponse.getEntity(Domain)

        then:
        updateDomainResponse.status == SC_OK
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.description == description
        domainEntity.rackspaceCustomerNumber == rcn
        domainEntity.sessionInactivityTimeout.toString() == identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString()

        when: "Attempt to update attribute on a domain using a user-manage token"
        def userManageToken = utils.getToken(userManage.username)
        updateDomainResponse = cloud20.updateDomain(userManageToken, domainId, updateDomain, accept, content)
        domainEntity = updateDomainResponse.getEntity(Domain)

        then:
        updateDomainResponse.status == SC_OK
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.description == description
        domainEntity.rackspaceCustomerNumber == rcn
        domainEntity.sessionInactivityTimeout.toString() == identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString()

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        updateDomain                                                     | accept                          | content
        v2Factory.createDomain(null, "otherName")                        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        v2Factory.createDomain(null, "otherName")                        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        v2Factory.createDomain(null, null, true, "otherDesc")            | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        v2Factory.createDomain(null, null, true, "otherDesc")            | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        v2Factory.createDomain(null, null, true, null, null, "otherRCN") | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        v2Factory.createDomain(null, null, true, null, null, "otherRCN") | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "Verify sessionInactivityTimeout is return on get/list domain"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "Get domain"
        def getDomainResponse = cloud20.getDomain(utils.getIdentityAdminToken(), domainId)
        def domainEntity = getDomainResponse.getEntity(Domain)

        then:
        domainEntity.id == domainId
        domainEntity.name == domainId
        domainEntity.enabled
        domainEntity.sessionInactivityTimeout.toString() == identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().toString()

        when: "List domains"
        def getDomainsResponse = cloud20.getAccessibleDomains(utils.getIdentityAdminToken())
        def domainsEntity = getDomainsResponse.getEntity(Domains)

        then:
        for (def domain : domainsEntity.domain){
            assert domain.sessionInactivityTimeout.toString() != null
        }

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def removeDomainFromUser(username) {
        def user = userService.checkAndGetUserByName(username)
        user.setDomainId(null)
        userService.updateUser(user)
    }

    def void assertDomainDoesNotContainTenant(domainId, tenantId) {
        def domainTenantResponse = cloud20.getDomainTenants(utils.getServiceAdminToken(), domainId)
        if (domainTenantResponse.status == HttpStatus.SC_NOT_FOUND) {
            IdmAssert.assertOpenStackV2FaultResponse(domainTenantResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, GlobalConstants.ERROR_MSG_NO_TENANTS_BELONG_TO_DOMAIN)
        } else {
            assert domainTenantResponse.getEntity(Tenants).value.tenant.find {it.id == tenantId} == null
        }
     }

    def void assertDomainContainsTenant(domainId, tenantId) {
        def domainTenantResponse = cloud20.getDomainTenants(utils.getServiceAdminToken(), domainId)
        assert domainTenantResponse.getEntity(Tenants).value.tenant.find {it.id == tenantId} != null
    }

    def void assertSessionInactivityTimeoutForSingleDomainList(response, contentType, expectedSessionInactivityTimeout) {
        def returnedSessionInactivityTimeout
        if (contentType == MediaType.APPLICATION_XML_TYPE) {
            def parsedResponse = response.getEntity(Domains)
            returnedSessionInactivityTimeout = parsedResponse.domain[0].sessionInactivityTimeout.toString()
        } else {
            def parsedResponse = new JsonSlurper().parseText(response.getEntity(String))
            returnedSessionInactivityTimeout = parsedResponse[JSONConstants.RAX_AUTH_DOMAINS][0].sessionInactivityTimeout
        }

        assert returnedSessionInactivityTimeout == expectedSessionInactivityTimeout
    }

    def getDomainFromAccessibleDomainListById(def response, def domainId) {
        int page = 0
        def queryParams = parseLinks(response.headers.get("link"))
        Map<Integer, Integer> responseStatus = new HashMap<Integer, Integer>()
        def domain = response.getEntity(Domains).domain.find({it.id == domainId})

        while (queryParams.containsKey("next") && domain == null) {
            page++
            response = cloud20.getAccessibleDomains(utils.getIdentityAdminToken(), queryParams["next"][0], queryParams["next"][1])
            domain = response.getEntity(Domains).domain.find({it.id == domainId})
            queryParams = parseLinks(response.headers.get("link"))
            responseStatus.put(page, response.status)
        }

        return domain
    }

    def parseLinks(List<String> header) {
        List<String> links = header[0].split(",")
        Map<String, String[]> params = new HashMap<String, String[]>()
        setLinkParams(links, params)
        return params
    }

}
