package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfigHolder
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainAuthRequest
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainRequestHandler
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.lang3.RandomStringUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService
import org.opensaml.security.credential.Credential
import spock.lang.Shared
import testHelpers.RootServiceTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import java.security.Security

class FederatedDomainRequestHandlerTest extends RootServiceTest {

    @Shared FederatedDomainRequestHandler service

    @Shared FederatedDomainAuthRequestGenerator sharedDomainRequestGenerator
    @Shared Credential sharedBrokerIdpCredential
    @Shared Credential sharedOriginIdpCredential

    @Shared IdentityConfig oldIdentityConfig

    def setupSpec() {
        Security.addProvider(new BouncyCastleProvider())
        InitializationService.initialize()
        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedDomainRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        oldIdentityConfig = IdentityConfigHolder.IDENTITY_CONFIG
    }

    def setup() {
        service = new FederatedDomainRequestHandler()
        mockIdentityConfig(service)
        IdentityConfigHolder.IDENTITY_CONFIG = identityConfig
        reloadableConfig.shouldV2FederationValidateOriginIssueInstant() >> false
        identityConfig.getReloadableConfig().getFederatedDomainTokenLifetimeMax() >> 1000
        mockDomainService(service)
        mockFederatedUserDao(service)
        mockIdentityUserService(service)
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockAuthorizationService(service)
        mockApplicationService(service)
        mockScopeAccessService(service)
        mockAuthenticationContext(service)
        mockUserGroupService(service)
    }

    void cleanupSpec() {
        /**
         * This is a complete hack. The IdentityConfigHolder is a hack in and of itself to allow non spring injected beans
         * access to the spring loaded IdentityConfig bean into a static context. However, this test uses this class which
         * requires the IdentifyConfig set so injects it with Mock. However, if Spring context was loaded for an integration
         * test prior to running this component test then the spring loaded config will be replaced with a mock and never
         * replaced. So this test needs to reset the IdentifyConfigHolder to the old value.
         */
        IdentityConfigHolder.IDENTITY_CONFIG = oldIdentityConfig
    }

    def "requested groups are required to exist"() {
        given:
        IdentityProvider idp = new IdentityProvider().with {
            it.approvedDomainGroup = "GLOBAL"
            it
        }
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def groupName = RandomStringUtils.randomAlphanumeric(8)
        def userGroup = new UserGroup().with {
            it.uniqueId = "o=${RandomStringUtils.randomAlphanumeric(8)}"
            it
        }
        FederatedDomainAuthGenerationRequest authGenerationRequest = createValidDomainAuthGenerationRequest().with {
            it.groupNames = [groupName]
            it.domainId = domainId
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(authGenerationRequest)
        FederatedDomainAuthRequest authRequest = new FederatedDomainAuthRequest(samlResponse)
        domainService.getDomain(domainId) >> new Domain().with {
            it.enabled = true
            it
        }
        def userAdmin = new User().with {
            it.enabled = true
            it
        }
        domainService.getDomainAdmins(domainId) >> [userAdmin]
        authorizationService.getCachedIdentityRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> new ImmutableClientRole(new ClientRole())
        tenantService.getTenantRolesForUser(userAdmin) >> []
        scopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo()

        when:
        service.processAuthRequestForProvider(authRequest, idp, false)

        then:
        1 * userGroupService.getGroupByNameForDomain(groupName, domainId) >> userGroup
        1 * federatedUserDao.addUser(idp, _) >> { args ->
            FederatedUser fedUserToSave = args[1]
            assert fedUserToSave.userGroupDNs.contains(userGroup.getGroupDn())
        }

        when:
        authGenerationRequest = createValidDomainAuthGenerationRequest().with {
            it.groupNames = ["thisGroupDoesNotExist"]
            it.domainId = domainId
            it
        }
        samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(authGenerationRequest)
        authRequest = new FederatedDomainAuthRequest(samlResponse)
        domainService.getDomain(domainId) >> new Domain().with {
            it.enabled = true
            it
        }
        service.processAuthRequestForProvider(authRequest, idp, false)

        then:
        thrown BadRequestException
        0 * federatedUserDao.addUser(_, _)
    }

    def "previously assigned groups are removed from the fed user if they are not provided in the most recent request"() {
        given:
        IdentityProvider idp = new IdentityProvider().with {
            it.approvedDomainGroup = "GLOBAL"
            it
        }
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def groupName = RandomStringUtils.randomAlphanumeric(8)
        def userGroup = new UserGroup().with {
            it.uniqueId = "group=${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = groupName
            it
        }
        FederatedDomainAuthGenerationRequest authGenerationRequest = createValidDomainAuthGenerationRequest().with {
            it.groupNames = [groupName]
            it.domainId = domainId
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(authGenerationRequest)
        FederatedDomainAuthRequest authRequest = new FederatedDomainAuthRequest(samlResponse)
        domainService.getDomain(domainId) >> new Domain().with {
            it.enabled = true
            it
        }
        def userAdmin = new User().with {
            it.enabled = true
            it.domainId = domainId
            it
        }
        domainService.getDomainAdmins(domainId) >> [userAdmin]
        authorizationService.getCachedIdentityRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> new ImmutableClientRole(new ClientRole())
        tenantService.getTenantRolesForUser(userAdmin) >> []
        scopeAccessService.getServiceCatalogInfo(_) >> new ServiceCatalogInfo()
        def userGroupToRemove = new UserGroup().with {
            it.uniqueId = "group=${RandomStringUtils.randomAlphanumeric(8)}"
            it
        }
        FederatedUser federatedUser = new FederatedUser().with {
            it.username = authGenerationRequest.username
            it.domainId = domainId
            it.expiredTimestamp = new DateTime().plusYears(1).toDate()
            it.userGroupDNs = [userGroupToRemove.getGroupDn()]
            it
        }
        federatedUserDao.getUserByUsernameForIdentityProviderId(authGenerationRequest.username, _) >> federatedUser
        tenantService.getRbacRolesForUser(federatedUser) >> []

        when:
        service.processAuthRequestForProvider(authRequest, idp, false)

        then:
        1 * userGroupService.getGroupByNameForDomain(groupName, domainId) >> userGroup
        1 * federatedUserDao.updateUser(federatedUser) >> { args ->
            FederatedUser fedUserToUpdate = args[0]
            assert fedUserToUpdate.getUserGroupDNs().contains(userGroup.getGroupDn())
            assert !fedUserToUpdate.getUserGroupDNs().contains(userGroupToRemove.getGroupDn())
        }
    }

    def createValidDomainAuthGenerationRequest(username = UUID.randomUUID()) {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = RandomStringUtils.randomAlphanumeric(10)
            it.validitySeconds = 100
            it.brokerIssuer = RandomStringUtils.randomAlphanumeric(16)
            it.originIssuer = RandomStringUtils.randomAlphanumeric(16)
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = username
            it.roleNames = [] as Set
            it
        }
    }

}
