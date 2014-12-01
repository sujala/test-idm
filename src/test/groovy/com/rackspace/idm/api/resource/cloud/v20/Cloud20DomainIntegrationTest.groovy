package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.HttpHeaders
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.UriInfo

import static com.rackspace.idm.Constants.*

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

}
