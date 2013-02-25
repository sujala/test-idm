package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.FilterParam
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Users
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/6/12
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultUserServiceTest extends RootServiceTest {
    @Shared DefaultUserService service

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def FilterParam[] roleFilter
    @Shared def FilterParam[] domainAndRoleFilters
    @Shared def userList
    @Shared def userIdList
    @Shared def users

    def setupSpec(){
        service = new DefaultUserService()
        sharedRandom = ("$sharedRandomness").replace('-',"")
        userList = new ArrayList<User>()
        userIdList = new ArrayList<String>()
        for (int i = 5; i > 0; i--) {
            def id = UUID.randomUUID().toString().replace('-', "")
            def user = entityFactory.createUser(i.toString(), id, "domainId", "region").with {
                it.roles = [ entityFactory.createTenantRole().with { it.roleRsId = sharedRandom; it} ].asList()
                return it
            }
            userList.add(user)
            userIdList.add(id)
        }
        users = entityFactory.createUsers(userList)

        roleFilter = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom)]
        domainAndRoleFilters = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom),
                                new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, sharedRandom)]
    }

    def setup() {
        mockPasswordComplexityService(service)
        mockScopeAccessDao(service)
        mockAuthDao(service)
        mockApplicationService(service)
        mockConfiguration(service)
        mockUserDao(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockTenantDao(service)
        mockEndpointService(service)
        mockAuthorizationService(service)
        mockCloudRegionService(service)
        mockValidator(service)
        mockTenantRoleDao(service)
        mockApplicationRoleDao(service)
    }

    def "Add BaseUrl to user"() {
        given:
        User user = new User()
        user.id = "1"
        user.nastId = "123"
        user.mossoId = 123

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.addBaseUrlId("2")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        service.addBaseUrlToUser(1, user)

        then:
        1 * this.tenantService.updateTenant(_)
    }

    def "Add BaseUrl to user - dup baseUrl on tenant"() {
        given:
        User user = new User()
        user.id = "1"
        user.nastId = "123"
        user.mossoId = 123

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.addBaseUrlId("1")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        service.addBaseUrlToUser(1, user)

        then:
        thrown(BadRequestException)
    }

    def "Add BaseUrl to user - empty baseUrl on tenant"() {
        given:
        User user = new User()
        user.nastId = "123"

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        service.addBaseUrlToUser(1, user)

        then:
        1 * this.tenantService.updateTenant(_)

    }

    def "addUser keeps specified region for user"() {
        given:
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("notthesame", "cloud", true)
        userDao.isUsernameUnique(_) >> true

        when:
        service.addUser(createUser("region", true, "id", "email@email.com", 1, "nast"))

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.region[0].equals("region"))
        }
    }

    def "addUser adds region to user if not present"() {
        given:
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("region", "cloud", true)
        userDao.isUsernameUnique(_) >> true

        when:
        service.addUser(createUser(null, true, "id", "email@email.com", 1, "nast"))

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.region[0].equals("region"))
        }
    }

    def "checkAndGetUserByName gets user"() {
        when:
        service.checkAndGetUserByName("username")

        then:
        1 * userDao.getUserByUsername("username") >> entityFactory.createUser()
    }

    def "checkAndGetUserByName throws NotFoundException if the user does not exist"() {
        when:
        service.checkAndGetUserByName("username")

        then:
        thrown(NotFoundException)
    }

    def "a user is returned by getUserByUsernameForAuthentication"() {
        when:
        def user = service.getUserByUsernameForAuthentication("username")

        then:
        1 * userDao.getUserByUsername("username") >> entityFactory.createUser()
        user != null
    }

    def "NotAuthenticatedException is thrown if the user is not found"() {
        when:
        service.getUserByUsernameForAuthentication("username")

        then:
        thrown(NotAuthenticatedException)
    }


    def "GET - user by tenant id - size 1" (){
        given:
        setupMocks()
        String[] tenantIds = ["1","2"]
        tenantDao.getAllTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        Users users = new Users()
        users.users = new ArrayList<User>();
        users.getUsers().add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users

        when:
        User user = this.service.getUserByTenantId("1")

        then:
        user != null;
        user.getMossoId() == 1
    }

    def "GET - user by tenant id - size > 1 - isUserAdmin=true" (){
        given:
        setupMocks()
        String[] tenantIds = ["1","2"]
        tenantDao.getAllTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        Users users = new Users()
        users.users = new ArrayList<User>();
        users.getUsers().add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.getUsers().add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users
        authorizationService.hasUserAdminRole(_) >>> [false] >> true

        when:
        User user = this.service.getUserByTenantId("1")

        then:
        user != null;
        user.getMossoId() == 1
        user.id == "2"
    }

    def "GET - user by tenant id - size > 1 - isUserAdmin=false" (){
        given:
        setupMocks()
        String[] tenantIds = ["1","2"]
        tenantDao.getAllTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        Users users = new Users()
        users.users = new ArrayList<User>();
        users.getUsers().add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.getUsers().add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users
        authorizationService.hasUserAdminRole(_) >> false

        when:
        User user = this.service.getUserByTenantId("1")

        then:
        user == null;
    }

    def "GET - users by tenant id - size > 1" (){
        given:
        setupMocks()
        String[] tenantIds = ["1","2"]
        tenantDao.getAllTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        Users users = new Users()
        users.users = new ArrayList<User>();
        users.getUsers().add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.getUsers().add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users
        authorizationService.hasUserAdminRole(_) >> true

        when:
        Users results = this.service.getUsersByTenantId("1")

        then:
        results.users != null;
        results.users.size() == 2
        results.users.get(0).mossoId == 1
    }


    def "getUsersWithRole calls tenantDao.getMultipleTenantRoles"() {
        given:
        def stringPaginator = createStringPaginatorContext()

        when:
        service.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
    }

    def "getUsersWithRole calls getUserById"() {
        given:
        def stringPaginator = createStringPaginatorContext().with {
            it.valueList = [ "one", "two", "three" ].asList()
            return it
        }

        when:
        service.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
        3 * userDao.getUserById(_ as String)
    }

    def "getUsersWithRole calls first setUserContext"() {
        given:
        def listOfUsers = [ entityFactory.createUser() ].asList()
        def listofUserIds = [ "1" ].asList()
        def stringPaginator = createStringPaginatorContext().with {
            it.valueList = listofUserIds
            return it
        }

        when:
        def userContext = service.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
        userDao.getUserById(_ as String) >>> [ entityFactory.createUser() ]

        userContext.getValueList().equals(listOfUsers)
        userContext.getLimit() == stringPaginator.getLimit()
        userContext.getOffset() == stringPaginator.getOffset()
        userContext.getTotalRecords() == stringPaginator.getTotalRecords()
    }

    def "getUsersWithRole calls second setUserContext"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with { it.roleRsId = sharedRandom; return it}
        tenantService.getGlobalRolesForUser(_) >> [tenantRole].asList()

        when:
        def userContext = service.getUsersWithRole(domainAndRoleFilters, sharedRandom, 0, 10)

        then:
        userDao.getAllUsersNoLimit(domainAndRoleFilters) >> entityFactory.createUsers(userList)

        userContext.getValueList().equals(userList)
        userContext.getLimit() == 10
        userContext.getOffset() == 0
        userContext.getTotalRecords() == 5

    }

    def "getUsersWithRole calls getAllUsersNoLimit"() {
        given:
        tenantService.getGlobalRolesForUser(_) >> [].asList()

        when:
        service.getUsersWithRole(domainAndRoleFilters, sharedRandom, 0, 10)

        then:
        userDao.getAllUsersNoLimit(domainAndRoleFilters) >> entityFactory.createUsers(userList)
    }

    def "getAllUsersNoLimit calls userDao getAllUsersNoLimit"() {
        given:

        when:
        service.getAllUsersNoLimit(domainAndRoleFilters)
        service.getAllUsers(roleFilter)

        then:
        1 * userDao.getAllUsersNoLimit(_ as FilterParam[])
    }

    def "getSubList throws BadRequest"() {
        when:
        service.getSubList(userList, 10, 5)

        then:
        thrown BadRequestException
    }

    def "getSubList returns same list"() {
        when:
        def returnedListOne = service.getSubList(userList, 0, 5)
        def returnedListTwo = service.getSubList(userList, 0, 10)

        then:
        returnedListOne.equals(userList)
        returnedListTwo.equals(userList)
    }

    def "getSubList returns subList"() {
        when:
        def listOne = service.getSubList(userList, 1, 2)
        def listTwo = service.getSubList(userList, 0, 2)
        def listThree = service.getSubList(userList, 3, 3)
        def listFour = service.getSubList(userList, 3, 6)
        def listFive = service.getSubList(userList, 0, 7)

        then:
        listOne.equals(userList.subList(1, 3))
        listTwo.equals(userList.subList(0, 2))
        listThree.equals(userList.subList(3, userList.size()))
        listFour.equals(userList.subList(3, userList.size()))
        listFive.equals(userList)
    }

    def "filterUsersForRole no Users have role"() {
        given:
        def list = new ArrayList<User>()
        def roleId = String.format("%s%s", sharedRandom, "5")
        tenantService.getGlobalRolesForUser(_) >> new ArrayList<TenantRole>();

        when:
        service.filterUsersForRole(users, list, roleId)

        then:
        list.empty
    }

    def "filterUsersForRole all Users have role"() {
        given:
        def list = [].asList()
        tenantService.getGlobalRolesForUser(_) >> [].asList()

        when:
        service.filterUsersForRole(users, list, sharedRandom)

        then:
        list.equals(users.users)
    }

    def "filterUsersForRole some Users have role"() {
        given:
        def role = entityFactory.createTenantRole().with {
            it.roleRsId = sharedRandom
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ role ].asList()
            it.username = "userWithRole"
            return it
        }
        def users = entityFactory.createUsers().with {
            it.users = [ user, entityFactory.createUser() ].asList()
            return it
        }
        def usersWithRole = [].asList()

        tenantService.getGlobalRolesForUser(_ as User) >>> [ [].asList(), [].asList() ]

        when:
        service.filterUsersForRole(users, usersWithRole, sharedRandom)

        then:
        usersWithRole.size() == 1
        usersWithRole.get(0) == user
    }

    def "getUsersWeight gets users tenantRoles"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with { it.roleRsId = "3"; return it }
        def user = entityFactory.createUser().with {
            it.username = "username"
            it.roles = [ tenantRole ].asList()
            return it
        }

        when:
        service.getUserWeight(user, "applicationId")

        then:
        1 * tenantRoleDao.getTenantRolesForUser(_, _) >> new ArrayList<TenantRole>()
    }

    def "getUsersWeight finds identity:serviceAdminRole role weight for user"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "0"
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ tenantRole ].asList()
            return it
        }
        def applicationRole = entityFactory.createClientRole("identity:service-admin").with { it.rsWeight = 0; return it }
        def tenantRoles = [ tenantRole ].asList()

        tenantRoleDao.getTenantRolesForUser(user, "applicationId") >> tenantRoles
        applicationRoleDao.getClientRole("0") >> applicationRole

        when:
        def weight = service.getUserWeight(user, "applicationId")

        then:
        weight == 0
    }

    def "getUsersWeight finds identity:admin role weight for user"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "0"
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ tenantRole ].asList()
            return it
        }
        def applicationRole = entityFactory.createClientRole("identity:admin").with { it.rsWeight = 100; return it }
        def tenantRoles = [ tenantRole ].asList()

        tenantRoleDao.getTenantRolesForUser(user, "applicationId") >> tenantRoles
        applicationRoleDao.getClientRole("0") >> applicationRole

        when:
        def weight = service.getUserWeight(user, "applicationId")

        then:
        weight == 100
    }

    def "getUsersWeight finds identity:user-admin role weight for user"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "0"
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ tenantRole ].asList()
            return it
        }
        def applicationRole = entityFactory.createClientRole("identity:user-admin").with { it.rsWeight = 1000; return it }
        def tenantRoles = [ tenantRole ].asList()

        tenantRoleDao.getTenantRolesForUser(user, "applicationId") >> tenantRoles
        applicationRoleDao.getClientRole("0") >> applicationRole

        when:
        def weight = service.getUserWeight(user, "applicationId")

        then:
        weight == 1000
    }

    def "getUsersWeight finds identity:default role weight for user"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "0"
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ tenantRole ].asList()
            return it
        }
        def applicationRole = entityFactory.createClientRole("identity:user").with { it.rsWeight = 2000; return it }
        def tenantRoles = [ tenantRole ].asList()

        tenantRoleDao.getTenantRolesForUser(user, "applicationId") >> tenantRoles
        applicationRoleDao.getClientRole("0") >> applicationRole

        when:
        def weight = service.getUserWeight(user, "applicationId")

        then:
        weight == 2000
    }

    def "getUsersWeight finds generic role weight for user"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "0"
            return it
        }
        def user = entityFactory.createUser().with {
            it.roles = [ tenantRole ].asList()
            return it
        }
        def applicationRole = entityFactory.createClientRole("role").with { it.rsWeight = 2000; return it }
        def tenantRoles = [ tenantRole ].asList()

        tenantRoleDao.getTenantRolesForUser(user, "applicationId") >> tenantRoles
        applicationRoleDao.getClientRole("0") >> applicationRole
        config.getInt("cloudAuth.defaultUser.rsWeight") >> 2000

        when:
        def weight = service.getUserWeight(user, "applicationId")

        then:
        weight == 2000
    }

    def "GET - Users by tenantId - verify that userId is not null"(){
        given:
        def tenantRole = entityFactory.createTenantRole()
        tenantRole.userId = null
        tenantDao.getAllTenantRolesForTenant(_) >> [tenantRole].asList()


        when:
        service.getUsersByTenantId("1")

        then:
        1 * tenantService.addUserIdToTenantRole(_) >> {TenantRole tenantRole1 ->
            tenantRole1.userId = "1"
        }
    }

    def createStringPaginatorContext() {
        return new PaginatorContext<String>().with {
            it.limit = 25
            it.offset = 0
            it.totalRecords = 0
            it.searchResultEntryList = [].asList()
            it.valueList = [].asList()
            return it
        }
    }

    def createUser(String region, boolean enabled, String id, String email, int mossoId, String nastId) {
        new User().with {
            it.region = region
            it.enabled = enabled
            it.id = id
            it.email = email
            it.mossoId = mossoId
            it.nastId = nastId
            return it
        }
    }

    def createRegionEntity(String name, String cloud, boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = isDefault
            return it
        }
    }

    def createTenantRole(String name, String userId, String[] tenantIds) {
        new TenantRole().with {
            it.name = name
            it.userId = userId
            it.tenantIds = tenantIds
            return it
        }
    }

    def setupMocks() {
        mockEndpointService(service)
        mockTenantService(service)
        mockCloudRegionService(service)
        mockUserDao(service)
        mockConfiguration(service)
        mockValidator(service)
        mockScopeAccessDao(service)
        mockScopeAccessService(service)
        mockTenantDao(service)
        mockAuthorizationService(service)
    }
}
