package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.security.ConfigurableTokenFormatSelector
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.ProvisionedUserSourceFederationHandler
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.opensaml.saml2.core.Response
import org.opensaml.xml.signature.Signature
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.junit.IgnoreByRepositoryProfile
import testHelpers.saml.SamlAssertionFactory

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.*
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE

class FederatedUserIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedUserIntegrationTest.class)

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

    def setup() {
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
        utils.deleteEndpointTemplate(usGlobalEndpointEnabled)
        utils.deleteEndpointTemplate(usGlobalEndpointDisabled)
        utils.deleteEndpointTemplate(lonGlobalEndpointEnabled)
        utils.deleteEndpointTemplate(lonGlobalEndpointDisabled)
    }

    def "initial user populated appropriately from saml no roles provided"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
    def "Token format based on property config"() {
        given:
        //ensure system will recognize AE tokens as AE tokens
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_DECRYPT, true)

        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when: "auth - default token format set to UUID"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.UUID.name())
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value

        then: "Token is a UUID token"
        configurableTokenFormatSelector.formatForExistingToken(authResponse.token.id) == TokenFormat.UUID

        when: "auth - default token format set to AE"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.AE.name())
        samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpServletResponse.SC_OK
        authResponse = samlResponse.getEntity(AuthenticateResponse).value

        then: "Token is a AE token"
        configurableTokenFormatSelector.formatForExistingToken(authResponse.token.id) == TokenFormat.AE

        when: "IDP override property sets idp token format to UUID when default is set to AE"
        reloadableConfiguration.setProperty(String.format(IdentityConfig.IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_OVERRIDE_PROP_REG, DEFAULT_IDP_URI), TokenFormat.UUID.name())
        samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpServletResponse.SC_OK
        authResponse = samlResponse.getEntity(AuthenticateResponse).value

        then: "Token is a UUID token"
        configurableTokenFormatSelector.formatForExistingToken(authResponse.token.id) == TokenFormat.UUID

        cleanup:
        staticIdmConfiguration.reset() //reset to default config since we messed with configuration in this test
        reloadableConfiguration.reset()
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }


    def "initial user populated appropriately from saml - user admin group added to federated user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name), email);
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

    def "Can specify a role with a space in the name"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(role1000.name), email);
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
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        List<String> roleNames = Arrays.asList(delimitedRoleNames.split(","))

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, roleNames, email);
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
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        def samlAssertionNone = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def samlAssertionRbac1 = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name), email);
        def samlAssertionRbac1And2 = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name, rbacRole2.name), email);
        def samlAssertionRbac2 = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole2.name), email);

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

    def "federated user is disabled when last user admin on domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def userAdmin1, userAdmin2, users1, users2
        def disabledDomainErrorMessage = String.format(ProvisionedUserSourceFederationHandler.DISABLED_DOMAIN_ERROR_MESSAGE, domainId)
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId)

        when: "try to pass a saml assertion for a domain with 2 user admins"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request succeeds"
        samlResponse.status == 200

        when: "disable one of the user admins and try to pass a saml assertion again for the same user"
        utils.disableUser(userAdmin1)
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request succeeds"
        samlResponse.status == 200

        when: "disable the other user admin and try to pass a saml assertion again for the same user"
        utils.disableUser(userAdmin2)
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == 400
        samlResponse.getEntity(BadRequestFault).value.message == disabledDomainErrorMessage

        when: "try to pass a saml assertion for a new user in the same domain"
        samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, testUtils.getRandomUUID(), expDays, domainId, null);
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == 400
        samlResponse.getEntity(BadRequestFault).value.message == disabledDomainErrorMessage

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "federated token contains tenant: #mediaType"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null);
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

    def "federated and provisioned user tokens are revoked when the last user admin for the domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def userAdmin1, userAdmin2, users1, users2
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlAuthToken = samlResponse.getEntity(AuthenticateResponse).value.token.id
        def provisionedUser = utils.createUserWithUser(userAdmin1)
        def provisionedUserToken = utils.authenticate(provisionedUser).token.id

        when: "validate the tokens with 2 enabled user admins"
        def validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken)
        def validateProvisionedTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserToken)

        then: "the tokens are still valid"
        validateSamlTokenResponse.status == 200
        validateProvisionedTokenResponse.status == 200

        when: "disable one user admin"
        utils.disableUser(userAdmin1)
        validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken)
        validateProvisionedTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserToken)

        then: "the token is still valid"
        validateSamlTokenResponse.status == 200
        validateProvisionedTokenResponse.status == 200

        when: "disable the other user admin"
        utils.disableUser(userAdmin2)
        validateSamlTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), samlAuthToken)
        validateProvisionedTokenResponse = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserToken)

        then: "the token is no longer valid"
        validateSamlTokenResponse.status == 404
        validateProvisionedTokenResponse.status == 404

        cleanup:
        utils.deleteUser(provisionedUser)
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
        staticIdmConfiguration.reset()
    }

    def "federated users are limited within each IDP"() {
        given:
        //set the user limit low for lower overhead
        staticIdmConfiguration.setProperty("maxNumberOfFederatedUsersInDomainPerIdp", 2)
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("samlUser")
        def username2 = testUtils.getRandomUUID("samlUser")
        def username3 = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        //fill the domain with the max allowed number of users
        assert cloud20.samlAuthenticate(new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username1, expDays, domainId, null)).status == 200
        assert cloud20.samlAuthenticate(new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username2, expDays, domainId, null)).status == 200

        when: "try to exceed the limit under the current IDP"
        def samlResponse = cloud20.samlAuthenticate(new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username3, expDays, domainId, null));

        then: "the response is a failure"
        samlResponse.status == 400

        when: "try to create the same user under a different IDP (the limit is per IDP per domain)"
        samlResponse = cloud20.samlAuthenticate(new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(IDP_2_URI, username3, expDays, domainId, null, DEFAULT_FED_EMAIL, IDP_2_PRIVATE_KEY, IDP_2_PUBLIC_KEY));

        then: "the request succeeds"
        samlResponse.status == 200

        cleanup:
        deleteFederatedUserQuietly(username1)
        deleteFederatedUserQuietly(username2)
        deleteFederatedUserQuietly(username3)
        utils.deleteUsers(users)
        staticIdmConfiguration.reset()
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
        assert authResponse.user.federatedIdp == DEFAULT_IDP_URI
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
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null);
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
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def samlAssertion2 = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId2, null);
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
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"
        def adminToken = utils.getIdentityAdminToken()

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        def expDays = 5
        def samlFactor = new SamlAssertionFactory()
        def domainId = utils.createDomain()
        def email = "fedIntTest@invalid.rackspace.com"

        Response samlAssertion = samlFactor.generateSamlAssertionResponseForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        Response samlAssertion2 = samlFactor.generateSamlAssertionResponseForFederatedUser(DEFAULT_IDP_URI, username, 6, domainId, null, email);

        //replace first assertion with second to make an invalid assertion
        Signature sig = samlAssertion2.getSignature()
        sig.detach()
        samlAssertion.setSignature(sig)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlFactor.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpServletResponse.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SIGNATURE)
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                if (RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                    federatedUser = sqlFederatedUserRepository.findOneByUsernameAndFederatedIdpName(username, DEFAULT_IDP_NAME)
                    sqlFederatedUserRepository.delete(federatedUser)
                } else {
                    federatedUserRepository.deleteObject(federatedUser)
                }
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

}
