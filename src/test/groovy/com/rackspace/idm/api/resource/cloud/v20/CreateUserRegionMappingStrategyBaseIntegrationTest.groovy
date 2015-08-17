package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultEndpointService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspacecloud.docs.auth.api.v1.User
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.math.RandomUtils
import org.openstack.docs.identity.api.v2.EndpointList
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

abstract class CreateUserRegionMappingStrategyBaseIntegrationTest extends RootIntegrationTest {
    @Shared def identityAdminToken

    @Autowired def UserService userService
    @Autowired def Configuration config
    @Autowired def EndpointService endpointService

    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
    }

    def void "assertEndpointLogic"(int baseUrlID, String baseUrlrsRegion, boolean shouldBaseUrlHaveBeenAdded) {
        try {
            //add new endpoint template/base url (use 1.1 way in order to set def to true)
            def addBaseUrlResponse = utils11.addBaseUrl(baseUrlID, "cloudServers", baseUrlrsRegion, true, true, "http://public.com/v1", "http://adminURL.com/v1", "http://internalURL.com/v1", DefaultUserService.MOSSO_BASE_URL_TYPE)
            def baseUrlLocation = addBaseUrlResponse.getHeaders().get('Location')[0]
            def returnedBaseUrlId = utils11.baseUrlIdFromLocation(baseUrlLocation)
            assert Integer.parseInt(returnedBaseUrlId) == baseUrlID

            assertEndpointLogic20(baseUrlID, shouldBaseUrlHaveBeenAdded)
            assertEndpointLogic11(baseUrlID, shouldBaseUrlHaveBeenAdded)

        } finally {
            cloud20.deleteEndpointTemplate(identityAdminToken, String.valueOf(baseUrlID))
        }
    }
    private def void "assertEndpointLogic20"(int baseUrlID, boolean shouldBaseUrlHaveBeenAdded) {
        def username = "v20Username" + testUtils.getRandomUUID()
        def userEntity;
        def tenants;
        def domainId = utils.createDomain()
        try {
            //add a new v2.0 user with one user call (to add mosso/nast tenants)
            def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, null, domainId, "Password1")
            def secretQA = v2Factory.createSecretQA("question", "answer")
            user.secretQA = secretQA
            cloud20.createUser(identityAdminToken, user)
            userEntity = userService.getUser(username)

            //get the mosso tenant created for the user
            tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value
            def mossoTenant = tenants.tenant.find{ t -> t.id == domainId}  //mosso tenant has id == domain id
            assert mossoTenant != null

            EndpointList endpoints = utils.listEndpointsForTenant(identityAdminToken, mossoTenant.id)

            assert (endpoints.endpoint.find{ e -> e.id == baseUrlID} != null) == shouldBaseUrlHaveBeenAdded
        }
        finally {
            cloud20.deleteUser(identityAdminToken, userEntity.id)
            cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
            cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
            cloud20.deleteDomain(identityAdminToken, domainId)
        }
    }

    private def void "assertEndpointLogic11"(int baseUrlID, boolean shouldBaseUrlHaveBeenAdded) {
        def username = "v11Username" + testUtils.getRandomUUID()
        def userEntity;
        def tenants;
        def domainId = utils.createDomain()
        try {
            //add a new v1.1 user (to add mosso/nast tenants)
            def user = v1Factory.createUser(username, "asdf", Integer.parseInt(domainId))
            cloud11.createUser(user)
            userEntity = userService.getUser(username)

            //get the mosso tenant created for the user
            tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value
            def mossoTenant = tenants.tenant.find{ t -> t.id == domainId}  //mosso tenant has id == domain id
            assert mossoTenant != null

            EndpointList endpoints = utils.listEndpointsForTenant(identityAdminToken, mossoTenant.id)

            assert (endpoints.endpoint.find{ e -> e.id == baseUrlID} != null) == shouldBaseUrlHaveBeenAdded
        }
        catch (Exception ex) {
            def x = 1
        }
        finally {
            cloud20.deleteUser(identityAdminToken, userEntity.id)
            cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
            cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
            cloud20.deleteDomain(identityAdminToken, domainId)
        }
    }
}
