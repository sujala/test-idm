package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.HttpHeaders
import testHelpers.RootIntegrationTest

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

}
