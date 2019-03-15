package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.api.security.ImmutableTenantRole
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.DomainSubUserDefaults
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleLevelEnum
import com.rackspace.idm.domain.service.rolecalculator.UserRoleLookupService
import com.rackspace.idm.exception.ClientConflictException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.rackspace.idm.util.RoleUtil
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum.STANDARD
import static com.rackspace.idm.domain.service.IdentityUserTypeEnum.USER_ADMIN

class DefaultTenantServiceTest extends RootServiceTest {
    @Shared DefaultTenantService service
    @Shared FederatedUserDao mockFederatedUserDao

    String clientId = "clientId"
    String name = "name"
    String customerId = "customerId"

    def setup() {
        service = new DefaultTenantService()

        mockConfiguration(service)
        mockIdentityConfig(service)
        mockDomainService(service)
        mockTenantDao(service)
        mockTenantService(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)
        mockAuthorizationService(service)
        mockUserService(service)
        mockEndpointService(service)
        mockScopeAccessService(service)
        mockAtomHopperClient(service)
        mockFederatedUserDao(service)
        mockUserGroupService(service)
        mockUserGroupAuthorizationService(service)
        mockIdentityUserService(service)
        mockDelegationService(service)
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

    def "delete rbac roles for user deletes all user-managable rbac roles"() {
        given:
        def role = entityFactory.createTenantRole()
        role.roleRsId = "id"
        def clientRole = entityFactory.createClientRole(RoleLevelEnum.LEVEL_1000.levelAsInt)
        def user = entityFactory.createUser()

        tenantRoleDao.getTenantRolesForUser(user) >> [ role ].asList()
        applicationService.getClientRoleById(_) >> clientRole

        when:
        service.deleteRbacRolesForUser(user)

        then:
        applicationService.getClientRoleById(_) >> clientRole
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
    }

    def "calling getTenantsForUserByTenantRoles returns tenants"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant()
        def tenantRole = entityFactory.createTenantRole().with { it.tenantIds = [ "tenantId" ]; it.name = "identity:default"; return it }
        def tenantRoles = [ tenantRole ].asList()

        when:
        def tenants = service.getTenantsForUserByTenantRoles(user)

        then:
        tenants == [ tenant ].asList()
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole]
        1 * applicationService.getCachedClientRoleById(tenantRole.roleRsId) >> new ImmutableClientRole(new ClientRole().with {
            it.name = tenantRole.name
            it.id = tenantRole.roleRsId
            it}
        )
        1 * tenantDao.getTenant(_) >> tenant
    }

    def "getRoleDetails uses ApplicationService to retrieve client role connected to tenant role"() {
        given:
        def roles = [ entityFactory.createTenantRole() ].asList()

        when:
        service.getRoleDetails(roles)

        then:
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
    }

    def "getTenantRolesForClientRole uses DAO to get all TenantRoles for ClientRole"() {
        given:
        def role = entityFactory.createClientRole()

        when:
        service.getTenantRolesForClientRole(role)

        then:
        1 * tenantRoleDao.getAllTenantRolesForClientRole(role)
    }

    def "deleteTenantRoleForUser deletes propagating tenantRole from subUsers if user is user-admin"() {
        given:
        def userAdminRole = entityFactory.createClientRole().with {
            it.id = "useradmin"
            it.name = IdentityUserTypeEnum.USER_ADMIN.getRoleName()
            it.roleType = RoleTypeEnum.STANDARD
            return it
        }

        def role = entityFactory.createClientRole().with {
            it.id = "propRole"
            it.name = "aRole"
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }
        def tenantRole = entityFactory.createTenantRole(role.name).with {it.roleRsId = role.id; it}
        def identityRole = entityFactory.createTenantRole(userAdminRole.name).with {it.roleRsId = userAdminRole.id; it}

        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subUser", "subUserId", "domainId", "REGION")

        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        applicationService.getClientRoleById(userAdminRole.id) >> userAdminRole
        applicationService.getClientRoleById(role.id) >> role
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.deleteTenantRoleForUser(user, tenantRole, false)

        then:
        applicationService.getClientRoleByClientIdAndRoleName(_, role.name) >> role
        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, tenantRole)
        then:
        1 * userService.getSubUsers(user) >> [ subUser ].asList()
        1 * tenantRoleDao.deleteTenantRoleForUser(subUser, tenantRole)
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
        service.addTenantRoleToUser(null, entityFactory.createTenantRole(), false)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRoleToUser verifies that role is not null"() {
        when:
        service.addTenantRoleToUser(entityFactory.createUser(), null, false)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole verifies that user has uniqueId"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()
        user.uniqueId = null;

        when:
        service.addTenantRoleToUser(user, tenantRole, false)

        then:
        thrown(IllegalArgumentException)
    }

    def "addTenantRole uses ApplicationService to retrieve application for role and verifies it exists"() {
        given:
        def tenantRole = entityFactory.createTenantRole()
        def user = entityFactory.createUser()

        when:
        service.addTenantRoleToUser(user, tenantRole, false)

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
        service.addTenantRoleToUser(user, tenantRole, false)

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
        service.addTenantRoleToUser(user, tenantRole, false)

        then:
        tenantRoleDao.addTenantRoleToUser(user, tenantRole)
    }

    def "addTenantRole to user admin with sub users sends atom feed for all users"() {
        given:
        force_user_admin()
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subuser", "subuserid", "domainid", "region")
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        userService.getSubUsers(_) >> [ subUser ].asList()
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, role, true)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, role)
        1 * tenantRoleDao.addTenantRoleToUser(subUser, role)
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
        1 * atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, _)
    }

    def "addTenantRole does not send atom feed if user already has role"() {
        force_user_admin()
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        tenantRoleDao.addTenantRoleToUser(user, role) >> {throw new ClientConflictException()}
        userService.getSubUsers(user) >> [].asList()

        when:
        try {
            service.addTenantRoleToUser(user, role, true)
        } catch (ClientConflictException ex) {
            //we forced this exception
        }

        then:
        0 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def force_user_admin() {
        force_user_admin(entityFactory.createClientRole())
    }

    def force_user_admin(ClientRole role) {
        tenantRoleDao.getTenantRolesForUser(_) >> [ entityFactory.createTenantRole(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) ].asList()
        role.name = IdentityUserTypeEnum.USER_ADMIN.getRoleName()
        applicationService.getClientRoleById(_) >> role
    }

    @Unroll
    def "deleteTenantRole to user admin with sub users sends atom feed for all users - sendEvent = #sendEventForTargetUser"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser("subuser", "subuserid", "domainid", "region")
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }

        force_user_admin(cRole)
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        userService.getSubUsers(_) >> [ subUser ].asList()
        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.deleteTenantRoleForUser(user, role, sendEventForTargetUser)

        then:
        1 * tenantRoleDao.deleteTenantRoleForUser(user, role)
        1 * tenantRoleDao.deleteTenantRoleForUser(subUser, role)
        if (sendEventForTargetUser) {
            1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
        }
        1 * atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, _)

        where:
        sendEventForTargetUser << [true, false]
    }

    def "deleteTenantRole does not send atom feed if user does not have role"() {
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()
        def cRole = entityFactory.createClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }

        force_user_admin(cRole)
        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> cRole
        tenantRoleDao.deleteTenantRoleForUser(user, role) >> {throw new NotFoundException()}
        userService.getSubUsers(user) >> [].asList()

        when:
        try {
            service.deleteTenantRoleForUser(user, role, true)
        } catch (NotFoundException ex) {
            //we forced this exception
        }

        then:
        0 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "isUserAdmin verifies if user has userAdmin role"() {
        when:
        def user = entityFactory.createUser()
        def tRole = entityFactory.createTenantRole()
        def cRole1 = entityFactory.createClientRole("roleOne")
        def cRole2;
        if (addUserAdmin) {
            cRole2 = entityFactory.createClientRole(IdentityUserTypeEnum.USER_ADMIN.getRoleName())
        } else {
            cRole2 = entityFactory.createClientRole("roleTwo")
        }
        tenantRoleDao.getTenantRolesForUser(_) >> [ tRole, tRole ].asList()
        applicationService.getClientRoleById(_) >>> [ cRole1, cRole2 ]

        def result = service.isUserAdmin(user)

        then:
        result == addUserAdmin

        where:
        addUserAdmin << [true, false]
    }

    def "addTenantRole adds role to subusers if user is user-admin"() {
        given:
        def userAdminRole = entityFactory.createClientRole().with {
            it.id = "useradmin"
            it.name = IdentityUserTypeEnum.USER_ADMIN.getRoleName()
            it.roleType = RoleTypeEnum.STANDARD
            return it
        }

        def role = entityFactory.createClientRole().with {
            it.id = "propRole"
            it.name = "aRole"
            it.roleType = RoleTypeEnum.PROPAGATE
            return it
        }
        def tenantRole = entityFactory.createTenantRole(role.name).with {it.roleRsId = role.id; it}
        def identityRole = entityFactory.createTenantRole(userAdminRole.name).with {it.roleRsId = userAdminRole.id; it}
        def user = entityFactory.createUser("user", "userId", "domainId", "region")
        def subUser = entityFactory.createUser()

        applicationService.getById(_) >> entityFactory.createApplication()
        applicationService.getClientRoleByClientIdAndRoleName(_, userAdminRole.name) >> userAdminRole
        applicationService.getClientRoleByClientIdAndRoleName(_, role.name) >> role
        applicationService.getClientRoleById(userAdminRole.id) >> userAdminRole
        applicationService.getClientRoleById(role.id) >> role
        tenantRoleDao.getTenantRolesForUser(user) >> [ identityRole ].asList()
        userService.getSubUsers(user) >> [ subUser ].asList()

        mockFederatedUserDao.getUsersByDomainId(_) >> [].asList()

        when:
        service.addTenantRoleToUser(user, tenantRole, false)

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

    /**
     * Verify the inferring logic for determining tenant types for tenants works as expected
     * if a tenant does not have a tenant type associated with it and the feature flag `feature.infer.default.tenant.type` the application must infer the tenant type as follows:
     * <ol>
     *     <li>If the tenant's id can be parsed as a java integer (a legacy mosso restriction), set/infer the tenant type 'cloud'</li>
     *     <li>Else if the tenant id is prefixed with the value of the configuration property 'nast.tenant.prefix', set/infer the tenant type "files"</li>
     *     <li>Else if the tenant id is prefixed w/ "hybrid:", set/infer the tenant type "managed_hosting"</li>
     *     <li>Else if the tenant id contains a ":", search for a tenant type that matches the string prior to the first ":". If exists, set/infer that as tenant type (e.g. asdf:332 would set/infer a tenant type "asdf" if it exists)</li>
     *     <li>Else, do not set/infer a tenant type</li>
     * </ol>
     *
     * @return
     */
    @Unroll
    def "Correctly infer tenant types on tenant when enabled: tenantName: #tenantName; tenantTypes: #tenantTypes; existingTypes: #existingTypes; expected: #expectedInferredTenantType"() {
        given:
        reloadableConfig.inferTenantTypeForTenant() >> true
        staticConfig.getNastTenantPrefix() >> "MossoCloudFS_"
        Tenant tenant = new Tenant().with {
            it.name = tenantName
            it.tenantId = tenantName
            it.types = tenantTypes
            it
        }

        when:
        service.setInferredTenantTypeOnTenantIfNecessary(tenant, existingTypes)

        then:
        tenant.types == expectedInferredTenantType

        where:
        tenantName          | tenantTypes | existingTypes                       | expectedInferredTenantType
        // Only can set existing tenant types
        "12345"             | [] as Set    | ["managed_hosting"] as Set         | [] as Set
        "MossoCloudFS_12345"| [] as Set    | [] as Set                          | [] as Set
        "hybrid:123"        | [] as Set    | [] as Set                          | [] as Set
        "asdf:123"          | [] as Set    | ["cloud"] as Set                   | [] as Set

        // Inferred rules used
        "12345"             | [] as Set    | ["cloud"] as Set                   | ["cloud"] as Set
        "MossoCloudFS_12345"| [] as Set    | ["cloud","managed_hosting","files"] as Set  | ["files"] as Set
        "hybrid:123"        | [] as Set    | ["cloud", "managed_hosting"] as Set          | ["managed_hosting"] as Set
        "asdf:12345"        | [] as Set    | ["cloud","asdf","files"] as Set    | ["asdf"] as Set
        "asdf:"             | [] as Set    | ["cloud","asdf","files"] as Set    | [] as Set
        ":asdf"             | [] as Set    | ["cloud","asdf","files"] as Set    | [] as Set
        "asdf:qwer:asdf"             | [] as Set    | ["cloud","asdf","files"] as Set    | ["asdf"] as Set
        "asdf"             | [] as Set    | ["cloud","asdf","files"] as Set    | [] as Set

        // Doesn't override existing
        "12345"             | ["files"] as Set    | ["cloud"] as Set                   | ["files"] as Set
        "MossoCloudFS_12345"| ["cloud"] as Set    | ["cloud","managed_hosting","files"] as Set  | ["cloud"] as Set
        "hybrid:123"        | ["files"] as Set    | ["cloud", "files"] as Set          | ["files"] as Set
        "asdf:12345"        | ["files"] as Set    | ["cloud","asdf","files"] as Set    | ["files"] as Set
    }

    @Unroll
    def "Does not infer tenant types on tenant when disabled: tenantName: #tenantName; tenantTypes: #tenantTypes; existingTypes: #existingTypes; expected: #expectedInferredTenantType"() {
        given:
        reloadableConfig.inferTenantTypeForTenant() >> false
        staticConfig.getNastTenantPrefix() >> "MossoCloudFS_"
        Tenant tenant = new Tenant().with {
            it.name = tenantName
            it.tenantId = tenantName
            it.types = tenantTypes
            it
        }

        when:
        service.setInferredTenantTypeOnTenantIfNecessary(tenant, existingTypes)

        then:
        tenant.types == expectedInferredTenantType

        where:
        tenantName          | tenantTypes | existingTypes                   | expectedInferredTenantType
        "12345"             | [] as Set    | ["cloud"] as Set               | [] as Set
        "MossoCloudFS_12345"| [] as Set    | ["files"] as Set               | [] as Set
        "hybrid:123"        | [] as Set    | ["managed_hosting"] as Set     | [] as Set
        "asdf:12345"        | [] as Set    | ["asdf"] as Set                | [] as Set

        // Doesn't override existing
        "12345"             | ["files"] as Set    | ["cloud"] as Set                   | ["files"] as Set
        "MossoCloudFS_12345"| ["cloud"] as Set    | ["cloud","managed_hosting","files"] as Set  | ["cloud"] as Set
        "hybrid:123"        | ["files"] as Set    | ["cloud", "files"] as Set          | ["files"] as Set
        "asdf:12345"        | ["files"] as Set    | ["cloud","asdf","files"] as Set    | ["files"] as Set
    }

    def "test getTenantRolesForUserPerformant with retrieving cached role by name from applicationService/authorizationService"() {
        given:
        def domainId = "testDomainId"
        def roleId = "roleId"

        def domain = entityFactory.createDomain(domainId)
        def tenant = entityFactory.createTenant()
        def tenantIds = ["12", "21", "321"] as String[]
        def user = entityFactory.createUser("test1","userId", domainId, "region")

        domain.setTenantIds(tenantIds)
        tenant.setDomainId(domainId)

        domainService.getDomain(user.getDomainId()) >> domain

        def tenantRole1 = entityFactory.createTenantRole()
        def tenantRole2 = entityFactory.createTenantRole().with {it.roleRsId = 2; it}
        List<TenantRole> tenantRoles = [tenantRole1, tenantRole2]

        tenantRoleDao.getTenantRolesForUser(user) >> (Iterable<TenantRole>)tenantRoles

        when:
        def tenantRoleList = service.getTenantRolesForUserPerformant(user)

        then:
        !tenantRoleList.isEmpty()
        tenantRoleList.size() == 3

        (1.._) * applicationService.getCachedClientRoleById(_) >> createImmutableClientRole(roleId, USER_ADMIN.levelAsInt)
        (1.._) * applicationService.getCachedClientRoleByName(_) >> createImmutableClientRole(roleId, USER_ADMIN.levelAsInt)
    }

    /**
     * Verify the service retrieves the roles the user is granted based on association with groups
     */
    def "getTenantRolesForUserPerformant: Retrieves roles based on groups assigned per feature flag: flag: #flag"() {
        given:

        def domainId = "testDomainId"
        def groupId = "1234"
        def groupId2 = "abcd"
        def user = entityFactory.createUser("test1","userId", domainId, "region").with {
            it.userGroupDNs = [new DN("rsId=$groupId,ou=groups")
                               , new DN("rsId=$groupId2,ou=groups")] as Set // Add group to user
            it
        }

        when:
        def tenantRoleList = service.getTenantRolesForUserPerformant(user)

        then:
        1 * userGroupAuthorizationService.areUserGroupsEnabledForDomain(domainId) >> flag
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [] // Assume zilch roles returned
        if (flag) {
            // Roles should be retrieved for both groups assigned
            1 * userGroupService.getRoleAssignmentsOnGroup(groupId) >> []
            1 * userGroupService.getRoleAssignmentsOnGroup(groupId2) >> []
        } else {
            // Roles should not be retrieved for groups
            0 * userGroupService.getRoleAssignmentsOnGroup(_)
        }

        where:
        flag << [true, false]
    }

    @Unroll
    def "getTenantRolesForUserPerformant: Calls merge util to merge all assigned tenant roles together regardless if groups enabled: groupsEnabled: #flag"() {
        given:
        userGroupAuthorizationService.areUserGroupsEnabledForDomain(_) >> flag
        RoleUtil mockRoleUtil = Mock()
        service.roleUtil = mockRoleUtil

        def groupId = "1234"
        def user = entityFactory.createUser("test1","userId", "testDomainId", "region").with {
            it.userGroupDNs = [new DN("rsId=$groupId,ou=groups")] as Set // Add group to user
            it
        }

        def userAssignedRoles = [new TenantRole().with {
            it.roleRsId = "ua1"
            it
        }]

        def groupAssignedRoles = [new TenantRole().with {
            it.roleRsId = "ug1"
            it
        }]

        when:
        service.getTenantRolesForUserPerformant(user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(user) >> userAssignedRoles
        /*
         This test does not set up requisite state to generate identity:tenant-access roles for the user so only role
         being merged to users are group roles
          */
        if (flag) {
            1 * userGroupService.getRoleAssignmentsOnGroup(groupId) >> groupAssignedRoles
            1 * mockRoleUtil.mergeTenantRoleSets(_) >> { args ->
                Iterable<TenantRole>[] roleIterables = args[0]
                assert userAssignedRoles == roleIterables[0]
                List<TenantRole> roles = roleIterables[1]
                assert roles.size() == 1
                assert roles.find { it.roleRsId == groupAssignedRoles[0].roleRsId } != null
                []
            }
        } else {
            // Roles should not be retrieved for groups
            0 * userGroupService.getRoleAssignmentsOnGroup(_)
            1 * mockRoleUtil.mergeTenantRoleSets(_) >> { args ->
                Iterable<TenantRole>[] roleIterables = args[0]
                assert userAssignedRoles == roleIterables[0]
                List<TenantRole> roles = roleIterables[1]
                assert roles.size() == 0
                []
            }
        }

        where:
        flag << [true, false]
    }

    def "deleteTenant: Updates tenant roles and domain appropriately"() {
        given:
        identityConfig.getReloadableConfig().getIdentityRoleDefaultTenant() >> "defaultTenant"
        Tenant tenant = new Tenant().with {
            it.tenantId = "tenantId"
            it.domainId = "domainId"
            it
        }
        def otherTenantId = "otherTenantId"
        TenantRole trSingleTenant = new TenantRole().with {
            it.tenantIds = [tenant.tenantId]
            it
        }
        TenantRole trMultipleTenants = new TenantRole().with {
            it.tenantIds = [tenant.tenantId, otherTenantId]
            it
        }
        Domain domain = new Domain().with {
            it.domainId = tenant.domainId
            it.tenantIds = [tenant.tenantId]
            it
        }

        when:
        service.deleteTenant(tenant)

        then:
        1 * tenantRoleDao.getAllTenantRolesForTenant(tenant.getTenantId()) >> [trSingleTenant, trMultipleTenants]
        1 * tenantRoleDao.updateTenantRole(trMultipleTenants) >> {args ->
            TenantRole tr = args[0]
            assert tr.tenantIds == [otherTenantId] as Set // Deleted tenant is removed
            null
        }
        1 * tenantRoleDao.deleteTenantRole(trSingleTenant)
        1 * tenantDao.deleteTenant(tenant.tenantId)
        1 * domainService.getDomain(tenant.getDomainId()) >> domain
        1 * domainService.updateDomain(domain) >> { args ->
            Domain dom = args[0]
            assert dom.tenantIds.size() == 0
            null
        }
    }
      
    def "getEnabledUsersForTenantWithRole: recognizes tenantRole in users and userGroup "() {
        given:
        def paginationParams = new PaginationParams(0, 25)
        def tenantRole = entityFactory.createTenantRole("somerole")
        tenantRole.tenantIds = ["12345"]

        def tenantRoleInUserGroup = entityFactory.createTenantRole("tenantRoleInUserGroup").with {
            it.uniqueId = "roleRsId=1234,cn=ROLES,rsId=f2b6f73730324548a729d6a878640558,ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com"
            return it
        }
        def userGroup = new UserGroup().with {
            it.id = "f2b6f73730324548a729d6a878640558"
            it
        }

        def user = entityFactory.createRandomUser()
        def groupUser = entityFactory.createRandomUser()
        def tenant = entityFactory.createTenant()

        when:
        def paginatedUser = service.getEnabledUsersForTenantWithRole(tenant, null, paginationParams)

        then: "Retrieves all tenant roles associated with tenant"
        1 * tenantRoleDao.getAllTenantRolesForTenant(tenant.tenantId) >> [tenantRole, tenantRoleInUserGroup].asList()

        and: "Looks up user and group appropriately for type of tenant roles assigned"
        1 * userGroupService.getGroupById(userGroup.id) >> userGroup
        1 * userService.getUserById(tenantRole.getUserId()) >> user
        1 * identityUserService.getEndUsersInUserGroup(userGroup) >> [groupUser]
        0 * userService.getUserById("f2b6f73730324548a729d6a878640558")

        paginatedUser.totalRecords == 2
        paginatedUser.getValueList().size() == 2
        paginatedUser.getValueList().find {it.id == user.id} != null
        paginatedUser.getValueList().find {it.id == groupUser.id} != null
    }

    def "getEnabledUsersForTenantWithRole: ignores non-provisioned and disabled users in userGroups"() {
        given:
        def paginationParams = new PaginationParams(0, 25)
        def tenantRole = entityFactory.createTenantRole("somerole")
        tenantRole.tenantIds = ["12345"]

        def userGroup = new UserGroup().with {
            it.id = "f2b6f73730324548a729d6a878640558"
            it
        }

        def tenantRoleInUserGroup = entityFactory.createTenantRole("tenantRoleInUserGroup").with {
            it.uniqueId = "roleRsId=1234,cn=ROLES,rsId=${userGroup.id},ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com"
            return it
        }

        def user = entityFactory.createRandomUser()
        def groupUser = entityFactory.createRandomUser()
        def groupFedUser = entityFactory.createFederatedUser()
        def groupDisabledUser = entityFactory.createRandomUser().with {
            it.enabled = false
            it
        }

        def tenant = entityFactory.createTenant()

        when:
        def paginatedUser = service.getEnabledUsersForTenantWithRole(tenant, null, paginationParams)

        then: "Retrieves all tenant roles associated with tenant"
        1 * tenantRoleDao.getAllTenantRolesForTenant(tenant.tenantId) >> [tenantRole, tenantRoleInUserGroup].asList()

        and: "Looks up user and group appropriately for type of tenant roles assigned"
        1 * userGroupService.getGroupById(userGroup.id) >> userGroup
        1 * userService.getUserById(tenantRole.getUserId()) >> user
        1 * identityUserService.getEndUsersInUserGroup(userGroup) >> [groupUser, groupFedUser, groupDisabledUser]
        0 * userService.getUserById(userGroup.id) // Verify didn't errantly look up group as a user
        0 * userGroupService.getGroupById(tenantRole.getUserId()) // Verify didn't errantly look up user as a group

        paginatedUser.totalRecords == 2
        paginatedUser.getValueList().size() == 2
        paginatedUser.getValueList().find {it.id == user.id} != null
        paginatedUser.getValueList().find {it.id == groupUser.id} != null
    }

    def "getEnabledUsersForTenantWithRole: Users in groups are not looked up individually even if assigned explicit role on tenant"() {
        given:
        def paginationParams = new PaginationParams(0, 25)
        def explicitUser1 = entityFactory.createRandomUser()
        def groupUser = entityFactory.createRandomUser()

        def tenantRole1 = entityFactory.createTenantRoleForUser(explicitUser1.id).with {
            it.tenantIds = ["12345"]
            it
        }

        // Assign the group user an explicit role as well
        def tenantRole2 = entityFactory.createTenantRoleForUser(groupUser.id).with {
            it.tenantIds = ["12345"]
            it
        }

        def tenantRoleInUserGroup = entityFactory.createTenantRoleForGroup("tenantRoleInUserGroup")
        def userGroup = new UserGroup().with {
            it.id = tenantRoleInUserGroup.getIdOfEntityAssignedRole()
            it
        }

        def tenant = entityFactory.createTenant()

        when:
        def paginatedUser = service.getEnabledUsersForTenantWithRole(tenant, null, paginationParams)

        then: "Retrieves all tenant roles associated with tenant"
        1 * tenantRoleDao.getAllTenantRolesForTenant(tenant.tenantId) >> [tenantRole1, tenantRole2, tenantRoleInUserGroup].asList()

        and: "Looks up group users for group tenant roles"
        1 * userGroupService.getGroupById(userGroup.id) >> userGroup
        1 * identityUserService.getEndUsersInUserGroup(userGroup) >> [groupUser]

        and: "looks up individual users not included in group"
        1 * userService.getUserById(explicitUser1.id) >> explicitUser1
        0 * userService.getUserById(groupUser.id)

        and: "result returns both users"
        paginatedUser.totalRecords == 2
        paginatedUser.getValueList().size() == 2
        paginatedUser.getValueList().find {it.id == explicitUser1.id} != null
        paginatedUser.getValueList().find {it.id == groupUser.id} != null
    }

    def "getEnabledUsersWithContactIdForTenant: Retrieves users with provided contactId for tenant"() {
        given:
        def tenantId = "tenantId"
        def contactId = "contactId"
        def user = entityFactory.createUser().with {
            it.contactId = contactId
            it
        }
        def tenantRole = entityFactory.createTenantRole("tenantRole").with {
            it.tenantIds.add(tenantId)
            it
        }

        when:
        List<User> userList = service.getEnabledUsersWithContactIdForTenant(tenantId, contactId)

        then:
        1 * userService.getEnabledUsersByContactId(contactId) >> [user].asList()
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole].asList()

        userList.size() == 1
        userList.get(0) == user
    }

    def "getEnabledUsersWithContactIdForTenant: no tenant role on matching users"() {
        given:
        def tenantId = "tenantId"
        def contactId = "contactId"
        def user = entityFactory.createUser().with {
            it.contactId = contactId
            it
        }
        def tenantRole = entityFactory.createTenantRole("tenantRole")

        when:
        List<User> userList = service.getEnabledUsersWithContactIdForTenant(tenantId, contactId)

        then:
        1 * userService.getEnabledUsersByContactId(contactId) >> [user].asList()
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole].asList()

        userList.size() == 0
    }

    def "getEnabledUsersWithContactIdForTenant: no users found with provided contactId"() {
        given:
        def tenantId = "tenantId"
        def contactId = "contactId"

        when:
        List<User> userList = service.getEnabledUsersWithContactIdForTenant(tenantId, contactId)

        then:
        1 * userService.getEnabledUsersByContactId(contactId) >> [].asList()
        userList.size() == 0
    }

    def "getEffectiveGlobalRolesForUserApplyRcnRoles populates the propagating attribute (by populating the role type) correctly"() {
        given:
        def user = new User()
        def userTenantRoles = []
        def propagatingRole = new TenantRole(). with {
            it.roleRsId = RandomStringUtils.randomAlphanumeric(8)
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }
        userTenantRoles << propagatingRole
        def propagatingClientRole = new ClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it.id = propagatingRole.roleRsId
            it
        }

        when:
        List<TenantRole> roles = service.getEffectiveGlobalRolesForUserExcludeRcnRoles(user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(user) >> userTenantRoles
        1 * applicationService.getClientRoleById(propagatingRole.roleRsId) >> propagatingClientRole
        roles.find { role -> role.roleRsId == propagatingRole.roleRsId }.propagate == true
    }

    def "getEffectiveGlobalRolesForUser populates the propagating attribute (by populating the role type) correctly"() {
        given:
        def user = new User()
        def userTenantRoles = []
        def propagatingRole = new TenantRole(). with {
            it.roleRsId = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        userTenantRoles << propagatingRole
        def nonPropagatingRole = new TenantRole(). with {
            it.roleRsId = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        userTenantRoles << nonPropagatingRole
        def propagatingClientRole = new ClientRole().with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it.id = propagatingRole.roleRsId
            it
        }
        def nonPropagatingClientRole = new ClientRole().with {
            it.roleType = RoleTypeEnum.STANDARD
            it.id = propagatingRole.roleRsId
            it
        }

        when:
        List<TenantRole> roles = service.getEffectiveGlobalRolesForUserIncludeRcnRoles(user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(user) >> userTenantRoles
        1 * applicationService.getClientRoleById(propagatingRole.roleRsId) >> propagatingClientRole
        1 * applicationService.getClientRoleById(nonPropagatingRole.roleRsId) >> nonPropagatingClientRole
        roles.find { role -> role.roleRsId == propagatingRole.roleRsId }.propagate == true
        roles.find { role -> role.roleRsId == nonPropagatingRole.roleRsId }.propagate == false
    }

    def "doesUserContainTenantRole checks provisioned users role assignment"() {
        given:
        def domain = Mock(Domain)
        domain.domainId >> "domainId"

        def user = Mock(User)
        user.id >> "id"
        user.domainId >> "domainId"

        def tenantRole = new TenantRole().with {
            it.roleRsId = "id"
            it.name = "name"
            it
        }

        def clientRole = new ClientRole().with {
            it.name = "identity:admin"
            it.id = "id"
            it
        }

        def delegationAgreement = Mock(DelegationAgreement)
        def subUserDefaults = Mock(DomainSubUserDefaults)
        subUserDefaults.domainId >> "domainId"
        subUserDefaults.getSubUserTenantRoles() >> [new ImmutableTenantRole(tenantRole)]

        def provisionedUserDelegate = new ProvisionedUserDelegate(subUserDefaults, delegationAgreement, user)
        GroovyMock(EndUserDenormalizedSourcedRoleAssignmentsBuilder, global: true)
        def endUserDenormalizedSourcedRoleAssignmentsBuilder = Mock(EndUserDenormalizedSourcedRoleAssignmentsBuilder)
        def userRoleLookupService = Mock(UserRoleLookupService)
        endUserDenormalizedSourcedRoleAssignmentsBuilder.userRoleLookupService >> userRoleLookupService
        domainService.getDomain(_) >> domain
        applicationService.getCachedClientRoleById(_) >> new ImmutableClientRole(clientRole)

        when:
        def containsRole = service.doesUserContainTenantRole(provisionedUserDelegate, roleId)

        then:
        shouldContainRole == containsRole

        where:
        shouldContainRole | roleId
        true              | "id"
        false             | "id2"
    }

    def "getTenantsByDomainId only uses tenants when feature flag getOnlyUseTenantDomainPointers is enabled"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant()

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> true

        when:
        def result = service.getTenantsByDomainId(domainId)

        then:
        1 * tenantDao.getTenantsByDomainId(domainId) >> [tenant]
        0 * domainService.getDomain(domainId)
        result.size() == 1
        result.get(0) == tenant
    }

    @Unroll
    def "getTenantsByDomainId with enabled filter; enabled = #enabled, getOnlyUseTenantDomainPointers = false"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant().with {
            it.domainId = domainId
            it.tenantId = "tenantId"
            it.enabled = enabled
            it
        }
        def domain = entityFactory.createDomain().with {
            it.tenantIds = [tenant.tenantId]
            it
        }

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> false

        when:
        def result = service.getTenantsByDomainId(domainId, enabled)

        then:
        result.size() == 1
        result.get(0) == tenant

        0 * tenantDao.getTenantsByDomainId(domainId)
        1 * domainService.getDomain(domainId) >> domain
        1 * tenantDao.getTenant(tenant.tenantId) >> tenant


        where:
        enabled << [true, false]
    }

    @Unroll
    def "getTenantsByDomainId with enabled filter; enabled = #enabled, getOnlyUseTenantDomainPointers = true"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant().with {
            it.domainId = domainId
            it.tenantId = "tenantId"
            it.enabled = enabled
            it
        }
        def domain = entityFactory.createDomain().with {
            it.tenantIds = [tenant.tenantId]
            it
        }

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> true

        when:
        def result = service.getTenantsByDomainId(domain.domainId, enabled)

        then:
        result.size() == 1
        result.get(0) == tenant

        1 * tenantDao.getTenantsByDomainId(domainId) >> [tenant]

        where:
        enabled << [true, false]
    }

    def "getTenantsByDomainId uses domains when feature flag getOnlyUseTenantDomainPointers is disabled"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant()
        def tenantId = tenant.tenantId
        def domain = entityFactory.createDomain().with {
            it.tenantIds = [tenantId]
            it
        }

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> false

        when:
        def result = service.getTenantsByDomainId(domainId)

        then:
        0 * tenantDao.getTenantsByDomainId(domainId)
        1 * domainService.getDomain(domainId) >> domain
        1 * tenantDao.getTenant(tenantId) >> tenant
        result.size() == 1
        result.get(0) == tenant
    }

    def "getTenantIdsForDomain only uses tenants when feature flag getOnlyUseTenantDomainPointers is enabled"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant()
        def tenantId = tenant.tenantId
        def domain = entityFactory.createDomain().with {
            it.tenantIds = [tenantId]
            it
        }

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> true

        when:
        def result = service.getTenantIdsForDomain(domain)

        then:
        1 * tenantDao.getTenantsByDomainId(domainId) >> [tenant]
        result.size() == 1
        result[0] == tenantId
    }

    def "getTenantIdsForDomain uses domains when feature flag getOnlyUseTenantDomainPointers is disabled"() {
        given:
        def domainId = "domainId"
        def tenant = entityFactory.createTenant()
        def tenantId = tenant.tenantId
        def domain = entityFactory.createDomain().with {
            it.tenantIds = [tenantId]
            it
        }

        identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled() >> false

        when:
        def result = service.getTenantIdsForDomain(domain)

        then:
        0 * tenantDao.getTenantsByDomainId(domainId)
        result.size() == 1
        result[0] == tenantId
    }

    def "addTenantRolesToUser: calls correct services"() {
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole().with {
            it.clientId = "clientId"
            it.name = "name"
            it
        }
        def clientRole = entityFactory.createClientRole()
        def tenantRoles = [tenantRole]

        when:
        service.addTenantRolesToUser(user, tenantRoles)

        then:
        1 * tenantRoleDao.addTenantRoleToUser(user, tenantRole)
        2 * applicationService.getClientRoleByClientIdAndRoleName(tenantRole.getClientId(), tenantRole.getName()) >> clientRole
        1 * applicationService.getById(tenantRole.getClientId()) >> entityFactory.createApplication()
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "addTenantRolesToUser: send user feed event if at least one role was added"() {
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole().with {
            it.clientId = "clientId"
            it.name = "name"
            it.roleRsId = "roleId"
            it
        }
        def tenantRole2 = entityFactory.createTenantRole().with {
            it.clientId = "clientId"
            it.name = "name2"
            it.roleRsId = "roleId2"
            it
        }
        def clientRole = entityFactory.createClientRole()
        def tenantRoles = [tenantRole, tenantRole2]

        when:
        service.addTenantRolesToUser(user, tenantRoles)

        then:
        thrown(Exception)

        1 * tenantRoleDao.addTenantRoleToUser(user, tenantRole)
        1 * tenantRoleDao.addTenantRoleToUser(user, tenantRole2) >> {throw new Exception()}
        2 * applicationService.getClientRoleByClientIdAndRoleName(tenantRole.getClientId(), tenantRole.getName()) >> clientRole
        1 * applicationService.getClientRoleByClientIdAndRoleName(tenantRole2.getClientId(), tenantRole2.getName()) >> clientRole
        2 * applicationService.getById(tenantRole.getClientId()) >> entityFactory.createApplication()
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "deleteRbacRolesForUser: send user feed event if at least one role was deleted"() {
        given:
        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole().with {
            it.clientId = "clientId"
            it.name = "name"
            it.roleRsId = "roleId"
            it
        }
        def tenantRole2 = entityFactory.createTenantRole().with {
            it.clientId = "clientId"
            it.name = "name2"
            it.roleRsId = "roleId2"
            it
        }
        def clientRole = entityFactory.createClientRole().with {
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it
        }
        def tenantRoles = [tenantRole, tenantRole2]

        when:
        service.deleteRbacRolesForUser(user)

        then:
        thrown(Exception)

        1 * tenantRoleDao.getTenantRolesForUser(user) >> tenantRoles
        4 * applicationService.getClientRoleById(_) >> clientRole
        1 * tenantRoleDao.deleteTenantRoleForUser(user, tenantRole)
        1 * tenantRoleDao.deleteTenantRoleForUser(user, tenantRole2) >> {throw new Exception()}
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def createImmutableClientRole(String name, int weight = 1000) {
        return new ImmutableClientRole(new ClientRole().with {
            it.name = name
            it.id = name
            it.roleType = STANDARD
            it.rsWeight = weight
            it
        })
    }

    def mockFederatedUserDao(service) {
        mockFederatedUserDao = Mock(FederatedUserDao)
        service.federatedUserDao = mockFederatedUserDao
    }
}