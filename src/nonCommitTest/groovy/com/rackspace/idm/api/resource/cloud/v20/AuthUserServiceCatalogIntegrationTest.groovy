package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class AuthUserServiceCatalogIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdminToken

    @Autowired def ScopeAccessService scopeAccessService
    @Autowired def TenantService tenantService
    @Autowired def UserService userService
    @Autowired def Configuration config
    @Autowired def ApplicationRoleDao applicationRoleDao

    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "service catalog IS NOT filtered when tenant IS NOT specified in auth request"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value

        then:
        // ensure tenants were created
        !tenants.tenant.isEmpty()
        tenants.tenant.size == 2

        when:
        def authRequest = v2Factory.createPasswordAuthenticationRequest(username, "Password1")
        def auth = cloud20.authenticate(authRequest).getEntity(AuthenticateResponse).value

        then:
        auth.serviceCatalog.service.size == 9
        auth.token.tenant.id == domainId

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
        cloud20.deleteDomain(identityAdminToken, domainId)
    }

    def "service catalog is NOT filtered when mosso tenant specified in auth request"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value

        then:
        // ensure tenants were created
        !tenants.tenant.isEmpty()
        tenants.tenant.size == 2

        def mossoTenant = tenants.tenant.find {
            !it.name.startsWith(Constants.NAST_TENANT_PREFIX)
        }
        def mossoId = mossoTenant.id
        def mossoName = mossoTenant.name

        when:
        def authRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(username, "Password1", mossoId)
        def auth = cloud20.authenticate(authRequest).getEntity(AuthenticateResponse).value

        then:
        auth.serviceCatalog.service.size == 9
        auth.token.tenant.id == domainId

        when:
        def authRequest2 = v2Factory.createPasswordAuthenticationRequestWithTenantName(username, "Password1", mossoName)
        def auth2 = cloud20.authenticate(authRequest2).getEntity(AuthenticateResponse).value

        then:
        auth2.serviceCatalog.service.size == 9
        auth.token.tenant.id == domainId

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
        cloud20.deleteDomain(identityAdminToken, domainId)
    }

    def "service catalog IS filtered when tenant other than mossoId is specified in auth request"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value

        then:
        // ensure tenants were created
        !tenants.tenant.isEmpty()
        tenants.tenant.size == 2

        def nastTenant = tenants.tenant.find {
            it.name.startsWith(Constants.NAST_TENANT_PREFIX)
        }
        def nastId = nastTenant.id
        def nastName = nastTenant.name

        when:
        def authRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(username, "Password1", nastId)
        def auth = cloud20.authenticate(authRequest).getEntity(AuthenticateResponse).value

        then:
        auth.serviceCatalog.service.size == 2
        auth.token.tenant.id == nastId

        when:
        def authRequest2 = v2Factory.createPasswordAuthenticationRequestWithTenantName(username, "Password1", nastName)
        def auth2 = cloud20.authenticate(authRequest2).getEntity(AuthenticateResponse).value

        then:
        auth2.serviceCatalog.service.size == 2
        auth.token.tenant.id == nastId

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
        cloud20.deleteDomain(identityAdminToken, domainId)
    }
}
