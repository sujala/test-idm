package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.Domain
import com.unboundid.ldap.sdk.DN
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
        service.getEnabledDomainAdmins("domainId")

        then:
        1 * userService.getUsersWithDomainAndEnabledFlag("domainId", _) >> [].asList()
    }

    def "getDomainAdmins uses userService to retrieve users in domain with domainId filter and without enabled filter"() {
        when:
        service.getDomainAdmins("domainId")

        then:
        1 * userService.getUsersWithDomain("domainId") >> [].asList()
    }

    def "getDomainAdmins (domain and enabled filters) filters list of user by user-admin role"() {
        given:
        userService.getUsersWithDomainAndEnabledFlag("domainId", _) >> [ entityFactory.createUser() ].asList()

        when:
        service.getEnabledDomainAdmins("domainId")

        then:
        1 * authorizationService.hasUserAdminRole(_)
    }

    def "getDomainAdmins (domain filter) filters list of user by user-admin role"() {
        given:
        userService.getUsersWithDomain("domainId") >> [ entityFactory.createUser() ].asList()

        when:
        service.getDomainAdmins("domainId")

        then:
        1 * authorizationService.hasUserAdminRole(_)
    }

    def "addDomainUserAdminDN - calls correct DAO methods"() {
        def domain = entityFactory.createDomain()
        def trUserAdmin = entityFactory.createTenantRole(Constants.IDENTITY_USER_ADMIN_ROLE)
        def user = entityFactory.createUser().with {
            it.domainId == domain.domainId
            it.roles.add(trUserAdmin)
            it
        }
        when:
        service.updateDomainUserAdminDN(user)

        then:
        1 * domainDao.getDomain(user.domainId) >> domain
        1 * domainDao.domainExistsWithNameAndNotId(domain.getName(), domain.getDomainId()) >> false
        1 * domainDao.updateDomain(domain) >> {arg ->
            def d = (Domain) arg[0]
            assert d.userAdminDN == user.getDn()
        }
    }

    def "addDomainUserAdminDN - error check"() {
        def domain = entityFactory.createDomain()
        def trUserAdmin = entityFactory.createTenantRole(Constants.IDENTITY_USER_ADMIN_ROLE)
        def userNullUniqueId = entityFactory.createUser().with {
            it.uniqueId = null
            it.domainId = domain.domainId
            it.roles.add(trUserAdmin)
            it
        }
        def userNullDomainId = entityFactory.createUser().with {
            it.domainId = null
            it.roles.add(trUserAdmin)
            it
        }
        def userEmptyRoles = entityFactory.createUser()

        def nonUserAdmin = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it.roles.add(entityFactory.createTenantRole(Constants.USER_MANAGE_ROLE_NAME))
            it
        }

        when: "user is null"
        service.updateDomainUserAdminDN(null)

        then:
        thrown(IllegalArgumentException)

        when: "user's unique ID is null"
        service.updateDomainUserAdminDN(userNullUniqueId)

        then:
        thrown(IllegalArgumentException)

        when: "user's domain ID is null"
        service.updateDomainUserAdminDN(userNullDomainId)

        then:
        thrown(IllegalArgumentException)

        when: "user's roles are empty"
        service.updateDomainUserAdminDN(userEmptyRoles)

        then:
        thrown(IllegalArgumentException)

        when: "user is a user-manage"
        service.updateDomainUserAdminDN(nonUserAdmin)

        then:
        thrown(IllegalArgumentException)
    }

    def "deleteDomainUserAdminDN - calls correct DAO methods"() {
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain().with {
            it.domainId = user.domainId
            it.userAdminDN = user.getDn()
            it
        }

        when:
        service.removeDomainUserAdminDN(user)

        then:
        1 * domainDao.getDomain(user.domainId) >> domain
        1 * domainDao.updateDomain(domain) >> {arg ->
            def d = (Domain) arg[0]
            assert d.userAdminDN == null
        }
    }

    def "deleteDomainUserAdminDN - does not update domain if specified user's DN does not match domain's userAdminDN"() {
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain().with {
            it.domainId = user.domainId
            it.userAdminDN = new DN("rsId=otherDN")
            it
        }

        when:
        service.removeDomainUserAdminDN(user)

        then:
        1 * domainDao.getDomain(user.domainId) >> domain
        0 * domainDao.updateDomain(domain)
    }

    def "deleteDomainUserAdminDN - error check"() {
        def domain = entityFactory.createDomain()
        def userNullUniqueId = entityFactory.createUser().with {
            it.uniqueId = null
            it.domainId = domain.domainId
            it
        }
        def userNullDomainId = entityFactory.createUser().with {
            it.domainId = null
            it
        }
        when: "user is null"
        service.updateDomainUserAdminDN(null)

        then:
        thrown(IllegalArgumentException)

        when: "user's unique ID is null"
        service.updateDomainUserAdminDN(userNullUniqueId)

        then:
        thrown(IllegalArgumentException)

        when: "user's domain ID is null"
        service.updateDomainUserAdminDN(userNullDomainId)

        then:
        thrown(IllegalArgumentException)
    }
}
