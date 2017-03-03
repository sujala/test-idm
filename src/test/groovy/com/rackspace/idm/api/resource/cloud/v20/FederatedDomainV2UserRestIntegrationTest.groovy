package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.ConfigurableTokenFormatSelector
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import com.rackspace.idm.util.SamlUnmarshaller
import org.apache.commons.lang.BooleanUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.SC_CREATED

class FederatedDomainV2UserRestIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedDomainV2UserRestIntegrationTest.class)

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
        verifyAuthenticateResult(authResponse, fedRequest, userAdminEntity)

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
            utils.deleteUsers(userAdmin)
        } catch (Exception ex) {
            // Eat
        }
    }

    def "New fed user created correctly when valid roles provided"() {
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
        verifyAuthenticateResult(authResponse, fedRequest, userAdminEntity)

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
            utils.deleteUsers(userAdmin)
        } catch (Exception ex) {
            // Eat
        }
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

    def void verifyAuthenticateResult(AuthenticateResponse authResponse, FederatedDomainAuthGenerationRequest originalRequest, User userAdminEntity) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == originalRequest.username
        assert authResponse.user.federatedIdp == originalRequest.originIssuer
        assert authResponse.user.defaultRegion == userAdminEntity.region

        //check the token
        assert authResponse.token.id != null
        assert authResponse.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
        assert authResponse.token.tenant.id == userAdminEntity.domainId

        //check the standard roles all fed users should get (assigned identity default role as well as (propagating roles))
        authResponse.user.getRoles().role.find { r -> r.name == IdentityUserTypeEnum.DEFAULT_USER.name() } != null
        def userAdminRoles = tenantService.getTenantRolesForUser(userAdminEntity)
        userAdminRoles.each() { userAdminRole ->
            if (BooleanUtils.isTrue(userAdminRole.propagate)) {
                assert authResponse.user.getRoles().role.find { r -> r.name == userAdminRole.name && r.id == userAdminRole.roleRsId } != null
            }
        }

        if (originalRequest.roleNames) {
            originalRequest.roleNames.each() { requestedRole ->
                assert authResponse.user.getRoles().role.find { r -> r.name == requestedRole} != null
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
    def createFedRequest() {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = sharedUserAdmin.domainId
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
}