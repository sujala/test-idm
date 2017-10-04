package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.UserDisabledException
import com.rackspace.idm.validation.Validator
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import spock.lang.Shared
import testHelpers.RootServiceTest

import static com.rackspace.idm.Constants.*

class DefaultUserServiceTest extends RootServiceTest {
    @Shared DefaultUserService service
    @Shared FederatedUserDao mockFederatedUserDao
    @Shared Validator mockValidator
    @Shared RoleService mockRoleService

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def FilterParam[] roleFilter
    @Shared def FilterParam[] domainAndRoleFilters
    @Shared def userList
    @Shared def userIdList
    @Shared def users
    @Shared def domainId = "domainId"

    @Shared serviceAdminRole
    @Shared identityAdminRole
    @Shared userAdminRole
    @Shared userManageRole
    @Shared defaultRole
    @Shared computeDefaultRole
    @Shared objectStoreRole

    def setupSpec(){
        service = new DefaultUserService()
        identityConfig = new IdentityConfig(Mock(Configuration), Mock(Configuration))
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

        serviceAdminRole = entityFactory.createClientRole().with{
            it.id = "serviceAdminRoleId"
            it.name = "identity:service-admin"
            return it
        }

        identityAdminRole = entityFactory.createClientRole().with{
            it.id = "identityAdminRoleId"
            it.name = "identity:admin"
            return it
        }

        userAdminRole = entityFactory.createClientRole().with{
            it.id = "userAdminRoleId"
            it.name = "identity:user-admin"
            return it
        }

        userManageRole = entityFactory.createClientRole().with{
            it.id = "userManageRoleId"
            it.name = "identity:user-manage"
            return it
        }

        defaultRole = entityFactory.createClientRole().with{
            it.id = "defaultRoleId"
            it.name = "identity:default"
            return it
        }

        computeDefaultRole = entityFactory.createClientRole().with{
            it.id = "computeDefaultRoleId"
            it.name = "compute:default"
            return it
        }

        objectStoreRole = entityFactory.createClientRole().with{
            it.id = "objectStoreRoleId"
            it.name = "object-store:default"
            return it
        }
    }

