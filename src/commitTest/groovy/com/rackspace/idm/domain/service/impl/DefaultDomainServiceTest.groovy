package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.User
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

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
        mockIdentityConfig(service)
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

    @Unroll
    def "getEnabledDomainAdmins: call correct service methods - domainUserAdminLookup = #domainUserAdminLookup"() {
        def domain = entityFactory.createDomain()
        def user = entityFactory.createUser()

        when: "enabled user-admins"
        List<User> userAdminList = service.getEnabledDomainAdmins(domain.domainId)

        then:
        1 * identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * domainDao.getDomain(domain.domainId) >> domain
            1 * userService.getUserAdminByDomain(domain) >> user
            0 * userService.getUsersWithDomainAndEnabledFlag(_, _)
        } else {
            0 * domainDao.getDomain(_)
            0 * userService.getUserAdminByDomain(_)
            1 * userService.getUsersWithDomainAndEnabledFlag(domain.domainId, true) >> [user]
            1 * authorizationService.hasUserAdminRole(user) >> true
        }

        userAdminList.size() == 1

        when: "disabled user-admins"
        user.enabled = false
        userAdminList = service.getEnabledDomainAdmins(domain.domainId)

        then:
        1 * identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * domainDao.getDomain(domain.domainId) >> domain
            1 * userService.getUserAdminByDomain(domain) >> user
            0 * userService.getUsersWithDomainAndEnabledFlag(_, _)
        } else {
            0 * domainDao.getDomain(_)
            0 * userService.getUserAdminByDomain(_)
            1 * userService.getUsersWithDomainAndEnabledFlag(domain.domainId, true) >> []
        }

        userAdminList.size() == 0

        where:
        domainUserAdminLookup << [true, false]
    }

    /**
     * This method will make extra calls when 'feature.enable.user.admin.look.up.by.domain' is enabled for domains that
     * have not had their user-admin migrated, the user is disabled, or if the user or domain do not exist.
     */
    def "getEnabledDomainAdmins:  test 'feature.enable.user.admin.look.up.by.domain' set to true"() {
        def domain = entityFactory.createDomain()

        identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> true

        when: "domain not found"
        List<User> userAdminList = service.getEnabledDomainAdmins(domain.domainId)

        then:
        1 * domainDao.getDomain(domain.domainId) >> null
        0 * userService.getUserAdminByDomain(_)
        1 * userService.getUsersWithDomainAndEnabledFlag(domain.domainId, true) >> []

        userAdminList.size() == 0

        when: "domain's userAdminDN is not set or the user is not found"
        userAdminList = service.getEnabledDomainAdmins(domain.domainId)

        then:
        1 * domainDao.getDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> null
        1 * userService.getUsersWithDomainAndEnabledFlag(domain.domainId, true) >> []

        userAdminList.size() == 0
    }

    @Unroll
    def "getDomainAdmins: call correct service methods - domainUserAdminLookup = #domainUserAdminLookup"() {
        def domain = entityFactory.createDomain()
        def user = entityFactory.createUser()

        when: "enabled user-admins"
        List<User> userAdminList = service.getDomainAdmins(domain.domainId)

        then:
        1 * identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * domainDao.getDomain(domain.domainId) >> domain
            1 * userService.getUserAdminByDomain(domain) >> user
            0 * userService.getUsersWithDomain(_)
        } else {
            0 * domainDao.getDomain(_)
            0 * userService.getUserAdminByDomain(_)
            1 * userService.getUsersWithDomain(domain.domainId) >> [user]
            1 * authorizationService.hasUserAdminRole(user) >> true
        }

        userAdminList.size() == 1
        userAdminList.get(0) == user

        when: "disabled user-admins"
        user.enabled = false
        userAdminList = service.getDomainAdmins(domain.domainId)

        then:
        1 * identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * domainDao.getDomain(domain.domainId) >> domain
            1 * userService.getUserAdminByDomain(domain) >> user
            0 * userService.getUsersWithDomain(_)
        } else {
            0 * domainDao.getDomain(_)
            0 * userService.getUserAdminByDomain(_)
            1 * userService.getUsersWithDomain(domain.domainId) >> [user]
            1 * authorizationService.hasUserAdminRole(user) >> true
        }

        userAdminList.size() == 1
        userAdminList.get(0) == user

        where:
        domainUserAdminLookup << [true, false]
    }

    /**
     * This method will make extra calls when 'feature.enable.user.admin.look.up.by.domain' is enabled for domains that
     * have not had their user-admin migrated, or if the user or domain do not exist.
     */
    def "getDomainAdmins: test 'feature.enable.user.admin.look.up.by.domain' set to true"() {
        def domain = entityFactory.createDomain()

        identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> true

        when: "domain not found"
        List<User> userAdminList = service.getDomainAdmins(domain.domainId)

        then:
        1 * domainDao.getDomain(domain.domainId) >> null
        0 * userService.getUserAdminByDomain(_)
        1 * userService.getUsersWithDomain(domain.domainId) >> []

        userAdminList.size() == 0

        when: "domain's userAdminDN is not set or the user is not found"
        userAdminList = service.getDomainAdmins(domain.domainId)

        then:
        1 * domainDao.getDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> null
        1 * userService.getUsersWithDomain(domain.domainId) >> []

        userAdminList.size() == 0
    }

    /**
     * This test will verify a possible state when running an older version of identity which does not have the logic
     * to remove or update the userAdminDN on a domain. This state can happen if the a user-admin was created using idm
     * artifact 3.21.0, and then the user was deleted or was moved from a domain using an older version. This will
     * cause the userAdminDN reference on domain to point to a user that does not exist or no longer belong to the
     * specified domain.
     */
    def "getDomainAdmins: verify that if user retrieve no longer belong to the domainId specified fallback to the old logic"() {
        def domainId = "someDomainId"
        def domain = entityFactory.createDomain(domainId)
        def user = entityFactory.createUser().with {
            it.domainId = "otherDomainId"
            it
        }

        when:
        def userAdminList = service.getDomainAdmins(domain.domainId)

        then:
        1 * identityConfig.getReloadableConfig().isUserAdminLookUpByDomain() >> true
        1 * domainDao.getDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user
        1 * userService.getUsersWithDomain(domain.domainId) >> []

        userAdminList.size() == 0
    }

    @Unroll
    def "doDomainsShareRcn: a null or blank value for either domain will result in false: d1: '#d1'; d2: '#d2'"() {
        when:
        def result = service.doDomainsShareRcn(d1, d2)

        then:
        !result
        0 * domainDao.getDomain(d1)
        0 * domainDao.getDomain(d2)

        where:
        d1 | d2
        null | null
        "d1" | null
        null | "d2"
        "" | "d2"
        "d1" | "   "
    }

    @Unroll
    def "doDomainsShareRcn: equal domains (case insensitive), will result in true: d1: '#d1'; d2: '#d2'"() {
        when:
        def result = service.doDomainsShareRcn(d1, d2)

        then:
        result
        0 * domainDao.getDomain(d1)
        0 * domainDao.getDomain(d2)

        where:
        d1 | d2
        "value" | "VaLuE"
        "123" | "123"
        "value" | "value"
    }

    @Unroll
    def "doDomainsShareRcn: distinct domains with missing RCNs or unequal RCNs (case insensitive), will result in false: d1 RCN: '#d1Rcn'; d2 RCN: '#d2Rcn'"() {
        Domain d1 = new Domain().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.rackspaceCustomerNumber = d1Rcn
            it
        }
        Domain d2 = new Domain().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.rackspaceCustomerNumber = d2Rcn
            it
        }
        domainDao.getDomain(d1.getDomainId()) >> d1
        domainDao.getDomain(d2.getDomainId()) >> d2

        expect:
        !service.doDomainsShareRcn(d1.domainId, d2.domainId)

        where:
        d1Rcn | d2Rcn
        "" | ""
        " " | "r2"
        null | null
        "r1" | "r2"
        "r1" | null
        null | "r2"
    }

    @Unroll
    def "doDomainsShareRcn: distinct domains with equal RCNs (case insensitive), will result in true: d1 RCN: '#d1Rcn'; d2 RCN: '#d2Rcn'"() {
        Domain d1 = new Domain().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.rackspaceCustomerNumber = d1Rcn
            it
        }
        Domain d2 = new Domain().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.rackspaceCustomerNumber = d2Rcn
            it
        }
        domainDao.getDomain(d1.getDomainId()) >> d1
        domainDao.getDomain(d2.getDomainId()) >> d2

        expect:
        service.doDomainsShareRcn(d1.domainId, d2.domainId)

        where:
        d1Rcn | d2Rcn
        "r1" | "r1"
        "r1" | "R1"
    }

}
