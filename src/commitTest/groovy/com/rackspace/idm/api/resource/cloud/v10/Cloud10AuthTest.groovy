package com.rackspace.idm.api.resource.cloud.v10

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11
import com.rackspace.idm.api.resource.cloud.CloudClient
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog
import testHelpers.RootServiceTest

class Cloud10AuthTest extends RootServiceTest {
    Cloud10VersionResource service

    CloudClient cloudClient
    EndpointConverterCloudV11 endpointConverterCloudV11
    Cloud10VersionResource cloud10VersionResource

    public void setup() {
        cloudClient = Mock(CloudClient)
        scopeAccessService = Mock(ScopeAccessService)
        endpointConverterCloudV11 = Mock(EndpointConverterCloudV11)
        endpointService = Mock(EndpointService)
        authWithApiKeyCredentials = Mock(AuthWithApiKeyCredentials)
        authorizationService = Mock(AuthorizationService)

        service = new Cloud10VersionResource(scopeAccessService, endpointConverterCloudV11, authWithApiKeyCredentials, authorizationService);
        service.requestContextHolder = Mock(RequestContextHolder)
        service.requestContextHolder.getAuthenticationContext() >> Mock(AuthenticationContext)
    }

    /**
     * It's difficult to demonstrate that auth 1.0 uses cached roles through an integration/API
     * test because roles are not returned in the service's response like they are with v2.0 auth or validate.
     *
     * The ScopeAccessService getServiceCatalogInfo method is verified through other tests to appropriate use cached
     * roles or not based on various property configuration settings.
     *
     * By showing that this method calls the method that has been verified to correctly use (or not) the cache
     * we verify this service would use the cached roles itself, as appropriate.
     */
    def "v11 auth retrieves service catalog info from DefaultScopeAccessService"() {
        given:
        User user = entityFactory.createUser()
        UserScopeAccess sa = entityFactory.createUserToken()
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, sa)

        scopeAccessService.createScopeAccessForUserAuthenticationResult(_) >> authResponseTuple
        userService.getUserByScopeAccess(_, _) >> user
        authorizationService.restrictTokenEndpoints(_) >> true
        authWithApiKeyCredentials.authenticate(_, _) >> new UserAuthenticationResult(user, true)
        endpointConverterCloudV11.toServiceCatalog(_) >> new ServiceCatalog()

        when:
        service.getCloud10VersionInfo(headers, "username", "password", null, null)

        then:
        1 * scopeAccessService.getServiceCatalogInfo(user) >> new ServiceCatalogInfo()
    }

}
