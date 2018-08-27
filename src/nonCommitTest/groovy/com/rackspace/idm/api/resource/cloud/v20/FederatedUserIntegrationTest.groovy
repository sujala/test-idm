package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.domain.config.IdentityConfig

import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.ProvisionedUserSourceFederationHandler
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams
import com.rackspace.idm.util.SamlUnmarshaller
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.lang.BooleanUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.mockserver.verify.VerificationTimes
import org.opensaml.saml.saml2.core.LogoutResponse
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.StatusCode
import org.opensaml.xmlsec.signature.Signature
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static org.apache.http.HttpStatus.*

class FederatedUserIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedUserIntegrationTest.class)

    static String TENANT_TYPE = "dumb_tenant_type"
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

    @Shared String specificationServiceAdminToken;

    @Autowired
    V2Factory factory

    /**
     * An identity provider created for this class. No code should modify this provider.
     */
    @Shared IdentityProvider sharedIdentityProvider
    @Shared SamlProducer samlProducerForSharedIdp

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
        def serviceAdminAuthResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        if (serviceAdminAuthResponse.value instanceof IdentityFault) {
            def fault = (IdentityFault)serviceAdminAuthResponse.value
            LOG.error("Error authenticating service admin to setup test run. '" + fault.getMessage() + "'", fault)
        }
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        specificationServiceAdminToken = serviceAdminAuthResponse.value.token.id

        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)
        samlProducerForSharedIdp = new SamlProducer(SamlCredentialUtils.generateX509Credential(cert1, keyPair1))
        sharedIdentityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        cloud20.createIdentityProvider(specificationServiceAdminToken, sharedIdentityProvider)

        def tenantType = new TenantType().with {
            it.name = TENANT_TYPE
            it.description = "description"
            it
        }
        cloud20.addTenantType(specificationServiceAdminToken, tenantType)
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
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser.domainId == domainId
        fedUser.email == email
        fedUser.region == userAdminEntity.region
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "Fed user includes auto-assigned roles on authenticate"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when: "auth"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content and auto-assigned roles"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)
        def roles = authResponse.user.roles.role
        roles.size() == 5
        def mossoRole = roles.find {it.id == Constants.MOSSO_ROLE_ID}
        mossoRole != null
        def nastRole = roles.find {it.id == Constants.NAST_ROLE_ID}
        nastRole != null

        roles.find {it.id == Constants.DEFAULT_USER_ROLE_ID} != null
        roles.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoRole.tenantId} != null
        roles.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastRole.tenantId} != null

        when: "validate the token w/ feature enabled"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id)

        then: "the token is still valid and returns auto-assigned roles"
        validateSamlTokenResponse.status == 200
        AuthenticateResponse valResponse = validateSamlTokenResponse.getEntity(AuthenticateResponse).value
        def roles2 = valResponse.user.roles.role
        roles2.size() == 5
        def mossoRole2 = roles2.find {it.id == Constants.MOSSO_ROLE_ID}
        mossoRole2 != null
        def nastRole2 = roles2.find {it.id == Constants.NAST_ROLE_ID}
        nastRole2 != null

        roles2.find {it.id == Constants.DEFAULT_USER_ROLE_ID} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoRole2.tenantId} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastRole2.tenantId} != null

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    def "Can handle attribute values without datatype specified"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlFactory = new SamlFactory()
        //specify assertion with no roles
        HashMap<String, List<Object>> attributes = new HashMap<String, List<Object>>()
        attributes.put("email", [email])
        attributes.put("domain", [Integer.valueOf(domainId)]) //make the domain a list so the xsany type is used
        def samlResponse = samlFactory.generateSamlAssertion(Constants.DEFAULT_IDP_URI, username, expSecs, attributes, SAMLAuthContext.PASSWORD.samlAuthnContextClassRef)
        def samlAssertion = samlFactory.convertResponseToString(samlResponse)


        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlAuthResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlAuthResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlAuthResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser.domainId == domainId
        fedUser.email == email
        fedUser.region == userAdminEntity.region
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    // [CIDMDEV-5294] Mark Federated Users as eligible for deletion
    def "auth user updates the expiration time on it"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)
        def authResponse, fedUser, samlResponse, previousExpiration

        when: "Auth first (creates the user)"
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "retrieve user from backend"
        authResponse = samlResponse.getEntity(AuthenticateResponse).value
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        when: "test if another token changes the timestamp"
        previousExpiration = fedUser.expiredTimestamp
        samlResponse = cloud20.samlAuthenticate(samlAssertion)
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
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "retrieve user from backend"
        authResponse = samlResponse.getEntity(AuthenticateResponse).value
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)


        then: "reflects current state and expiration date must be in future"
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)
        fedUser.expiredTimestamp != null
        fedUser.expiredTimestamp.after(authResponse.token.expires.toGregorianCalendar().getTime())

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Unroll
    def "test response age validation for federation: secToAddToAge == #secToAddToAge, expectedResponse == #expectedResponse"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def assertionFactory = new SamlFactory()
        def maxResponseAge = identityConfig.getReloadableConfig().getFederatedResponseMaxAge()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when: "issueInstant is in the past but not older than max saml response age"
        def issueInstant = new DateTime().minusSeconds(maxResponseAge)
        def samlResponse = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, 60, domainId, null, email, Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstant);
        def response = cloud20.samlAuthenticate(assertionFactory.convertResponseToString(samlResponse))

        then:
        response.status == 200

        when: "issueInstant is in the past but not older than max saml response age + skew"
        issueInstant = new DateTime().minusSeconds(maxResponseAge)
        //subtracting a few seconds off of the skew. Making it exactly equal will fail b/c of the time for the round trip
        issueInstant = issueInstant.minusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() - 3)
        samlResponse = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, 60, domainId, null, email, Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstant);
        response = cloud20.samlAuthenticate(assertionFactory.convertResponseToString(samlResponse))

        then:
        response.status == 200

        when: "issueInstant is in the past and older than max saml response age + skew"
        issueInstant = new DateTime().minusSeconds(maxResponseAge)
        //subtracting a few seconds off of the skew. Making it exactly equal will fail b/c of the time for the round trip
        issueInstant = issueInstant.minusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() + 60)
        samlResponse = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, 60, domainId, null, email, Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstant);
        response = cloud20.samlAuthenticate(assertionFactory.convertResponseToString(samlResponse))

        then:
        response.status == 400

        when: "issueInstant is in the future but within the allowed skew"
        issueInstant = new DateTime()
        issueInstant = issueInstant.plusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() - 3)
        samlResponse = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, 60, domainId, null, email, Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstant);
        response = cloud20.samlAuthenticate(assertionFactory.convertResponseToString(samlResponse))

        then:
        response.status == 200

        when: "issueInstant is in the future but outside of the the allowed skew"
        issueInstant = new DateTime()
        issueInstant = issueInstant.plusSeconds(identityConfig.getReloadableConfig().getFederatedResponseMaxSkew() + 60)
        samlResponse = assertionFactory.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, 60, domainId, null, email, Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstant);
        response = cloud20.samlAuthenticate(assertionFactory.convertResponseToString(samlResponse))

        then:
        response.status == 400

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Unroll
    def "test token lifetime validation for domain user federated tokens: secToAddToExp == #secToAddToExp, expectedResponse == #expectedResponse"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def expSeconds = identityConfig.getReloadableConfig().getFederatedDomainTokenLifetimeMax() + secToAddToExp
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSeconds, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == expectedResponse

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        secToAddToExp | expectedResponse
        -60           | 200
        0             | 200
        60            | 400
    }

    def "initial user populated appropriately from saml - user admin group added to federated user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        // add group to user admin
        def group = utils.createGroup();
        userService.addGroupToUser(group.id, userAdminEntity.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state including groups"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser.domainId == domainId
        fedUser.email == email
        fedUser.region == userAdminEntity.region
        fedUser.rsGroupId.size() == 2
        fedUser.rsGroupId.contains(group.id)

        when: "check to make sure group shows up in list user groups call"
        def listGroupsForUserResponse = cloud20.listGroupsForUser(utils.getServiceAdminToken(), authResponse.user.id)
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        then:
        groups.getGroup().size == 2
        groups.group.findAll({it.id == group.id}).size() == 1

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteGroup(group)
    }

    def "initial user populated appropriately from saml with 1 role provided"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(rbacRole1.name), email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity, Arrays.asList(rbacRole1))

        when: "retrieve user from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser.domainId == domainId
        fedUser.email == email
        fedUser.region == userAdminEntity.region

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Unroll
    def "SAML authenticate w/ explicit v1.0 request produces - #accept"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(role1000.name), email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.federatedAuthenticate(samlAssertion, false, GlobalConstants.FEDERATION_API_V1_0, accept)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        def authResponse
        if(accept == APPLICATION_XML_TYPE) {
            authResponse = samlResponse.getEntity(AuthenticateResponse).value
        } else {
            authResponse = samlResponse.getEntity(AuthenticateResponse)
        }
        verifyResponseFromSamlRequestAndBackendRoles(authResponse, username, userAdminEntity, Arrays.asList(role1000))

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        accept                | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    def "Legacy SAML authenticate with 'x-www-form-urlencoded' media type"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(role1000.name), email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        byte[] encodedSamlAssertion = Base64.encodeBase64(samlAssertion.getBytes(), false, true)
        def samlResponse = cloud20.samlAuthenticate("SAMLResponse=" + new String(encodedSamlAssertion), MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequestAndBackendRoles(authResponse, username, userAdminEntity, Arrays.asList(role1000))

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "Can specify a role with a space in the name"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(role1000.name), email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequestAndBackendRoles(authResponse, username, userAdminEntity, Arrays.asList(role1000))

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Unroll
    def "samlResponse rejected when specify illegal role set '#delimitedRoleNames' because #rejectionReason"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        List<String> roleNames = Arrays.asList(delimitedRoleNames.split(","))

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, roleNames, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "returns error error"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse.getEntity(String.class).contains(errorMessageContains)

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        delimitedRoleNames | rejectionReason | errorMessageContains
        "identity:default" | "not 1000 weight" | "Invalid role"
        "compute:default" | "not 1000 weight" | "Invalid role"
        "non-existant_role_name" | "non-existant role" | "Invalid role"
        RBACROLE1_NAME + "," + RBACROLE1_NAME | "duplicate role included" | "specified more than once"
    }

    def "User roles reflect last saml response"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertionNone = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def samlAssertionRbac1 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(rbacRole1.name), email);
        def samlAssertionRbac1And2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(rbacRole1.name, rbacRole2.name), email);
        def samlAssertionRbac2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList(rbacRole2.name), email);

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertionNone)

        then: "user has no rbac roles"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value, username, userAdminEntity, null, Arrays.asList(rbacRole1, rbacRole2))

        when:
        samlResponse = cloud20.samlAuthenticate(samlAssertionRbac1)

        then: "user has rbacRole1"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value, username, userAdminEntity, Arrays.asList(rbacRole1), Arrays.asList(rbacRole2))

        when:
        samlResponse = cloud20.samlAuthenticate(samlAssertionRbac1And2)

        then: "user has rbacRole1 and rbacRole2"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value, username, userAdminEntity, Arrays.asList(rbacRole1, rbacRole2), null)

        when:
        samlResponse = cloud20.samlAuthenticate(samlAssertionRbac2)

        then: "user has rbacRole2"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value, username, userAdminEntity, Arrays.asList(rbacRole2), Arrays.asList(rbacRole1))

        when:
        samlResponse = cloud20.samlAuthenticate(samlAssertionNone)

        then: "user has no rbac roles"
        verifyResponseFromSamlRequestAndBackendRoles(samlResponse.getEntity(AuthenticateResponse).value, username, userAdminEntity, null, Arrays.asList(rbacRole1, rbacRole2))

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "federated user is disabled when user admin on domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)

        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        def disabledDomainErrorMessage = String.format(ProvisionedUserSourceFederationHandler.DISABLED_DOMAIN_ERROR_MESSAGE, domainId)
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)

        when: "try to pass a saml assertion for a domain"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request succeeds"
        samlResponse.status == 200

        when: "disable the user admins and try to pass a saml assertion again for the same user"
        utils.disableUser(userAdmin1)
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == 400
        samlResponse.getEntity(BadRequestFault).value.message == disabledDomainErrorMessage

        when: "try to pass a saml assertion for a new user in the same domain"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, testUtils.getRandomUUID(), expSecs, domainId, null);
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == 400
        samlResponse.getEntity(BadRequestFault).value.message == disabledDomainErrorMessage

        cleanup:
        utils.deleteUsers(users1)
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "federated token contains tenant: #mediaType"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)

        when: "authenticate with saml"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id

        then: "the tenant is populated"
        samlResponse.status == 200
        samlAuthToken.tenant != null
        samlAuthToken.tenant.id != null
        samlAuthToken.tenant.id == domainId

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthTokenId)

        then: "the token is still valid"
        validateSamlTokenResponse.status == 200

        cleanup:
        utils.deleteUsers(users1)
        staticIdmConfiguration.reset()

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "domain federated token contains correct authBy values: #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = 500
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)

        when: "authenticate with saml"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token

        then: "the authBy is populated"
        samlResponse.status == 200
        samlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        samlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken.id)
        def validateSamlAuthToken = validateSamlTokenResponse.getEntity(AuthenticateResponse).value.token

        then: "the returned token also has valid authBy values"
        validateSamlTokenResponse.status == 200
        validateSamlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        validateSamlAuthToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        cleanup:
        utils.deleteUsers(users1)
        staticIdmConfiguration.reset()

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    def "federated and provisioned user tokens are revoked when the user admin for the domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)

        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlAuthToken = samlResponse.getEntity(AuthenticateResponse).value.token.id
        def provisionedUser = utils.createUserWithUser(userAdmin1)
        def provisionedUserToken = utils.authenticate(provisionedUser).token.id

        when: "validate the tokens with enabled user admins"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken)
        def validateProvisionedTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserToken)

        then: "the tokens are still valid"
        validateSamlTokenResponse.status == 200
        validateProvisionedTokenResponse.status == 200

        when: "disable user admin"
        utils.disableUser(userAdmin1)
        validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken)
        validateProvisionedTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserToken)

        then: "the token is no longer valid"
        validateSamlTokenResponse.status == 404
        validateProvisionedTokenResponse.status == 404

        cleanup:
        utils.deleteUser(provisionedUser)
        utils.deleteUsers(users1)
        staticIdmConfiguration.reset()
    }

    def "federated users are limited within each IDP"() {
        given:
        //set the user limit low for lower overhead
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, 2)
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("samlUser")
        def username2 = testUtils.getRandomUUID("samlUser")
        def username3 = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        //fill the domain with the max allowed number of users
        assert cloud20.samlAuthenticate(new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username1, expSecs, domainId, null)).status == 200
        assert cloud20.samlAuthenticate(new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username2, expSecs, domainId, null)).status == 200

        when: "try to exceed the limit under the current IDP"
        def samlResponse = cloud20.samlAuthenticate(new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username3, expSecs, domainId, null));

        then: "the response is a failure"
        samlResponse.status == 400

        when: "auth with existing user under the current IDP"
        samlResponse = cloud20.samlAuthenticate(new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username1, expSecs, domainId, null));

        then: "the response is a success"
        samlResponse.status == 200

        when: "try to create the same user under a different IDP (the limit is per IDP per domain)"
        samlResponse = cloud20.samlAuthenticate(new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.IDP_2_URI, username3, expSecs, domainId, null, Constants.DEFAULT_FED_EMAIL, Constants.IDP_2_PRIVATE_KEY, Constants.IDP_2_PUBLIC_KEY));

        then: "the request succeeds"
        samlResponse.status == 200

        cleanup:
        deleteFederatedUserQuietly(username1)
        deleteFederatedUserQuietly(username2)
        deleteFederatedUserQuietly(username3)
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    def "Deleting a Domain federated user returns logout response"() {
        given:
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def saToken = utils.getServiceAdminToken()

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username1, expSecs, domainId, null)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpStatus.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlAuthToken = authResponse.token.id

        //verify token is good and user can be retrieved
        utils.getUserById(authResponse.user.id) != null
        utils.validateToken(samlAuthToken)

        when: "delete the user"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(Constants.DEFAULT_IDP_URI, username1)
        def logoutResponse = cloud20.federatedLogout(logoutRequest);

        then: "the response is a success"
        logoutResponse.status == HttpStatus.SC_OK
        LogoutResponse obj = samlUnmarshaller.unmarshallLogoutRespone(StringUtils.getBytesUtf8(logoutResponse.getEntity(String.class)))
        obj.getStatus().getStatusCode().value == StatusCode.SUCCESS

        and: "the user does not exist in backend"
        cloud20.getUserById(saToken, authResponse.user.id).status == HttpStatus.SC_NOT_FOUND

        and: "the previously issued token is no longer valid"
        cloud20.validateToken(saToken, samlAuthToken).status == HttpStatus.SC_NOT_FOUND

        when: "delete the user again"
        logoutRequest = new SamlFactory().generateLogoutRequestEncoded(Constants.DEFAULT_IDP_URI, username1)
        logoutResponse = cloud20.federatedLogout(logoutRequest);

        then: "the response is a failure marked as requestor failure"
        logoutResponse.status == HttpStatus.SC_BAD_REQUEST
        LogoutResponse logoutResponseObj = samlUnmarshaller.unmarshallLogoutRespone(StringUtils.getBytesUtf8(logoutResponse.getEntity(String.class)))
        logoutResponseObj.getStatus().getStatusCode().value == StatusCode.REQUESTER

        cleanup:
        deleteFederatedUserQuietly(username1)
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    def "verify federated request: A valid signature is successful when request references an existing user"() {
        given:
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def saToken = utils.getServiceAdminToken()

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username1, expSecs, domainId, null)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpStatus.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlAuthToken = authResponse.token.id

        //verify token is good and user can be retrieved
        utils.getUserById(authResponse.user.id) != null
        utils.validateToken(samlAuthToken)

        when: "verify the logout request"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(Constants.DEFAULT_IDP_URI, username1)
        def validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "the response is a success"
        validateResponse.status == HttpStatus.SC_OK

        cleanup:
        deleteFederatedUserQuietly(username1)
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    @Unroll
    def "verify federated request: A valid signature is successful regardless of nameId in request: #nameId"() {
        when: "verify the logout request"
        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(Constants.DEFAULT_IDP_URI, nameId)
        def validateResponse = cloud20.federatedValidateRequest(logoutRequest)

        then: "The response is a success"
        validateResponse.status == HttpStatus.SC_OK

        where:
        nameId << ["", "non-exist", null]
    }

    @Unroll
    def "verify federated request: Old requests are rejected: #requestAge"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEDERATED_RESPONSE_MAX_SKEW, 0)
        def issueInstance = new DateTime().minusSeconds(requestAge)

        def logoutRequest = new SamlFactory().generateLogoutRequestEncoded(Constants.DEFAULT_IDP_URI, "user", Constants.DEFAULT_IDP_PRIVATE_KEY, Constants.DEFAULT_IDP_PUBLIC_KEY, issueInstance)

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
        IdmAssert.assertOpenStackV2FaultResponse(validateResponse, BadRequestFault, SC_BAD_REQUEST, "Saml issueInstant cannot be older than " + maxAge + " seconds.")

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
        assert authResponse.user.federatedIdp == Constants.DEFAULT_IDP_URI
        assert authResponse.user.defaultRegion == userAdminEntity.region

        //check the token
        assert authResponse.token.id != null
        assert authResponse.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
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
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse1.status == HttpServletResponse.SC_OK
        samlResponse2.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse1 = samlResponse1.getEntity(AuthenticateResponse).value
        AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        authResponse1.user.id == authResponse2.user.id

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "passing multiple saml requests with same user, but different domain id throws error"() {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId2, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)


        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)

        then:
        samlResponse1.status == HttpServletResponse.SC_OK
        samlResponse2.status == HttpServletResponse.SC_CONFLICT

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
    }

    def "test federated user with a disabled domain"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def adminToken = utils.getIdentityAdminToken()

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "first authenticate the token"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id
        def validateResponse = cloud20.validateToken(adminToken, samlToken)

        then: "response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        validateResponse.status == HttpServletResponse.SC_OK

        when: "disable the domain"
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        utils.updateDomain(domainId, domain)

        then: "token should not work"
        def validateResponse2 = cloud20.validateToken(adminToken, samlToken)
        validateResponse2.status == HttpServletResponse.SC_NOT_FOUND

        when: "try to get another token"
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion)

        then: "token should not work"
        samlResponse2.status == HttpServletResponse.SC_BAD_REQUEST

        when: "enable the domain again"
        domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = true
            it
        }
        utils.updateDomain(domainId, domain)

        then: "old token should not work [B-71699]"
        def validateResponse3 = cloud20.validateToken(adminToken, samlToken)
        validateResponse3.status == HttpServletResponse.SC_NOT_FOUND

        when: "try to get another token"
        def samlResponse3 = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse2 = samlResponse3.getEntity(AuthenticateResponse).value
        def samlToken2 = authResponse2.token.id
        def validateResponse4 = cloud20.validateToken(adminToken, samlToken2)

        then: "response contains appropriate content"
        samlResponse3.status == HttpServletResponse.SC_OK
        validateResponse4.status == HttpServletResponse.SC_OK

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "Invalid SAML signature results in 400"() {
        given:
        def username = Constants.RACKER_NOGROUP
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlFactor = new SamlFactory()
        def domainId = utils.createDomain()
        def email = "fedIntTest@invalid.rackspace.com"

        Response samlAssertion = samlFactor.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        Response samlAssertion2 = samlFactor.generateSamlAssertionResponseForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs+100, domainId, null, email);

        //replace first assertion with second to make an invalid assertion
        Signature sig = samlAssertion2.getSignature()
        sig.detach()
        samlAssertion.setSignature(sig)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlFactor.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpServletResponse.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SIGNATURE)
    }

    def "empty IssueInstant should give bad request"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        samlAssertion = samlAssertion.replaceAll("IssueInstant=\"([^\"]+)\"", "IssueInstant=\"\"")

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains bad request"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "empty Version should give bad request"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        samlAssertion = samlAssertion.replaceAll("Version=\"([^\"]+)\"", "Version=\"\"")

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains bad request"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "invalid NotOnOrAfter should give bad request"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        samlAssertion = samlAssertion.replaceAll("NotOnOrAfter=\"([^\"]+)\"", "NotOnOrAfter=\"test\"")

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains bad request"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "invalid AuthnInstant should give bad request"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        samlAssertion = samlAssertion.replaceAll("AuthnInstant=\"([^\"]+)\"", "AuthnInstant=\"test\"")

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains bad request"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "empty Algorithm should give bad request"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        samlAssertion = samlAssertion.replaceAll("Algorithm=\"([^\"]+)\"", "Algorithm=\"\"")

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains bad request"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "federated user is not able to update another user"() {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def email = "fedIntTest@invalid.rackspace.com"
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(sharedIdentityProvider.issuer, username, 5000, domainId, null, email, samlProducerForSharedIdp);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse

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

        when: "try to update a different user"
        def response = cloud20.updateUser(samlToken, userAdmin2.id, userAdmin2)

        then: "bad request"
        response.status == 403

        cleanup:
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
    }

    def "domain cannot be deleted if a federated user exists in the domain"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def email = "fedIntTest@invalid.rackspace.com"
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(sharedIdentityProvider.issuer, username, 5000, domainId, null, email, samlProducerForSharedIdp);
        def fedAddResponse =  cloud20.samlAuthenticate(samlAssertion)
        def fedAddResponseEntity = fedAddResponse.getEntity(String)
        if (fedAddResponse.status != HttpStatus.SC_OK) {
            LOG.error(String.format("Failed to add fed user. Test will fail. Add Request: '%s', Add response: '%s'", samlAssertion, fedAddResponseEntity))
            assert fedAddResponse.status == HttpStatus.SC_OK //force the failure
        }

        when: "delete the user-admin and try to delete the domain"
        utils.deleteUser(userAdmin)
        def response = cloud20.deleteDomain(utils.getServiceAdminToken(), domainId)

        then: "bad request"
        response.status == 400

        when: "logout the federated user (deletes the federated user) and then try again"
        utils.logoutFederatedUser(username, sharedIdentityProvider.issuer, samlProducerForSharedIdp)
        utils.disableDomain(domainId)

        response = cloud20.deleteDomain(utils.getServiceAdminToken(), domainId)

        then: "success"
        response.status == 204

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "forbidden (403) returned when trying to auth federated user in a domain without a user admin"() {
        given:
        def domainId = utils.createDomain()
        utils.createDomainEntity(domainId)
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(sharedIdentityProvider.issuer, testUtils.getRandomUUID("fedUser"), 5000, domainId, null, "feduser@invalid.rackspace.com", samlProducerForSharedIdp);

        when:
        def response =  cloud20.samlAuthenticate(samlAssertion)

        then:
        response.status == 403
        response.getEntity(IdentityFault).value.message == ProvisionedUserSourceFederationHandler.NO_USER_ADMIN_FOR_DOMAIN_ERROR_MESSAGE
    }

    def "fed user can get admin for own domain"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, userAdminEntity)

        when: "retrieve admin for fed user"
        def response = cloud20.getAdminsForUser(authResponse.token.id, authResponse.user.id)

        then: "get admin"
        response.status == HttpStatus.SC_OK
        def admins = response.getEntity(UserList).value
        admins.getUser().size == 1
        admins.getUser().getAt(0).id == userAdminEntity.id

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Unroll
    def "Return 403 when target user of Identity MFA Service is a federated user: #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
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
        utils.deleteUsers(users1)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Allow deleting federated users by id: #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def samlAuthTokenId = samlAuthToken.id
        def userId = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user.id : samlAuthResponse.user.id
        def user = cloud20.getUserById(utils.getServiceAdminToken(), userId)

        when: "validate the token"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthTokenId)

        then: "the token is still valid"
        validateSamlTokenResponse.status == SC_OK

        when:
        resetCloudFeedsMock()
        def response = cloud20.deleteUser(utils.getServiceAdminToken(), userId)

        then:
        response.status == SC_NO_CONTENT

        and: "verify that event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        when: "validate the token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthTokenId)

        then: "the token is no longer valid"
        response.status == SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users1)

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

        when: "default user cannot delete federatedUser"
        def federatedUserId = getFederatedUser(domainId, mediaType)
        def defaultUserToken = utils.getToken(defaultUser.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.deleteUser(defaultUserToken, federatedUserId)

        then:
        response.status == SC_FORBIDDEN

        when: "user manage can delete federatedUser"
        def userManageToken = utils.getToken(userManage.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userManageToken, federatedUserId)

        then:
        response.status == SC_NO_CONTENT

        when: "user admin can delete federatedUser"
        federatedUserId = getFederatedUser(domainId, mediaType)
        def userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userAdminToken, federatedUserId)

        then:
        response.status == SC_NO_CONTENT

        when: "identity admin can delete federatedUser"
        federatedUserId = getFederatedUser(domainId, mediaType)
        def identityAdminToken = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(identityAdminToken, federatedUserId)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
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
        def federatedUserId = getFederatedUser(domainId2, mediaType)

        when: "default user cannot delete federatedUser in different domain"
        def defaultUserToken = utils.getToken(defaultUser.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(defaultUserToken, federatedUserId)

        then:
        response.status == SC_FORBIDDEN

        when: "user manage cannot delete federatedUser in different domain"
        def userManageToken = utils.getToken(userManage.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userManageToken, federatedUserId)

        then:
        response.status == SC_FORBIDDEN

        when: "user admin cannot delete federatedUser in different domain"
        def userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(userAdminToken, federatedUserId)

        then:
        response.status == SC_FORBIDDEN

        when: "identity admin can delete federatedUser in different domain"
        def identityAdminToken = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        response = cloud20.deleteUser(identityAdminToken, federatedUserId)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, user)
        utils.deleteDomain(domainId)

        where:
        mediaType             | _
        APPLICATION_XML_TYPE  | _
        APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "service/identity admins can update federated user's contactId - media = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def federatedUserId = getFederatedUser(domainId, mediaType)
        def federatedUser = utils.getUserById(federatedUserId)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }

        when: "get federated user by ID"
        def response = cloud20.getUserById(utils.getIdentityAdminToken(), federatedUserId)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == null

        when: "update federated user using service admin"
        response = cloud20.updateUser(utils.getServiceAdminToken(), federatedUserId, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId

        when: "update federated user using identity admin"
        contactId = testUtils.getRandomUUID("contactId")
        userForCreate.contactId = contactId
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

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
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def federatedUserId = getFederatedUser(domainId, mediaType)
        def federatedUser = utils.getUserById(federatedUserId)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }

        when: "get federated user by ID"
        def response = cloud20.getUserById(utils.getIdentityAdminToken(), federatedUserId)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == null

        when: "update federated user"
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId

        when: "attempt to unset contactId with null"
        userForCreate.contactId = null
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId

        when: "attempt to unset contactId with empty string"
        userForCreate.contactId = ""
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)
        entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Updating federated user ignores all attributes other than contactId - username = #username, domainId = #domainId, enabled = #enabled, federatedIdp = #federatedIdp, defaultRegion = #defaultRegion"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def federatedUserId = getFederatedUser(domainId, APPLICATION_XML_TYPE)
        def federatedUser = utils.getUserById(federatedUserId)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }
        cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate)
        def retrievedUser = cloud20.getUserById(utils.getIdentityAdminToken(), federatedUserId).getEntity(org.openstack.docs.identity.api.v2.User).value

        when: "update federated user"
        userForCreate = new UserForCreate().with {
            it.username = username
            it.domainId = domainId
            it.enabled = enabled
            it.federatedIdp = federatedIdp
            it.defaultRegion = defaultRegion
            it
        }
        def response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate)
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
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

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

        def federatedUserId = getFederatedUser(domainId, mediaType)
        def federatedUser = utils.getUserById(federatedUserId)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }

        when: "attempt to update federated user with user-admin"
        def response = cloud20.updateUser(utils.getToken(userAdmin.username), federatedUserId, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "attempt to update federated user with user-manage"
        response = cloud20.updateUser(utils.getToken(userManage.username), federatedUserId, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "attempt to update federated user with default user"
        response = cloud20.updateUser(utils.getToken(defaultUser.username), federatedUserId, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "update user with invalid id"
        response = cloud20.updateUser(utils.getToken(defaultUser.username), "invalid", userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultCloud20Service.USER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "update federated user with invalid id set on the entity"
        userForCreate.id = "invalid"
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, DefaultCloud20Service.ID_MISMATCH)

        when: "update federated user's contactId exceeding max length"
        userForCreate.id = null
        userForCreate.contactId = testUtils.getRandomUUIDOfLength("contactId", 100)
        response = cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
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
        def userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)

        def federatedUserId = getFederatedUser(domainId, mediaType)
        def federatedUser = utils.getUserById(federatedUserId)
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }
        cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)

        when: "get user by id"
        userForCreate.contactId = null
        def response = cloud20.getUserById(utils.getIdentityAdminToken(), federatedUserId, mediaType)
        def entityUser = testUtils.getEntity(response, org.openstack.docs.identity.api.v2.User)

        then:
        response.status == SC_OK
        entityUser.id == federatedUserId
        entityUser.contactId == contactId
        entityUser.federatedIdp == Constants.DEFAULT_IDP_URI
        entityUser.domainId == domainId

        when: "list users by domain"
        response = cloud20.getUsersByDomainId(utils.getIdentityAdminToken(), domainId, mediaType)
        def entityUserList = testUtils.getEntity(response, UserList)

        then:
        response.status == SC_OK
        entityUserList.user.find({it.id == federatedUserId}).id == federatedUserId
        entityUserList.user.find({it.id == federatedUserId}).contactId == contactId
        entityUserList.user.find({it.id == federatedUserId}).federatedIdp == Constants.DEFAULT_IDP_URI
        entityUserList.user.find({it.id == federatedUserId}).domainId == domainId
        entityUserList.user.find({ it.id == federatedUserId }).multiFactorEnabled == false

        when:
        response = cloud20.listUsers(userAdminToken, "0", "1000", mediaType)
        entityUserList = testUtils.getEntity(response, UserList)

        then:
        response.status == SC_OK
        entityUserList.user.find({ it.id == federatedUserId }).id == federatedUserId
        entityUserList.user.find({ it.id == federatedUserId }).contactId == contactId
        entityUserList.user.find({ it.id == federatedUserId }).federatedIdp == Constants.DEFAULT_IDP_URI
        entityUserList.user.find({ it.id == federatedUserId }).domainId == domainId
        entityUserList.user.find({ it.id == federatedUserId }).multiFactorEnabled == false

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Validate contactId on authentication response for auth/validate - media = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        // Create federated user
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUserId = samlAuthResponse.value.user.id
        def federatedUser = utils.getUserById(federatedUserId)

        // Update fed user's contactId
        def contactId = testUtils.getRandomUUID("contactId")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = federatedUserId
            it.contactId = contactId
            it
        }
        cloud20.updateUser(utils.getIdentityAdminToken(), federatedUserId, userForCreate, mediaType)

        when: "saml auth does not expose contactId"
        samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
        samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUserEntity = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user : samlAuthResponse.user

        then:
        samlResponse.status == SC_OK

        federatedUserEntity != null
        federatedUserEntity.contactId == null

        when: "validate token returns contactId"
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token.id : samlAuthResponse.token.id
        def response = cloud20.validateToken(utils.getIdentityAdminToken(), samlAuthToken, mediaType)
        def responseEntity = testUtils.getEntity(response, AuthenticateResponse)

        then:
        response.status == SC_OK

        federatedUserEntity != null
        responseEntity.user.contactId == contactId

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        mediaType  << [APPLICATION_XML_TYPE, APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Injecting comments post signature allows attackers to alter values only if ignore comments is disabled: ignoreComments: #ignoreComments"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_IGNORE_COMMENTS_FOR_SAML_PARSER_PROP, ignoreComments)

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        def usernamePart1 = RandomStringUtils.randomAlphabetic(8)
        def usernamePart2 = RandomStringUtils.randomAlphabetic(8)
        def username = "$usernamePart1$usernamePart2"
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def original = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, Arrays.asList("nova:observer"), email);
        def exploited = original.replaceAll("nova:observer", "nova:<!---->observer")
                .replaceAll(username, "$usernamePart1<!---->$usernamePart2")

        when: "Auth with unaltered response"
        def authClientResponse = cloud20.federatedAuthenticate(original, false, GlobalConstants.FEDERATION_API_V1_0)

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        authResponse.user.name == username
        authResponse.user.roles.role.find {it.name == "nova:observer"} != null

        when: "Auth with altered response"
        def authClientResponseAltered = cloud20.federatedAuthenticate(exploited, false, GlobalConstants.FEDERATION_API_V1_0)

        then: "Processed successfully w/ altered username and roles if disabled"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponseAltered = authClientResponseAltered.getEntity(AuthenticateResponse).value
        if (ignoreComments) {
            assert authResponseAltered.user.name == username
            assert authResponseAltered.user.roles.role.find {it.name == "nova:observer"} != null
        } else {
            assert authResponseAltered.user.name == usernamePart2
            assert authResponseAltered.user.roles.role.find {it.name == "observer"} != null
        }

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        ignoreComments << [false, true]
    }

    def "Federated Authentication for Managed Public Cloud - whitelist role based on tenant type"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("${TENANT_TYPE}:")
        def tenant = utils.createTenantWithTypes(tenantName, [TENANT_TYPE])
        utils.addTenantToDomain(domainId, tenant.id)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:default")
        def samlResponse = cloud20.federatedAuthenticate(samlAssertion, true, null)

        then: "roles are returned for tenant"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def roles = authResponse.user.roles.role
        roles.find {it.tenantId == tenant.id} != null

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type without role"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:service-admin")
        samlResponse = cloud20.federatedAuthenticate(samlAssertion, true, null)

        then: "role with tenant type matching white list is not returned"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse2 = samlResponse.getEntity(AuthenticateResponse).value
        def roles2 = authResponse2.user.roles.role
        roles2.find {it.tenantId == tenant.id} == null

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Federated Authentication for Managed Public Cloud - whitelist role endpoints on tenant type"() {
        given:
        reloadableConfiguration.reset()
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def tenantName = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenantWithTypes(tenantName, [CLOUD])
        utils.addTenantToDomain(domainId, tenant.id)

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY, "identity:default")
        def samlResponse = cloud20.federatedAuthenticate(samlAssertion, true, null)

        then: "endpoints are returned for tenant"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def endpoints = authResponse.serviceCatalog.service
        endpoints.findAll { it.type == "compute" }.size() > 0

        when: "auth with apply_rcn_roles and feature flag enabled and white list tenant type without role"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP , true)
        reloadableConfiguration.setProperty(WHITE_LIST_FILTER_PROPERTY_CLOUD, "identity:service-admin")
        samlResponse = cloud20.federatedAuthenticate(samlAssertion, true, null)

        then: "endpoint with tenant type matching white list is not returned"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse2 = samlResponse.getEntity(AuthenticateResponse).value
        def endpoints2 = authResponse2.serviceCatalog.service
        endpoints2.findAll { it.type == "compute" }.size() == 0

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "Authenticating or updating existing federated user does not send CREATE feed event"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        // Create federated user
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        cloud20.samlAuthenticate(samlAssertion)
        resetCloudFeedsMock()

        when: "saml auth of existing federated user"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def federatedUser = utils.getUserById(samlAuthResponse.value.user.id)

        then: "assert 200"
        samlResponse.status == SC_OK

        and: "verify that user CREATE event is not posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(federatedUser, AtomHopperConstants.CREATE),
                VerificationTimes.exactly(0)
        )

        when: "updating federated user email"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, "test@mail.com");
        samlResponse = cloud20.samlAuthenticate(samlAssertion)
        federatedUser = utils.getUserById(samlAuthResponse.value.user.id)

        then:
        samlResponse.status == SC_OK
        federatedUser.email == "test@mail.com"

        and: "verify that user CREATE event is not posted"
        cloudFeedsMock.verify(
                testUtils.createFedUserFeedsRequest(federatedUser, AtomHopperConstants.CREATE),
                VerificationTimes.exactly(0)
        )

        cleanup:
        utils.logoutFederatedUser(federatedUser.username)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def getFederatedUser(String domainId, mediaType) {
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion, mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        def samlAuthToken = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.token : samlAuthResponse.token
        def userId = mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value.user.id : samlAuthResponse.user.id
        return userId
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, Constants.DEFAULT_IDP_ID)
            if (federatedUser != null) {
                federatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

}
