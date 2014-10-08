package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriInfo

import static org.mockito.Mockito.mock;

class Cloud20DomainIntegrationTest extends RootIntegrationTest {

    @Autowired DomainService domainService;
    @Autowired Cloud20Service cloud20Service;
    @Autowired UserService userService;
    @Autowired TenantService tenantService;

    @Autowired UserDao userDao;

    // Keystone V3 compatibility
    def "Test if 'cloud20Service.addTenant(...)' adds users 'domainId' to the tenant"() {
        given:
        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)

        def userId = utils.createIdentityAdmin().id
        def user = userDao.getUserById(userId)
        user.setDomainId(domainId)
        userDao.updateUser(user)

        def token = utils.getToken(user.username)
        def tenantName = testUtils.getRandomUUID("tenant")

        when:
        def tenant = new org.openstack.docs.identity.api.v2.Tenant()
        tenant.setName(tenantName)
        cloud20Service.addTenant(mock(HttpHeaders), mock(UriInfo), token, tenant)
        def result = tenantService.getTenantByName(tenantName)
        def domain = domainService.getDomain(domainId)

        then:
        result.domainId == domainId
        Arrays.asList(domain.tenantIds).contains(result.tenantId)

        cleanup:
        try { tenantService.deleteTenant(tenantName) } catch (Exception e) {}
        try { userDao.deleteUser(user) } catch (Exception e) {}
        try { domainService.deleteDomain(domainId) } catch (Exception e) {}
    }

    // Keystone V3 compatibility
    def "Test if 'domainService.addTenantToDomain(...)' adds 'domainId' to the tenant"() {
        given:
        def domainId = utils.createDomain()
        domainService.createNewDomain(domainId)
        def tenant = utils.createTenant()

        when:
        domainService.addTenantToDomain(tenant.id, domainId)
        def result = tenantService.getTenantByName(tenant.name)

        then:
        result.domainId == domainId

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
}
