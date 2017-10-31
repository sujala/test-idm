package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.TokenForAuthenticationRequest
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.xml.bind.JAXBElement

class DefaultAuthenticateResponseServiceTest extends RootServiceTest {

    @Shared AuthenticateResponseService service

    def setup() {
        service = new DefaultAuthenticateResponseService()
        mockAuthenticationContext(service)
        mockDomainService(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockIdentityConfig(service)
        mockAuthorizationService(service)
        mockTokenConverter(service)
        mockAuthConverterCloudV20(service)
        mockJAXBObjectFactories(service)
    }

    @Unroll
    def "buildAuthResponseForAuthenticate: filter service catalog for impersonation tokens of suspended users, shouldDisplayServiceCatalog = #shouldDisplayServiceCatalog"() {
        given:
        String token = RandomStringUtils.randomAlphanumeric(8)
        String domainId = RandomStringUtils.randomAlphanumeric(8)
        User user = new User().with {
            it.domainId = domainId
            it
        }
        def tenant = new Tenant().with {
            it.tenantId = RandomStringUtils.randomAlphanumeric(8)
            it.name = it.tenantId
            it
        }
        UserScopeAccess scopeAccess = new UserScopeAccess()
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, scopeAccess).with {
            it.impersonatedScopeAccess = new ImpersonatedScopeAccess()
            it
        }
        AuthenticationRequest authenticationRequest = new AuthenticationRequest().with {
            it.tenantId = tenant.tenantId
            it.token = new TokenForAuthenticationRequest().with {
                it.id = token
                it
            }
            it
        }
        OpenstackEndpoint endpoint = new OpenstackEndpoint().with {
            it.tenantId = tenant.tenantId
            it.tenantName = tenant.name
            it.baseUrls = [new CloudBaseUrl()]
            it
        }
        ServiceCatalogInfo serviceCatalogInfo = new ServiceCatalogInfo().with {
            it.userTenants = [tenant]
            it.userEndpoints = [endpoint]
            it
        }
        jaxbObjectFactories.getOpenStackIdentityV2Factory().createAccess(_) >> Mock(JAXBElement)

        when:
        service.buildAuthResponseForAuthenticate(authResponseTuple, authenticationRequest)

        then: "correct backend services are called"
        1 * scopeAccessService.getServiceCatalogInfo(authResponseTuple.getUser()) >> serviceCatalogInfo
        authorizationService.restrictUserAuthentication(_) >> true
        1 * identityConfig.getReloadableConfig().shouldDisplayServiceCatalogForSuspendedUserImpersonationTokens() >> shouldDisplayServiceCatalog
        1 * authConverterCloudV20.toAuthenticationResponse(_ as AuthResponseTuple, _ as ServiceCatalogInfo) >> { args ->
            ServiceCatalogInfo scInfo = args[1]
            if (shouldDisplayServiceCatalog) {
                assert !scInfo.getUserEndpoints().isEmpty()
            } else {
                assert scInfo.getUserEndpoints().isEmpty()
            }
            return new AuthenticateResponse().with {
                it.user = new UserForAuthenticateResponse()
                it
            }
        }

        where:
        shouldDisplayServiceCatalog << [true, false]
    }

}
