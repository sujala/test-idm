package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
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

    def setupSpec(){
        service = new DefaultUserService()
    }

    def setup() {
        setupMocks()
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
