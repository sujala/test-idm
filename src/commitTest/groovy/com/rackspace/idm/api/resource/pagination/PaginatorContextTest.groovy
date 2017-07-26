package com.rackspace.idm.api.resource.pagination

import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.User
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 29/10/12
 * Time: 15:00
 * To change this template use File | Settings | File Templates.
 */

class PaginatorContextTest extends RootServiceTest {

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def userList
    @Shared def userIdList
    @Shared def users
    @Shared PaginatorContext<User> paginatorContext

    def setupSpec() {
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

        paginatorContext = new PaginatorContext<User>().with() {
            it.setOffset(1)
            it.setLimit(10)
            it.setTotalRecords(25)
            return it
        }
    }

    def cleanupSpec() {
    }

    def "getSearchResultEntryList returns empty list"() {
        when:
        def list = paginatorContext.getSearchResultEntryList()

        then:
        list.size == 0
    }

    def "getValueList returns empty list"() {
        when:
        def list = paginatorContext.getValueList()

        then:
        list.size == 0
    }

    def "getSubList returns empty list"() {
        when:
        def list = paginatorContext.getSubList(userList, 100, 50)

        then:
        list.size == 0
    }

    def "getSubList returns subList"() {
        when:
        def listOne = paginatorContext.getSubList(userList, 1, 2)
        def listTwo = paginatorContext.getSubList(userList, 0, 2)
        def listThree = paginatorContext.getSubList(userList, 3, 3)
        def listFour = paginatorContext.getSubList(userList, 3, 6)
        def listFive = paginatorContext.getSubList(userList, 0, 7)

        then:
        listOne.equals(userList.subList(1, 3))
        listTwo.equals(userList.subList(0, 2))
        listThree.equals(userList.subList(3, userList.size()))
        listFour.equals(userList.subList(3, userList.size()))
        listFive.equals(userList)
    }

    def "getSubList returns same list"() {
        when:
        def returnedListOne = paginatorContext.getSubList(userList, 0, 5)
        def returnedListTwo = paginatorContext.getSubList(userList, 0, 10)

        then:
        returnedListOne.equals(userList)
        returnedListTwo.equals(userList)
    }
}
