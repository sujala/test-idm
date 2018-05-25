package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.impl.DefaultUserService

import org.apache.commons.collections.CollectionUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import static com.rackspace.idm.Constants.DEFAULT_IDP_URI

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationRolesIntegrationTest extends RootIntegrationTest {
    def IDENTITY_DEFAULT_ROLE_NAME = "identity:default"

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    TenantDao tenantDao

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    DefaultUserService userService

    @Autowired
    DomainDao domainDao

    @Autowired
    TenantService tenantService

    @Autowired
    IdentityConfig identityConfig

    @Autowired(required = false)
    LdapFederatedUserRepository ldapFederatedUserRepository

    def "identity:default role is added to user as global role by default"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, [].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        RoleList roles = authResponse.user.roles

        when:
        Role addedIdentityRole = roles.role.find {it.name == IDENTITY_DEFAULT_ROLE_NAME}

        then:
        samlResponse.status == 200
        addedIdentityRole != null
        addedIdentityRole.tenantId == null

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
    }

    def "add propagating role to user-admin adds the role to the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }

        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding global propagating role to user-admin"
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        then: "federated sub-user gets global propagating role"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        when: "adding tenant based propagating role to user-admin"
        utils.deleteRoleOnUser(userAdmin, propagatingRole.id)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)

        then: "federated sub-user get propagating role on tenant"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
        utils.deleteRole(propagatingRole)
    }

    def "add non-propagating role to user-admin does not add the role to the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role"))
        def responseRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def nonPRole = responseRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding non-propagating role to user-admin with federated sub-users"
        utils.addRoleToUser(userAdmin, nonPRole.id)

        then: "federated sub-user does not have non-propagating role"
        assertFederatedUserDoesNotHaveRole(fedUser, nonPRole)

        cleanup:
        utils.deleteUsersQuietly(users)
        deleteFederatedUser(username)
        utils.deleteRoleQuietly(nonPRole)
    }

    def "new federated users get global propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating a saml user under a user-admin with a propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user's roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
        utils.deleteRole(propagatingRole)
    }

    def "new federated users get tenant based propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)

        when: "creating a saml user under a user-admin with a tenant propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the tenant propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
        utils.deleteRole(propagatingRole)
    }

    def "new federated users do not get non-propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role"))
        def responseRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def gRole = responseRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, gRole.id)

        when: "creating a saml user under a user-admin with a non-propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the non-propagating role is not shown in the response"
        !samlResponse.user.roles.role.id.contains(gRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the non-propagating role is not on the user in the directory"
        assertFederatedUserDoesNotHaveRole(fedUser, gRole)

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteRoleQuietly(gRole)
        deleteFederatedUser(username)
    }

    def "remove propagating role from user-admin removes the role from the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating the federated user under a user-admin with a propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the federated user has the propagating role"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating role is removed from the user-admin"
        cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), propagatingRole.id, userAdmin.id)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        then: "the propagating role is also removed from the user in the directory"
        fedUser != null
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "remove propagating tenant role assigned to only one tenant on user-admin removes the role from the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        assert samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating tenant role is removed from the user-admin"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), domainId, userAdmin.id, propagatingRole.id)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        then: "the propagating role is also removed from the user in the directory"
        fedUser != null
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "when remove one of two tenants on propagating role assigned to user-admin removes only one tenant from the role on the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def nastTenantId = userService.getNastTenantId(domainId)
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        utils.addRoleToUserOnTenantId(userAdmin, nastTenantId, propagatingRole.id)

        //create the user
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        when: "the propagating role is removed from the user-admin on nast tenant"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), nastTenantId, userAdmin.id, propagatingRole.id)

        then: "the nastTenantId is removed from propagating role on federated user"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)
        assertFederatedUserDoesNotHaveRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
        utils.deleteRole(propagatingRole)
    }

    def "trying to pass a saml assertion for a domain with more than one user admin returns 500 if 'domain.restricted.to.one.user.admin.enabled' == true"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        // This scenario will only work when user-admin lookup by domain is disabled. When enabled, the list of enabled
        // user-admins on domain will be 1, resulting in a valid request.
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        def userAdmin2 = utils.createUser(utils.getToken(userAdmin1.username))
        // Create second userAdmin in domain by avoiding api restrictions
        BaseUser userAdmin2BaseUser = entityFactory.createUser().with {
            it.uniqueId = String.format("rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", userAdmin2.id)
            it.id = userAdmin2.id
            it
        }
        // Add user admin role
        TenantRole tenantRole = new TenantRole().with {
            it.uniqueId = String.format("roleRsId=%s,cn=ROLES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", Constants.USER_ADMIN_ROLE_ID, userAdmin2.id)
            it.roleRsId = Constants.USER_ADMIN_ROLE_ID
            it.name = Constants.IDENTITY_USER_ADMIN_ROLE
            it.clientId = Constants.IDENTITY_SERVICE_ID
            it
        }
        tenantService.addTenantRoleToUser(userAdmin2BaseUser, tenantRole)
        // Delete default user role
        tenantRole.uniqueId = String.format("roleRsId=%s,cn=ROLES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", Constants.DEFAULT_USER_ROLE_ID, userAdmin2.id)
        tenantRole.roleRsId = Constants.DEFAULT_USER_ROLE_ID
        tenantRole.name = Constants.DEFAULT_USER_ROLE_NAME
        tenantService.deleteTenantRoleForUser(userAdmin2BaseUser, tenantRole)

        when:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", true)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 500

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(userAdmin2)
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    def "identity access roles can not be provided in saml assertions"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "service admin role"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, ["identity:service-admin"].asList());
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 400

        when: "identity admin role"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, ["identity:admin"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 400

        when: "user admin role"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, ["identity:user-admin"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 400

        when: "user manage role"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, ["identity:user-manage"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 400

        when: "default user role"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, ["identity:default"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 400

        cleanup:
        utils.deleteUsers(users)
    }


    def "caller cannot delete a role assigned to dedicated user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def serviceAdminToken = utils.getServiceAdminToken()
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        assert samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when:
        def response = cloud20.deleteRole(serviceAdminToken, propagatingRole.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
        utils.deleteRole(propagatingRole)
    }


    def void assertFederatedUserHasGlobalRole(FederatedUser user, role) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert CollectionUtils.isEmpty(roleOnUser.tenantIds)
    }

    def void assertFederatedUserHasRoleOnTenant(FederatedUser user, role, tenantId) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert roleOnUser.tenantIds.contains(tenantId)
    }

    def void assertFederatedUserDoesNotHaveRoleOnTenant(FederatedUser user, role, tenantId) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert !roleOnUser.tenantIds.contains(tenantId)
    }

    def void assertFederatedUserDoesNotHaveRole(FederatedUser user, role) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser == null
    }


    def deleteFederatedUser(username) {
        def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, Constants.DEFAULT_IDP_ID)
        if(federatedUser != null) ldapFederatedUserRepository.deleteObject(federatedUser)
    }

}
