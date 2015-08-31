package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
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

import static com.rackspace.idm.Constants.*

import static org.mockito.Mockito.mock;

class Cloud20DomainIntegrationTest extends RootIntegrationTest {

    @Autowired DomainService domainService;
    @Autowired Cloud20Service cloud20Service;
    @Autowired UserService userService;
    @Autowired TenantService tenantService;

    @Autowired
    private IdentityConfig identityConfig

    @Autowired UserDao userDao;

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
        tenantService.deleteTenant(tenant.id)
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
        tenantService.deleteTenant(tenant.id)
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
        tenantService.deleteTenant(tenant.id)
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
        tenantService.deleteTenant(tenant.id)
        domainService.deleteDomain(domainId)
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
        utils.deleteDomain(domainId1)
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

}
