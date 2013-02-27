package com.rackspace.idm.domain.service.impl

import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 2/27/13
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultDomainServiceTest extends RootServiceTest {

    @Shared def DefaultDomainService service

    def setupSpec() {
        service = new DefaultDomainService()
    }

    def setup() {
        mockConfiguration(service)
        mockUserService(service)
        mockTenantService(service)
        mockDomainDao(service)
        mockAuthorizationService(service)
    }

    def "filterUserAdmins checks users for user-admin role"() {
        given:
        def user1 = entityFactory.createUser().with {
            it.username = "userOne"
            return it
        }
        def user2 = entityFactory.createUser().with {
            it.username = "userTwo"
            return it
        }
        def userList = [ user1, user2 ].asList()

        when:
        service.filterUserAdmins(userList)

        then:
        2 * authorizationService.hasUserAdminRole(_)
    }

    def "filterUserAdmins returns only users with user-admin role"() {
        given:
        def user1 = entityFactory.createUser().with {
            it.username = "userOne"
            return it
        }
        def user2 = entityFactory.createUser().with {
            it.username = "userTwo"
            return it
        }
        def userList = [ user1, user2 ].asList()

        authorizationService.hasUserAdminRole(user1) >> false
        authorizationService.hasUserAdminRole(user2) >> true

        when:
        def newList = service.filterUserAdmins(userList)

        then:
        newList.size() == 1
        newList.get(0).equals(user2)
    }

    def "filterUserAdmins returns empty list if no users have user-admin role"() {
        given:
        def user1 = entityFactory.createUser().with {
            it.username = "userOne"
            return it
        }
        def user2 = entityFactory.createUser().with {
            it.username = "userTwo"
            return it
        }
        def userList = [ user1, user2 ].asList()

        authorizationService.hasUserAdminRole(user1) >> false
        authorizationService.hasUserAdminRole(user2) >> false

        when:
        def newList = service.filterUserAdmins(userList)

        then:
        newList != null
        newList.size() == 0
    }

    def "getDomainAdmins uses userService to retrieve users in domain with domainId and enabled filter"() {
        when:
        service.getDomainAdmins("domainId", true)

        then:
        1 * userService.getUsersInDomain("domainId", true) >> [].asList()
    }

    def "getDomainAdmins uses userService to retrieve users in domain with domainId filter and without enabled filter"() {
        when:
        service.getDomainAdmins("domainId")

        then:
        1 * userService.getUsersInDomain("domainId") >> [].asList()
    }

    def "getDomainAdmins (domain and enabled filters) filters list of user by user-admin role"() {
        given:
        userService.getUsersInDomain("domainId", true) >> [ entityFactory.createUser() ].asList()

        when:
        service.getDomainAdmins("domainId", true)

        then:
        1 * authorizationService.hasUserAdminRole(_)
    }

    def "getDomainAdmins (domain filter) filters list of user by user-admin role"() {
        given:
        userService.getUsersInDomain("domainId") >> [ entityFactory.createUser() ].asList()

        when:
        service.getDomainAdmins("domainId")

        then:
        1 * authorizationService.hasUserAdminRole(_)
    }
}
