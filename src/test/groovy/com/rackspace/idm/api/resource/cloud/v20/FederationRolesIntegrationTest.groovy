package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedTokenDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.service.UserService
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
    FederatedTokenDao federatedTokenDao

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    DefaultUserService userService

    @Autowired
    DomainDao domainDao

    def "passing roles in saml assertion adds roles to user as global roles"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def role = utils.createRole()
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, [IDENTITY_DEFAULT_ROLE_NAME, role.name].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        RoleList roles = samlResponse.getEntity(AuthenticateResponse).value.user.roles
        Role addedIdentityRole = roles.role.find {it.name == IDENTITY_DEFAULT_ROLE_NAME}
        Role addedCustomRole = roles.role.find {it.name == role.name}

        then:
        samlResponse.status == 200
        addedIdentityRole != null
        addedIdentityRole.tenantId == null
        addedCustomRole != null
        addedCustomRole.tenantId == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(role)
        deleteFederatedUser(username)
    }

    def "add propagating role to user-admin adds the role to the federated sub-user's existing tokens"() {
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

        when: "adding global propagating role to user-admin"
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "federated sub-user's tokens get global propagating role"
        assertAllFederatedTokensHaveGlobalRole(federatedTokens, propagatingRole)

        when: "adding tenant based propagating role to user-admin"
        utils.addRoleToUserOnTenantId(userAdmin, domainId, propagatingRole.id)
        federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "federated sub-user's tokens get propagating role on tenant"
        assertAllFederatedTokensHaveRoleForTenant(federatedTokens, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "add non-propagating role to user-admin does not add the role to the federated sub-user's tokens"() {
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

        when: "adding non-propagating role to user-admin with federated sub-users"
        utils.addRoleToUser(userAdmin, propagatingRole.id)
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "federated sub-user's tokens do not have non-propagating role"
        assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

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
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default", propagatingRole.name].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, propagatingRole.id)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        when: "adding role to user-admin with federated sub-users"
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "federated sub-user's tokens have propagating role assigned only once"
        def roleCount = 0
        for(token in federatedTokens.toList()) {
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                if(curTenantRole.roleRsId.equals(propagatingRole.id)) {
                    roleCount++
                }
            }
        }
        roleCount == 1

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated tokens get global propagating roles"() {
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

        when: "loading the federated user's tokens from the directory"
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the propagating role is also added to the token in the directory"
        assertAllFederatedTokensHaveGlobalRole(federatedTokens, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated tokens get tenant based propagating roles"() {
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

        when: "loading the federated user's tokens from the directory"
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the tenant propagating role is also added to the token in the directory"
        assertAllFederatedTokensHaveRoleForTenant(federatedTokens, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "new federated tokens do not get non-propagating roles"() {
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

        when: "loading the federated user's tokens from the directory"
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the non-propagating role is not added to the token in the directory"
        assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, propagatingRole)

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
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the propagating role is removed from the federated user's token"
        assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, propagatingRole)

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
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the propagating tenant role is completely removed from the federated user's token"
        assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, propagatingRole)

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
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)
        assertAllFederatedTokensHaveRoleForTenant(federatedTokens, propagatingRole, domainId)
        assertAllFederatedTokensHaveRoleForTenant(federatedTokens, propagatingRole, nastTenantId)

        when: "the propagating role is removed from the user-admin on nast tenant"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), nastTenantId, userAdmin.id, propagatingRole.id)
        federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the nastTenantId is removed from propagating role on all tokens"
        assertNoFederatedTokenHasRoleForTenant(federatedTokens, propagatingRole, nastTenantId)
        assertAllFederatedTokensHaveRoleForTenant(federatedTokens, propagatingRole, domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

    def "remove propagating role from user-admin removes the role from all federated sub-user tokens"() {
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

        when: "creating another token for the federated user under a user-admin with a propagating role"
        samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the federated user has the propagating role"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating role is removed from the user-admin"
        cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), propagatingRole.id, userAdmin.id)
        def federatedTokens = federatedTokenDao.getFederatedTokensByUserId(samlResponse.user.id)

        then: "the propagating role is removed from all of the federated user's tokens"
        assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, propagatingRole)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteRole(propagatingRole)
        deleteFederatedUser(username)
    }

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

    def "identity access roles in saml assertions are limited to the default and user manage roles"() {
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
        samlResponse.status == 200

        cleanup:
        utils.deleteUsers(users)
        deleteFederatedUser(username)
    }

    def void assertAllFederatedTokensHaveGlobalRole(federatedTokens, role) {
        def allTokensHaveRole = true
        for(token in federatedTokens) {
            def tokenHasProperRole = false
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                if (!tokenHasProperRole && (curTenantRole.roleRsId.equals(role.id))) {
                    //should NOT have any role
                    tokenHasProperRole = CollectionUtils.isEmpty(curTenantRole.tenantIds)
                }
            }
            allTokensHaveRole = allTokensHaveRole && tokenHasProperRole
        }
        assert allTokensHaveRole
    }

    def void assertNoFederatedTokenHasGlobalRole(federatedTokens, role) {
        for(token in federatedTokens) {
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                //may have tenant role, which is ok. just not global
                assert !(curTenantRole.roleRsId.equals(role.id) && CollectionUtils.isEmpty(curTenantRole.tenantIds))
            }
        }
    }

    def void assertNoFederatedTokenHasRoleAsGlobalOrTenant(federatedTokens, role) {
        def aTokenHasRole = false
        for(token in federatedTokens) {
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                //may have tenant role, which is ok. just not global
                assert !curTenantRole.roleRsId.equals(role.id)
            }
        }
    }

    def void assertAllFederatedTokensHaveRoleForTenant(federatedTokens, role, tenantId) {
        def allTokensHaveRole = true
        for(token in federatedTokens) {
            def tokenHasProperRole = false
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                if (!tokenHasProperRole && (curTenantRole.roleRsId.equals(role.id))) {
                    tokenHasProperRole = curTenantRole.tenantIds.contains(tenantId)
                }
            }
            allTokensHaveRole = allTokensHaveRole && tokenHasProperRole
        }
        assert allTokensHaveRole
    }

    def void assertNoFederatedTokenHasRoleForTenant(federatedTokens, role, tenantId) {
        for(token in federatedTokens) {
            def tenantRoles = tenantRoleDao.getTenantRolesForScopeAccess(token)
            for(curTenantRole in tenantRoles) {
                if (curTenantRole.roleRsId.equals(role.id)) {
                    //a global role is fine, just not a tenant based role
                    assert !curTenantRole.tenantIds.contains(tenantId)
                }
            }
        }
    }

    def deleteFederatedUser(username) {
        def federatedUser = ldapFederatedUserRepository.getUserByUsername(username, DEFAULT_IDP_NAME)
        ldapFederatedUserRepository.deleteObject(federatedUser)
    }

}
