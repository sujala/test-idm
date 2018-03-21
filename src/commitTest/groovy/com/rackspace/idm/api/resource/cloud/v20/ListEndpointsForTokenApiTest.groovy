package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.ProvisionedUserDelegate
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.DomainSubUserDefaults
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import org.opensaml.core.config.InitializationService
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.xml.bind.JAXBElement

class ListEndpointsForTokenApiTest extends RootServiceTest {

    DefaultCloud20Service service = new DefaultCloud20Service()

    def setupSpec() {
        // Always need to initialize SAML when creating DefaultCloud20Service
        InitializationService.initialize()
    }

    def setup() {
        mockRequestContextHolder(service)
        mockIdentityConfig(service)
        mockAuthorizationService(service)
        mockScopeAccessService(service)
        mockUserService(service)
        mockEndpointConverter(service)
        mockJAXBObjectFactories(service)
        mockExceptionHandler(service)
        service.setJaxbObjectFactories(new JAXBObjectFactories())

        ScopeAccess access = new UserScopeAccess()
        service.requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(_) >> access
        scopeAccessService.getScopeAccessByAccessToken(_) >> access

        headers = Mock(HttpHeaders)
        jaxbMock = Mock(JAXBElement)
    }

    /**
     * It's difficult to demonstrate that the listEndpointsForToken service uses cached roles through an integration
     * test because roles are not returned in the response like they are with auth or validate. The
     * ScopeAccessService getServiceCatalogInfo method is verified through other tests to appropriate use cached
     * roles or not based on various property configuration settings.
     *
     * By showing that this method calls the method that has been verified to correctly use (or not) the cache
     * we verify the service would use the cached roles itself, as appropriate.
     */
    def "List endpoints for tokens calls Scope Access Service getServiceCatalogInfo"() {
        given:
        def tokenId = "token"
        User user = entityFactory.createUser()
        userService.getUserByScopeAccess(_, _) >> user
        authorizationService.restrictTokenEndpoints(_) >> true

        when:
        service.listEndpointsForToken(headers, tokenId, tokenId, false)

        then:
        1 * scopeAccessService.getServiceCatalogInfo(user) >> new ServiceCatalogInfo()
    }

    @Unroll
    def "listEndpointsForToken: always applies RCN roles for DA tokens, applyRcnRoles = #applyRcnRoles"() {
        given:
        def callerToken = "callerToken"
        def subjectToken = "subjectToken"
        ScopeAccess sa = entityFactory.createUserToken()
        def subjectUser = Mock(ProvisionedUserDelegate)
        scopeAccessService.getScopeAccessByAccessToken(subjectToken) >> sa
        userService.getUserByScopeAccess(_, false) >> subjectUser
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(callerToken) >> entityFactory.createUserToken()

        when:
        service.listEndpointsForToken(headers, callerToken, subjectToken, applyRcnRoles)

        then:
        1 * scopeAccessService.getServiceCatalogInfoApplyRcnRoles(subjectUser) >> new ServiceCatalogInfo()
        0 * scopeAccessService.getServiceCatalogInfo(subjectUser)

        where:
        applyRcnRoles << [true, false]
    }

}
