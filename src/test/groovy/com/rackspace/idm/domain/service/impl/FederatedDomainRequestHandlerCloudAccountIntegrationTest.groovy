package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainAuthRequest
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainRequestHandler
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateUsernameException
import com.rackspace.idm.exception.ForbiddenException
import org.apache.commons.lang.RandomStringUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import java.security.cert.X509Certificate

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*

/**
 * Tests the various functionality for federating against Cloud Accounts
 */
class FederatedDomainRequestHandlerCloudAccountIntegrationTest extends RootIntegrationTest {
    private static final Logger log = Logger.getLogger(FederatedDomainRequestHandlerCloudAccountIntegrationTest.class)

    @Autowired
    FederatedDomainRequestHandler federatedDomainRequestHandler

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    DefaultIdentityUserService identityUserService

    @Autowired
    IdentityProviderDao identityProviderDao

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    FederatedUserDao federatedUserDao

    @Shared IdentityProvider sharedBrokerIdp
    @Shared com.rackspace.idm.domain.entity.IdentityProvider sharedBrokerIdpEntity
    @Shared Credential sharedBrokerIdpCredential

    @Shared IdentityProvider sharedOriginIdp
    @Shared com.rackspace.idm.domain.entity.IdentityProvider sharedOriginIdpEntity
    @Shared Credential sharedOriginIdpCredential

    @Shared FederatedDomainAuthRequestGenerator sharedRequestGenerator

    @Shared String sharedServiceAdminToken
    @Shared String sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared com.rackspace.idm.domain.entity.User sharedUserAdminEntity

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential();

