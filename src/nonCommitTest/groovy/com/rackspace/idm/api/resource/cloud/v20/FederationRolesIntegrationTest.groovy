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
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.impl.DefaultUserService

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import static com.rackspace.idm.Constants.DEFAULT_IDP_URI
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_PRIVATE_KEY
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_PUBLIC_KEY
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_URI
import static com.rackspace.idm.Constants.getDEFAULT_FED_EMAIL
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_PRIVATE_KEY
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_PUBLIC_KEY
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_URI
import static com.rackspace.idm.SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS

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
        def userAdmin = utils.createCloudAccount()
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def request = getFederatedDomainAuthGenerationRequest(userAdmin.domainId)
        def inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))
        def samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)
        assert samlResponse.status == HttpStatus.SC_OK
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse).value
        RoleList roles = samlAuthResponse.user.roles

        when:
        Role addedIdentityRole = roles.role.find {it.name == IDENTITY_DEFAULT_ROLE_NAME}

        then:
        addedIdentityRole != null
        addedIdentityRole.tenantId == null

        cleanup:
        utils.deleteFederatedUserQuietly(samlAuthResponse.user.name)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "add propagating role to user-admin adds the role to the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }

        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding global propagating role to user-admin"
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        then: "federated sub-user gets global propagating role"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        when: "adding tenant based propagating role to user-admin"
        utils.deleteRoleOnUser(userAdmin, propagatingRole.id)
        utils.addRoleToUserOnTenantId(userAdmin, userAdmin.domainId, propagatingRole.id)

        then: "federated sub-user get propagating role on tenant"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, userAdmin.domainId)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRole(propagatingRole)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "add non-propagating role to user-admin does not add the role to the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role"))
        def responseRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def nonPRole = responseRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        when: "adding non-propagating role to user-admin with federated sub-users"
        utils.addRoleToUser(userAdmin, nonPRole.id)

        then: "federated sub-user does not have non-propagating role"
        assertFederatedUserDoesNotHaveRole(fedUser, nonPRole)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteRoleQuietly(nonPRole)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "new federated users get global propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating a saml user under a user-admin with a propagating role"
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)

        then: "the propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user's roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasGlobalRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRole(propagatingRole)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "new federated users get tenant based propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUserOnTenantId(userAdmin, userAdmin.domainId, propagatingRole.id)

        when: "creating a saml user under a user-admin with a tenant propagating role"
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)

        then: "the tenant propagating role is shown in the response"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the propagating role is also added to the user in the directory"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, userAdmin.domainId)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRole(propagatingRole)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "new federated users do not get non-propagating roles"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role"))
        def responseRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def gRole = responseRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUser(userAdmin, gRole.id)

        when: "creating a saml user under a user-admin with a non-propagating role"
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)

        then: "the non-propagating role is not shown in the response"
        !samlResponse.user.roles.role.id.contains(gRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the non-propagating role is not on the user in the directory"
        assertFederatedUserDoesNotHaveRole(fedUser, gRole)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteRoleQuietly(gRole)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "remove propagating role from user-admin removes the role from the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUser(userAdmin, propagatingRole.id)

        when: "creating the federated user under a user-admin with a propagating role"
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)

        then: "the federated user has the propagating role"
        samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating role is removed from the user-admin"
        cloud20.deleteApplicationRoleFromUser(utils.getServiceAdminToken(), propagatingRole.id, userAdmin.id)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        then: "the propagating role is also removed from the user in the directory"
        fedUser != null
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteRole(propagatingRole)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "remove propagating tenant role assigned to only one tenant on user-admin removes the role from the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUserOnTenantId(userAdmin, userAdmin.domainId, propagatingRole.id)
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        assert samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when: "the propagating tenant role is removed from the user-admin"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), userAdmin.domainId, userAdmin.id, propagatingRole.id)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        then: "the propagating role is also removed from the user in the directory"
        fedUser != null
        assertFederatedUserDoesNotHaveRole(fedUser, propagatingRole)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteRole(propagatingRole)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "when remove one of two tenants on propagating role assigned to user-admin removes only one tenant from the role on the federated sub-user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("role")).with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        def responsePropagateRole = cloud20.createRole(utils.getServiceAdminToken(), role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUserOnTenantId(userAdmin, userAdmin.domainId, propagatingRole.id)
        def nastTenantId = utils.getNastTenant(userAdmin.domainId)
        utils.addRoleToUserOnTenantId(userAdmin, nastTenantId, propagatingRole.id)

        //create the user
        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)

        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, userAdmin.domainId)
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        when: "the propagating role is removed from the user-admin on nast tenant"
        cloud20.deleteRoleFromUserOnTenant(utils.getServiceAdminToken(), nastTenantId, userAdmin.id, propagatingRole.id)

        then: "the nastTenantId is removed from propagating role on federated user"
        assertFederatedUserHasRoleOnTenant(fedUser, propagatingRole, userAdmin.domainId)
        assertFederatedUserDoesNotHaveRoleOnTenant(fedUser, propagatingRole, nastTenantId)

        cleanup:
        utils.deleteFederatedUserQuietly(fedUser.username)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRole(propagatingRole)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    def "trying to pass a saml assertion for a domain with more than one user admin returns 500 if 'domain.restricted.to.one.user.admin.enabled' == true"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        // This scenario will only work when user-admin lookup by domain is disabled. When enabled, the list of enabled
        // user-admins on domain will be 1, resulting in a valid request.
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, false)

        def userAdmin = utils.createCloudAccount()
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def request = getFederatedDomainAuthGenerationRequest(userAdmin.domainId)
        def inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))

        def userAdmin2 = utils.createUser(utils.getToken(userAdmin.username))
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
        tenantService.addTenantRoleToUser(userAdmin2BaseUser, tenantRole, false)
        // Delete default user role
        tenantRole.uniqueId = String.format("roleRsId=%s,cn=ROLES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com", Constants.DEFAULT_USER_ROLE_ID, userAdmin2.id)
        tenantRole.roleRsId = Constants.DEFAULT_USER_ROLE_ID
        tenantRole.name = Constants.DEFAULT_USER_ROLE_NAME
        tenantService.deleteTenantRoleForUser(userAdmin2BaseUser, tenantRole, false)

        when:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", true)
        def samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then:
        samlResponse.status == 500

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUsers(userAdmin2)
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    def "identity access roles can not be provided in saml assertions"() {
        given:
        def userAdmin = utils.createCloudAccount()
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def request = getFederatedDomainAuthGenerationRequest(userAdmin.domainId)

        when: "service admin role"
        request.roleNames = [IdentityUserTypeEnum.SERVICE_ADMIN.roleName]
        def inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))
        def samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then:
        samlResponse.status == 400

        when: "identity admin role"
        request.roleNames = [IdentityUserTypeEnum.IDENTITY_ADMIN.roleName]
        inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))
        samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then:
        samlResponse.status == 400

        when: "user admin role"
        request.roleNames = [IdentityUserTypeEnum.USER_ADMIN.roleName]
        inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))
        samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then:
        samlResponse.status == 400

        when: "default user role"
        request.roleNames = [IdentityUserTypeEnum.DEFAULT_USER.roleName]
        inputSamlResponseStr = federatedDomainAuthRequestGenerator.convertResponseToString(federatedDomainAuthRequestGenerator.createSignedSAMLResponse(request))
        samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then:
        samlResponse.status == 400

        cleanup:
        utils.deleteUserQuietly(userAdmin)
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

        def userAdmin = utils.createCloudAccount()
        utils.addRoleToUserOnTenantId(userAdmin, userAdmin.domainId, propagatingRole.id)

        def samlResponse = utils.authenticateFederatedUser(userAdmin.domainId)
        assert samlResponse.user.roles.role.id.contains(propagatingRole.id)

        when:
        def response = cloud20.deleteRole(serviceAdminToken, propagatingRole.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        cleanup:
        utils.deleteFederatedUserQuietly(samlResponse.user.name)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRole(propagatingRole)
    }

    FederatedDomainAuthGenerationRequest getFederatedDomainAuthGenerationRequest(String domainId, String username = "fedUser${RandomStringUtils.randomAlphanumeric(8)}") {
        return new FederatedDomainAuthGenerationRequest().with {
            it.domainId = domainId
            it.validitySeconds = 1000
            it.brokerIssuer = DEFAULT_BROKER_IDP_URI
            it.originIssuer = IDP_V2_DOMAIN_URI
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = username
            it.roleNames = []
            it.groupNames = []
            it
        }
    }


    void assertFederatedUserHasGlobalRole(FederatedUser user, role) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert CollectionUtils.isEmpty(roleOnUser.tenantIds)
    }

    void assertFederatedUserHasRoleOnTenant(FederatedUser user, role, tenantId) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert roleOnUser.tenantIds.contains(tenantId)
    }

    void assertFederatedUserDoesNotHaveRoleOnTenant(FederatedUser user, role, tenantId) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser != null
        assert !roleOnUser.tenantIds.contains(tenantId)
    }

    void assertFederatedUserDoesNotHaveRole(FederatedUser user, role) {
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(user, role.id)
        assert roleOnUser == null
    }

}
