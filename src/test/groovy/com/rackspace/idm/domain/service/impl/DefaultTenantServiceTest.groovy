package com.rackspace.idm.domain.service.impl
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.exception.ClientConflictException
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultTenantServiceTest extends RootServiceTest {
    @Shared DefaultTenantService service
    @Shared FederatedUserDao mockFederatedUserDao

    String clientId = "clientId"
    String name = "name"
    String customerId = "customerId"

    def setupSpec() {
        service = new DefaultTenantService()
    }

    def setup() {
        mockConfiguration(service)
        mockDomainService(service)
        mockTenantDao(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)
        mockUserService(service)
        mockEndpointService(service)
        mockScopeAccessService(service)
        mockAtomHopperClient(service)
        mockFederatedUserDao(service)
    }

    def "get mossoId from roles returns compute:default tenantId"() {
        given:
        def computeRoleWithTenant = entityFactory.createTenantRole("compute:default")
        computeRoleWithTenant.tenantIds = ["tenantId"]
        def computeRoleWithoutTenant = entityFactory.createTenantRole("compute:default")

        when:
        def mossoId1 = service.getMossoIdFromTenantRoles([computeRoleWithTenant].asList())
        def mossoId2 = service.getMossoIdFromTenantRoles([computeRoleWithoutTenant].asList())

        then:
        mossoId1 == "tenantId"
        mossoId2 == null
    }

    def "get mossoId from roles returns tenantId that is all digits"() {
        given:
        def computeRoleWithTenant = entityFactory.createTenantRole("somerole")
        computeRoleWithTenant.tenantIds = ["12345"]
        def computeRoleWithoutTenant = entityFactory.createTenantRole("somerole")

        when:
        def mossoId1 = service.getMossoIdFromTenantRoles([computeRoleWithTenant].asList())
        def mossoId2 = service.getMossoIdFromTenantRoles([computeRoleWithoutTenant].asList())

        then:
        mossoId1 == "12345"
        mossoId2 == null
    }

    def "delete rbac roles for user deletes all rbac roles (roles where weight=default.rsWeight)"() {
        given:
        def role = entityFactory.createTenantRole()
        role.roleRsId = "id"
        def clientRole = entityFactory.createClientRole(1000)
        def user = entityFactory.createUser()

        tenantRoleDao.getTenantRolesForUser(user) >> [ role ].asList()
        applicationService.getClientRoleById(_) >> clientRole
        config.getInt("default.rsWeight") >> 1000

        when:
        service.deleteRbacRolesForUser(user)

        then:
        applicationService.getClientRoleById(_) >> clientRole
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
    }

    def "add TenantRole To Client throws NotFoundException if clientRole is Null"() {
        given:
        def tenantRole = entityFactory.createTenantRole("role")
        def application = entityFactory.createApplication()

        applicationService.getById(clientId) >> application
        applicationService.getClientRoleByClientIdAndRoleName(clientId, "role") >> null

        when:
        service.addTenantRoleToClient(application, tenantRole)

        then:
        thrown(NotFoundException)
    }

    def "add TenantRole To Client throws NotFoundException if owner Is Null"() {
        given:
        def app = entityFactory.createApplication()
        def tenantRole = entityFactory.createTenantRole("role")

        applicationService.getById(clientId) >> null

        when:
        service.addTenantRoleToClient(app,tenantRole);

        then:
        thrown(NotFoundException)
    }

    def "add TenantRole To Client does Not Call ScopeAccessService Method if scopeAccess Is Not Null"() {
        given:
        def tenantRole = entityFactory.createTenantRole("role").with { it.clientId = clientId; return it }
        def application = entityFactory.createApplication()
        applicationService.getById(_) >> application
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> new ClientRole()
        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> new ScopeAccess()
        service.addTenantRoleToClient(application, tenantRole) >> void

        when:
        service.addTenantRoleToClient(application,tenantRole);

        then:
        0 * scopeAccessService.addDirectScopeAccess(_, _)
    }

    def "calling getTenantsForUserByTenantRoles returns tenants"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant()
        def tenantRole = entityFactory.createTenantRole().with { it.tenantIds = [ "tenantId" ]; return it }
        def tenantRoles = [ tenantRole ].asList()

        when:
        def tenants = service.getTenantsForUserByTenantRoles(user)

        then:
        tenants == [ tenant ].asList()
        1 * tenantRoleDao.getTenantRolesForUser(_) >> tenantRoles
        1 * tenantDao.getTenant(_) >> tenant
    }

    def "hasTenantAccess returns false if user is null or tenantId is blank"() {
        expect:
        result == false

        where:
        result << [
                service.hasTenantAccess(null, "tenantId"),
                service.hasTenantAccess(entityFactory.createUser(), ""),
                service.hasTenantAccess(entityFactory.createUser(), null)
        ]
    }

    def "hasTenantAccess returns true if it contains tenant; false otherwise"() {
        given:
        def tenantRole = entityFactory.createTenantRole("tenantName").with { it.tenantIds = [ "tenantId" ]; return it }
        def tenantRoles = [ tenantRole ].asList()
        def tenantWithMatchingId = entityFactory.createTenant("tenantId", "noMatch")
        def tenantWithMatchingName = entityFactory.createTenant("notTenantId", "match")
        def user = entityFactory.createUser()

        when:
        def result1 = service.hasTenantAccess(user, "tenantId")
        def result2 = service.hasTenantAccess(user, "tenantId")
        def result3 = service.hasTenantAccess(user, "match")

        then:
        3 * tenantRoleDao.getTenantRolesForUser(_) >>> [ [].asList() ] >> tenantRoles
        2 * tenantDao.getTenant(_) >>> [ tenantWithMatchingId, tenantWithMatchingName]

        result1 == false
        result2 == true
        result3 == true
    }

    def "addCallerTenantRolesToUser gets callers tenant roles by userObject"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        service.addCallerTenantRolesToUser(caller, user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(caller) >> [].asList()
    }

    def "addCallerTenantRolesToUser verifies that role to be added is not an identity:* role"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()
        def tenantRoleList = [ entityFactory.createTenantRole() ].asList()

        tenantDao.getTenantRolesForUser(caller) >> tenantRoleList
        tenantRoleDao.getTenantRolesForUser(_) >> [].asList()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()

        when:
        service.addCallerTenantRolesToUser(caller, user)

        then:
        config.getString("cloudAuth.adminRole") >> ""
        config.getString("cloudAuth.serviceAdminRole") >> ""
        config.getString("cloudAuth.userAdminRole") >> ""
        config.getString("cloudAuth.userRole") >> ""
    }

    def "getRoleDetails uses ApplicationService to retrieve client role connected to tenant role"() {
        given:
        def roles = [ entityFactory.createTenantRole() ].asList()

        when:
        service.getRoleDetails(roles)

        then:
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
    }

    def "if scope access for tenant roles for scopeAccess with null scopeAccess returns IllegalState" () {
        when:
        service.getTenantRolesForScopeAccess(null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null user returns IllegalState" () {
        given:
        def application = entityFactory.createApplication()

        when:
        service.deleteTenantRoleForApplication(application, null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null application returns IllegalState" () {
        given:
        def tenantRole = entityFactory.createTenantRole()

        when:
        service.deleteTenantRoleForApplication(null, tenantRole)

        then:
        thrown(IllegalStateException)
    }

    def "getTenantRolesForClientRole uses DAO to get all TenantRoles for ClientRole"() {
        given:
        def role = entityFactory.createClientRole()

        when:
        service.getTenantRolesForClientRole(role)

        then:
        1 * tenantRoleDao.getAllTenantRolesForClientRole(role)
    }

    def "deleteTenantRoleForUser deletes tenantRole from subUsers if user is user-admin"() {
        given:
        def roleName = "identity"
        def identityRole = entityFactory.createTenantRole(roleName)
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {it.name = roleName; it.propagate = true; return it}
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subUser", "subUserId", "domainId", "REGION")

        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        applicationService.getClientRoleById(_) >> cRole
        config.getString(_) >> roleName
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.deleteTenantRoleForUser(user, role)

        then:
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
        then:
        1 * userService.getSubUsers(user) >> [ subUser ].asList()
        1 * tenantRoleDao.deleteTenantRoleForUser(subUser, role)
    }

    def "deleteTenantRole uses DAO to delete role"() {
        given:
        def role = entityFactory.createTenantRole()

        when:
        service.deleteTenantRole(role)

        then:
        1 * tenantRoleDao.deleteTenantRole(role)
    }

    def "getTenantRoleForUser uses DAO to get tenantRole for user"() {
        given:
        def list = [].asList()
        def user = entityFactory.createUser()

        when:
        service.getTenantRolesForUserById(user, list)

        then:
        1 * tenantRoleDao.getTenantRoleForUser(user, list)
    }

    def "getIdsForUsersWithTenantRole calls DAO to retrieve context object"() {
        given:
        def sizeLimit = 123;

        when:
        service.getIdsForUsersWithTenantRole("roleId", sizeLimit)

        then:
        1 * tenantRoleDao.getIdsForUsersWithTenantRole("roleId", sizeLimit)
    }

    def "addTenantRoleToUser verifies that user is not null"() {
        when:
        service.addTenantRoleToUser(null, entityFactory.createTenantRole())

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRoleToUser verifies that role is not null"() {
        when:
        service.addTenantRoleToUser(entityFactory.createUser(), null)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole verifies that user has uniqueId"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        user.uniqueId = null;

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole uses ApplicationService to retrieve application for role and verifies it exists"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * applicationService.getById(_) >> null
        then:
        thrown(NotFoundException)
    }

    def "addTenantRole uses ApplicationService to retrieve application role linked to tenantrole and verifies it exists"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        def application = entityFactory.createApplication()

        applicationService.getById(_) >> application

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * applicationService.getClientRoleByClientIdAndRoleName(_, _) >> null
        then:
        thrown(NotFoundException)
    }

    def "addTenantRole uses DAO to add tenant role to user"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        def application = entityFactory.createApplication()
        def cRole = entityFactory.createClientRole()

        applicationService.getById(_) >> application
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        config.getString(_) >> ""
        tenantRoleDao.getTenantRolesForUser(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        tenantRoleDao.addTenantRoleToUser(user, tenantRole)
    }

    def "addTenantRole sends atom feed for user"() {
        given:
        force_user_admin()
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = false
            return it
        }

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole


        when:
        service.addTenantRoleToUser(user, role)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, role)
        1 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
    }

    def "addTenantRole to user admin with sub users sends atom feed for all users"() {
        given:
        force_user_admin()
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subuser", "subuserid", "domainid", "region")
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = true
            return it
        }

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        userService.getSubUsers(_) >> [ subUser ].asList()
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, role)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, role)
        1 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
        1 * tenantRoleDao.addTenantRoleToUser(subUser, role)
        1 * atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE)
    }

    def "addTenantRole does not send atom feed if user already has role"() {
        force_user_admin()
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = true
            return it
        }

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        tenantRoleDao.addTenantRoleToUser(user, role) >> {throw new ClientConflictException()}
        userService.getSubUsers(user) >> [].asList()

        when:
        try {
            service.addTenantRoleToUser(user, role)
        } catch (ClientConflictException ex) {
            //we forced this exception
        }

        then:
        0 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
    }

    def force_user_admin() {
        force_user_admin(entityFactory.createClientRole())
    }

    def force_user_admin(ClientRole role) {
        tenantRoleDao.getTenantRolesForUser(_) >> [ entityFactory.createTenantRole("admin_role") ].asList()
        config.getString("cloudAuth.userAdminRole") >> "admin_role"
        role.name = "admin_role"
        applicationService.getClientRoleById(_) >> role
    }

    def "deleteTenantRole sends atom feed for user"() {
        given:
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = false
            return it
        }

        force_user_admin(cRole)
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole


        when:
        service.deleteTenantRoleForUser(user, role)

        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
        1 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
    }

    def "deleteTenantRole to user admin with sub users sends atom feed for all users"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subuser", "subuserid", "domainid", "region")
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = true
            return it
        }

        force_user_admin(cRole)
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        userService.getSubUsers(_) >> [ subUser ].asList()
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.deleteTenantRoleForUser(user, role)

        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
        1 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
        1 * tenantRoleDao.deleteTenantRoleForUser(subUser, role)
        1 * atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE)
    }

    def "deleteTenantRole does not send atom feed if user already has role"() {
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.propagate = true
            return it
        }

        force_user_admin(cRole)
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        tenantRoleDao.deleteTenantRoleForUser(user, role) >> {throw new NotFoundException()}
        userService.getSubUsers(user) >> [].asList()

        when:
        try {
            service.deleteTenantRoleForUser(user, role)
        } catch (NotFoundException ex) {
            //we forced this exception
        }

        then:
        0 * atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE)
    }

    def "isUserAdmin verifies if user has userAdmin role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2 = entityFactory.createClientRole("roleTwo")
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        config.getString("cloudAuth.userAdminRole") >> userAdminRoleName
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isUserAdmin(user)

        then:
        result == isUserAdmin

        where:
        isUserAdmin | userAdminRoleName
        false       | "roleThree"
        true        | "roleOne"
    }

    def "addTenantRole adds role to subusers if user is user-admin"() {
        given:
        def roleName = "thisRole"
        def role = entityFactory.createClientRole().with {
            it.name = roleName
            it.propagate = true
            return it
        }
        def tenantRole = entityFactory.createTenantRole()
        def identityRole = entityFactory.createTenantRole(roleName)
        def user = entityFactory.createUser("user", "userId", "domainId", "region")
        def subUser = entityFactory.createUser()

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> role
        applicationService.getClientRoleById(_) >> role
        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        userService.getSubUsers(user) >> [ subUser ].asList()
        config.getString("cloudAuth.userAdminRole") >> roleName
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, tenantRole)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, tenantRole)
        then:
        1 * tenantRoleDao.addTenantRoleToUser(subUser, tenantRole)
    }

    def "Add userId To TenantRole if it does not have it set"() {
        given:
        def userId = "1"
        def tenantRole = entityFactory.createTenantRole()

        when:
        service.addUserIdToTenantRole(tenantRole)

        then:
        1 * tenantRoleDao.getUserIdForParent(_) >> userId
        1 * tenantRoleDao.updateTenantRole(_)
        tenantRole.getUserId() != null
        tenantRole.getUserId() == userId

    }

    def "Add userId To TenantRole - sending null"() {
        when:
        service.addUserIdToTenantRole(null)

        then:
        notThrown(Exception)
    }

    def "GET - tenantRoles for tenant"(){
        when:
        def tenantRole = null
        if (useTenantsRole){
            tenantRole = entityFactory.createTenantRole()
            tenantRole.name == null
        }
        def clientRole = null
        if (useClientRole){
            clientRole = entityFactory.createClientRole()
            clientRole.description = "desc"
            clientRole.name = "name"
        }
        if (tenantRole != null){
            tenantRoleDao.getAllTenantRolesForTenant(_) >> [tenantRole].asList()
        }else{
            tenantRoleDao.getAllTenantRolesForTenant(_) >> [].asList()
        }
        applicationService.getClientRoleById(_) >> clientRole
        def result = service.getTenantRolesForTenant(value)

        then:
        if (result != null && result.size() != 0){
            result.get(0).description == desc
            result.get(0).name == name
        }else{
            result == [].asList()
        }

        where:
        value   | desc    | name   | useTenantsRole  | useClientRole
        null    | null    | null   | false           | false
        "1"     | "desc"  | "name" | true            | true

    }

    def mockFederatedUserDao(service) {
        mockFederatedUserDao = Mock(FederatedUserDao)
        service.federatedUserDao = mockFederatedUserDao
    }
}