        sharedBrokerIdp = cloud20.generateIdentityProviderWithCred(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = cloud20.generateIdentityProviderWithCred(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        sharedRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
    }

    def cleanupSpec() {
        try {
            cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedBrokerIdp.id)
            cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedOriginIdp.id)
            cloud20.deleteUser(sharedServiceAdminToken, sharedUserAdmin.id)
        } catch (Exception ex) {
            // EAT
            log.warn("Error cleanin up after test", ex)
        }
    }

    def setup() {
        sharedUserAdminEntity = identityUserService.getProvisionedUserById(sharedUserAdmin.id)
        sharedOriginIdpEntity = identityProviderDao.getIdentityProviderById(sharedOriginIdp.id)
        sharedBrokerIdpEntity = identityProviderDao.getIdentityProviderById(sharedBrokerIdp.id)
    }

    /**
     * Tests the golden case where all values are provided appropriately, IDP exists in correct state in LDAP, etc
     * @return
     */
    def "Valid: Fed request with no IDP requested roles"() {
        given:
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        def samlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        samlAuthResponse.user != null
        samlAuthResponse.user instanceof FederatedUser
        samlAuthResponse.token != null
        samlAuthResponse.endpoints != null
        samlAuthResponse.userRoles != null

        and: "Token is set"
        samlAuthResponse.token.accessTokenString != null
        samlAuthResponse.token.authenticatedBy.find {it ==  AuthenticatedByMethodEnum.FEDERATION.value} != null
        samlAuthResponse.token.authenticatedBy.find {it ==  AuthenticatedByMethodEnum.PASSWORD.value} != null
        scopeAccessService.getScopeAccessByAccessToken(samlAuthResponse.token.accessTokenString) //unmarshall token to verify

        and: "User attributes set as expected"
        def federatedUser = (FederatedUser) samlAuthResponse.user
        federatedUser.domainId == req.domainId
        federatedUser.federatedIdpUri == req.originIssuer
        federatedUser.username == req.username
        federatedUser.email == req.email
        federatedUser.region == sharedUserAdmin.defaultRegion
        federatedUser.expiredTimestamp > samlAuthResponse.token.accessTokenExp

        and: "Roles are as expected"
        List<TenantRole> userRoles = samlAuthResponse.userRoles
        userRoles.size() == 3
        userRoles.find {it.name == IdentityUserTypeEnum.DEFAULT_USER.getRoleName()} != null
        userRoles.find {it.name == GlobalConstants.COMPUTE_DEFAULT_ROLE} != null
        userRoles.find {it.name == GlobalConstants.FILES_DEFAULT_ROLE} != null

        and: "Catalogs are in sync"
        /*
         Compare endpoints of fed user to that of user-admin by receiving raw user-admin catalog which is comparable
         to the raw endpoints returned by fed service
         */
        List<OpenstackEndpoint> fedUserEndpoints = samlAuthResponse.endpoints
        ServiceCatalogInfo userAdminCatalog = scopeAccessService.getServiceCatalogInfo(sharedUserAdminEntity)

        userAdminCatalog.userEndpoints.each {
            userAdminOSEndpoint ->
                def fedUserOsEndpoint = fedUserEndpoints.find {
                    it.tenantId == userAdminOSEndpoint.tenantId
                }
                fedUserEndpoints != null
                userAdminOSEndpoint.baseUrls.each {
                    userAdminBaseUrl ->
                        fedUserOsEndpoint.baseUrls.find {
                            it.baseUrlId == userAdminBaseUrl.baseUrlId
                        }
                } != null
        }
    }

    def "Valid: Fed request w/ IDP requested roles"() {
        given:
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.roleNames = [ROLE_RBAC1_NAME, ROLE_RBAC2_NAME] as Set
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        def samlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        List<TenantRole> userRoles = samlAuthResponse.userRoles
        userRoles.size() == 5
        userRoles.find {it.name == IdentityUserTypeEnum.DEFAULT_USER.getRoleName()} != null
        userRoles.find {it.name == GlobalConstants.COMPUTE_DEFAULT_ROLE} != null
        userRoles.find {it.name == GlobalConstants.FILES_DEFAULT_ROLE} != null
        userRoles.find {it.name == ROLE_RBAC1_NAME} != null
        userRoles.find {it.name == ROLE_RBAC2_NAME} != null
    }

    def "Valid: Fed request for same username as expired user first deletes expired user"() {
        given:
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)
        def samlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        //expire this user
        FederatedUser originalUser = (FederatedUser) samlAuthResponse.user
        originalUser.expiredTimestamp = new DateTime().minusDays(1).toDate()
        federatedUserDao.updateUser(originalUser)
        assert federatedUserDao.getUserById(originalUser.id) != null

        when:
        samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        request = new FederatedDomainAuthRequest(samlResponse)
        samlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        samlAuthResponse.user != null
        samlAuthResponse.user.id != originalUser.id
        samlAuthResponse.user.username == originalUser.username
        federatedUserDao.getUserById(originalUser.id) == null
    }

    def "Creation of fed users controlled by max number feature flag"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, 1)

        // Create a new user-admin w/ no fed users
        def userAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.domainId = userAdmin.domainId
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request1 = new FederatedDomainAuthRequest(samlResponse)

        req.username = req.username + "2"
        def samlResponse2 = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request2 = new FederatedDomainAuthRequest(samlResponse2)

        when: "Request first fed user"
        def samlAuthResponse1 = federatedDomainRequestHandler.processAuthRequestForProvider(request1, sharedOriginIdpEntity)

        then: "Created successfully"
        samlAuthResponse1.user != null

        when: "Request second fed user"
        federatedDomainRequestHandler.processAuthRequestForProvider(request2, sharedOriginIdpEntity)

        then: "Fails due to max exceeded"
        def ex = thrown(ForbiddenException)
        ex.errorCode == ERROR_CODE_FEDERATION2_FORBIDDEN_REACHED_MAX_USERS_LIMIT

        when: "Raise limit and try again"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, 2)
        def samlAuthResponse2 = federatedDomainRequestHandler.processAuthRequestForProvider(request2, sharedOriginIdpEntity)

        then: "Created successfully"
        samlAuthResponse2.user != null

        when: "Limit is not excedeed if ignore expired users"
        // First expire existing user
        FederatedUser federatedUser = (FederatedUser) samlAuthResponse2.user
        federatedUser.setExpiredTimestamp(new DateTime().minusDays(1).toDate())
        federatedUserDao.updateUser(federatedUser)

        // Request new user
        req.username = req.username + "3"
        def samlResponse3 = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request3 = new FederatedDomainAuthRequest(samlResponse3)
        def samlAuthResponse3 = federatedDomainRequestHandler.processAuthRequestForProvider(request3, sharedOriginIdpEntity)

        then: "User is created"
        samlAuthResponse3.user != null

        cleanup:
        reloadableConfiguration.reset()
    }

    def "Existing users are updated w/ latest info"() {
        given:
        // Create initial request
        FederatedDomainAuthGenerationRequest origReq = createValidFedRequest().with {
            it.roleNames = [ROLE_RBAC1_NAME] as Set
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(origReq)
        FederatedDomainAuthRequest origRequest = new FederatedDomainAuthRequest(samlResponse)

        // Create another request that changes the updatable info
        FederatedDomainAuthGenerationRequest updatedReq = createValidFedRequest().with {
            it.roleNames = [ROLE_RBAC2_NAME] as Set
            it.username = origReq.username
            it.email = RandomStringUtils.randomAlphabetic(10)
            it
        }
        updatedReq.roleNames = [ROLE_RBAC2_NAME] as Set
        def samlResponse2 = sharedRequestGenerator.createSignedSAMLResponse(updatedReq)
        FederatedDomainAuthRequest updatedRequest = new FederatedDomainAuthRequest(samlResponse2)

        when: "Request initial"
        def origSamlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(origRequest, sharedOriginIdpEntity)

        then: "Created successfully"
        origSamlAuthResponse.user != null
        origSamlAuthResponse.userRoles.find {it.name == ROLE_RBAC1_NAME} != null
        origSamlAuthResponse.userRoles.find {it.name == ROLE_RBAC2_NAME} == null

        when: "Request updated user"
        def updatedSamlAuthResponse = federatedDomainRequestHandler.processAuthRequestForProvider(updatedRequest, sharedOriginIdpEntity)

        then: "User info is updated"
        updatedSamlAuthResponse.user.id == origSamlAuthResponse.user.id
        updatedSamlAuthResponse.user.email == updatedReq.email
        updatedSamlAuthResponse.userRoles.find {it.name == ROLE_RBAC1_NAME} == null
        updatedSamlAuthResponse.userRoles.find {it.name == ROLE_RBAC2_NAME} != null

        cleanup:
        reloadableConfiguration.reset()
    }

    @Unroll
    def "Error: Fed request w/ invalid IDP role. Reason: #errorScenario"() {
        given:
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.roleNames = [roleName] as Set
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ERROR_CODE_FEDERATION2_FORBIDDEN_FEDERATED_ROLE

        where:
        roleName | errorScenario
        IdentityUserTypeEnum.USER_MANAGER.roleName | "Non RBAC type role"
        UUID.randomUUID().toString() | "Non-existant role"
    }

    def "Error: Fed request trying to assign a tenant-only assignable role"() {
        given:
        Role theRole = v2Factory.createRole().with {
            it.serviceId = IDENTITY_SERVICE_ID
            it.name = UUID.randomUUID().toString()
            it.assignment = RoleAssignmentEnum.TENANT
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it.roleType = RoleTypeEnum.STANDARD
            it
        }
        Role createdRole = utils.createRole(theRole)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.roleNames = [createdRole.name] as Set
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ERROR_CODE_FEDERATION2_FORBIDDEN_FEDERATED_ROLE
    }

    def "Request token lifetime max based on config property"() {
        given:
        def maxLifetime = 100
        reloadableConfiguration.setProperty(IdentityConfig.FEDERATED_DOMAIN_USER_MAX_TOKEN_LIFETIME, maxLifetime)

        when: "request > lifetime"
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.validitySeconds = maxLifetime + 10
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP

        when: "request < lifetime"
        req = createValidFedRequest().with {
            it.validitySeconds = maxLifetime - 10
            it
        }
        samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        request = new FederatedDomainAuthRequest(samlResponse)
        def response = federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        response != null
        response.token != null

        cleanup:
        reloadableConfiguration.reset()
    }

    /**
     * This test verifies a few things. First, that can only authorize against a domain the IDP is allowed, but also
     * that this check is done PRIOR TO checking whether the domain actually exists. This is necessary to prevent bad
     * actors from trolling for valid domains via this service.
     */
    def "Error: Origin IDP can not auth against a domain for which it is unauthorized"() {
        given:
        // Create an IDP authorized for a different domain than the shared userAdmin account
        def originIdpCredential = SamlCredentialUtils.generateX509Credential()
        List<X509Certificate> certs = [originIdpCredential.entityCertificate]
        com.rackspace.idm.domain.entity.IdentityProvider originIdp = entityFactory.createIdentityProviderWithCertificates(certs).with {
            it.approvedDomainIds = ["abcde"]
            it.approvedDomainGroup = null
            it
        }
        identityProviderDao.addIdentityProvider(originIdp)
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, originIdpCredential)

        when: "Request auth against an existing domain not authorized for"
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)
        federatedDomainRequestHandler.processAuthRequestForProvider(request, originIdp)

        then: "Get a Forbidden error"
        def ex = thrown(ForbiddenException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        when: "Request auth against a non-existent domain not authorized for"
        req = createValidFedRequest().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        samlResponse = generator.createSignedSAMLResponse(req)
        request = new FederatedDomainAuthRequest(samlResponse)
        federatedDomainRequestHandler.processAuthRequestForProvider(request, originIdp)

        then: "Get a Forbidden error"
        def ex2 = thrown(ForbiddenException)
        ex2.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        when: "Request auth against a disabled domain not authorized for"
        def domainId = RandomStringUtils.randomAlphabetic(10)
        Domain disabledDomain = v2Factory.createDomain(domainId, domainId).with {
            it.enabled = false
            it
        }
        utils.createDomain(disabledDomain)
        req = createValidFedRequest().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        samlResponse = generator.createSignedSAMLResponse(req)
        request = new FederatedDomainAuthRequest(samlResponse)
        federatedDomainRequestHandler.processAuthRequestForProvider(request, originIdp)

        then: "Get a Forbidden error"
        def ex3 = thrown(ForbiddenException)
        ex2.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        cleanup:
        try {
            identityProviderDao.deleteIdentityProviderById(originIdp.providerId)
        } catch (Exception b) {
            // Eat. Just doing cleanup
        }

        try {
            utils.deleteDomain(domainId)
        } catch (Exception c) {
            // Eat
        }
    }

    def "Error: Receive error when trying to auth against non-existent domain"() {
        given:
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)

        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE
    }

    def "Error: Receive error when trying to auth against disabled domain"() {
        given:
        def domainId = RandomStringUtils.randomAlphabetic(10)
        Domain disabledDomain = v2Factory.createDomain(domainId, domainId).with {
            it.enabled = false
            it
        }
        utils.createDomain(disabledDomain)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.domainId = domainId
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        cleanup:
        try {
            utils.deleteDomain(domainId)
        } catch (Exception c) {
            // Eat
        }
    }

    /**
     * This is not allowed because various information for the new user account is based on the user-admin such as
     * propagating roles, default region, etc. Furthermore, it simply doesn't make sense to create a subuser in a domain
     * w/o a user-admin (root user)
     */
    def "Error: Receive error when trying to auth against domain w/ no user-admin"() {
        given:
        // Create a domain w/ no user-admin
        def domainId = RandomStringUtils.randomAlphabetic(10)
        Domain disabledDomain = v2Factory.createDomain(domainId, domainId).with {
            it.enabled = true
            it
        }
        utils.createDomain(disabledDomain)

        // Generate a request against this domain
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.domainId = domainId
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(ForbiddenException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        cleanup:
        try {
            utils.deleteDomain(domainId)
        } catch (Exception c) {
            // Eat
        }
    }

    /**
     * This is not allowed because various information for the new user account is based on the user-admin such as
     * propagating roles, default region, etc. Furthermore, it simply doesn't make sense to create a subuser in a domain
     * w/o a user-admin (root user)
     */
    def "Error: Receive error when trying to auth against domain w/ only a disabled user-admin"() {
        given:
        def userAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        userAdmin.enabled = false
        utils.updateUser(userAdmin)

        // Generate a request against this domain
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.domainId = userAdmin.domainId
            it
        }
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(req)
        FederatedDomainAuthRequest request = new FederatedDomainAuthRequest(samlResponse)

        when:
        federatedDomainRequestHandler.processAuthRequestForProvider(request, sharedOriginIdpEntity)

        then:
        def ex = thrown(ForbiddenException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        cleanup:
        try {
            utils.deleteDomain(domainId)
        } catch (Exception c) {
            // Eat
        }
    }

    def "Error: Receive error when same IDP tries authenticating for the same user in a different domain"() {
        given:
        def userAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        FederatedDomainAuthGenerationRequest reqInitial = createValidFedRequest()
        def samlResponse = sharedRequestGenerator.createSignedSAMLResponse(reqInitial)
        FederatedDomainAuthRequest requestOne = new FederatedDomainAuthRequest(samlResponse)

        FederatedDomainAuthGenerationRequest reqSecond = createValidFedRequest().with {
            it.username = reqInitial.username
            it.domainId = userAdmin.domainId
            it
        }
        def samlResponse2 = sharedRequestGenerator.createSignedSAMLResponse(reqSecond)
        FederatedDomainAuthRequest requestTwo = new FederatedDomainAuthRequest(samlResponse2)

        when: "Make initial request"
        def response = federatedDomainRequestHandler.processAuthRequestForProvider(requestOne, sharedOriginIdpEntity)

        then: "Is successful"
        response.token != null

        when: "Make second request"
        response = federatedDomainRequestHandler.processAuthRequestForProvider(requestTwo, sharedOriginIdpEntity)

        then: "Fails"
        def ex = thrown(DuplicateUsernameException)
        ex.errorCode == ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE

        cleanup:
        try {
            utils.deleteUser(userAdmin)
            utils.deleteDomain(userAdmin.domainId)
        } catch (Exception c) {
            // Eat
        }
    }

    def createValidFedRequest(domainId = sharedUserAdmin.domainId) {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = domainId
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = DEFAULT_FED_EMAIL
            it.requestIssueInstant = new DateTime()
            it.samlAuthContext = SAMLAuthContext.PASSWORD
            it.username = UUID.randomUUID()
            it.roleNames = [] as Set
            it
        }
    }
}
