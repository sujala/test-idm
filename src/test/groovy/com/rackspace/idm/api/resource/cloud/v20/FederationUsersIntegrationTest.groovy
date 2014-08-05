package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedTokenDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.util.SamlResponseValidator
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.DEFAULT_IDP_NAME
import static com.rackspace.idm.Constants.DEFAULT_IDP_URI

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationUsersIntegrationTest extends RootIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederationUsersIntegrationTest.class)

    @Autowired
    FederatedTokenDao federatedTokenDao

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    TenantService tenantService

    @Autowired
    RoleService roleService

    @Autowired
    UserService userService

    @Autowired
    DomainDao domainDao

    private static final String RBACROLE1_NAME = "rbacRole1"
    private static final String RBACROLE2_NAME = "rbacRole2"
    private static final String ROLE_1000_NAME = "Role 1000"

    ClientRole rbacRole1;
    ClientRole rbacRole2;
    ClientRole role1000;

    def setup() {
        //expected to be pre-existing 1000 weight roles in default ldif
        rbacRole1 = roleService.getRoleByName(RBACROLE1_NAME)
        rbacRole2 = roleService.getRoleByName(RBACROLE2_NAME)
        role1000 = roleService.getRoleByName(ROLE_1000_NAME)

        assert rbacRole1.rsWeight == 1000
        assert rbacRole2.rsWeight == 1000
        assert role1000.rsWeight == 1000
    }

    def "initial user populated appropriately from saml no roles provided"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        FederatedUser fedUser = ldapFederatedUserRepository.getUserById(authResponse.user.id)

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

    def "initial user populated appropriately from saml - user admin group added to federated user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        FederatedUser fedUser = ldapFederatedUserRepository.getUserById(authResponse.user.id)

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

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name), email);
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
        FederatedUser fedUser = ldapFederatedUserRepository.getUserById(authResponse.user.id)

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

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(role1000.name), email);
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

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, roleNames, email);
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

        def samlAssertionNone = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def samlAssertionRbac1 = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name), email);
        def samlAssertionRbac1And2 = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole1.name, rbacRole2.name), email);
        def samlAssertionRbac2 = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, Arrays.asList(rbacRole2.name), email);

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
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def userAdmin1, userAdmin2, users1, users2
        def disabledDomainErrorMessage = String.format(SamlResponseValidator.DISABLED_DOMAIN_ERROR_MESSAGE, domainId)
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
        samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, testUtils.getRandomUUID(), expDays, domainId, null);
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "the request returns an error that matches that of a disabled domain"
        samlResponse.status == 400
        samlResponse.getEntity(BadRequestFault).value.message == disabledDomainErrorMessage

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
        staticIdmConfiguration.reset()
    }

    def "federated and provisioned user tokens are revoked when the last user admin for the domain is disabled"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null);
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
        FederatedUser fedUser = ldapFederatedUserRepository.getUserById(authResponse.user.id)
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
    }

    def "passing multiple saml requests with same info references same user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null);
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
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def samlAssertion2 = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId2, null);
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
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

}
