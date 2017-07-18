package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.ConfigurableTokenFormatSelector
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.federation.v2.FederatedRoleAssignment
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import com.rackspace.idm.util.SamlUnmarshaller
import groovy.json.JsonSlurper
import org.apache.commons.lang.BooleanUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Tenants
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.SC_CREATED

class FederatedDomainV2UserRestIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedDomainV2UserRestIntegrationTest.class)
    @Shared AuthenticatedByMethodGroup fedAndPasswordGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.PASSWORD)
    @Shared AuthenticatedByMethodGroup fedAndOtherGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.OTHER)

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    TenantService tenantService

    @Autowired
    RoleService roleService

    @Autowired
    UserService userService

    @Autowired(required = false)
    FederatedUserRepository sqlFederatedUserRepository

    @Autowired
    ConfigurableTokenFormatSelector configurableTokenFormatSelector

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    SamlUnmarshaller samlUnmarshaller

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    DomainService domainService

    @Shared String sharedServiceAdminToken
    @Shared String sharedIdentityAdminToken

    /**
     * An identity provider created for this class. No code should modify this provider.
     */
    @Shared
    IdentityProvider sharedBrokerIdp
    @Shared
    Credential sharedBrokerIdpCredential
    @Shared
    IdentityProvider sharedOriginIdp
    @Shared
    Credential sharedOriginIdpCredential

    @Shared org.openstack.docs.identity.api.v2.User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant
    @Shared
    FederatedDomainAuthRequestGenerator sharedFederatedDomainAuthRequestGenerator

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        sharedFederatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def setup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    def cleanup() {
    }

    def "New fed user created correctly when no roles provided"() {
        given:
        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest()
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndPasswordGroup, userAdminEntity)

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == fedRequest.username
        fedUser.domainId == fedRequest.domainId
        fedUser.email == fedRequest.email
        fedUser.region == userAdminEntity.region
        fedUser.expiredTimestamp != null

        //just check that the user will expire after the token expires
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
        } catch (Exception ex) {
            // Eat
        }
    }

    @Unroll
    def "authContextRefClass mapped to appropriate auth by: authContextRefClass: '#authContextRefClass'; expectedAuthBy: '#authBy'"() {
        given:
        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest().with {
            it.authContextRefClass = authContextRefClass
            it
        }
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, authBy, userAdminEntity)

        where:
        authContextRefClass | authBy
        SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS | fedAndPasswordGroup
        null | fedAndOtherGroup
        "" | fedAndOtherGroup
        UUID.randomUUID().toString() | fedAndOtherGroup
    }

    def "missing authContextRefClass mapped to OTHER auth by"() {
        given:
        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest().with {
            it.authContextRefClass = null
            it
        }
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)


        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndOtherGroup, userAdminEntity)
    }

    def "New fed user created correctly when valid global assigned roles provided"() {
        given:
        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest().with {
            it.roleNames = [ROLE_RBAC1_NAME, ROLE_RBAC2_NAME] as Set
            it
        }

        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndPasswordGroup, userAdminEntity)

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
        } catch (Exception ex) {
            // Eat
        }
    }

    @Unroll
    def "Federated v2 Authentication with 'apply_rcn_roles=#applyRcnRoles'"() {
        given:
        def fedRequest = createFedRequest().with {
            it.roleNames = [ROLE_RBAC1_NAME, ROLE_RBAC2_NAME] as Set
            it
        }

        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticate(
                sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse),
                applyRcnRoles,
                GlobalConstants.FEDERATION_API_V2_0)
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value

        then: "Assert default role"
        authClientResponse.status == HttpServletResponse.SC_OK
        if (applyRcnRoles) {
            assert authResponse.user.roles.role.findAll{it.tenantId == null}.size() == 0
        } else {
            assert authResponse.user.roles.role.find{it.name == IdentityUserTypeEnum.DEFAULT_USER.roleName}.tenantId == null
            assert authResponse.user.roles.role.find{it.name == ROLE_RBAC1_NAME}.tenantId == null
            assert authResponse.user.roles.role.find{it.name == ROLE_RBAC2_NAME}.tenantId == null
        }

        cleanup:
        try { deleteFederatedUserQuietly(fedRequest.username) } catch (Exception ex) {}

        where:
        applyRcnRoles << [true, false]
    }

    def "New fed user created correctly when valid tenant assigned roles provided"() {
        given:
        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest().with {
            it.roleNames = [ROLE_RBAC1_NAME + "/" + sharedUserAdminCloudTenant.name
                            , ROLE_RBAC2_NAME + "/" + sharedUserAdminFilesTenant.name] as Set
            it
        }

        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndPasswordGroup, userAdminEntity)

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
        } catch (Exception ex) {
            // Eat
        }
    }

    /**
     * This tests the use of the client role cache in authentication
     *
     * @return
     */
    def "Federated Authentication uses cached roles"() {
        given:
        // Create Fed assignable role
        def originalRole = utils.createRole()

        User userAdminEntity = userService.getUserById(sharedUserAdmin.id)
        def fedRequest = createFedRequest().with {
            it.roleNames = [originalRole.name] as Set
            it
        }

        def samlResponseInitial = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "Auth"
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator
                .convertResponseToString(samlResponseInitial)).getEntity(AuthenticateResponse).value

        then: "User has role"
        authClientResponse.user.roles.role.find {it.name == originalRole.name} != null

        when: "Change role name and auth again"
        ClientRole updatedRole = applicationRoleDao.getClientRole(originalRole.id)
        updatedRole.setName(org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10))
        applicationRoleDao.updateClientRole(updatedRole)

        /*
         Update the request because the validation that the roles in the SAML Response match to existing roles names does
         not use a cache, but hit directly against LDAP (we don't cache by name).
          */
        def fedRequest2 = createFedRequest().with {
            it.roleNames = [updatedRole.name] as Set
            it
        }

        def samlResponseUpdated = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest2)
        authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator
                .convertResponseToString(samlResponseUpdated)).getEntity(AuthenticateResponse).value

        then: "Roles returned in auth use the cache as appropriate"
        // The role name should be the old value as the client role was cached during initial auth
        assert authClientResponse.user.roles.role.find {it.name == originalRole.name} != null
        assert authClientResponse.user.roles.role.find {it.name == updatedRole.name} == null

        cleanup:
        deleteFederatedUserQuietly(fedRequest.username)
    }


    def "Error: BadRequest against authorized, but non-existent domain"() {
        given:
        def fedRequest = createFedRequest().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }

        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(authClientResponse, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE)
    }

    @Unroll
    def "Session timeout is set correctly for domain federated users: accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def fedRequest = createFedRequest(userAdmin)
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def domain = utils.getDomain(domainId)

        when: "auth w/ the default session timeout"
        def response = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse), accept)

        then:
        response.status == HttpServletResponse.SC_OK
        assertSessionInactivityTimeout(response, accept, IdentityConfig.DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT.toString())

        when: "update to a non-default session timeout"
        def domainDuration = DatatypeFactory.newInstance().newDuration(
                identityConfig.getReloadableConfig().getDomainDefaultSessionInactivityTimeout().plusHours(3).toString());
        domain.sessionInactivityTimeout = domainDuration
        utils.updateDomain(domain.id, domain)
        response = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse), accept)

        then:
        response.status == HttpServletResponse.SC_OK
        assertSessionInactivityTimeout(response, accept, domain.sessionInactivityTimeout.toString())

        cleanup:
        try { deleteFederatedUserQuietly(fedRequest.username) } catch (Exception ex) { /* Eat */ }
        utils.deleteUsers(users)

        where:
        accept                          | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def void verifyAuthenticateResult(FederatedDomainAuthGenerationRequest originalRequest, AuthenticateResponse authResponse, AuthenticatedByMethodGroup authByGroup, User userAdminEntity) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == originalRequest.username
        assert authResponse.user.federatedIdp == originalRequest.originIssuer
        assert authResponse.user.defaultRegion == userAdminEntity.region

        def domain = domainService.getDomain(userAdminEntity.domainId)
        assert authResponse.user.sessionInactivityTimeout.toString() == domain.sessionInactivityTimeout != null ? domain.sessionInactivityTimeout.toString() : IdentityConfig.DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT

        // Check the token
        assert authResponse.token.id != null
        assert authResponse.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
        assert authResponse.token.tenant.id == userAdminEntity.domainId

        // Check Auth By
        List<String> expectedAuthByVals = authByGroup.authenticatedByMethodsAsValues
        assert authResponse.token.authenticatedBy.credential.size() == expectedAuthByVals.size()
        expectedAuthByVals.each {
            assert authResponse.token.authenticatedBy.credential.contains(it)
        }

        // Check the standard roles all fed users should get (assigned identity default role as well as (propagating roles))
        authResponse.user.getRoles().role.find { r -> r.name == IdentityUserTypeEnum.DEFAULT_USER.name() } != null
        def userAdminRoles = tenantService.getTenantRolesForUser(userAdminEntity)
        userAdminRoles.each() { userAdminRole ->
            if (BooleanUtils.isTrue(userAdminRole.propagate)) {
                assert authResponse.user.getRoles().role.find { r -> r.name == userAdminRole.name && r.id == userAdminRole.roleRsId } != null
            }
        }

        if (originalRequest.roleNames) {
            originalRequest.roleNames.each() { requestedRole ->
                FederatedRoleAssignment ra = new FederatedRoleAssignment(requestedRole)
                if (StringUtils.isNotBlank(ra.tenantName)) {
                    assert authResponse.user.getRoles().role.find { r -> r.name == ra.roleName && r.tenantId == ra.tenantName} != null
                } else {
                    assert authResponse.user.getRoles().role.find { r -> r.name == ra.roleName && ra.tenantName == null} != null
                }
            }
        }

        // Compare service catalog of fed user to that of user-admin
        AuthenticateResponse userAdminResponse = utils.authenticate(userAdminEntity.username)

        assert authResponse.serviceCatalog != null
        assert authResponse.serviceCatalog.service.size() > 0

        // Contains same services as user-admin
        userAdminResponse.serviceCatalog.service.each { userAdminService ->
            def fedUserService = authResponse.serviceCatalog.service.find { s -> s.name == userAdminService.name }
            assert fedUserService != null
            userAdminService.endpoint.each { userAdminEndpoint ->
                fedUserService.endpoint.find { e -> e.publicURL == userAdminEndpoint.publicURL } != null
            }
        }
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, DEFAULT_IDP_ID)
            if (federatedUser != null) {
                if (RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                    federatedUser = sqlFederatedUserRepository.findOneByUsernameAndFederatedIdpId(username, DEFAULT_IDP_ID)
                    sqlFederatedUserRepository.delete(federatedUser)
                } else {
                    federatedUserRepository.deleteObject(federatedUser)
                }
            }
        } catch (Exception e) {
            // Eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

/**
 * Creates a fed request specifying shared broker/origin as the issuers and no requested roles
 * @return
 */
    def createFedRequest(userAdmin = sharedUserAdmin) {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdmin.domainId
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it
        }
    }

    def void assertSessionInactivityTimeout(response, contentType, expectedSessionInactivityTimeout) {
        def returnedSessionInactivityTimeout
        if (contentType == MediaType.APPLICATION_XML_TYPE) {
            def parsedResponse = response.getEntity(AuthenticateResponse).value
            returnedSessionInactivityTimeout = parsedResponse.user.sessionInactivityTimeout.toString()
        } else {
            def authResponseData = new JsonSlurper().parseText(response.getEntity(String))
            returnedSessionInactivityTimeout = authResponseData.access.user[JSONConstants.RAX_AUTH_SESSION_INACTIVITY_TIMEOUT]
        }

        assert returnedSessionInactivityTimeout == expectedSessionInactivityTimeout
    }

}
