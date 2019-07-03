package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams
import com.rackspace.idm.util.SamlUnmarshaller
import com.rackspace.idm.validation.Validator20
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.mockserver.verify.VerificationTimes
import org.opensaml.saml.saml2.core.LogoutResponse
import org.opensaml.saml.saml2.core.StatusCode
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static org.apache.http.HttpStatus.*

class FederatedUserIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedUserIntegrationTest.class)

    static String TENANT_TYPE = "test_tenant_type"
    static String CLOUD = "cloud"
    static String WHITE_LIST_FILTER_PROPERTY = IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + TENANT_TYPE
    static String WHITE_LIST_FILTER_PROPERTY_CLOUD = IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PREFIX + "." + CLOUD

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    TenantService tenantService

    @Autowired
    RoleService roleService

    @Autowired
    UserService userService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    SamlUnmarshaller samlUnmarshaller

    @Shared String sharedServiceAdminToken

    @Shared String sharedIdentityAdminToken

    @Shared
    IdentityProvider sharedBrokerIdp

    @Shared
    Credential sharedBrokerIdpCredential

    @Shared
    IdentityProvider sharedOriginIdp

    @Shared
    Credential sharedOriginIdpCredential

    @Shared
    org.openstack.docs.identity.api.v2.User sharedUserAdmin

    @Shared
    FederatedDomainAuthRequestGenerator sharedFederatedDomainAuthRequestGenerator

    @Autowired
    V2Factory factory

    private static final String RBACROLE1_NAME = "rbacRole1"
    private static final String RBACROLE2_NAME = "rbacRole2"
    private static final String ROLE_1000_NAME = "Role 1000"

    ClientRole rbacRole1;
    ClientRole rbacRole2;
    ClientRole role1000;

    def usGlobalEndpointEnabled
    def usGlobalEndpointDisabled
    def lonGlobalEndpointEnabled
    def lonGlobalEndpointDisabled
    def globalEndpointTemplateRegion = "ORD"
    def lonGlobalEndpointTemplateRegion = "LON"

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        sharedFederatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        def tenantType = new TenantType().with {
            it.name = TENANT_TYPE
            it.description = "description"
            it
        }
        cloud20.addTenantType(sharedServiceAdminToken, tenantType)
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def setup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()

        //expected to be pre-existing 1000 weight roles in default ldif
        rbacRole1 = roleService.getRoleByName(RBACROLE1_NAME)
        rbacRole2 = roleService.getRoleByName(RBACROLE2_NAME)
        role1000 = roleService.getRoleByName(ROLE_1000_NAME)

        assert rbacRole1.rsWeight == 1000
        assert rbacRole2.rsWeight == 1000
        assert role1000.rsWeight == 1000

        usGlobalEndpointEnabled = utils.createEndpointTemplate(true, null, true, "compute", globalEndpointTemplateRegion)
        usGlobalEndpointDisabled = utils.createEndpointTemplate(true, null, false, "compute", globalEndpointTemplateRegion)
        lonGlobalEndpointEnabled = utils.createEndpointTemplate(true, null, true, "compute", lonGlobalEndpointTemplateRegion)
        lonGlobalEndpointDisabled = utils.createEndpointTemplate(true, null, false, "compute", lonGlobalEndpointTemplateRegion)
    }

    def cleanup() {
        utils.disableAndDeleteEndpointTemplate(usGlobalEndpointEnabled.id.toString())
        utils.disableAndDeleteEndpointTemplate(usGlobalEndpointDisabled.id.toString())
        utils.disableAndDeleteEndpointTemplate(lonGlobalEndpointEnabled.id.toString())
        utils.disableAndDeleteEndpointTemplate(lonGlobalEndpointDisabled.id.toString())
    }

    def "initial user populated appropriately from saml no roles provided"() {
        given:
        //specify assertion with no roles
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id))

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == fedRequest.username
        fedUser.domainId == sharedUserAdmin.domainId
        fedUser.email == fedRequest.email
        fedUser.region == sharedUserAdmin.defaultRegion
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())
    }

    def "Fed user includes auto-assigned roles on authenticate"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "auth"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content and auto-assigned roles"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id))
        def roles = authResponse.user.roles.role
        roles.size() == 5
        def mossoRole = roles.find {it.id == MOSSO_ROLE_ID}
        mossoRole != null
        def nastRole = roles.find {it.id == NAST_ROLE_ID}
        nastRole != null

        roles.find {it.id == DEFAULT_USER_ROLE_ID} != null
        roles.find {it.id == IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoRole.tenantId} != null
        roles.find {it.id == IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastRole.tenantId} != null

        when: "validate the token w/ feature enabled"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id)

        then: "the token is still valid and returns auto-assigned roles"
        validateSamlTokenResponse.status == 200
        AuthenticateResponse valResponse = validateSamlTokenResponse.getEntity(AuthenticateResponse).value
        def roles2 = valResponse.user.roles.role
        roles2.size() == 5
        def mossoRole2 = roles2.find {it.id == MOSSO_ROLE_ID}
        mossoRole2 != null
        def nastRole2 = roles2.find {it.id == NAST_ROLE_ID}
        nastRole2 != null

        roles2.find {it.id == DEFAULT_USER_ROLE_ID} != null
        roles2.find {it.id == IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoRole2.tenantId} != null
        roles2.find {it.id == IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastRole2.tenantId} != null

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        reloadableConfiguration.reset()
    }

    // [CIDMDEV-5294] Mark Federated Users as eligible for deletion
    def "auth user updates the expiration time on it"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def authResponse, fedUser, samlResponse, previousExpiration

        when: "auth"
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "retrieve user from backend"
        authResponse = samlResponse.getEntity(AuthenticateResponse).value
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id))
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        when: "test if another token changes the timestamp"
        previousExpiration = fedUser.expiredTimestamp
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        authResponse = samlResponse.getEntity(AuthenticateResponse).value
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "shouldn't change the date"
        fedUser.expiredTimestamp == previousExpiration

        when: "force change the user expiration"
        fedUser.expiredTimestamp = new Date(0)
        federatedUserRepository.updateUser(fedUser)
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "date should not match previous token"
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.before(authResponse.token.expires.toGregorianCalendar().getTime())

        when: "Auth second (updates expiration)"
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "retrieve user from backend"
        authResponse = samlResponse.getEntity(AuthenticateResponse).value
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state and expiration date must be in future"
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id))
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    @Unroll
    def "test response age validation for federation"() {
        given:
        def maxResponseAge = identityConfig.getReloadableConfig().getFederatedResponseMaxAge()

        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.validitySeconds = 60

        when: "issueInstant is in the past but not older than max saml response age"
        fedRequest.originIssueInstant = new DateTime().minusSeconds(maxResponseAge)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == SC_OK

        when: "issueInstant is in the past but not older than max saml response age + skew"
        fedRequest.originIssueInstant = new DateTime().minusSeconds(maxResponseAge).minusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() - 3)
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == SC_OK

        when: "issueInstant is in the past and older than max saml response age + skew"
        //subtracting a few seconds off of the skew. Making it exactly equal will fail b/c of the time for the round trip
        fedRequest.originIssueInstant = new DateTime().minusSeconds(maxResponseAge).minusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() + 60)
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == SC_BAD_REQUEST

        when: "issueInstant is in the future but within the allowed skew"
        fedRequest.originIssueInstant = new DateTime().plusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() - 3)
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == SC_OK

        when: "issueInstant is in the future but outside of the the allowed skew"
        fedRequest.originIssueInstant = new DateTime().plusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() + 60)
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == SC_BAD_REQUEST

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    @Unroll
    def "test token lifetime validation for domain user federated tokens: secToAddToExp == #secToAddToExp, expectedResponse == #expectedResponse"() {
        given:
        def expSeconds = identityConfig.getReloadableConfig().getFederatedDomainTokenLifetimeMax() + secToAddToExp
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.validitySeconds = expSeconds
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse.status == expectedResponse

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)

        where:
        secToAddToExp | expectedResponse
        -60           | 200
        0             | 200
        60            | 400
    }

    def "initial user populated appropriately from saml - user admin group added to federated user"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        // add group to user admin
        def group = utils.createGroup()
        userService.addGroupToUser(group.id, sharedUserAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id))

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state including groups"
        fedUser.id == authResponse.user.id
        fedUser.username == fedRequest.username
        fedUser.domainId == fedRequest.domainId
        fedUser.email == fedRequest.email
        fedUser.region == sharedUserAdmin.defaultRegion
        fedUser.rsGroupId.size() == 1
        fedUser.rsGroupId.contains(group.id)

        when: "check to make sure group shows up in list user groups call"
        def listGroupsForUserResponse = cloud20.listGroupsForUser(sharedServiceAdminToken, authResponse.user.id)
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        then:
        groups.group.findAll({it.id == group.id}).size() == 1

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.removeUserFromGroup(group, sharedUserAdmin)
        utils.deleteGroup(group)
    }

    def "initial user populated appropriately from saml with 1 role provided"() {
        given:
        cloudFeedsMock.reset()

        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = [rbacRole1.name]
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id), Arrays.asList(rbacRole1))

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == fedRequest.username
        fedUser.domainId == sharedUserAdmin.domainId
        fedUser.email == fedRequest.email
        fedUser.region == sharedUserAdmin.defaultRegion

        and: "verify that events were posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(fedUser, EventType.CREATE.value()),
                VerificationTimes.exactly(1)
        )
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(fedUser, EventType.UPDATE.value()),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "Legacy SAML authenticate with 'x-www-form-urlencoded' media type"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = [role1000.name]
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        byte[] encodedSamlAssertion = Base64.encodeBase64(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion).bytes, false, true)
        def samlResponse = cloud20.samlAuthenticate("SAMLResponse=" + new String(encodedSamlAssertion), MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id), Arrays.asList(role1000))

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "Can specify a role with a space in the name"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = [role1000.name]
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username , userService.getUserById(sharedUserAdmin.id), Arrays.asList(role1000))

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    @Unroll
    def "samlResponse rejected when specify illegal role set '#delimitedRoleNames' because #rejectionReason"() {
        given:
        List<String> roleNames = Arrays.asList(delimitedRoleNames.split(","))
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = roleNames
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse.getEntity(String.class).contains(errorMessageContains)

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)

        where:
        delimitedRoleNames | rejectionReason | errorMessageContains
        "identity:default" | "not 1000 weight" | "is either invalid or unknown"
        "compute:default" | "not 1000 weight" | "is either invalid or unknown"
        "non-existant_role_name" | "non-existant role" | "is either invalid or unknown"
        // RBACROLE1_NAME + "," + RBACROLE1_NAME | "duplicate role included" | "specified more than once" NOTE: Fed V2 allows for duplicate role names
    }

    def "User roles reflect last saml response"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)

        def samlAssertionNone = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        fedRequest.roleNames = Arrays.asList(rbacRole1.name)
        def samlAssertionRbac1 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        fedRequest.roleNames = Arrays.asList(rbacRole1.name, rbacRole2.name)
        def samlAssertionRbac1And2 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        fedRequest.roleNames = Arrays.asList(rbacRole2.name)
        def samlAssertionRbac2 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)


        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertionNone))

        then: "user has no rbac roles"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value,fedRequest.username, userService.getUserById(sharedUserAdmin.id), null, Arrays.asList(rbacRole1, rbacRole2))

        when:
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertionRbac1))

        then: "user has rbacRole1"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value,fedRequest.username, userService.getUserById(sharedUserAdmin.id), Arrays.asList(rbacRole1), Arrays.asList(rbacRole2))

        when:
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertionRbac1And2))

        then: "user has rbacRole1 and rbacRole2"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value,fedRequest.username, userService.getUserById(sharedUserAdmin.id), Arrays.asList(rbacRole1, rbacRole2), null)

        when:
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertionRbac2))

        then: "user has rbacRole2"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value,fedRequest.username, userService.getUserById(sharedUserAdmin.id), Arrays.asList(rbacRole2), Arrays.asList(rbacRole1))

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "federated user is disabled when user admin on domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)

        def userAdmin = utils.createCloudAccount(sharedIdentityAdminToken)

        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "try to pass a saml assertion for a domain"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "the request succeeds"
        samlResponse.status == SC_OK

        when: "disable the user admins and try to pass a saml assertion again for the same user"
        utils.disableUser(userAdmin)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == SC_FORBIDDEN // NOTE: Disabling user-admin results in a 403 (Fed v2.0) vs a 400 (Fed v1.0)
        samlResponse.getEntity(BadRequestFault).value.message == String.format("Error code: 'FED2-014'; Domain %s is disabled.", userAdmin.domainId)

        when: "try to pass a saml assertion for a new user in the same domain"
        def fedRequest2 = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest2)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == SC_FORBIDDEN
        samlResponse.getEntity(BadRequestFault).value.message == String.format("Error code: 'FED2-014'; Domain %s is disabled.", userAdmin.domainId)

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "federated token contains tenant: #mediaType"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)

        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "authenticate with saml"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)

        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id

        then: "the tenant is populated"
        samlResponse.status == SC_OK
        samlAuthToken.tenant != null
        samlAuthToken.tenant.id != null
        samlAuthToken.tenant.id == sharedUserAdmin.domainId

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(sharedServiceAdminToken, samlAuthTokenId)

        then: "the token is still valid"
        validateSamlTokenResponse.status == SC_OK

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        staticIdmConfiguration.reset()

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "domain federated token contains correct authBy values: #mediaType"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.validitySeconds = 500
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "authenticate with saml"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token

        then: "the authBy is populated"
        samlResponse.status == SC_OK
        samlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        samlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(sharedServiceAdminToken, samlAuthToken.id)
        def validateSamlAuthToken = validateSamlTokenResponse.getEntity(AuthenticateResponse).value.token

        then: "the returned token also has valid authBy values"
        validateSamlTokenResponse.status == SC_OK
        validateSamlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        validateSamlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        staticIdmConfiguration.reset()

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    def "federated and provisioned user tokens are revoked when the user admin for the domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)

        def userAdmin = utils.createCloudAccount(sharedIdentityAdminToken)
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthToken = samlResponse.getEntity(AuthenticateResponse).value.token.id
        def provisionedUser = utils.createUserWithUser(userAdmin)
        def provisionedUserToken = utils.authenticate(provisionedUser).token.id

        when: "validate the tokens with enabled user admins"
        def validateSamlTokenResponse = cloud20.validateToken(sharedServiceAdminToken, samlAuthToken)
        def validateProvisionedTokenResponse = cloud20.validateToken(sharedServiceAdminToken, provisionedUserToken)

        then: "the tokens are still valid"
        validateSamlTokenResponse.status == SC_OK
        validateProvisionedTokenResponse.status == SC_OK

        when: "disable user admin"
        utils.disableUser(userAdmin)
        validateSamlTokenResponse = cloud20.validateToken(sharedServiceAdminToken, samlAuthToken)
        validateProvisionedTokenResponse = cloud20.validateToken(sharedServiceAdminToken, provisionedUserToken)

        then: "the token is no longer valid"
        validateSamlTokenResponse.status == SC_NOT_FOUND
        validateProvisionedTokenResponse.status == SC_NOT_FOUND

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.deleteUserQuietly(provisionedUser)
        utils.deleteUserQuietly(userAdmin)
        staticIdmConfiguration.reset()
    }

    def "federated users are limited within each IDP"() {
        given:
        //set the user limit low for lower overhead
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, 2)

        def brokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        def originIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        def userAdmin = utils.createCloudAccount()

        def fedRequest = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        assert samlResponse.status == SC_OK

        def fedRequest2 = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)
        def samlAssertion2 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest2)
        def samlResponse2 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion2))
        assert samlResponse2.status == SC_OK

        def fedRequest3 = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)
        def samlAssertion3 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest3)

        when: "try to exceed the limit under the current IDP"
        def samlResponse3 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion3))

        then: "the response is a failure"
        samlResponse3.status == SC_FORBIDDEN // NOTE: Fed v2 returns forbidden when max number of users per domain is reached.

        when: "auth with existing user under the current IDP"
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "the response is a success"
        samlResponse.status == SC_OK

        when: "try to create the same user under a different IDP (the limit is per IDP per domain)"
        def brokerIdp2 = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        def originIdp2 = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        fedRequest3 = utils.createFedRequest(userAdmin, brokerIdp2.issuer,originIdp2.issuer)
        samlAssertion3 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest3)
        samlResponse3 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion3))

        then: "the request succeeds"
        samlResponse3.status == SC_OK

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, originIdp.id)
        utils.deleteFederatedUserQuietly(fedRequest2.username, originIdp.id)
        utils.deleteFederatedUserQuietly(fedRequest3.username, originIdp2.id)
        utils.deleteIdentityProvider(brokerIdp)
        utils.deleteIdentityProvider(originIdp)
        utils.deleteIdentityProvider(brokerIdp2)
        utils.deleteIdentityProvider(originIdp2)

        reloadableConfiguration.reset()
    }

    def "Deleting a Domain federated user returns logout response"() {
        given:
        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def fedRequest = utils.createFedRequest(sharedUserAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        assert samlResponse.status == SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlAuthToken = authResponse.token.id

        //verify token is good and user can be retrieved
        utils.getUserById(authResponse.user.id) != null
        utils.validateToken(samlAuthToken)

        when: "delete the user"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(IDP_V2_DOMAIN_URI, fedRequest.username)
        def logoutResponse = cloud20.federatedLogout(logoutRequest)

        then: "the response is a success"
        logoutResponse.status == SC_OK
        LogoutResponse obj = samlUnmarshaller.unmarshallLogoutRespone(StringUtils.getBytesUtf8(logoutResponse.getEntity(String.class)))
        obj.getStatus().getStatusCode().value == StatusCode.SUCCESS

        and: "the user does not exist in backend"
        cloud20.getUserById(sharedServiceAdminToken, authResponse.user.id).status == SC_NOT_FOUND

        and: "the previously issued token is no longer valid"
        cloud20.validateToken(sharedServiceAdminToken, samlAuthToken).status == SC_NOT_FOUND

        when: "delete the user again"
        logoutRequest = new SamlFactory().generateLogoutRequestEncoded(IDP_V2_DOMAIN_URI, fedRequest.username)
        logoutResponse = cloud20.federatedLogout(logoutRequest)

        then: "the response is a failure marked as requestor failure"
        logoutResponse.status == SC_BAD_REQUEST
        LogoutResponse logoutResponseObj = samlUnmarshaller.unmarshallLogoutRespone(StringUtils.getBytesUtf8(logoutResponse.getEntity(String.class)))
        logoutResponseObj.getStatus().getStatusCode().value == StatusCode.REQUESTER

        cleanup:
        reloadableConfiguration.reset()
    }

    def "verify federated request: A valid signature is successful when request references an existing user"() {
        given:
        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def fedRequest = utils.createFedRequest(sharedUserAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        assert samlResponse.status == SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlAuthToken = authResponse.token.id

        //verify token is good and user can be retrieved
        utils.getUserById(authResponse.user.id) != null
        utils.validateToken(samlAuthToken)

        when: "verify the logout request"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(DEFAULT_IDP_URI, fedRequest.username)
        def validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "the response is a success"
        validateResponse.status == SC_OK

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, IDP_V2_DOMAIN_ID)
    }

    @Unroll
    def "verify federated request: A valid signature is successful regardless of nameId in request: #nameId"() {
        when: "verify the logout request"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(DEFAULT_IDP_URI, nameId)
        def validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "The response is a success"
        validateResponse.status == SC_OK

        where:
        nameId << ["", "non-exist", null]
    }

    @Unroll
    def "verify federated request: Old requests are rejected: #requestAge"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEDERATED_RESPONSE_MAX_SKEW, 0)
        def issueInstance = new DateTime().minusSeconds(requestAge)

        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(DEFAULT_IDP_URI, "user", DEFAULT_IDP_PRIVATE_KEY, DEFAULT_IDP_PUBLIC_KEY, issueInstance)

        when: "When the logout request is not too old"
        reloadableConfiguration.setProperty(IdentityConfig.FEDERATED_RESPONSE_MAX_AGE, requestAge + 10)
        def validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "Passes"
        notThrown(BadRequestException)
        validateResponse.status == SC_OK

        when: "When the logout request is too old"
        def maxAge = requestAge - 10
        reloadableConfiguration.setProperty(IdentityConfig.FEDERATED_RESPONSE_MAX_AGE, maxAge)
        validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "Fails"
        IdmAssert.assertOpenStackV2FaultResponse(validateResponse, BadRequestFault, SC_BAD_REQUEST, "SAML issueInstant cannot be older than " + maxAge + " seconds.")

        cleanup:
        reloadableConfiguration.reset()

        where:
        requestAge << [20, 40]
    }

    def "verify federated request: A non-saml entity request body results in error"() {
        when: "Request is not saml entity"
        def validateResponse = cloud20.federatedValidateRequest("non logout request")

        then: "The request fails"
        IdmAssert.assertOpenStackV2FaultResponse(validateResponse, BadRequestFault, SC_BAD_REQUEST, "Invalid saml entity. Please check your syntax and try again.")
    }

    def "verify federated request: A non-logout request body results in error"() {
        when: "Request is a saml auth response"
        def samlResponse = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, "user", 60, "anyId", null)
        def encodedsamlResponse = Base64.encodeBase64URLSafeString(samlResponse.getBytes())
        def validateResponse = cloud20.federatedValidateRequest(encodedsamlResponse)

        then: "The request fails"
        IdmAssert.assertOpenStackV2FaultResponse(validateResponse, BadRequestFault, SC_BAD_REQUEST, "Only logout requests are supported.")
    }

    /**
     * Verify that the response to the saml request contains the appropriate information, the persisted federated user has the specified 'expectedRbacRoles' rbac roles,
     * does NOT have the specified 'notExpectedRbacRoles' roles, and has the appropriate propagated roles.
     *
     * @param authResponse
     * @param expectedUserName
     * @param userAdminEntity
     * @param expectedRbacRoles
     * @param notExpectedRbacRoles
     */
    def void verifyResponseFromSamlRequestAndBackendRoles(authResponse, expectedUserName, User userAdminEntity, List<ClientRole> expectedRbacRoles = Collections.EMPTY_LIST, List<ClientRole> notExpectedRbacRoles = Collections.EMPTY_LIST) {
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)
        verifyResponseFromSamlRequest(authResponse, expectedUserName, userAdminEntity, expectedRbacRoles, notExpectedRbacRoles)
        verifyUserHasRbacRoles(fedUser, expectedRbacRoles, notExpectedRbacRoles)
    }

    def void verifyUserHasRbacRoles(FederatedUser user, List<ClientRole> expectedRbacRoles = Collections.EMPTY_LIST, List<ClientRole> notExpectedRbacRoles = Collections.EMPTY_LIST) {
        List<TenantRole> userGlobalRoles = tenantService.getGlobalRolesForUser(user)

        expectedRbacRoles.each() { rbacRole ->
            assert userGlobalRoles.find{r -> r.name == rbacRole.name && r.roleRsId == rbacRole.id} != null
        }

        notExpectedRbacRoles.each() { rbacRole ->
            assert userGlobalRoles.find{r -> r.name == rbacRole.name && r.roleRsId == rbacRole.id} == null
        }
    }

    def void verifyResponseFromSamlRequest(authResponse, expectedUserName, User userAdminEntity, List<ClientRole> expectedRbacRoles = Collections.EMPTY_LIST, List<ClientRole> notExpectedRbacRoles = Collections.EMPTY_LIST) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == expectedUserName
        assert authResponse.user.federatedIdp == sharedOriginIdp.issuer
        assert authResponse.user.defaultRegion == userAdminEntity.region

        //check the token
        assert authResponse.token.id != null
        assert authResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.value)
        assert authResponse.token.tenant.id == userAdminEntity.domainId

        //check the roles (assigned identity default role as well as compute:default,object-store:default (propagating roles) by default
        //should query the useradmin to figure out the roles, but
        authResponse.user.getRoles().role.find{r -> r.name == 'identity:default'} != null
        def userAdminRoles = tenantService.getTenantRolesForUser(userAdminEntity)
        userAdminRoles.each() { userAdminRole ->
            if (BooleanUtils.isTrue(userAdminRole.propagate)) {
                assert authResponse.user.getRoles().role.find{r -> r.name == userAdminRole.name && r.id == userAdminRole.roleRsId} != null
            }
        }

        expectedRbacRoles.each() { rbacRole ->
            assert authResponse.user.getRoles().role.find{r -> r.name == rbacRole.name && r.id == rbacRole.id} != null
        }

        notExpectedRbacRoles.each() { rbacRole ->
            assert authResponse.user.getRoles().role.find{r -> r.name == rbacRole.name && r.id == rbacRole.id} == null
        }

        //TODO: Service catalog checks
        assert authResponse.serviceCatalog != null
        assert authResponse.serviceCatalog.service.size() > 0

        def foundUsGlobalEndpointEnabled = false
        def foundUsGlobalEndpointDisabled = false
        def foundLonGlobalEndpointEnabled = false
        def foundLonGlobalEndpointDisabled = false
        String usTenantEndpointEnabled = String.format("%s/%s", usGlobalEndpointEnabled.publicURL, userAdminEntity.mossoId)
        String usTenantEndpointDisabled = String.format("%s/%s", usGlobalEndpointDisabled.publicURL, userAdminEntity.mossoId)
        String lonTenantEndpointEnabled = String.format("%s/%s", lonGlobalEndpointEnabled.publicURL, userAdminEntity.mossoId)
        String lonTenantEndpointDisabled = String.format("%s/%s", lonGlobalEndpointDisabled.publicURL, userAdminEntity.mossoId)
        for (List publicUrls : authResponse.serviceCatalog.service.endpoint.publicURL) {
            if (publicUrls.contains(usTenantEndpointEnabled)) {
                foundUsGlobalEndpointEnabled = true
            }
            if (publicUrls.contains(usTenantEndpointDisabled)) {
                foundUsGlobalEndpointDisabled = true
            }
            if (publicUrls.contains(lonTenantEndpointEnabled)) {
                foundLonGlobalEndpointEnabled = true
            }
            if (publicUrls.contains(lonTenantEndpointDisabled)) {
                foundLonGlobalEndpointDisabled = true
            }
        }
        assert foundUsGlobalEndpointEnabled
        assert !foundUsGlobalEndpointDisabled
        assert !foundLonGlobalEndpointEnabled
        assert !foundLonGlobalEndpointDisabled
    }

    def "passing multiple saml requests with same info references same user"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlResponse2 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        samlResponse1.status == SC_OK
        samlResponse2.status == SC_OK
        AuthenticateResponse authResponse1 = samlResponse1.getEntity(AuthenticateResponse).value
        AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        authResponse1.user.id == authResponse2.user.id

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "passing multiple saml requests with same user, but different domain id throws error"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def userAdmin = utils.createCloudAccount(sharedIdentityAdminToken)
        def fedRequest2 = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdmin.domainId
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = fedRequest.username
            it.roleNames = [] as Set
            it
        }
        def samlAssertion2 = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest2)

        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlResponse2 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion2))

        then:
        samlResponse1.status == SC_OK
        samlResponse2.status == SC_CONFLICT

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "test federated user with a disabled domain"() {
        given:
        def userAdmin = utils.createCloudAccount(sharedIdentityAdminToken)
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "first authenticate the token"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def validateResponse = cloud20.validateToken(sharedIdentityAdminToken, samlToken)

        then: "response contains appropriate content"
        samlResponse.status == SC_OK
        validateResponse.status == SC_OK

        when: "disable the domain"
        utils.disableDomain(userAdmin.domainId)

        then: "token should not work"
        def validateResponse2 = cloud20.validateToken(sharedIdentityAdminToken, samlToken)
        validateResponse2.status == SC_NOT_FOUND

        when: "try to get another token"
        def samlResponse2 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "token should not work"
        samlResponse2.status == SC_BAD_REQUEST

        when: "enable the domain again"
        utils.updateDomain(userAdmin.domainId, new Domain().with {it.enabled = true; it})

        then: "old token should not work [B-71699]"
        def validateResponse3 = cloud20.validateToken(sharedIdentityAdminToken, samlToken)
        validateResponse3.status == SC_NOT_FOUND

        when: "try to get another token"
        def samlResponse3 = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        AuthenticateResponse authResponse2 = samlResponse3.getEntity(AuthenticateResponse).value
        def samlToken2 = authResponse2.token.id
        def validateResponse4 = cloud20.validateToken(sharedIdentityAdminToken, samlToken2)

        then: "response contains appropriate content"
        samlResponse3.status == SC_OK
        validateResponse4.status == SC_OK

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "Invalid SAML signature results in 400"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, SC_BAD_REQUEST, ERROR_CODE_FEDERATION_INVALID_BROKER_SIGNATURE)
    }

    def "empty IssueInstant should give bad request"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def request = sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion)

        when:
        def samlResponse = cloud20.samlAuthenticate(request.replaceAll("IssueInstant=\"([^\"]+)\"", "IssueInstant=\"\""))

        then: "Response contains bad request"
        samlResponse.status == SC_BAD_REQUEST
    }

    def "empty Version should give bad request"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def request = sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion)

        when:
        def samlResponse = cloud20.samlAuthenticate(request.replaceAll("Version=\"([^\"]+)\"", "Version=\"\""))

        then: "Response contains bad request"
        samlResponse.status == SC_BAD_REQUEST
    }

    def "invalid NotOnOrAfter should give bad request"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def request = sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion)

        when:
        def samlResponse = cloud20.samlAuthenticate(request.replaceAll("NotOnOrAfter=\"([^\"]+)\"", "NotOnOrAfter=\"\""))

        then: "Response contains bad request"
        samlResponse.status == SC_BAD_REQUEST
    }

    def "invalid AuthnInstant should give bad request"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def request = sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion)

        when:
        def samlResponse = cloud20.samlAuthenticate(request.replaceAll("AuthnInstant=\"([^\"]+)\"", "AuthnInstant=\"\""))

        then: "Response contains bad request"
        samlResponse.status == SC_BAD_REQUEST
    }

    def "empty Algorithm should give bad request"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def request = sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion)

        when:
        def samlResponse = cloud20.samlAuthenticate(request.replaceAll("Algorithm=\"([^\"]+)\"", "Algorithm=\"\""))

        then: "Response contains bad request"
        samlResponse.status == SC_BAD_REQUEST
    }

    def "federated user is not able to update another user"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        AuthenticateResponse authResponse

        def resp = samlResponse.getEntity(AuthenticateResponse).value
        if (resp instanceof AuthenticateResponse) {
            authResponse = (AuthenticateResponse) resp
        } else {
            //bad request?
            if (resp instanceof IdentityFault) {
                LOG.error(String.format("Setup SAML Authentication failed with %s", ((IdentityFault)resp).getMessage()));
                throw new RuntimeException(String.format("Setup SAML Authentication failed with %s", ((IdentityFault)resp).getMessage()))
            }
        }

        def samlToken = authResponse.token.id
        def userAdmin = utils.createCloudAccount()

        when: "try to update a different user"
        def response = cloud20.updateUser(samlToken, userAdmin.id, new UserForCreate().with {it.enabled = true; it})

        then: "forbidden"
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteDomain(userAdmin.domainId)
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "domain cannot be deleted if a federated user exists in the domain"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def fedRequest = utils.createFedRequest(userAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def fedAddResponseEntity = samlResponse.getEntity(String)
        if (samlResponse.status != SC_OK) {
            LOG.error(String.format("Failed to add fed user. Test will fail. Add Request: '%s', Add response: '%s'", samlAssertion, fedAddResponseEntity))
            assert samlResponse.status == SC_OK //force the failure
        }

        when: "delete the user-admin and try to delete the domain"
        utils.deleteUser(userAdmin)
        def response = cloud20.deleteDomain(sharedServiceAdminToken, userAdmin.domainId)

        then: "bad request"
        response.status == SC_BAD_REQUEST

        when: "logout the federated user (deletes the federated user) and then try again"
        utils.logoutFederatedUser(fedRequest.username, IDP_V2_DOMAIN_URI)
        utils.disableDomain(userAdmin.domainId)

        response = cloud20.deleteDomain(sharedServiceAdminToken, userAdmin.domainId)

        then: "success"
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    def "forbidden (403) returned when trying to auth federated user in a domain without a user admin"() {
        given:
        def userAdmin = utils.createCloudAccount()

        // delete user admin
        utils.deleteUser(userAdmin)

        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(samlResponse, ForbiddenFault, SC_FORBIDDEN, ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE, String.format("The domain %s cannot be used for federation because it does not have a user admin.", userAdmin.domainId))
    }

    def "fed user can get admin for own domain"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        samlResponse.status == SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, fedRequest.username, userService.getUserById(sharedUserAdmin.id))

        when: "retrieve admin for fed user"
        def response = cloud20.getAdminsForUser(authResponse.token.id, authResponse.user.id)

        then: "get admin"
        response.status == SC_OK
        def admins = response.getEntity(UserList).value
        admins.getUser().size == 1
        admins.getUser().getAt(0).id == sharedUserAdmin.id

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    @Unroll
    def "Return 403 when target user of Identity MFA Service is a federated user: #mediaType"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id
        def userId = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user.id : samlAuthResponse.user.id

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthTokenId)

        then: "the token is still valid"
        validateSamlTokenResponse.status == SC_OK

        when: "add OTP device"
        OTPDevice otpDevice = new OTPDevice()
        otpDevice.setName("test")
        def response = cloud20.addOTPDeviceToUser(utils.getServiceAdminToken(), userId, otpDevice, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "delete OTP device"
        response = cloud20.deleteOTPDeviceFromUser(utils.getServiceAdminToken(), userId, "id", mediaType, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "send verification code"
        response = cloud20.sendVerificationCode(utils.getServiceAdminToken(), userId, "id", mediaType, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "verify otp device"
        def verificationCode = cloud20.getV2Factory().createVerificationCode("code")
        response = cloud20.verifyOTPDevice(utils.getServiceAdminToken(), userId, "id", verificationCode, mediaType, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "list devices"
        response = cloud20.listDevices(utils.getServiceAdminToken(), userId, mediaType, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "list mfa devices"
        response = cloud20.getOTPDevicesFromUser(utils.getServiceAdminToken(), userId, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "get mfa device"
        response = cloud20.getOTPDeviceFromUser(utils.getServiceAdminToken(), userId, "id", mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "update mfa settings"
        def mfaSettings = cloud20.v2Factory.createMultiFactorSettings(false, false)
        response = cloud20.updateMultiFactorSettings(utils.getServiceAdminToken(), userId, mfaSettings, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "request bypass codes"
        def bypassCodes = cloud20.v2Factory.createBypassCode(30, 0)
        response = cloud20.getBypassCodes(utils.getServiceAdminToken(), userId, bypassCodes, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "Add phone to user"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone phone = new com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone()
        phone.number = "number"
        response = cloud20.addPhoneToUser(utils.getServiceAdminToken(), userId, phone, mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "Get phone from user"
        response = cloud20.getPhoneFromUser(utils.getServiceAdminToken(), userId, "id", mediaType)

        then:
        response.status == SC_FORBIDDEN

        when: "Delete phone from user"
        response = cloud20.deletePhoneFromUser(utils.getServiceAdminToken(), userId, "id", mediaType)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Allow deleting federated users by id: #mediaType"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id
        def userId = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user.id : samlAuthResponse.user.id

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(sharedServiceAdminToken, samlAuthTokenId)

        then: "the token is still valid"
        validateSamlTokenResponse.status == SC_OK

        when:
        resetCloudFeedsMock()
        def response = cloud20.deleteUser(sharedServiceAdminToken, userId)

        then:
        response.status == SC_NO_CONTENT

        and: "verify that event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        when: "validate the token"
        response = cloud20.validateToken(sharedServiceAdminToken, samlAuthTokenId)

        then: "the token is no longer valid"
        response.status == SC_NOT_FOUND

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "getting user's api key credentials using a federated user's token: #mediaType"() {
        given:

        def userAdmin = utils.createCloudAccount()
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username))
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = [IdentityUserTypeEnum.USER_MANAGER.roleName]
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id
        def userId = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user.id : samlAuthResponse.user.id

        when: "get api key for federated user"
        def response = cloud20.getUserApiKey(samlAuthTokenId, userId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format("User %s not found", userId))

        when: "get api key for provisioned user"
        response = cloud20.getUserApiKey(samlAuthTokenId, defaultUser.id)

        then:
        response.status == SC_OK

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userAdmin])
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "The same precedence rules that apply to deleting provisioned subusers apply to deleting federated subusers"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def federatedUser = getFederatedUser(userAdmin, mediaType)

        when: "default user cannot delete federatedUser"
        def defaultUserToken = utils.getToken(defaultUser.username, DEFAULT_PASSWORD)
        def response = cloud20.deleteUser(defaultUserToken, federatedUser.id)

        then:
        response.status == SC_FORBIDDEN

        when: "user manage can delete federatedUser"
        def userManageToken = utils.getToken(userManage.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userManageToken, federatedUser.id)

        then:
        response.status == SC_NO_CONTENT

        when: "user admin can delete federatedUser"
        def federatedUser2 = getFederatedUser(userAdmin, mediaType)
        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userAdminToken, federatedUser2.id)

        then:
        response.status == SC_NO_CONTENT

        when: "identity admin can delete federatedUser"
        def federatedUser3 = getFederatedUser(userAdmin, mediaType)
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(identityAdminToken, federatedUser3.id)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userManage, userAdmin, identityAdmin])
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteFederatedUserQuietly(federatedUser2.name, sharedOriginIdp.id)
        utils.deleteFederatedUserQuietly(federatedUser3.name, sharedOriginIdp.id)
        utils.deleteDomain(domainId)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "User-admins/user-manage can only delete fed users within own domain"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def domainId2 = utils.createDomain()
        def createUser = v2Factory.createUserForCreate(getRandomUUID("user"), "displayName", "test@rackspace.com", true, "ORD", domainId2, "Password1")
        def response = cloud20.createUser(utils.getIdentityAdminToken(), createUser, mediaType, mediaType)
        def user = getEntity(response, org.openstack.docs.identity.api.v2.User)
        def federatedUser = getFederatedUser(user, mediaType)

        when: "default user cannot delete federatedUser in different domain"
        def defaultUserToken = utils.getToken(defaultUser.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(defaultUserToken, federatedUser.id)

        then:
        response.status == SC_FORBIDDEN

        when: "user manage cannot delete federatedUser in different domain"
        def userManageToken = utils.getToken(userManage.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userManageToken, federatedUser.id)

        then:
        response.status == SC_FORBIDDEN

        when: "user admin cannot delete federatedUser in different domain"
        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userAdminToken, federatedUser.id)

        then:
        response.status == SC_FORBIDDEN

        when: "identity admin can delete federatedUser in different domain"
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        response = cloud20.deleteUser(identityAdminToken, federatedUser.id)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, user)
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteDomain(domainId)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "service/identity admins can update federated user's contactId - media = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def federatedUser = getFederatedUser(userAdmin, mediaType)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUser.id
            it.contactId = contactId
            it
        }

        when: "get federated user by ID"
        def response = cloud20.getUserById(sharedIdentityAdminToken, federatedUser.id)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == null

        when: "update federated user using service admin"
        response = cloud20.updateUser(utils.getServiceAdminToken(), federatedUser.id, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == contactId

        when: "update federated user using identity admin"
        contactId = testUtils.getRandomUUID("contactId")
        userForCreate.contactId = contactId
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUser.id, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == contactId

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    def "Updating federated user's contactId does not erase legacy groups or user groups"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        // Assign user-admin to 2 legacy groups
        utils.addUserToGroupWithId("0", userAdmin)
        utils.addUserToGroupWithId(Constants.RAX_STATUS_RESTRICTED_GROUP_ID, userAdmin)

        def userGroup = utils.createUserGroup(userAdmin.domainId)

        AuthenticateResponse fedAuthResponse = utils.authenticateFederatedUser(userAdmin.domainId, [userGroup.name] as Set)

        def federatedUserId = fedAuthResponse.user.id
        def federatedUser = utils.getUserById(federatedUserId)

        // Fed user has groups
        assert utils.listGroupsForUser(federatedUser).group.size() == 2
        assert utils.listUserGroupsForDomain(federatedUser.getDomainId(), new UserGroupSearchParams(null, federatedUserId)).userGroup.size() == 1

        // Contact id is null
        def user = utils.getUserById(federatedUserId)
        assert user.contactId == null

        when: "update federated user using service admin"
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }
        utils.updateUser(userForCreate)

        then: "contact id updated"
        utils.getUserById(federatedUserId).contactId == contactId

        and: "legacy groups remain"
        utils.listGroupsForUser(federatedUser).group.size() == 2

        and: "user groups remain"
        utils.listUserGroupsForDomain(federatedUser.getDomainId(), new UserGroupSearchParams(null, federatedUserId)).userGroup.size() == 1
    }

    @Unroll
    def "Ensure contactId cannot be unset on a federated user - media = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()

        def federatedUser = getFederatedUser(userAdmin, mediaType)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUser.id
            it.contactId = contactId
            it
        }

        when: "get federated user by ID"
        def response = cloud20.getUserById(sharedIdentityAdminToken, federatedUser.id)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == null

        when: "update federated user"
        response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == contactId

        when: "attempt to unset contactId with null"
        userForCreate.contactId = null
        response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == contactId

        when: "attempt to unset contactId with empty string"
        userForCreate.contactId = ""
        response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(Validator20.EMPTY_ATTR_MESSAGE, "contactId"))

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteDomain(userAdmin.domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Updating federated user ignores all attributes other than contactId - username = #username, domainId = #domainId, enabled = #enabled, federatedIdp = #federatedIdp, defaultRegion = #defaultRegion"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def federatedUser = getFederatedUser(userAdmin, APPLICATION_XML_TYPE)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUser.id
            it.contactId = contactId
            it
        }
        cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate)
        def retrievedUser = cloud20.getUserById(sharedIdentityAdminToken, federatedUser.id).getEntity(org.openstack.docs.identity.api.v2.User).value

        when: "update federated user"
        userForCreate = new UserForCreate().with {
            it.username = username
            it.domainId = domainId
            it.enabled = enabled
            it.federatedIdp = federatedIdp
            it.defaultRegion = defaultRegion
            it
        }
        def response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == retrievedUser.id
        entityUser.contactId == retrievedUser.contactId
        entityUser.username == retrievedUser.username
        entityUser.domainId == retrievedUser.domainId
        entityUser.enabled == retrievedUser.enabled
        entityUser.federatedIdp == retrievedUser.federatedIdp
        entityUser.defaultRegion == retrievedUser.defaultRegion

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteUsers(userAdmin)
        utils.deleteDomain(userAdmin.domainId)

        where:
        username | domain  | enabled | federatedIdp | defaultRegion
        "other"  | null    | null    | null         | null
        null     | "other" | null    | null         | null
        null     | null    | false   | null         | null
        null     | null    | null    | "otherIdp"   | null
        null     | null    | null    | null         | "other"
    }

    @Unroll
    def "Update federated user error check - media = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def federatedUser = getFederatedUser(userAdmin, mediaType)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUser.id
            it.contactId = contactId
            it
        }

        when: "update user with invalid id"
        def response = cloud20.updateUser(utils.getToken(defaultUser.username), "invalid", userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultCloud20Service.USER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "update federated user with invalid id set on the entity"
        userForCreate.id = "invalid"
        response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, DefaultCloud20Service.ID_MISMATCH)

        when: "update federated user's contactId exceeding max length"
        userForCreate.id = null
        userForCreate.contactId = testUtils.getRandomUUIDOfLength("contactId", 100)
        response = cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ERROR_CODE_MAX_LENGTH_EXCEEDED)

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "get/list services for federated users return correct attributes - media = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)

        def federatedUser = getFederatedUser(userAdmin, mediaType)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUser.id
            it.contactId = contactId
            it
        }
        cloud20.updateUser(sharedIdentityAdminToken, federatedUser.id, userForCreate, mediaType)

        when: "get user by id"
        userForCreate.contactId = null
        def response = cloud20.getUserById(sharedIdentityAdminToken, federatedUser.id, mediaType)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUser.id
        entityUser.contactId == contactId
        entityUser.federatedIdp == sharedOriginIdp.issuer
        entityUser.domainId == domainId

        when: "list users by domain"
        response = cloud20.getUsersByDomainId(sharedIdentityAdminToken, domainId, mediaType)
        def entityUserList = testUtils.getEntity(response, UserList)

        then:
        response.status == SC_OK
        entityUserList.user.find({it.id == federatedUser.id}).id == federatedUser.id
        entityUserList.user.find({it.id == federatedUser.id}).contactId == contactId
        entityUserList.user.find({it.id == federatedUser.id}).federatedIdp == sharedOriginIdp.issuer
        entityUserList.user.find({it.id == federatedUser.id}).domainId == domainId
        entityUserList.user.find({ it.id == federatedUser.id }).multiFactorEnabled == false

        when:
        response = cloud20.listUsers(userAdminToken, "0", "1000", mediaType)
        entityUserList = testUtils.getEntity(response, UserList)

        then:
        response.status == SC_OK
        entityUserList.user.find({ it.id == federatedUser.id }).id == federatedUser.id
        entityUserList.user.find({ it.id == federatedUser.id }).contactId == contactId
        entityUserList.user.find({ it.id == federatedUser.id }).federatedIdp == sharedOriginIdp.issuer
        entityUserList.user.find({ it.id == federatedUser.id }).domainId == domainId
        entityUserList.user.find({ it.id == federatedUser.id }).multiFactorEnabled == false

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.name, sharedOriginIdp.id)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Validate contactId on authentication response for auth/validate - media = #mediaType"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        assert samlResponse.status == SC_OK
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUserEntity = samlAuthResponse.value.user

        // Update fed user's contactId
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserEntity.id
            it.contactId = contactId
            it
        }
        cloud20.updateUser(sharedIdentityAdminToken, federatedUserEntity.id, userForCreate, mediaType)

        when: "saml auth does not expose contactId"
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        federatedUserEntity = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user : samlAuthResponse.user

        then:
        samlResponse.status == SC_OK

        federatedUserEntity != null
        federatedUserEntity.contactId == null

        when: "validate token returns contactId"
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token.id : samlAuthResponse.token.id
        def response = cloud20.validateToken(sharedIdentityAdminToken, samlAuthToken, mediaType)
        def responseEntity = testUtils.getEntity(response, AuthenticateResponse)

        then:
        response.status == SC_OK

        federatedUserEntity != null
        responseEntity.user.contactId == contactId

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUserEntity.name, sharedOriginIdp.id)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    def "Federated Authentication for Managed Public Cloud - whitelist role based on tenant type"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        utils.addTenantToDomain(userAdmin.domainId, tenant.id)

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:default")
        def samlResponse = cloud20.federatedAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), true)

        then: "roles are returned for tenant"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def roles = authResponse.user.roles.role
        roles.find {it.tenantId == tenant.id} != null

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type without role"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        samlResponse = cloud20.federatedAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), true)

        then: "role with tenant type matching white list is not returned"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse2 = samlResponse.getEntity(AuthenticateResponse).value
        def roles2 = authResponse2.user.roles.role
        roles2.find {it.tenantId == tenant.id} == null

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Federated Authentication for Managed Public Cloud - whitelist role endpoints on tenant type"() {
        given:
        reloadableConfiguration.reset()

        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def tenantName = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenantWithTypes(tenantName, [CLOUD])
        utils.addTenantToDomain(userAdmin.domainId, tenant.id)

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:default")
        def samlResponse = cloud20.federatedAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), true)

        then: "endpoints are returned for tenant"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def endpoints = authResponse.serviceCatalog.service
        endpoints.findAll { it.type == "compute" }.size() > 0

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type without role"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY_CLOUD, "identity:service-admin")
        samlResponse = cloud20.federatedAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), true)

        then: "endpoint with tenant type matching white list is not returned"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse2 = samlResponse.getEntity(AuthenticateResponse).value
        def endpoints2 = authResponse2.serviceCatalog.service
        endpoints2.findAll { it.type == "compute" }.size() == 0

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Authenticating or updating existing federated user does not send CREATE feed event"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        resetCloudFeedsMock()

        when: "saml auth of existing federated user"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUser = utils.getUserById(samlAuthResponse.value.user.id)

        then: "assert 200"
        samlResponse.status == SC_OK

        and: "verify that the user CREATE event is not posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(federatedUser, FeedsUserStatusEnum.CREATE.value()),
                VerificationTimes.exactly(0)
        )

        when: "updating federated user email"
        resetCloudFeedsMock()
        fedRequest.email = "test@mail.com"
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        federatedUser = utils.getUserById(samlAuthResponse.value.user.id)

        then:
        samlResponse.status == SC_OK
        federatedUser.email == "test@mail.com"

        and: "verify that the user CREATE event is not posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(federatedUser, FeedsUserStatusEnum.CREATE.value()),
                VerificationTimes.exactly(0)
        )

        when: "add role to existing federated user"
        resetCloudFeedsMock()
        fedRequest.roleNames = [ROLE_RBAC1_NAME]
        samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        federatedUser = utils.getUserById(samlAuthResponse.value.user.id)

        then:
        samlResponse.status == SC_OK

        and: "verify that the user UPDATE event is posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(federatedUser, FeedsUserStatusEnum.UPDATE.value()),
                VerificationTimes.exactly(1)
        )


        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.username, sharedOriginIdp.id)
    }

    def "federated user can revoke own token"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = samlAuthResponse.value.token
        def samlAuthTokenId = samlAuthToken.id

        when: "revoke token"
        def response = cloud20.revokeUserToken(samlAuthTokenId, samlAuthTokenId)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "federated user can retrieve own domain"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = samlAuthResponse.value.token
        def samlAuthTokenId = samlAuthToken.id

        when: "get domain"
        def response = cloud20.getDomain(samlAuthTokenId, sharedUserAdmin.domainId)
        def domainEntity = response.getEntity(Domain)

        then:
        response.status == SC_OK

        domainEntity.id == sharedUserAdmin.domainId

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "User-admin can revoke a federated user's token within own domain"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userAdmin2 = utils.createCloudAccount()
        def userAdmin2Token = utils.getToken(userAdmin2.username)

        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = samlAuthResponse.value.token
        def samlAuthTokenId = samlAuthToken.id

        when: "revoke token - userAdmin not in same domain"
        def response = cloud20.revokeUserToken(userAdmin2Token, samlAuthTokenId)

        then:
        response.status == SC_FORBIDDEN

        when: "revoke token - userAdmin within same domain"
        response = cloud20.revokeUserToken(userAdminToken, samlAuthTokenId)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(userAdmin2)
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "impersonating a user-admin should allow revoking federated user's token within same domain"() {
        given:
        def userAdmin = utils.createCloudAccount()

        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = samlAuthResponse.value.token
        def samlAuthTokenId = samlAuthToken.id

        // impersonate a user-admin
        ImpersonationResponse impersonationResponse = utils.impersonate(sharedIdentityAdminToken, userAdmin)
        def impersonateToken = impersonationResponse.getToken().id

        when: "revoke token"
        def response = cloud20.revokeUserToken(impersonateToken, samlAuthTokenId)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "user-admin shouldn't be able to revoke token obtained by impersonating a fed user"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def fedUserId = samlAuthResponse.value.user.id
        def fedUser = utils.getUserById(fedUserId)

        //impersonate federated user
        ImpersonationResponse impersonationResponse = utils.impersonate(sharedIdentityAdminToken, fedUser)
        def impersonateToken = impersonationResponse.getToken().id

        when: "revoke token"
        def response = cloud20.revokeUserToken(utils.getToken(userAdmin.username), impersonateToken)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    def "Fed user can be assigned the user-manager role"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        fedRequest.roleNames = [IdentityUserTypeEnum.USER_MANAGER.roleName]
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when: "auth"
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value

        then: "verify fed user has the user-manager role"
        samlResponse.status == HttpServletResponse.SC_OK

        verifyResponseFromSamlRequest(authResponse, fedRequest.username, userService.getUserById(sharedUserAdmin.id))
        def roles = authResponse.user.roles.role

        roles.find {it.id == DEFAULT_USER_ROLE_ID} != null
        roles.find {it.id == USER_MANAGE_ROLE_ID} != null

        when: "getting role accessible to user-manager"
        def fedUserToken = authResponse.token.id
        def response = cloud20.getRole(fedUserToken, ROLE_RBAC1_ID)
        def role = response.getEntity(Role).value

        then: "assert correct role is returned"
        response.status == SC_OK

        role.id == ROLE_RBAC1_ID

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username, sharedOriginIdp.id)
    }

    @Unroll
    def "UpdateUser: User can update only his phone pin and not contactId - media = #mediaType"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUserId = samlAuthResponse.value.user.id
        def federatedUser = utils.getUserById(federatedUserId)

        // Update fed user's contactId
        def contactId = testUtils.getRandomUUID("contactId")
        def phonePin = "786124"

        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it.phonePin = phonePin
            it
        }

        when: "identity-admin updates contactId and phonePin for Fed User"
        def updateUserResp = cloud20.updateUser(sharedIdentityAdminToken, federatedUserId, userForCreate, mediaType)
        samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUserEntity = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user : samlAuthResponse.user
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token.id : samlAuthResponse.token.id
        def response = cloud20.validateToken(sharedIdentityAdminToken, samlAuthToken, mediaType)
        def responseEntity = testUtils.getEntity(response, AuthenticateResponse)

        then: "only contactId should get updated and phonePin and other attributes should get ignored"
        response.status == SC_OK
        federatedUserEntity != null
        responseEntity.user.contactId == contactId
        responseEntity.user.phonePin != null
        responseEntity.user.phonePin != phonePin

        and: "update user response should not show phone pin attribute"
        updateUserResp.status == SC_OK
        if(mediaType == APPLICATION_XML_TYPE) {
            updateUserResp.getEntity(org.openstack.docs.identity.api.v2.User).value.phonePin == null
        }else{
            updateUserResp.getEntity(org.openstack.docs.identity.api.v2.User).phonePin != phonePin
        }

        when: "Fed user updates contactId and phonePin for himself"
        userForCreate.setContactId("newContactId")
        updateUserResp = cloud20.updateUser(samlAuthToken, federatedUserId, userForCreate, mediaType)
        response = cloud20.validateToken(utils.getIdentityAdminToken(), samlAuthToken, mediaType)
        responseEntity = testUtils.getEntity(response, AuthenticateResponse)

        then: "only phonePin should get updated and contactId and other attributes should get ignored"
        response.status == SC_OK
        federatedUserEntity != null
        responseEntity.user.contactId != "newContactId"
        responseEntity.user.phonePin != null
        responseEntity.user.phonePin == phonePin

        and: "update user response should show phone pin attribute"
        updateUserResp.status == SC_OK
        if(mediaType == APPLICATION_XML_TYPE) {
            updateUserResp.getEntity(org.openstack.docs.identity.api.v2.User).value.phonePin == phonePin
        }else{
            updateUserResp.getEntity(org.openstack.docs.identity.api.v2.User).phonePin == phonePin
        }

        cleanup:
        utils.deleteFederatedUserQuietly(federatedUser.username, sharedOriginIdp.id)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    def getFederatedUser(userAdmin, mediaType = APPLICATION_XML) {
        def fedRequest = utils.createFedRequest(userAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlAssertion = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        def samlResponse = cloud20.samlAuthenticate(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion), mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def user = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user : samlAuthResponse.user
        return user
    }

}
