package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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

    @Unroll
    def "Test global endpoints in service catalog, feature.global.endpoints.for.all.roles.enabled = #flag" () {
        given: "New user"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, flag)
        def adminToken = utils.getIdentityAdminToken()
        def serviceAdminToken = utils.getServiceAdminToken()
        def username = "globalEndpointUser" + getRandomUUID()
        def domainId = utils.createDomain()
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def createUserResponse = cloud20.createUser(adminToken, userForCreate)
        def user = createUserResponse.getEntity(User).value

        when: "Authenticating user"
        AuthenticateResponse auth = utils.authenticate(userForCreate)

        then: "Assert empty catalog"
        auth.serviceCatalog.service.size == 0

        when: "Assign two roles to user on tenant and authenticate"
        def createdTenant = utils.createTenant()
        // Add role manually to force order when building service catalog
        def roleName1 = getRandomUUID("role1")
        def identityId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        ClientRole role1  = entityFactory.createClientRole(roleName1).with {
            it.id = getRandomUUID("a")
            it.clientId = identityId
            it
        }
        Application identity = entityFactory.createApplication(identityId, "Cloud Auth Service")
        applicationRoleDao.addClientRole(identity, role1)
        def cloudServersId = "a45b14e394a57e3fd4e45d59ff3693ead204998b";
        def roleName2 = getRandomUUID("role2")
        ClientRole role2  = entityFactory.createClientRole(roleName2).with {
            it.id = getRandomUUID("b")
            it.clientId = cloudServersId
            it
        }
        Application cloudServers = entityFactory.createApplication(cloudServersId, "cloudServers")
        applicationRoleDao.addClientRole(cloudServers, role2)
        utils.addRoleToUserOnTenant(user, createdTenant, role1.id)
        utils.addRoleToUserOnTenant(user, createdTenant, role2.id)
        auth = utils.authenticate(userForCreate)

        then: "Assert three roles and empty catalog"
        auth.user.roles.role.size == 3
        auth.serviceCatalog.service.size == 0

        when: "Add new MOSSO global endpoint and update to global"
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def publicUrl = "http://publicUrl.com"
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, publicUrl, null, true, null, cloudServersId, "MOSSO")
        cloud20.addEndpointTemplate(adminToken, endpointTemplate)
        endpointTemplate.enabled = true
        endpointTemplate.global = true
        def updateEndpointTemplateResponse = cloud20.updateEndpointTemplate(serviceAdminToken, endpointTemplateId, endpointTemplate)

        then: "Assert updated global endpoint"
        updateEndpointTemplateResponse.status == 200

        when: "Authenticate"
        auth = utils.authenticate(userForCreate)

        then: "Assert catalog size"
        if (flag) {
            assert auth.serviceCatalog.service.size == 1
        } else {
            assert auth.serviceCatalog.service.size == 0
        }

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        cloud20.deleteTenant(identityAdminToken, createdTenant.id)
        cloud20.deleteRole(identityAdminToken, role1.id)
        cloud20.deleteRole(identityAdminToken, role2.id)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)

        where:
        flag  | _
        true  | _
        false | _
    }

    @Unroll
    def "Assert global endpoints in service catalog are not duplicate with two roles in the same service, feature.global.endpoints.for.all.roles.enabled = #flag" () {
        given: "New user"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, flag)
        def adminToken = utils.getIdentityAdminToken()
        def serviceAdminToken = utils.getServiceAdminToken()
        def username = "globalEndpointUser" + getRandomUUID()
        def domainId = utils.createDomain()
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def createUserResponse = cloud20.createUser(adminToken, userForCreate)
        def user = createUserResponse.getEntity(User).value

        when: "Authenticating user"
        AuthenticateResponse auth = utils.authenticate(userForCreate)

        then: "Assert empty catalog"
        auth.serviceCatalog.service.size == 0

        when: "Assign two roles to user on tenant and authenticate"
        def createdTenant = utils.createTenant()
        // Add role manually to force order when building service catalog
        def cloudServersId = "a45b14e394a57e3fd4e45d59ff3693ead204998b";
        Application cloudServers = entityFactory.createApplication(cloudServersId, "cloudServers")
        def roleName1 = getRandomUUID("role1")
        ClientRole role1  = entityFactory.createClientRole(roleName1).with {
            it.id = getRandomUUID("a")
            it.clientId = cloudServersId
            it
        }
        applicationRoleDao.addClientRole(cloudServers, role1)
        def roleName2 = getRandomUUID("role2")
        ClientRole role2  = entityFactory.createClientRole(roleName2).with {
            it.id = getRandomUUID("b")
            it.clientId = cloudServersId
            it
        }
        applicationRoleDao.addClientRole(cloudServers, role2)
        utils.addRoleToUserOnTenant(user, createdTenant, role1.id)
        utils.addRoleToUserOnTenant(user, createdTenant, role2.id)
        auth = utils.authenticate(userForCreate)

        then: "Assert three roles and empty catalog"
        auth.user.roles.role.size == 3
        auth.serviceCatalog.service.size == 0

        when: "Add new MOSSO global endpoint and update to global"
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def publicUrl = "http://publicUrl.com"
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, publicUrl, null, true, null, cloudServersId, "MOSSO")
        cloud20.addEndpointTemplate(adminToken, endpointTemplate)
        endpointTemplate.enabled = true
        endpointTemplate.global = true
        def updateEndpointTemplateResponse = cloud20.updateEndpointTemplate(serviceAdminToken, endpointTemplateId, endpointTemplate)

        then: "Assert updated global endpoint"
        updateEndpointTemplateResponse.status == 200

        when: "Authenticate"
        auth = utils.authenticate(userForCreate)

        then: "Assert catalog size"
        assert auth.serviceCatalog.service.size == 1
        assert auth.serviceCatalog.service[0].endpoint.size() == 1

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        cloud20.deleteTenant(identityAdminToken, createdTenant.id)
        cloud20.deleteRole(identityAdminToken, role1.id)
        cloud20.deleteRole(identityAdminToken, role2.id)
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)

        where:
        flag  | _
        true  | _
        false | _
    }

    @Unroll
    def "Assert global endpoints in service catalog for multiple services, feature.global.endpoints.for.all.roles.enabled = #flag" () {
        given: "New user"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED, flag)
        def adminToken = utils.getIdentityAdminToken()
        def serviceAdminToken = utils.getServiceAdminToken()
        def username = "globalEndpointUser" + getRandomUUID()
        def domainId = utils.createDomain()
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def createUserResponse = cloud20.createUser(adminToken, userForCreate)
        def user = createUserResponse.getEntity(User).value

        when: "Authenticating user"
        AuthenticateResponse auth = utils.authenticate(userForCreate)

        then: "Assert empty catalog"
        auth.serviceCatalog.service.size == 0

        when: "Assign two roles to user on tenant and authenticate"
        def createdTenant = utils.createTenant()
        // Add role manually to force order when building service catalog
        def cloudServersId = "a45b14e394a57e3fd4e45d59ff3693ead204998b";
        Application cloudServers = entityFactory.createApplication(cloudServersId, "cloudServers")
        def roleName1 = getRandomUUID("role1")
        ClientRole role1  = entityFactory.createClientRole(roleName1).with {
            it.id = getRandomUUID("a")
            it.clientId = cloudServersId
            it
        }
        applicationRoleDao.addClientRole(cloudServers, role1)
        def cloudFilesId = "6047d506862b81d6d99273b4853adfe81e0478c1"
        Application cloudFiles = entityFactory.createApplication(cloudFilesId, "cloudFiles")
        def roleName2 = getRandomUUID("role2")
        ClientRole role2  = entityFactory.createClientRole(roleName2).with {
            it.id = getRandomUUID("b")
            it.clientId = cloudFilesId
            it
        }
        applicationRoleDao.addClientRole(cloudFiles, role2)
        utils.addRoleToUserOnTenant(user, createdTenant, role1.id)
        utils.addRoleToUserOnTenant(user, createdTenant, role2.id)
        auth = utils.authenticate(userForCreate)

        then: "Assert three roles and empty catalog"
        auth.user.roles.role.size == 3
        auth.serviceCatalog.service.size == 0

        when: "Add new MOSSO and NAST global endpoints and update them to global"
        def mossoEndpointTemplateId = testUtils.getRandomIntegerString()
        def mossoPublicUrl = "http://mossoPublicUrl.com"
        def mossoEndpointTemplate = v1Factory.createEndpointTemplate(mossoEndpointTemplateId, null, mossoPublicUrl, null, true, null, cloudServersId, "MOSSO")
        cloud20.addEndpointTemplate(adminToken, mossoEndpointTemplate)
        mossoEndpointTemplate.enabled = true
        mossoEndpointTemplate.global = true
        def updateMossoEndpointTemplateResponse = cloud20.updateEndpointTemplate(serviceAdminToken, mossoEndpointTemplateId, mossoEndpointTemplate)
        def nastEndpointTemplateId = testUtils.getRandomIntegerString()
        def nastPublicUrl = "http://nastPublicUrl.com"
        def nastEndpointTemplate = v1Factory.createEndpointTemplate(nastEndpointTemplateId, null, nastPublicUrl, null, true, null, cloudFilesId, "NAST")
        cloud20.addEndpointTemplate(adminToken, nastEndpointTemplate)
        nastEndpointTemplate.enabled = true
        nastEndpointTemplate.global = true
        def updateNastEndpointTemplateResponse = cloud20.updateEndpointTemplate(serviceAdminToken, nastEndpointTemplateId, nastEndpointTemplate)

        then: "Assert updated global endpoints"
        updateMossoEndpointTemplateResponse.status == 200
        updateNastEndpointTemplateResponse.status == 200

        when: "Authenticate"
        auth = utils.authenticate(userForCreate)

        then: "Assert catalog size"
        if (flag) {
            assert auth.serviceCatalog.service.size == 2
            def serviceCloudServers = auth.serviceCatalog.service.find { it.name == cloudServers.name }
            assert serviceCloudServers.endpoint.size() == 1
            assert serviceCloudServers.endpoint[0].publicURL == mossoPublicUrl.concat("/" + createdTenant.id)
            def serviceCloudFiles = auth.serviceCatalog.service.find { it.name == cloudFiles.name }
            assert serviceCloudFiles.endpoint.size() == 1
            assert serviceCloudFiles.endpoint[0].publicURL == nastPublicUrl.concat("/" + createdTenant.id)
        } else {
            assert auth.serviceCatalog.service.size == 1
            assert auth.serviceCatalog.service[0].endpoint.size() == 1
            assert auth.serviceCatalog.service[0].endpoint[0].publicURL == mossoPublicUrl.concat("/" + createdTenant.id)
        }

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        cloud20.deleteTenant(identityAdminToken, createdTenant.id)
        cloud20.deleteRole(identityAdminToken, role1.id)
        cloud20.deleteRole(identityAdminToken, role2.id)
        utils.disableAndDeleteEndpointTemplate(mossoEndpointTemplateId)
        utils.disableAndDeleteEndpointTemplate(nastEndpointTemplateId)

        where:
        flag  | _
        true  | _
        false | _
    }
}
