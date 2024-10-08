package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.event.identity.user.credential.CredentialTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.atomHopper.CredentialChangeEventData
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.entity.User.UserType
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DomainDefaultException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotAuthenticatedException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.UserDisabledException
import com.rackspace.idm.validation.Validator
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import static com.rackspace.idm.GlobalConstants.*

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
        mockIdentityConfig(service)
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
        mockRackerAuthDao(service)
        mockApplicationService(service)
        mockConfiguration(service)
        mockUserDao(service)
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
        mockTenantAssignmentService(service)
        mockAtomHopperClient(service)
        mockTokenRevocationService(service)
        mockPhonePinService(service)
        mockIdmCommonUtils(service)
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

    @Unroll
    def "addUserV20: creating user-admin calls appropriate services: createUserOneCall - #isCreateUserOneCall"() {
        given:
        def domain = entityFactory.createDomain()
        TenantRole userAdminRole = entityFactory.createTenantRole().with {
            it.name = Constants.IDENTITY_USER_ADMIN_ROLE
            it.roleRsId = Constants.USER_ADMIN_ROLE_ID
            it
        }
        ClientRole userAdminClientRole = entityFactory.createClientRole(Constants.IDENTITY_USER_ADMIN_ROLE)
        def user = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it.roles.add(userAdminRole)
            it
        }

        when:
        service.addUserAdminV20(user, isCreateUserOneCall)

        then:
        1 * domainService.getDomainAdmins(domain.domainId) >> []
        1 * userDao.getUsersByDomain(domain.domainId) >> []
        1 * userDao.addUser(user)
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.CREATE, _);
        1 * domainService.updateDomainUserAdminDN(user)
        if (isCreateUserOneCall) {
            2 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            2 * domainService.getDomain(domain.domainId) >> domain
        } else {
            1 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            1 * domainService.getDomain(domain.domainId) >> domain
        }

        where:
        isCreateUserOneCall << [true, false]
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
        1 * tokenRevocationService.revokeAllTokensForEndUser(currentUser)
        userDao.getUserById(_) >> currentUser
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
        1 * tokenRevocationService.revokeAllTokensForEndUser(_)
        identityUserService.getProvisionedUserByIdWithPwdHis(_) >> currentUser
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
        identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users

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
        identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users

        when:
        service.disableUserAdminSubUsers(user)

        then:
        authorizationService.hasUserAdminRole(_) >> true
        mockFederatedUserDao.getUsersByDomainId(_) >> [federatedSubUser].asList()
        tokenRevocationService.revokeAllTokensForEndUser(federatedSubUser)
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
        identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users

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
        identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users

        when:
        service.disableUserAdminSubUsers(user)

        then:
        userDao.getUsersByDomain(_) >> users
        authorizationService.hasUserAdminRole(_) >> true
        1 * tokenRevocationService.revokeAllTokensForEndUser(subUser)
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
        identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users

        when:
        service.updateUser(user)

        then:
        userDao.getUserById(_) >> currentUser
        userDao.getUsersByDomain(_) >> users
        1 * authorizationService.hasUserAdminRole(_) >> true
        1 * userDao.updateUser(subUser) >> { User subUser1 ->
            assert (subUser1.enabled == false)
        }
        1 * tokenRevocationService.revokeAllTokensForEndUser(currentUser)
        1 * tokenRevocationService.revokeAllTokensForEndUser(subUser)
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
        1 * identityUserService.getEndUsersByDomainId(_, UserType.VERIFIED) >> users
        1 * userDao.updateUser(_)
    }

    def "calling getUsersByEmail returns the user"() {
        given:
        def user = entityFactory.createUser()

        when:
        def result = service.getUsersByEmail("email@email.com", UserType.ALL)

        then:
        1 * userDao.getUsersByEmail(_, UserType.ALL) >> [user].asList()

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

    @Unroll
    def "getUsersByTenantId: Does not update or return non end users tenant roles - baseDN = #baseDN"() {
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
            it.uniqueId = "rsid=somewhere" + baseDN
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

        // Verify the get users call does NOT include non end users, but does include both end users
        1 * userDao.getUsers(_) >> {args ->
            List<String> userIds = args[0]
            assert userIds.size() == 2
            assert userIds.find {it == u1.userId} != null
            assert userIds.find {it == f1UpdatedId} != null
        }

        where:
        baseDN << [com.rackspace.idm.modules.usergroups.Constants.USER_GROUP_BASE_DN, LdapRepository.DELEGATION_AGREEMENT_BASE_DN]
    }

    def "getEnabledUsersByContactId: calls correct dao method"() {
        given:
        def contactId = "contactId"

        when:
        service.getEnabledUsersByContactId(contactId)

        then:
        1 * userDao.getEnabledUsersByContactId(contactId)
    }

    def "getEnabledUsersByContactId: null cases"() {
        when:
        service.getEnabledUsersByContactId(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getPasswordExpiration correctly calculates the expiration time"() {
        given:
        def passwordLastUpdated = new DateTime().minusWeeks(4) // last changed pwd 4 weeks ago
        def passwordPolicyLengthInWeeks = 6
        def user = entityFactory.createUser().with {
            it.passwordLastUpdated = passwordLastUpdated.toDate()
            it
        }
        def domain = entityFactory.createDomain().with {
            // Need to convert to days as that is the highest level of granularity supported by pw policies
            it.passwordPolicy = new PasswordPolicy("P${passwordPolicyLengthInWeeks * 7}D", null)
            it
        }

        when: "get pwd expiration for user w/ a pwd change date and domain w/ a password duration"
        def passwordExpiration = service.getPasswordExpiration(user)

        then: "the correct pwd expiration is returned"
        1 * domainService.getDomain(user.domainId) >> domain
        passwordExpiration == passwordLastUpdated.plus(passwordPolicyLengthInWeeks*7l*24l*60l*60l*1000l)

        when: "get pwd expiration for user w/o a pwd change date and domain w/ a password duration"
        user.passwordLastUpdated = null
        passwordExpiration = service.getPasswordExpiration(user)

        then: "a null pwd expiration is returned"
        1 * domainService.getDomain(user.domainId) >> domain
        passwordExpiration == null

        when: "get pwd expiration for user w/ a pwd change date and w/ a domain w/o a pwd policy duration"
        user.passwordLastUpdated = passwordLastUpdated.toDate()
        domain.passwordPolicy = new PasswordPolicy(null, 6)
        passwordExpiration = service.getPasswordExpiration(user)

        then: "a null pwd expiration is returned"
        1 * domainService.getDomain(user.domainId) >> domain
        passwordExpiration == null

        when: "call with null user"
        0 * domainService.getDomain(user.domainId)
        service.getPasswordExpiration(null)

        then:
        thrown IllegalArgumentException

        when: "call with user and null domain"
        passwordExpiration = service.getPasswordExpiration(user)

        then:
        1 * domainService.getDomain(user.domainId) >> null
        passwordExpiration == null
    }

    def "isPasswordExpired correctly determines if a user's password is expired"() {
        given:
        def passwordPolicyLengthInWeeks = 6
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain().with {
            // Need to convert to days as that is the highest level of granularity supported by pw policies
            it.passwordPolicy = new PasswordPolicy("P${passwordPolicyLengthInWeeks * 7}D", null)
            it
        }

        when: "user w/ password changed date and domain w/ pwd policy duration - pwd not expired"
        user.passwordLastUpdated = new DateTime().minusWeeks(4).toDate() // last changed pwd 4 weeks ago
        1 * domainService.getDomain(user.domainId) >> domain
        boolean isPasswordExpired = service.isPasswordExpired(user)

        then:
        !isPasswordExpired

        when: "user w/ password changed date and domain w/ pwd policy duration - pwd expired"
        user.passwordLastUpdated = new DateTime().minusWeeks(passwordPolicyLengthInWeeks + 1).toDate()
        1 * domainService.getDomain(user.domainId) >> domain
        isPasswordExpired = service.isPasswordExpired(user)

        then:
        isPasswordExpired

        when: "user w/ null password changed date and domain w/ pwd policy duration"
        user.passwordLastUpdated = null
        1 * domainService.getDomain(user.domainId) >> domain
        isPasswordExpired = service.isPasswordExpired(user)

        then:
        isPasswordExpired

        when: "user w/ password changed date and domain w/o pwd policy duration"
        user.passwordLastUpdated = new DateTime().minusWeeks(1000).toDate()
        domain.passwordPolicy = new PasswordPolicy(null, 6)
        1 * domainService.getDomain(user.domainId) >> domain
        isPasswordExpired = service.isPasswordExpired(user)

        then:
        !isPasswordExpired

        when: "user w/ password changed date and domain w/o pwd policy"
        user.passwordLastUpdated = new DateTime().minusWeeks(1000).toDate()
        domain.passwordPolicy = null
        1 * domainService.getDomain(user.domainId) >> domain
        isPasswordExpired = service.isPasswordExpired(user)

        then:
        !isPasswordExpired

        when: "user is null"
        0 * domainService.getDomain(user.domainId)
        service.isPasswordExpired(null)

        then:
        thrown IllegalArgumentException

        when: "domain is null"
        1 * domainService.getDomain(user.domainId) >> null
        service.isPasswordExpired(user)

        then:
        !isPasswordExpired
    }

    def "replaceRoleAssignmentsOnUser: calls correct service"() {
        given:
        def user = entityFactory.createUser()
        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(new TenantAssignment().with {
                it.onRole = "roleId"
                it.forTenants.addAll("tenantId")
                it
            })
            it.tenantAssignments = ta
            it
        }

        def callerType = IdentityUserTypeEnum.USER_ADMIN

        when:
        service.replaceRoleAssignmentsOnUser(user, assignments, callerType)

        then:
        1 * tenantAssignmentService.replaceTenantAssignmentsOnUser(user, assignments.tenantAssignments.tenantAssignment, callerType)
    }

    def "replaceRoleAssignmentsOnUser: Throws IllegalArgumentException if supplied user is invalid"() {
        def roleAssignments = new RoleAssignments()

        when: "user arg is null"
        service.replaceRoleAssignmentsOnUser(null, roleAssignments, IdentityUserTypeEnum.USER_ADMIN)

        then:
        thrown(IllegalArgumentException)

        when: "user arg has no unique id"
        service.replaceRoleAssignmentsOnUser(new User(), roleAssignments, IdentityUserTypeEnum.USER_ADMIN)

        then:
        thrown(IllegalArgumentException)
    }

    def "replaceRoleAssignmentsOnUser: Throws IllegalArgumentException if roleAssignments arg is null"() {
        when:
        service.replaceRoleAssignmentsOnUser(new User().with {it.uniqueId = "uniqueId";it}, null, IdentityUserTypeEnum.USER_ADMIN)

        then:
        thrown(IllegalArgumentException)
    }

    def "replaceRoleAssignmentsOnUser: Throws IllegalArgumentException if allowedRoleAccess arg is null"() {
        when:
        service.replaceRoleAssignmentsOnUser(new User().with {it.uniqueId = "uniqueId";it}, new RoleAssignments(), null)

        then:
        thrown(IllegalArgumentException)
    }


    def "Add UserV20 with phone PIN feature turned ON"() {
        given:
        def user = this.createUser("DFW", true, domainId)
        user.setRoles([entityFactory.createTenantRole("identity:user-admin")].asList())
        user.username = "userWithPin"
        def encryptionVersion = "1"
        def salt = "a1 b2"

        propertiesService.getValue(DefaultUserService.ENCRYPTION_VERSION_ID) >> encryptionVersion
        userDao.getUsersByDomain(domainId) >> [].asList()
        userDao.nextUserId >> "nextId"
        phonePinService.generatePhonePin() >> "246971"
        mockRoleService.getRoleByName(_) >> entityFactory.createClientRole("role")
        cryptHelper.generateSalt() >> salt
        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(_, ) >> true

        when:
        service.addUserAdminV20(user, false)

        then:
        1 * mockValidator.validateUser(user)
        1 * domainService.createDomainWithFallbackGet(domainId)
        1 * userDao.addUser(user)
        1 * tenantService.addTenantRoleToUser(user, _, false)

        user.password != null
        user.userPassword != null
        user.apiKey != null
        user.region != null
        user.encryptionVersion == encryptionVersion
        user.salt == salt;
        user.enabled == true

        user.phonePin != null
        user.phonePin.isNumber()
        user.phonePin.size() == PHONE_PIN_SIZE

        when: "create another user with the same PIN length"

        def user1 = this.createUser("DFW", true, domainId)
        user1.setRoles([entityFactory.createTenantRole("identity:user-admin")].asList())
        user1.username = "userWithPin1"

        service.addUserAdminV20(user1, false)

        then:

        user1.phonePin != null
        user1.phonePin.isNumber()
        user1.phonePin.size() == PHONE_PIN_SIZE
    }

    def "addUserV20: Generate phone PIN on creation if not provided"() {
        given:
        def user = this.createUser("DFW", true, domainId).with {
            it.roles = [entityFactory.createTenantRole("identity:user-admin")].asList()
            it.username = "userWithPin"
            it
        }

        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                IdentityUserTypeEnum.SERVICE_ADMIN.getRoleName(),
                IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName()) >> true
        userDao.getUsersByDomain(domainId) >> [].asList()
        mockRoleService.getRoleByName(_) >> entityFactory.createClientRole("role")

        when:
        service.addUserV20(user, false, false, false)

        then:
        1 * phonePinService.generatePhonePin() >> 324617
        1 * userDao.addUser(user) >> { args ->
            def u = args[0]
            assert u.phonePin != null
            assert u.phonePin == user.phonePin
        }
    }

    def "getUserAdminByDomain: call correct dao method"() {
        given:
        def domain = entityFactory.createDomain()
        def user = entityFactory.createUser()

        when:
        User userAdmin = service.getUserAdminByDomain(domain)

        then:
        1 * userDao.getUserAdminByDomain(domain) >> user
        userAdmin == user
    }

    def "getUserAdminByDomain: error check"() {
        when: "domain is null"
        service.getUserAdminByDomain(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getUserAdminByTenantId: calls correct dao methods"() {
        given:
        def domain = entityFactory.createDomain()
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant(tenantId, tenantId).with {
            it.domainId = domain.domainId
            it
        }
        def user = entityFactory.createUser()

        when:
        User adminUser = service.getUserAdminByTenantId(tenantId)

        then:
        1 * tenantService.getTenant(tenantId) >> tenant
        1 * domainService.getDomain(domain.domainId) >> domain
        1 * userDao.getUserAdminByDomain(domain) >> user

        adminUser == user
    }

    def "getUserAdminByTenantId: error check"() {
        given:
        def domain = entityFactory.createDomain()
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant(tenantId, tenantId).with {
            it.domainId = domain.domainId
            it
        }

        when: "tenantId is null"
        service.getUserAdminByTenantId(null)

        then:
        thrown(IllegalArgumentException)

        when: "tenant not found"
        User userAdmin = service.getUserAdminByTenantId(tenantId)

        then:
        1 * tenantService.getTenant(tenantId) >> null
        0 * domainService.getDomain(domain.domainId)
        0 * userDao.getUserAdminByDomain(domain)

        userAdmin == null

        when: "domain not found"
        service.getUserAdminByTenantId(tenantId)

        then:
        1 * tenantService.getTenant(tenantId) >> tenant
        1 * domainService.getDomain(domain.domainId) >> null
        0 * userDao.getUserAdminByDomain(domain)

        userAdmin == null

        when: "user not found"
        service.getUserAdminByTenantId(tenantId)

        then:
        1 * tenantService.getTenant(tenantId) >> tenant
        1 * domainService.getDomain(domain.domainId) >> domain
        1 * userDao.getUserAdminByDomain(domain) >> null

        userAdmin == null
    }

    @Unroll
    def "calling updateUser to change a user's password calls the atom hopper client to post the cred change event, featureEnabled = #featureEnabled"() {
        given:
        identityConfig.reloadableConfig.isPostCredentialChangeFeedEventsEnabled() >> featureEnabled
        def user = entityFactory.createUser().with {
            it.password = "myNewPassword"
            it
        }
        def currentUser = entityFactory.createUser().with {
            it.password = "myOldPassword"
            it
        }
        if (featureEnabled) {
            1 * atomHopperClient.asyncPostCredentialChangeEvent(_) >> { args ->
                CredentialChangeEventData eventData = args[0]
                assert eventData.username == user.username
                assert eventData.userId == user.id
                assert eventData.email == user.email
                assert eventData.domainId == user.domainId
                assert eventData.credentialUpdateDateTime != null
                assert eventData.credentialUpdateDateTime.isBeforeNow()
                assert eventData.credentialType == CredentialTypeEnum.PASSWORD
            }
        } else {
            0 * atomHopperClient.asyncPostCredentialChangeEvent(_)
        }

        when:
        service.updateUser(user)

        then:
        1 * identityUserService.getProvisionedUserByIdWithPwdHis(user.id) >> currentUser
        1 * userDao.updateUser(user)

        where:
        featureEnabled << [true, false]
    }

    def "addUnverifiedUser: can not create unverified user without an enabled account admin"() {
        given:
        mockCreateSubUserService(service)
        def unverifiedUser = entityFactory.createUnverifiedUser()

        when:
        service.addUnverifiedUser(unverifiedUser)

        then:
        1 * createSubUserService.calculateDomainSubUserDefaults(unverifiedUser.domainId) >> {throw new DomainDefaultException("","")}
        thrown(ForbiddenException)
    }

    def "assignUserRoles: sends feed event if at least one tenant role was added to the user"() {
        given:
        TenantRole tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "roleId"
            it.name = "tenantRoleName"
            it.tenantIds = ["tenantId"]
            it
        }
        TenantRole tenantRole2 = entityFactory.createTenantRole().with {
            it.roleRsId = "roleId2"
            it.name = "tenantRoleName2"
            it.tenantIds = ["tenantId2"]
            it
        }
        ClientRole clientRole = entityFactory.createClientRole().with {
            it.id = "clientRoleId"
            it.clientId = "clientId"
            it
        }
        def user = entityFactory.createUser().with {
            it.roles = [tenantRole, tenantRole2]
            it
        }

        when:
        service.assignUserRoles(user, false)

        then:
        thrown(Exception)

        1 * mockRoleService.getRoleByName(tenantRole.name) >> clientRole
        1 * mockRoleService.getRoleByName(tenantRole2.name) >> clientRole
        2 * tenantService.addTenantRoleToUser(user, _, false) >>> [null, {throw new Exception()}]
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, _)
    }

    def "createDomainIfItDoesNotExist - calls correct services"(){
        given:
        def domainId = "domainId"

        when:
        service.createDomainIfItDoesNotExist(domainId)

        then:
        1 * domainService.getDomain(domainId) >> null
        1 * domainService.createDomainWithFallbackGet(domainId)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(_, ) >> true
    }

    def "createDomainIfItDoesNotExist - domain does not exist"(){
        given:
        def domainId = "domainId"

        when:
        service.createDomainIfItDoesNotExist(domainId)

        then:
        1 * domainService.getDomain(domainId) >> null
        1 * domainService.createDomainWithFallbackGet(domainId)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                IdentityUserTypeEnum.SERVICE_ADMIN.roleName,
                IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName()) >> true
    }

    def "createDomainIfItDoesNotExist - error check"() {
        given:
        def domainId = "domainId"

        when: "caller does not have the identity:identity:rs-domain-admin role"
        service.createDomainIfItDoesNotExist(domainId)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, null, "Not Authorized")

        1 * domainService.getDomain(domainId) >> null
        0 * domainService.createDomainWithFallbackGet(domainId)
    }

    def "createDomainUserInCreateOneCall - calls correct services when domain does not exist"() {
        given:
        def domainId = "domainId"

        when:
        service.createDomainUserInCreateOneCall(domainId, true)

        then:
        1 * reloadableConfig.getTenantDefaultDomainId() >> "defaultDomainId"
        1 * domainService.getDomain(domainId) >> null
        1 * domainService.createDomainWithFallbackGet(domainId)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName()) >> true
    }

    def "createDomainUserInCreateOneCall - error check"(){
        given:
        def domainId = "domainId"

        when: "caller does not have the identity:identity:rs-domain-admin role"
        service.createDomainUserInCreateOneCall(domainId, true)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, ForbiddenException, null, "Not Authorized")

        1 * reloadableConfig.getTenantDefaultDomainId() >> "defaultDomainId"
        1 * domainService.getDomain(domainId) >> null
        0 * domainService.createDomainWithFallbackGet(domainId)
    }

    def "verifyCallerCanCreateTenants does not verify role when flag is diabled"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> false
        def user = entityFactory.createUser()

        when:
        service.verifyCallerCanCreateTenants(user, false)

        then:
        0 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
    }

    def "verifyCallerCanCreateTenants does verify role when flag is enabled and user contains missing tenant"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def tenantId = "tenantId"
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds = [tenantId]
            it
        }
        def user = entityFactory.createUser().with {
            it.roles = [tenantRole]
            it
        }

        when:
        service.verifyCallerCanCreateTenants(user, false)

        then:
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        1 * tenantService.getTenant(tenantId) >> null
    }

    def "verifyCallerCanCreateTenants does not verify role when flag is enabled and user contains existing tenant"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant(tenantId, tenantId)
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds = [tenantId]
            it
        }
        def user = entityFactory.createUser().with {
            it.roles = [tenantRole]
            it
        }

        when:
        service.verifyCallerCanCreateTenants(user, false)

        then:
        0 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        1 * tenantService.getTenant(tenantId) >> tenant
    }

    def "verifyCallerCanCreateTenants does not verify role when flag is enabled and user contains existing mosso and nast tenant"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def mossoTenantId = "tenantId"
        def mossoTenant = entityFactory.createTenant(mossoTenantId, mossoTenantId)
        def nastTenantid = service.getNastTenantId("tenantId")
        def nastTenant = entityFactory.createTenant(nastTenantid, nastTenantid)
        def user = entityFactory.createUser().with {
            it.domainId = mossoTenantId
            it
        }

        when:
        service.verifyCallerCanCreateTenants(user, true)

        then:
        0 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        1 * tenantService.getTenant(mossoTenantId) >> mossoTenant
        1 * tenantService.getTenant(nastTenantid) >> nastTenant
    }

    def "verifyCallerCanCreateTenants does verify role when flag is enabled and user contains missing mosso tenant"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def mossoTenantId = "tenantId"
        def user = entityFactory.createUser().with {
            it.domainId = mossoTenantId
            it
        }

        when:
        service.verifyCallerCanCreateTenants(user, true)

        then:
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        1 * tenantService.getTenant(mossoTenantId) >> null
    }

    def "verifyCallerCanCreateTenants does verify role when flag is enabled and user contains missing nast tenant"() {
        given:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def mossoTenantId = "tenantId"
        def mossoTenant = entityFactory.createTenant(mossoTenantId, mossoTenantId)
        def nastTenantid = service.getNastTenantId("tenantId")
        def user = entityFactory.createUser().with {
            it.domainId = mossoTenantId
            it
        }

        when:
        service.verifyCallerCanCreateTenants(user, true)

        then:
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
        1 * tenantService.getTenant(mossoTenantId) >> mossoTenant
        1 * tenantService.getTenant(nastTenantid) >> null
    }

    @Unroll
    def "attachEndpointsToTenant: test repository feature flag 'feature.enabled.use.domain.type.on.new.user.creation' - enabled = #enabled"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.type = DomainType.RACKSPACE_CLOUD_US.getName()
            it
        }
        def tenant = entityFactory.createTenant().with {
            it.domainId = domain.domainId
            it
        }
        def baseUrl = entityFactory.createCloudBaseUrl().with {
            it.baseUrlId = "baseUrlId"
            it.baseUrlType = "MOSSO"
            it.def = true
            it
        }
        def baseUrls = [baseUrl]

        when:
        service.attachEndpointsToTenant(tenant, baseUrls, domain)

        then:
        1 * identityConfig.getRepositoryConfig().shouldUseDomainTypeOnNewUserCreation() >> enabled

        if (enabled) {
            1 * endpointService.doesBaseUrlBelongToCloudRegion(baseUrl, domain) >> true
            1 * identityConfig.getReloadableConfig().getV1DefaultCloudEndpointsUs() >> [baseUrl.baseUrlId]
            0 * config.getList("v1defaultMosso")
        } else {
            1 * endpointService.doesBaseUrlBelongToCloudRegion(baseUrl) >> true
            0 * identityConfig.getReloadableConfig().getV1DefaultCloudEndpointsUs()
            1 * config.getList("v1defaultMosso") >> [baseUrl.baseUrlId]
        }

        where:
        enabled << [true, false]
    }

    @Unroll
    def "addUserV20: test default region when it is not provided with isCreateUserOneCall = #isCreateUserOneCall"() {
        given:
        def domain = entityFactory.createDomain()
        TenantRole userAdminRole = entityFactory.createTenantRole().with {
            it.name = Constants.IDENTITY_USER_ADMIN_ROLE
            it.roleRsId = Constants.USER_ADMIN_ROLE_ID
            it
        }
        ClientRole userAdminClientRole = entityFactory.createClientRole(Constants.IDENTITY_USER_ADMIN_ROLE)
        def user = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it.region = null
            it.roles.add(userAdminRole)
            it
        }

        def cloudRegion = "US"
        def region = new Region().with{
            it.name = "ORD"
            it
        }
        when:
        identityConfig.getRepositoryConfig().shouldUseDomainTypeOnNewUserCreation() >> false
        service.addUserAdminV20(user, isCreateUserOneCall)

        then:
        1 * domainService.getDomainAdmins(domain.domainId) >> []
        1 * userDao.getUsersByDomain(domain.domainId) >> []
        1 * userDao.addUser(user)
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.CREATE, _);
        1 * domainService.updateDomainUserAdminDN(user)
        1 * identityConfig.getStaticConfig().getCloudRegion() >> cloudRegion
        1 * cloudRegionService.getDefaultRegion(cloudRegion) >> region
        if (isCreateUserOneCall) {
            2 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            2 * domainService.getDomain(domain.domainId) >> domain
        } else {
            1 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            1 * domainService.getDomain(domain.domainId) >> domain
        }

        when:
        identityConfig.getRepositoryConfig().shouldUseDomainTypeOnNewUserCreation() >> true
        user.setRegion(null)
        service.addUserAdminV20(user, isCreateUserOneCall)

        then:
        1 * domainService.getDomainAdmins(domain.domainId) >> []
        1 * userDao.getUsersByDomain(domain.domainId) >> []
        1 * userDao.addUser(user)
        1 * atomHopperClient.asyncPost(user, FeedsUserStatusEnum.CREATE, _)
        1 * domainService.updateDomainUserAdminDN(user)
        0 * identityConfig.getStaticConfig().getCloudRegion() >> cloudRegion
        1 * cloudRegionService.getDefaultRegion(cloudRegion) >> region
        if (isCreateUserOneCall) {
            2 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            2 * domainService.getDomain(domain.domainId) >> domain
        } else {
            1 * mockRoleService.getRoleByName(userAdminRole.name) >> userAdminClientRole
            1 * domainService.getDomain(domain.domainId) >> domain
        }

        where:
        isCreateUserOneCall << [true , false]
    }

    @Unroll
    def "inferCloudBasedOnDomainType(): validate expected cloud region is returned "() {
        when:
        def cloudRegion = service.inferCloudBasedOnDomainType(domainType)

        then:
        cloudRegion.equalsIgnoreCase(expectedRegion)

        where:
        expectedRegion           | domainType
        CloudRegion.UK.getName() | DomainType.RACKSPACE_CLOUD_UK.getName()
        CloudRegion.US.getName() | DomainType.RACKSPACE_CLOUD_US.getName()
        CloudRegion.US.getName() | null
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
