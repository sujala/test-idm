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
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.domain.dao.UserDao

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 31/10/12
 * Time: 20:23
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultUserServiceTestGroovy extends Specification {

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
    }

    def createRole(roleId) {
        new TenantRole().with {
            it.roleRsId = roleId
            return it
        }
    }

    def createUser(roleId) {
        new User().with {
            def roles = new ArrayList<TenantRole>()
            roles.add(createRole(roleId))
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
            roles.add(createRole(roleId))

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