    def setup() {
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
        mockRoleService(service);
        mockFederatedUserDao(service)
        mockIdentityUserService(service)
        mockIdentityConfig(service)
        service.authenticationContext = Mock(AuthenticationContext)
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

    def "Add User"() {
        given:
        def domainId = "1234"
        def expectedNastTenantId = service.getNastTenantId(domainId)
        def user = this.createUser(null, true, domainId)
        user.setRoles([entityFactory.createTenantRole("roleName")].asList())
        def mossoBaseUrl = entityFactory.createCloudBaseUrl("mossoBaseUrlId", true)
        mossoBaseUrl.baseUrlType = "MOSSO"
        def nastBaseUrl = entityFactory.createCloudBaseUrl("nastBaseUrlId", true)
        nastBaseUrl.baseUrlType = "NAST"
        def encryptionVersion = "1"
        def salt = "a1 b2"

        domainService.getDomain(domainId) >>> [ null, entityFactory.createDomain() ]
        domainService.getDomainAdmins(domainId) >> [].asList()
        endpointService.getBaseUrlsByBaseUrlType(DefaultUserService.MOSSO_BASE_URL_TYPE) >> [ mossoBaseUrl ].asList()
        endpointService.getBaseUrlsByBaseUrlType(DefaultUserService.NAST_BASE_URL_TYPE) >> [ nastBaseUrl ].asList()
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("DFW", "cloud", true)
        propertiesService.getValue(DefaultUserService.ENCRYPTION_VERSION_ID) >> encryptionVersion
        config.getInt("maxNumberOfUsersInDomain") >> 100
        config.getList("v1defaultMosso") >> Constants.MOSSO_V1_DEF
        config.getList("v1defaultNast") >> Constants.NAST_V1_DEF
        userDao.getUsersByDomain(domainId) >> [].asList()
        userDao.nextUserId >> "nextId"
        mockRoleService.getRoleByName(_) >> entityFactory.createClientRole("role")
        cryptHelper.generateSalt() >> salt

        when:
        service.addUserv11(user)

        then:
        1 * mockValidator.validateUser(user)
        1 * domainService.createNewDomain(domainId)
        1 * tenantService.addTenant({ it.tenantId == domainId; it.baseUrlIds.contains("mossoBaseUrlId")  })
        1 * tenantService.addTenant({ it.tenantId == expectedNastTenantId; it.baseUrlIds.contains("nastBaseUrlId")  })
        1 * domainService.addTenantToDomain(domainId,domainId)
        1 * domainService.addTenantToDomain(expectedNastTenantId, domainId)
        1 * userDao.addUser(user)
        1 * tenantService.addTenantRoleToUser(user, _);
        endpointService.doesBaseUrlBelongToCloudRegion(_) >> true

        user.password != null
        user.userPassword != null
        user.apiKey != null
        user.region != null
        user.nastId == expectedNastTenantId
        user.encryptionVersion == encryptionVersion
        user.salt == salt;
        user.enabled == true
    }

    def "Add UserV20"() {
        given:
        def user = this.createUser(null, true, domainId)
        user.setRoles([entityFactory.createTenantRole("roleName")].asList())
        def mossoBaseUrl = entityFactory.createCloudBaseUrl("mossoBaseUrlId", true)
        def nastBaseUrl = entityFactory.createCloudBaseUrl("nastBaseUrlId", true)
        def encryptionVersion = "1"
        def salt = "a1 b2"

        endpointService.getBaseUrlsByBaseUrlType(DefaultUserService.MOSSO_BASE_URL_TYPE) >> [ mossoBaseUrl ].asList()
        endpointService.getBaseUrlsByBaseUrlType(DefaultUserService.NAST_BASE_URL_TYPE) >> [ nastBaseUrl ].asList()
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("DFW", "cloud", true)
        propertiesService.getValue(DefaultUserService.ENCRYPTION_VERSION_ID) >> encryptionVersion
        config.getInt("maxNumberOfUsersInDomain") >> 100
        userDao.getUsersByDomain(domainId) >> [].asList()
        userDao.nextUserId >> "nextId"
        mockRoleService.getRoleByName(_) >> entityFactory.createClientRole("role")
        cryptHelper.generateSalt() >> salt

        when:
        service.addUserv11(user)

        then:
        1 * mockValidator.validateUser(user)
        1 * domainService.createNewDomain(domainId)
        1 * userDao.addUser(user)
        1 * tenantService.addTenantRoleToUser(user, _);

        user.password != null
        user.userPassword != null
        user.apiKey != null
        user.region != null
        user.nastId == service.getNastTenantId(domainId)
        user.encryptionVersion == encryptionVersion
        user.salt == salt;
        user.enabled == true
    }

    def "Add User does not set default password if specified"() {
        given:
        minimumMocksForAddUser()
        def user = createUser(null, true, "id", "email@email.com", 1, "nast");
        user.password = "mypassword"

        when:
        service.addUserv11(user)

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.password[0].equals("mypassword"))
        }
    }

    def "Add User does not set default region if specified"() {
        given:
        minimumMocksForAddUser()

        when:
        service.addUserv11(createUser("region", true, "id", "email@email.com", 1, "nast"))

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.region[0].equals("region"))
        }
    }

    def "Add User does not set default api key if specified"() {
        given:
        minimumMocksForAddUser()
        def user = createUser("region", true, "id", "email@email.com", 1, "nast")
        user.apiKey = "apiKey"

        when:
        service.addUserv11(user)

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.apiKey[0].equals("apiKey"))
        }
    }

    def "Add User prohibited if max number of users in domain is exceeded"() {
        given:
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("region", "cloud", true)
        config.getInt("maxNumberOfUsersInDomain") >> 2
        userDao.getUsersByDomain(_) >> [ new User(), new User(), new User()].asList()
        def user = createUser("region", true, "id", "email@email.com", 1, "nast")
        user.domainId = "domainId"

        when:
        service.addUserv11(user)

        then:
        thrown(BadRequestException)
    }

    def "Add User does not create domain if it already exists"() {
        given:
        minimumMocksForAddUser()
        def user = this.createUser(null, true, domainId)

        domainService.getDomain(domainId) >> entityFactory.createDomain()

        when:
        service.addUserv11(user)

        then:
        0 * domainService.createNewDomain(domainId)
    }

    def "Add User does not create default domain tenants if domain already exists with admins"() {
        given:
        def user = this.createUser(null, true, domainId)
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("region", "cloud", true)
        userDao.getUsersByDomain(_) >> [ new User(), new User(), new User()].asList()
        config.getInt("maxNumberOfUsersInDomain") >> 100
        domainService.getDomain(domainId) >> entityFactory.createDomain()
        domainService.getDomainAdmins(domainId) >> [new User()].asList()

        when:
        service.addUserv11(user)

        then:
        0 * tenantService.addTenant(_)
        0 * domainService.addTenantToDomain(_,_)
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
        tenantService.getIdsForUsersWithTenantRole(_) >> [].asList()
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
        tenantService.getIdsForUsersWithTenantRole(_) >> [].asList()
    }

    def "getUsersWithRole calls getAllUsersNoLimit"() {
        given:
        tenantService.getGlobalRolesForUser(_) >> [].asList()

        when:
        service.getUsersWithRole(sharedRandom, 0, 10)

        then:
        userDao.getUsers(_) >> userList
        tenantService.getIdsForUsersWithTenantRole(_) >> [].asList()
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
        service.updateUser(user)

        then:
        1 * scopeAccessService.expireAllTokensForUser(_)
        userDao.getUserById(_) >> currentUser
        scopeAccessService.getScopeAccessListByUserId(_) >> [].asList()
    }

    def "updateUser expires tokens if password attribute is populated"() {
        given:
        def currentUser = entityFactory.createUser().with {
            it.enabled = true
            return it
        }
        def user = entityFactory.createUser().with {
            it.enabled = true
            it.password = "password"
            return it
        }

        when:
        service.updateUser(user)

        then:
        1 * scopeAccessService.expireAllTokensForUser(_)
        identityUserService.getProvisionedUserByIdWithPwdHis(_) >> currentUser
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
        identityUserService.getEndUsersByDomainId(_) >> users

        when:
        service.disableUserAdminSubUsers(user)

        then:
        userDao.getUsersByDomain(_) >> users
        authorizationService.hasUserAdminRole(_) >> true
        1 * userDao.updateUser(subUser) >> { User subUser1 ->
            assert (subUser1.enabled == false)
        }
    }

    def "disableUserAdminSubUsers expires all federated sub-users tokens"() {
        given:
        def user = entityFactory.createUser()
        def federatedUserId = "fedId"
        def federatedSubUser = entityFactory.createFederatedUser("federatedUsername", "idpName").with {
            it.id = federatedUserId
            it
        }
        def users = [].asList()
        domainService.getEnabledDomainAdmins(_) >> [].asList()
        identityUserService.getEndUsersByDomainId(_) >> users

        when:
        service.disableUserAdminSubUsers(user)

        then:
        authorizationService.hasUserAdminRole(_) >> true
        mockFederatedUserDao.getUsersByDomainId(_) >> [federatedSubUser].asList()
        scopeAccessService.expireAllTokensForUserById(federatedUserId)
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
        identityUserService.getEndUsersByDomainId(_) >> users

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
        identityUserService.getEndUsersByDomainId(_) >> users

        when:
        service.disableUserAdminSubUsers(user)

        then:
        userDao.getUsersByDomain(_) >> users
        authorizationService.hasUserAdminRole(_) >> true
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
        identityUserService.getEndUsersByDomainId(_) >> users

        when:
        service.updateUser(user)

        then:
        userDao.getUserById(_) >> currentUser
        userDao.getUsersByDomain(_) >> users
        1 * authorizationService.hasUserAdminRole(_) >> true
        scopeAccessService.getScopeAccessListByUserId(_) >> [].asList()
        1 * userDao.updateUser(subUser) >> { User subUser1 ->
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
        authorizationService.hasUserAdminRole(_) >> true

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
        authorizationService.hasUserAdminRole(_) >> true
        domainService.getEnabledDomainAdmins(_) >> [].asList()

        when:
        service.disableUserAdminSubUsers(userAdmin)

        then:
        1 * identityUserService.getEndUsersByDomainId(_) >> users
        1 * userDao.updateUser(_)
    }

    def "calling getUsersByEmail returns the user"() {
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

    def "validateUserIsEnabled throws UserDisabledException is user is disabled"() {
        given:
        User user = entityFactory.createUser().with {
            it.enabled = false
            it
        }
        Domain domain = entityFactory.createDomain()
        domainService.getDomain(_) >> domain

        when:
        service.validateUserIsEnabled(user)

        then:
        thrown(UserDisabledException)
    }

    def "validateUserIsEnabled throws UserDisabledException is domain is disabled"() {
        given:
        User user = entityFactory.createUser().with {
            it.enabled = true
            it
        }
        Domain domain = entityFactory.createDomain().with {
            it.enabled = false
            it
        }
        domainService.getDomain(_) >> domain

        when:
        service.validateUserIsEnabled(user)

        then:
        thrown(UserDisabledException)
    }

    def "validateUserIsEnabled does not throw UserDisabledException if user and domain is enabled"() {
        given:
        User user = entityFactory.createUser().with {
            it.enabled = true
            it
        }
        Domain domain = entityFactory.createDomain().with {
            it.enabled = true
            it
        }
        domainService.getDomain(_) >> domain

        when:
        service.validateUserIsEnabled(user)

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

    def "get federated user from federated token"() {
        given:
        def user = entityFactory.createFederatedUser()
        def federatedToken = entityFactory.createFederatedToken(user)
        mockFederatedUserDao(service)

        when:
        def result = service.getUserByScopeAccess(federatedToken)

        then:
        mockFederatedUserDao.getUserByToken(federatedToken) >> user
        result == user
        notThrown(NotFoundException)
    }

    def "Nast tenantId uses formula <prefix><domainId> where prefix is a config property"() {
        staticConfig.getNastTenantPrefix() >> prefix
        def domain = "abcd"

        expect:
        service.getNastTenantId(domain) == prefix+domain

        where:
        prefix << [RandomStringUtils.randomAscii(10), RandomStringUtils.randomAscii(10), ""]
    }

    def "getUsersByTenantId: Does not update or return user group tenant roles"() {
        def tenantId = "tenantId"
        TenantRole u1 = new TenantRole().with {
            it.roleRsId = "u1"
            it.uniqueId = "rsid=somewhere" + LdapRepository.USERS_BASE_DN
            it.userId = "someuse"
            it
        }
        TenantRole f1 = new TenantRole().with {
            it.roleRsId = "f1"
            it.uniqueId = "rsid=somewhere" + LdapRepository.EXTERNAL_PROVIDERS_BASE_DN
            it
        }
        TenantRole ug1 = new TenantRole().with {
            it.roleRsId = "ug1"
            it.uniqueId = "rsid=somewhere" + com.rackspace.idm.modules.usergroups.Constants.USER_GROUP_BASE_DN
            it
        }
        def tenantRoleList = [u1,ug1,f1]
        def f1UpdatedId = "updatedId"

        when:
        service.getUsersByTenantId(tenantId)

        then:
        1 * tenantService.getTenantRolesForTenant(tenantId) >> tenantRoleList

        // End user tenant roles without a userId already set are still updated to set the userId value
        1 * tenantService.addUserIdToTenantRole(f1) >> {args ->
            args[0].userId = f1UpdatedId // Mimic real call which will update the tenant role with the id
        }

        // Neither end user tenant roles with a userId nor user tenant roles are updated
        0 * tenantService.addUserIdToTenantRole(u1)
        0 * tenantService.addUserIdToTenantRole(ug1)

        // Verify the get users call does NOT include the user group, but does include both end users
        1 * userDao.getUsers(_) >> {args ->
            List<String> userIds = args[0]
            assert userIds.size() == 2
            assert userIds.find {it == u1.userId} != null
            assert userIds.find {it == f1UpdatedId} != null
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

    def mockFederatedUserDao(service) {
        mockFederatedUserDao = Mock()
        service.federatedUserDao = mockFederatedUserDao
    }

    def mockValidator(service) {
        mockValidator = Mock()
        service.validator = mockValidator
    }

    def mockRoleService(service) {
        mockRoleService = Mock()
        service.roleService = mockRoleService
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

    def createUser(String region, boolean enabled, String domainId) {
        new User().with {
            it.region = region
            it.enabled = enabled
            it.domainId = domainId
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

    def minimumMocksForAddUser() {
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("region", "cloud", true)
        domainService.getDomainAdmins(domainId) >> [].asList()
        userDao.getUsersByDomain(_) >> [].asList()
        endpointService.getBaseUrlsByBaseUrlType(_) >> [].asList()
    }

    def mockRoles() {
        mockRoleService.getSuperUserAdminRole() >> serviceAdminRole
        mockRoleService.getIdentityAdminRole() >> identityAdminRole
        mockRoleService.getUserAdminRole() >> userAdminRole
        mockRoleService.getUserManageRole() >> userManageRole
        mockRoleService.getDefaultRole() >> defaultRole
        mockRoleService.getComputeDefaultRole() >> computeDefaultRole
        mockRoleService.getObjectStoreDefaultRole() >> objectStoreRole
    }
}
