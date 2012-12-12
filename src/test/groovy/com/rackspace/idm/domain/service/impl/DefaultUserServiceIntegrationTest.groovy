package com.rackspace.idm.domain.service.impl

import spock.lang.Specification
import spock.lang.Shared
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.domain.entity.Users
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.domain.entity.FilterParam
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration

import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.impl.LdapApplicationRoleRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import com.rackspace.idm.domain.entity.ClientRole

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 31/10/12
 * Time: 20:23
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultUserServiceIntegrationTest extends Specification {

    @Autowired DefaultUserService userService
    @Autowired Configuration config

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def FilterParam[] roleFilter
    @Shared def FilterParam[] domainAndRoleFilters
    @Shared def userList
    @Shared def userIdList
    @Shared def users

    @Shared PaginatorContext<String> stringPaginator
    @Shared TenantDao tenantDao
    @Shared UserDao userDao
    @Shared LdapApplicationRoleRepository applicationRoleDao
    @Shared LdapTenantRoleRepository tenantRoleDao

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        userList = new ArrayList<User>()
        userIdList = new ArrayList<String>()
        for (int i = 5; i > 0; i--) {
            userList.add(createUser(sharedRandom))
            def id = UUID.randomUUID()
            userIdList.add(("$id").replace('-',""))
        }
        users = createUsers(userList)

        roleFilter = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom)]
        domainAndRoleFilters = [new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRandom),
                                new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, sharedRandom)]
    }

    def cleanupSpec() {
    }

    def "getUsersWithRole calls tenantDao.getMultipleTenantRoles"() {
        given:
        setupMocks()

        when:
        userService.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
    }

    def "getUsersWithRole calls getUserById"() {
        given:
        setupMocks()

        when:
        userService.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
        5 * userDao.getUserById(_ as String)
    }

    def "getUsersWithRole calls first setUserContext"() {
        given:
        setupMocks()
        def listOfUsers = new ArrayList<User>()
        listOfUsers.add(createUser("123456", "123456"))

        when:
        def userContext = userService.getUsersWithRole(roleFilter, sharedRandom, 0, 10)

        then:
        tenantDao.getMultipleTenantRoles(sharedRandom, 0, 10) >> stringPaginator
        userDao.getUserById(_ as String) >>> [ createUser("123456", "123456"), null, null, null, null ]

        userContext.getValueList().equals(listOfUsers)
        userContext.getLimit() == stringPaginator.getLimit()
        userContext.getOffset() == stringPaginator.getOffset()
        userContext.getTotalRecords() == stringPaginator.getTotalRecords()
    }

    def "getUsersWithRole calls second setUserContext"() {
        given:
        setupMocks()

        when:
        def userContext = userService.getUsersWithRole(domainAndRoleFilters, sharedRandom, 0, 10)

        then:
        userDao.getAllUsersNoLimit(domainAndRoleFilters) >> createUsers(userList)

        userContext.getValueList().equals(userList)
        userContext.getLimit() == 10
        userContext.getOffset() == 0
        userContext.getTotalRecords() == 5

    }

    def "getUsersWithRole calls getAllUsersNoLimit"() {
        given:
        setupMocks()

        when:
        userService.getUsersWithRole(domainAndRoleFilters, sharedRandom, 0, 10)

        then:
        userDao.getAllUsersNoLimit(domainAndRoleFilters) >> createUsers(userList)
    }

    def "getAllUsersNoLimit calls userDao getAllUsersNoLimit"() {
        given:
        setupMocks()

        when:
        userService.getAllUsersNoLimit(domainAndRoleFilters)
        userService.getAllUsers(roleFilter)

        then:
        1 * userDao.getAllUsersNoLimit(_ as FilterParam[])
    }

    def "getSubList throws BadRequest"() {
        when:
        userService.getSubList(userList, 10, 5)

        then:
        thrown BadRequestException
    }

    def "getSubList returns same list"() {
        when:
        def returnedListOne = userService.getSubList(userList, 0, 5)
        def returnedListTwo = userService.getSubList(userList, 0, 10)

        then:
        returnedListOne.equals(userList)
        returnedListTwo.equals(userList)
    }

    def "getSubList returns subList"() {
        when:
        def listOne = userService.getSubList(userList, 1, 2)
        def listTwo = userService.getSubList(userList, 0, 2)
        def listThree = userService.getSubList(userList, 3, 3)
        def listFour = userService.getSubList(userList, 3, 6)
        def listFive = userService.getSubList(userList, 0, 7)

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

        when:
        userService.filterUsersForRole(users, list, roleId)

        then:
        list.empty
    }

    def "filterUsersForRole all Users have role"() {
        given:
        def list = new ArrayList<User>()

        when:
        userService.filterUsersForRole(users, list, sharedRandom)

        then:
        list.equals(users.users)
    }

    def "filterUsersForRole some Users have role"() {
        given:
        def list = new ArrayList<User>()
        def compareTo = new ArrayList<User>()
        def userList = new ArrayList<User>()
        def user = createUser(sharedRandom)
        userList.add(user)
        userList.add(createUser("123456789"))
        userList.add(createUser("135792468"))
        compareTo.add(user)
        def users = createUsers(userList)

        when:
        userService.filterUsersForRole(users, list, sharedRandom)

        then:
        list.equals(compareTo)
    }

    def "getUsersWeight gets users tenantRoles"() {
        given:
        setupMocks()

        when:
        userService.getUserWeight(createUser("3","username"), "applicationId")

        then:
        1 * tenantRoleDao.getTenantRolesForUser(_, _) >> new ArrayList<TenantRole>()
    }

    def "getUsersWeight finds identity:* role for user"() {
        given:
        setupMocks()
        def serviceAdmin = createUser("0")
        def serviceAdminTenantRole = createTenantRole("0")
        def serviceAdminApplicationRole = createApplicationRole("identity:service-admin", 0)

        def admin = createUser("1")
        def adminTenantRole = createTenantRole("1")
        def adminApplicationRole = createApplicationRole("identity:admin", 100)

        def userAdmin = createUser("2")
        def userAdminTenantRole = createTenantRole("2")
        def userAdminApplicationRole = createApplicationRole("identity:user-admin", 1000)

        def defaultUser = createUser("3")
        def defaultUserTenantRole = createTenantRole("3")
        def defaultUserApplicationRole = createApplicationRole("identity:default", 2000)

        def none = createUser("4")
        def noneTenantRole = createTenantRole("4")
        def noneApplicationRole = createApplicationRole("some role", 2000)

        def serviceAdminTenantRoles = new ArrayList<TenantRole>()
        serviceAdminTenantRoles.add(serviceAdminTenantRole)
        def adminTenantRoles = new ArrayList<TenantRole>()
        adminTenantRoles.add(adminTenantRole)
        def userAdminTenantRoles = new ArrayList<TenantRole>()
        userAdminTenantRoles.add(userAdminTenantRole)
        def defaultUserTenantRoles = new ArrayList<TenantRole>()
        defaultUserTenantRoles.add(defaultUserTenantRole)
        def noneTenantRoles = new ArrayList<TenantRole>()
        noneTenantRoles.add(noneTenantRole)

        //mocks
        tenantRoleDao.getTenantRolesForUser(serviceAdmin, "applicationId") >> serviceAdminTenantRoles
        applicationRoleDao.getClientRole("0") >> serviceAdminApplicationRole

        tenantRoleDao.getTenantRolesForUser(admin, "applicationId") >> adminTenantRoles
        applicationRoleDao.getClientRole("1") >> adminApplicationRole

        tenantRoleDao.getTenantRolesForUser(userAdmin, "applicationId") >> userAdminTenantRoles
        applicationRoleDao.getClientRole("2") >> userAdminApplicationRole

        tenantRoleDao.getTenantRolesForUser(defaultUser, "applicationId") >> defaultUserTenantRoles
        applicationRoleDao.getClientRole("3") >> defaultUserApplicationRole

        tenantRoleDao.getTenantRolesForUser(none, "applicationId") >> noneTenantRoles
        applicationRoleDao.getClientRole("4") >> noneApplicationRole

        when:
        def serviceWeight = userService.getUserWeight(serviceAdmin, "applicationId")
        def adminWeight = userService.getUserWeight(admin, "applicationId")
        def userAdminWeight = userService.getUserWeight(userAdmin, "applicationId")
        def defaultUserWeight =  userService.getUserWeight(defaultUser, "applicationId")
        def noneWeight = userService.getUserWeight(none, "applicationId")

        then:
        serviceWeight == 0
        adminWeight == 100
        userAdminWeight == 1000
        defaultUserWeight == 2000
        noneWeight == 2000
    }

    def setupMocks() {
        stringPaginator = new PaginatorContext<String>().with() {
            it.limit = 10
            it.offset = 0
            it.totalRecords = 5
            it.valueList = userIdList
            return it
        }
        tenantDao = Mock()
        userService.tenantDao = tenantDao

        userDao = Mock()
        userService.userDao = userDao

        tenantRoleDao = Mock()
        userService.tenantRoleDao = tenantRoleDao

        applicationRoleDao = Mock()
        userService.applicationRoleDao = applicationRoleDao;
    }

    def createTenantRole(roleId) {
        new TenantRole().with {
            it.roleRsId = roleId
            return it
        }
    }

    def createApplicationRole(name, weight) {
        new ClientRole().with {
            it.name = name
            it.rsWeight = weight
            return it
        }
    }

    def createUser(roleId) {
        new User().with {
            def roles = new ArrayList<TenantRole>()
            roles.add(createTenantRole(roleId))
            def random = UUID.randomUUID()

            it.username = ("$random").replace('-',"")
            it.id = ("$random").replace('-', "")
            it.setRoles(roles)
            return it
        }
    }

    def createUser(roleId, username) {
        new User().with {
            def roles = new ArrayList<TenantRole>()
            roles.add(createTenantRole(roleId))

            it.username = username
            it.id = username
            it.setRoles(roles)
            return it
        }
    }

    def createUsers(userList) {
        new Users().with {
            it.users = userList
            return it
        }
    }
}
