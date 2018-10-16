package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory
import org.apache.commons.lang3.RandomStringUtils
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.xml.bind.JAXBElement

class UpdateDomainIntegrationTest extends RootServiceTest {

    @Shared DefaultCloud20Service service

    def setupSpec() {
        InitializationService.initialize()

        service = new DefaultCloud20Service()
    }

    def setup() {
        mockIdentityConfig(service)
        mockAuthorizationService(service)
        mockDomainService(service)
        mockValidator20(service)
        mockJAXBObjectFactories(service)
        mockScopeAccessService(service)
        mockUserService(service)
        mockDomainConverter(service)
        mockRequestContextHolder(service)

        service.jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory() >> Mock(ObjectFactory)
    }

    @Unroll
    def "a domain's RCN can only be updated using the update domain API call if the feature is enabled, featureEnabled = #featureEnabled"() {
        given:
        def token = "token"
        allowUserAccess()
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def rcn = RandomStringUtils.randomAlphanumeric(8)
        def domain = new Domain().with {
            it.id = domainId
            it.rackspaceCustomerNumber = rcn
            it.enabled = false
            it
        }
        def originalRcn = RandomStringUtils.randomAlphanumeric(8)
        def domainEntity = new com.rackspace.idm.domain.entity.Domain().with {
            it.rackspaceCustomerNumber = originalRcn
            it.enabled = !domain.enabled
            it
        }
        domainService.checkAndGetDomain(domainId) >> domainEntity
        service.jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomain(_) >> Mock(JAXBElement)
        identityConfig.getReloadableConfig().isUpdateDomainRcnOnUpdateDomainAllowed() >> featureEnabled

        when:
        service.updateDomain(token, domainId, domain).build()

        then:
        1 * domainService.updateDomain(_) >> { args ->
            com.rackspace.idm.domain.entity.Domain domainData = args[0]
            domainData.rackspaceCustomerNumber == featureEnabled ? rcn : originalRcn
            // Verify that the other provided attribute is still being updated
            domainData.enabled == domainEntity.enabled
        }

        where:
        featureEnabled << [true, false]
    }

}
