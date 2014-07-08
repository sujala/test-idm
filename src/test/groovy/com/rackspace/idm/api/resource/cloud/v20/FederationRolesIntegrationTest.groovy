package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.impl.DefaultUserService
import org.apache.commons.collections.CollectionUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import static com.rackspace.idm.Constants.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationRolesIntegrationTest extends RootIntegrationTest {
    def IDENTITY_DEFAULT_ROLE_NAME = "identity:default"

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    DefaultUserService userService

    @Autowired
    DomainDao domainDao

    def "identity:default role is added to user as global role by default"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, [].asList());
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
            it.propagate = true
            return it
        }

        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding global propagating role to user-admin"
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        then: "federated sub-user gets global propagating role"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        when: "adding tenant based propagating role to user-admin"
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)

        then: "federated sub-user get propagating role on tenant"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "add non-propagating role to user-admin does not add the role to the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = false
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding non-propagating role to user-admin with federated sub-users"
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        then: "federated sub-user does not have non-propagating role"
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    @Ignore("This shold be ignored until story allowing roles to be included in assertions is developed - B-74253")
    def "federated user cannot get a duplicate role by specifying propagating role in saml assertion that is on user-admin"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, [propagatingRole.name].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding role to user-admin with federated sub-users"
        def federatedUserRoles = tenantRoleDao.getTenantRolesForUser(fedUser)

        then: "federated sub-user have propagating role assigned only once"
        def roleCount = 0
        for(curTenantRole in federatedUserRoles) {
            if(curTenantRole.roleRsId.equals(propagatingRole.id)) {
                roleCount++
            }
        }
        roleCount == 1

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated users get global propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating a saml user under a user-admin with a propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user's roles from the directory"
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated users get tenant based propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)

        when: "creating a saml user under a user-admin with a tenant propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the tenant propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated users do not get non-propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = false
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating a saml user under a user-admin with a non-propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the non-propagating role is not shown in the response"
        !samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the non-propagating role is not on the user in the directory"
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "remove propagating role from user-admin removes the role from the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating the federated user under a user-admin with a propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the federated user has the propagating role"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating role is removed from the user-admin"
        cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), propagatingRole.id, userAdmin.id)
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)

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
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        assert samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating tenant role is removed from the user-admin"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), domainId, userAdmin.id, propagatingRole.id)
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)

        then: "the propagating role is also removed from the user in the directory"
        fedUser != null
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    @Ignore("Ignored because this demonstrates a bug D-18423. Once that bug is fixed, the ignore annotation should be removed to verify federated users will work appropriately in this situation")
    def "when remove one of two tenants on propagating role assigned to user-admin removes only one tenant from the role on the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.propagate = true
            return it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def nastTenantId = userService.getNastTenantId(domainId)
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        utils.addRoleToUserOnTenantId(userAdmin, nastTenantId, propagatingRole.id)

        //create the user
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def fedUser = ldapFederatedUserRepository.getUserById(samlResponse.user.id)

        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        when: "the propagating role is removed from the user-admin on nast tenant"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), nastTenantId, userAdmin.id, propagatingRole.id)

        then: "the nastTenantId is removed from propagating role on federated user"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, domainId)
        assertFederatedUserDoesNotHaveRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    @Ignore("We can't add a second user admin through the API because on the second add the mosso and nast tenants already exist which errors")
    def "trying to pass a saml assertion for a domain with more than one user admin returns 500 if 'domain.restricted.to.one.user.admin.enabled' == true"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def userAdmin1, userAdmin2, users1, users2
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId)

        when:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", true)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 500

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
        staticIdmConfiguration.reset()
    }

    def "trying to pass a saml assertion for a domain with no user admin returns 500"() {
        given:
        def domainId = utils.createDomain()
        Domain domain = new Domain();
        domain.setDomainId(domainId);
        domain.setEnabled(true);
        domain.setName(domainId);
        domain.setDescription("Domain for saml " + domainId);
        domainDao.addDomain(domain)
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 500

        cleanup:
        staticIdmConfiguration.reset()
        utils.deleteDomain(domainId)
        utils.deleteTenant(tenant.id)
    }

    @Ignore("This should be ignored until role logic is added to samlResponse - story B74253")
    def "identity access roles can not be provided in saml assertions"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "service admin role"
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:service-admin"].asList());
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 403

        when: "identity admin role"
        samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:admin"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 403

        when: "user admin role"
        samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:user-admin"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 403

        when: "user manage role"
        samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:user-manage"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 403

        when: "default user role"
        samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == 403

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
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
        def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
        ldapFederatedUserRepository.deleteObject(federatedUser)
    }

}
