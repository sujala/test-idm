package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.FilterParam
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.UserDisabledException
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
        users = userList

        roleFilter = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom)]
        domainAndRoleFilters = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom),
                                new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, sharedRandom)]
    }

    def setup() {
        mockPasswordComplexityService(service)
        mockScopeAccessService(service)
        mockAuthDao(service)
        mockApplicationService(service)
        mockConfiguration(service)
        mockUserDao(service)
        mockRackerDao(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockAuthorizationService(service)
        mockCloudRegionService(service)
        mockValidator(service)
        mockDomainService(service)
        mockPropertiesService(service)
        mockCryptHelper(service)
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
        tenant.getBaseUrlIds().add("2")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        service.addBaseUrlToUser("1", user)

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
        tenant.getBaseUrlIds().add("1")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        service.addBaseUrlToUser("1", user)

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
        service.addBaseUrlToUser("1", user)

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

    def "addUser uses nextid to get the userid"() {
        given:
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("notthesame", "cloud", true)
        userDao.isUsernameUnique(_) >> true

        when:
        service.addUser(createUser("region", true, null, "email@email.com", 1, "nast"))

        then:
        1 * userDao.getNextUserId() >> "100"
        1 * userDao.addUser(_) >> { User user ->
            assert(user.id != null)
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
        String[] tenantIds = ["1","2"]
        tenantService.getTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        def users = [].asList()
        users.add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users

        when:
        User user = this.service.getUserByTenantId("1")

        then:
        user != null;
        user.getMossoId() == 1
    }

    def "GET - user by tenant id - size > 1 - isUserAdmin=true" (){
        given:
        String[] tenantIds = ["1","2"]
        tenantService.getTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        def users = [].asList()
        users.add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
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
        String[] tenantIds = ["1","2"]
        tenantService.getTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        def users = [].asList()
        users.add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users
        authorizationService.hasUserAdminRole(_) >> false

        when:
        User user = this.service.getUserByTenantId("1")

        then:
        user != null;
    }

    def "GET - users by tenant id - size > 1" (){
        given:
        String[] tenantIds = ["1","2"]
        tenantService.getTenantRolesForTenant(_) >> [createTenantRole("someTenant", "1", tenantIds)].asList()
        def users = [].asList()
        users.add(createUser("ORD", true, "1", "someEmail", 1, "nast"))
        users.add(createUser("ORD", true, "2", "someEmail", 1, "nast"))
        userDao.getUsers(_) >> users
        authorizationService.hasUserAdminRole(_) >> true

        when:
        def results = this.service.getUsersByTenantId("1")

        then:
        results != null;
        results.size() == 2
        results.get(0).mossoId == 1
    }


    def "getUsersWithRole calls tenantService.getMultipleTenantRoles"() {
        given:
        def stringPaginator = createStringPaginatorContext()

        when:
        service.getUsersWithRole(sharedRandom, 0, 10)

        then:
        userDao.getUsers(_) >> [].asList()
        tenantService.getIdsForUsersWithTenantRole(sharedRandom, 0, 10) >> stringPaginator
    }

    def "getUsersWithRole calls getUserById"() {
        given:
        def stringPaginator = createStringPaginatorContext().with {
            it.valueList = [ "one", "two", "three" ].asList()
            return it
        }

        when:
        service.getUsersWithRole(sharedRandom, 0, 10)

        then:
        userDao.getUsers(_) >> [].asList()
        tenantService.getIdsForUsersWithTenantRole(sharedRandom, 0, 10) >> stringPaginator
    }

    def "getUsersWithRole calls getAllUsersNoLimit"() {
        given:
        tenantService.getGlobalRolesForUser(_) >> [].asList()

        when:
        service.getUsersWithRole(sharedRandom, 0, 10)

        then:
        userDao.getUsers(_) >> userList
    }

    def "filterUsersForRole no Users have role"() {
        given:
        def list = new ArrayList<User>()
        def roleId = String.format("%s%s", sharedRandom, "5")
        tenantService.getGlobalRolesForUser(_) >> new ArrayList<TenantRole>();

        when:
        service.filterUsersForRole(users, roleId)

        then:
        list.empty
    }

    def "filterUsersForRole all Users have role"() {
        given:
        tenantService.getGlobalRolesForUser(_) >> [].asList()

        when:
        def list = service.filterUsersForRole(users, sharedRandom)

        then:
        list.equals(users)
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
        def users = [ user, entityFactory.createUser() ].asList()

        tenantService.getGlobalRolesForUser(_ as User) >>> [ [].asList(), [].asList() ]

        when:
        def usersWithRole = service.filterUsersForRole(users, sharedRandom)

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
        1 * tenantService.getGlobalRolesForUser(_, _) >> new ArrayList<TenantRole>()
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

        tenantService.getGlobalRolesForUser(user, "applicationId") >> tenantRoles
        applicationService.getClientRoleById("0") >> applicationRole

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

        tenantService.getGlobalRolesForUser(user, "applicationId") >> tenantRoles
        applicationService.getClientRoleById("0") >> applicationRole

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

        tenantService.getGlobalRolesForUser(user, "applicationId") >> tenantRoles
        applicationService.getClientRoleById("0") >> applicationRole

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

        tenantService.getGlobalRolesForUser(user, "applicationId") >> tenantRoles
        applicationService.getClientRoleById("0") >> applicationRole

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

        tenantService.getGlobalRolesForUser(user, "applicationId") >> tenantRoles
        applicationService.getClientRoleById("0") >> applicationRole
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
        tenantService.getTenantRolesForTenant(_) >> [tenantRole].asList()


        when:
        service.getUsersByTenantId("1")

        then:
        1 * tenantService.addUserIdToTenantRole(_) >> {TenantRole tenantRole1 ->
            tenantRole1.userId = "1"
        }
    }

    def "updateUser expires tokens for user if the user is set to disabled"() {
        given:
        def currentUser = entityFactory.createUser().with {
            it.enabled = true
            return it
        }
        def user = entityFactory.createUser().with {
            it.enabled = false
            return it
        }

        when:
        service.updateUser(user, false)

        then:
        1 * scopeAccessService.expireAllTokensForUser(_)
        userDao.getUserById(_) >> currentUser
        scopeAccessService.getScopeAccessListByUserId(_) >> [].asList()
    }

    def "checkIfUserIsBeingDisabled test"() {
        when:
        def currentUser = entityFactory.createUser().with {
            it.enabled = currentUserEnabled
            return it
        }
        def user = entityFactory.createUser().with {
            it.enabled = userEnabled
            return it
        }
        def result = service.checkIfUserIsBeingDisabled(currentUser, user)

        then:
        expected == result

        where:
        expected    | currentUserEnabled    | userEnabled
        false       | false                 | false
        false       | false                 | true
        true        | true                  | false
        false       | true                  | true
    }

    def "disableUserAdminSubUsers does if user is not userAdmin"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser().with {
            it.id = "subUserId"
            it.enabled = true
            return it
        }
        def users = [subUser].asList()

        when:
        service.disableUserAdminSubUsers(user)

        then:
        authorizationService.hasUserAdminRole(user) >> false
        userDao.getUsersByDomain(_) >> users
        0 * userDao.updateUser(_, _)
    }

    def "disableUserAdminSubUsers disables sub users that are enabled"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser().with {
            it.id = "subUserId"
            return it
        }
        def users = [subUser].asList()
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.disableUserAdminSubUsers(user)

        then:
        userDao.getUsersByDomain(_) >> users
        authorizationService.hasUserAdminRole(user) >> true
        1 * userDao.updateUser(subUser, false) >> { User subUser1, boolean hasSelfUpdatePassword ->
            assert (subUser1.enabled == false)
        }
    }

    def "disableUserAdminSubUsers does not disable sub users that are disabled"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser().with {
            it.id = "subUserId"
            it.enabled = false
            return it
        }
        def users = [subUser].asList()
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.disableUserAdminSubUsers(user)

        then:
        authorizationService.hasUserAdminRole(user) >> true
        userDao.getUsersByDomain(_) >> users
        0 * userDao.updateUser(_, _)
    }

    def "disableUserAdminSubUsers disables sub users tokens"() {
        given:
        def user = entityFactory.createUser()
        def subUser = entityFactory.createUser().with {
            it.id = "subUserId"
            return it
        }
        def users = [subUser].asList()
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.disableUserAdminSubUsers(user)

        then:
        userDao.getUsersByDomain(_) >> users
        authorizationService.hasUserAdminRole(user) >> true
        1 * scopeAccessService.expireAllTokensForUser(subUser.getUsername());
    }

    def "updateUser disables sub users that are enabled when user is disabled"() {
        given:
        def user = entityFactory.createUser().with {
            it.enabled = false
            return it
        }
        def currentUser = entityFactory.createUser()
        def subUser = entityFactory.createUser().with {
            it.id = "subUserId"
            it.username = "subUserUsername"
            return it
        }
        def users = [subUser].asList()
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.updateUser(user, false)

        then:
        userDao.getUserById(_) >> currentUser
        userDao.getUsersByDomain(_) >> users
        1 * authorizationService.hasUserAdminRole(currentUser) >> true
        scopeAccessService.getScopeAccessListByUserId(_) >> [].asList()
        1 * userDao.updateUser(subUser, false) >> { User subUser1, boolean hasSelfUpdatePassword ->
            assert (subUser1.enabled == false)
        }
        1 * scopeAccessService.expireAllTokensForUser(user.getUsername());
        1 * scopeAccessService.expireAllTokensForUser(subUser.getUsername());
    }

    def "uses DAO to get users in domain with domainId"() {
        when:
        service.getUsersWithDomain("domainId")

        then:
        1 * userDao.getUsersByDomain("domainId")
    }

    def "disableUserAdminSubUsers verifies that user-admin is last user-admin of the domain"() {
        given:
        def userAdmin = entityFactory.createUser().with {
            it.username = "userAdmin"
            return it
        }
        authorizationService.hasUserAdminRole(userAdmin) >> true

        when:
        service.disableUserAdminSubUsers(userAdmin)

        then:
        1 * domainService.getEnabledDomainAdmins(userAdmin.getDomainId()) >> [ entityFactory.createUser() ].asList()
        0 * userDao.getUsersByDomain(userAdmin.getDomainId())
    }

    def "disableUserAdmin disables subUsers if last admin of domain"() {
        given:
        def userAdmin = entityFactory.createUser().with {
            it.username = "userAdmoin"
            it.id = "1"
            return it
        }
        def subUser = entityFactory.createUser().with {
            it.username = "subUser"
            it.id = "2"
            return it
        }
        def users = [subUser].asList()
        authorizationService.hasUserAdminRole(userAdmin) >> true
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.disableUserAdminSubUsers(userAdmin)

        then:
        1 * userDao.getUsersByDomain(_) >> users
        1 * userDao.updateUser(_, _)
    }

    def "when getting racker from scope access, return racker if enabled flag is missing"() {
        given:
        def racker = entityFactory.createRacker().with {
            it.enabled = null
            return it
        }

        when:
        def result = service.getUserByScopeAccess(createRackerScopeAcccss())

        then:
        rackerDao.getRackerByRackerId(_) >> racker
        result == racker

        then:
        notThrown(NotFoundException)
    }

    def "callig getUsersByEmail returns the user"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.getUsersByEmail("email@email.com")

        then:
        1 * userDao.getUsersByEmail(_) >> [user].asList()

        then:
        result.get(0).username == user.username
        result.get(0).email == user.email
    }

    def "reEncryptUsers re-encrypts users"() {
        given:
        def userId = "userId"
        User user = entityFactory.createUser().with {
            it.id = userId
            it
        }
        PaginatorContext<User> context1 = new PaginatorContext<>();
        context1.totalRecords = 1;
        context1.valueList = [user].asList()
        PaginatorContext<User> context2 = new PaginatorContext<>();
        context2.totalRecords = 1;
        context2.valueList = [].asList()

        when:
        service.reEncryptUsers()

        then:
        2 * userDao.getUsersToReEncrypt(_, _) >>> [context1, context2]
        1 * userDao.updateUserEncryption(userId)
    }

    def "Create User sets salt and encryption version"(){
        given:
        def encryptionVersion = "1"
        def salt = "a1 b2"

        when:
        service.addUser(entityFactory.createUser())

        then:
        1 * propertiesService.getValue(_) >> encryptionVersion
        1 * cryptHelper.generateSalt() >> salt
        1 * userDao.isUsernameUnique(_) >> true
        1 * userDao.addUser(_) >> { User arg1 ->
            assert(arg1.encryptionVersion == encryptionVersion)
            assert(arg1.salt == salt)
        }

    }

    def "authenticate validates user is enabled"() {
        given:
        User user = entityFactory.createUser().with {
            it.enabled = false
            it
        }
        UserAuthenticationResult authenticated = new UserAuthenticationResult(user, true)
        userDao.authenticate(_,_) >> authenticated

        when:
        service.authenticate("username", "password")

        then:
        thrown(UserDisabledException)
    }

    def "authenticateWithApiKey validates user is enabled"() {
        given:
        User user = entityFactory.createUser().with {
            it.enabled = false
            it
        }
        UserAuthenticationResult authenticated = new UserAuthenticationResult(user, true)
        userDao.authenticateByAPIKey(_,_) >> authenticated

        when:
        service.authenticateWithApiKey("username", "apiKey")

        then:
        thrown(UserDisabledException)
    }

    def "authenticate validates domain is enabled"() {
        given:
        User user = entityFactory.createUser()
        UserAuthenticationResult authenticated = new UserAuthenticationResult(user, true)
        Domain domain = entityFactory.createDomain().with {
            it.enabled = false
            it
        }
        domainService.getDomain(_) >> domain
        userDao.authenticate(_,_) >> authenticated

        when:
        service.authenticate("username", "password")

        then:
        thrown(UserDisabledException)
    }

    def "authenticateWithApiKey validates domain is enabled"() {
        given:
        User user = entityFactory.createUser()
        UserAuthenticationResult authenticated = new UserAuthenticationResult(user, true)
        Domain domain = entityFactory.createDomain().with {
            it.enabled = false
            it
        }
        domainService.getDomain(_) >> domain
        userDao.authenticateByAPIKey(_,_) >> authenticated

        when:
        service.authenticateWithApiKey("username", "password")

        then:
        thrown(UserDisabledException)
    }

    def "authenticate works if user and domain are enabled"() {
        given:
        User user = entityFactory.createUser()
        UserAuthenticationResult authenticated = new UserAuthenticationResult(user, true)
        Domain domain = entityFactory.createDomain()
        domainService.getDomain(_) >> domain
        userDao.authenticate(_,_) >> authenticated

        when:
        service.authenticate("username", "password")

        then:
        notThrown(UserDisabledException)
    }

    def "getAllEnabledUsersPaged gets the enabled users"() {
        given:
        PaginatorContext<User> context = new PaginatorContext<>()

        when:
        def result = service.getAllEnabledUsersPaged(0, 1)

        then:
        1 * userDao.getEnabledUsers(_, _) >> context
        result == context
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
}
