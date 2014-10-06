package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.exception.ClientConflictException
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapTenantRoleRepositoryIntegrationTest extends RootIntegrationTest {

    @Autowired
    private LdapTenantRoleRepository roleRepository

    @Autowired
    private LdapApplicationRepository applicationRepository

    @Autowired
    private LdapUserRepository userRepository

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired
    private LdapFederatedUserRepository federatedUserRepository

    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared User user;
    @Shared Application application;
    @Shared FederatedUser federatedUser;
    @Shared UserScopeAccess federatedToken;

    private IdentityProvider commonIdentityProvider;

    def setup() {
        sharedRandom = ("$sharedRandomness").replace('-',"")

        userRepository.addUser(getUser("$sharedRandom"))
        user = userRepository.getUserById("$sharedRandom")

        applicationRepository.addApplication(getApplication("app$sharedRandom"))
        application = applicationRepository.getApplicationByClientId("app$sharedRandom")

        commonIdentityProvider = ldapIdentityProviderRepository.getIdentityProviderByName(Constants.DEFAULT_IDP_NAME)
        federatedUserRepository.addUser(commonIdentityProvider, getFederatedUser("$sharedRandom"))
        federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderName("username$sharedRandom", commonIdentityProvider.getName())

        federatedToken = getFederatedToken(federatedUser)
        scopeAccessRepository.addScopeAccess(federatedUser, federatedToken)
    }

    def cleanup() {
        userRepository.deleteUser(user)
        applicationRepository.deleteApplication(application)
        federatedUserRepository.deleteObject(federatedUser)
    }

    def "tenant role crud for user"() {
        given:
        def applicationId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        def tenantId = "1"
        def tenantIds = [ tenantId ]
        def tenantRole = getTenantRole("name", "2", tenantIds, applicationId, user.id)

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user, tenantRole.clientId).collect()
        TenantRole role = roles.get(0)
        TenantRole role2 = roleRepository.getTenantRolesForUser(user, applicationId, tenantId).collect()[0]
        roleRepository.deleteTenantRole(roles.get(0))
        List<TenantRole> rolesAfterDelete = roleRepository.getTenantRolesForUser(user).collect()

        then:
        roles.size() == 1
        rolesAfterDelete.size() == 0
        def newTenantRole = roles.get(0)
        newTenantRole.roleRsId == tenantRole.roleRsId
        newTenantRole.tenantIds == tenantRole.tenantIds
        newTenantRole.clientId == tenantRole.clientId
        newTenantRole.userId == tenantRole.userId
    }

    def "tenant role crud for application"() {
        given:
        def tenantIds = [ "1" ]
        def tenantRole = getTenantRole("name", "2", tenantIds, application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToApplication(application, tenantRole)
        List<TenantRole> roles = roleRepository.getTenantRolesForApplication(application, tenantRole.clientId).collect()
        TenantRole role = roles.get(0)
        TenantRole role2 = roleRepository.getTenantRoleForApplication(application, role.roleRsId)
        roleRepository.deleteTenantRole(role)
        List<TenantRole> rolesAfterDelete = roleRepository.getTenantRolesForApplication(application).collect()

        then:
        roles.size() == 1
        role2 != null
        rolesAfterDelete.size() == 0
        def newTenantRole = roles.get(0)
        newTenantRole.roleRsId == tenantRole.roleRsId
        newTenantRole.tenantIds == tenantRole.tenantIds
        newTenantRole.clientId == tenantRole.clientId
        newTenantRole.userId == tenantRole.userId
    }

    def "tenant role crud for federated token"() {
        given:
        def applicationId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        def tenantId = "1"
        def tenantIds = [ tenantId ]
        def tenantRole = getTenantRole("name", "2", tenantIds, applicationId, "userId")

        when:
        roleRepository.addTenantRoleToUser(federatedUser, tenantRole)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(federatedUser).collect()
        TenantRole role = roleRepository.getTenantRoleForUser(federatedUser, "2")

        roleRepository.deleteTenantRole(roles.get(0))
        List<TenantRole> rolesAfterDelete = roleRepository.getTenantRolesForUser(federatedUser).collect()

        then:
        roles.size() == 1
        rolesAfterDelete.size() == 0
        def newTenantRole = roles.get(0)
        newTenantRole.roleRsId == tenantRole.roleRsId
        newTenantRole.tenantIds == tenantRole.tenantIds
        newTenantRole.clientId == tenantRole.clientId
        newTenantRole.userId == tenantRole.userId
    }

    def "add tenant role updates existing tenant role"() {
        given:
        def tenantId1 = "1"
        def tenantId2 = "2"
        def tenantRole1 = getTenantRole("name", "2", [ tenantId1 ], application.clientId, "userId")
        def tenantRole2 = getTenantRole("name", "2", [ tenantId2 ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole1)
        roleRepository.addTenantRoleToUser(user, tenantRole2)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user).collect()
        TenantRole role = roles.get(0)
        roleRepository.deleteTenantRole(role)

        then:
        roles.size() == 1
        role.tenantIds as Set == ["1", "2"] as Set
    }

    def "add tenant role already exists throws conflict exception"() {
        given:
        def tenantId = "1"
        def tenantRole = getTenantRole("name", "2", [ tenantId ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole)
        roleRepository.addTenantRoleToUser(user, tenantRole)

        then:
        thrown(ClientConflictException)
    }

    def "delete tenant deletes one at a time"() {
        given:
        def tenantId1 = "1"
        def tenantId2 = "2"
        def tenantRole1 = getTenantRole("name", "2", [ tenantId1 ], application.clientId, "userId")
        def tenantRole2 = getTenantRole("name", "2", [ tenantId2 ], application.clientId, "userId")

        when:
        roleRepository.addTenantRoleToUser(user, tenantRole1)
        roleRepository.addTenantRoleToUser(user, tenantRole2)
        List<TenantRole> roles = roleRepository.getTenantRolesForUser(user).collect()
        TenantRole role = roles.get(0)
        role.tenantIds = ["1"]
        roleRepository.deleteTenantRoleForUser(user, role)
        List<TenantRole> rolesAfterDelete1 = roleRepository.getTenantRolesForUser(user).collect()

        role.tenantIds = ["2"]
        roleRepository.deleteTenantRoleForUser(user, role)
        List<TenantRole> rolesAfterDelete2 = roleRepository.getTenantRolesForUser(user).collect()

        then:
        roles.size() == 1
        rolesAfterDelete1.size() == 1
        rolesAfterDelete1.get(0).tenantIds as Set == ["2"] as Set
        rolesAfterDelete2.size() == 0
    }

    def getTenantRole(name, roleRsId, tenantIds, clientId, userId) {
        new TenantRole().with {
            it.roleRsId = roleRsId
            it.tenantIds = tenantIds
            it.clientId = clientId
            it.userId = userId
            return it
        }
    }

    def getUser(id) {
        new User().with {
            it.id = id
            it.customerId = "customerId"
            it.username = "username$id"
            it.email = "username@test.com"
            it.password = 'Password1'
            return it
        }
    }

    def getFederatedUser(id) {
        new FederatedUser().with {
            it.id = id
            it.username = "username$id"
            it.email = "username@test.com"
            it.domainId="12345"
            it.region="ORD"
            it.federatedIdpUri = Constants.DEFAULT_IDP_URI
            return it
        }
    }

    def getApplication(id) {
        new Application().with {
            it.clientId = id
            it.name = id
            it.clientSecretObj = new ClientSecret()
            it.clientSecretObj.value = "secret"
            return it;
        }
    }

    def getFederatedToken(EndUser user, tokenStr = testUtils.getRandomUUID("token")) {
        new UserScopeAccess().with {
            it.userRsId = user.id
            it.accessTokenString = tokenStr
            it.accessTokenExp = new DateTime().plusDays(1).toDate()
            it.username = user.username
            it.clientId = "clientId"
            it.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
            return it
        }
    }
}
