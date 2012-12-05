package com.rackspace.idm.domain.service.impl

import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import com.rackspace.idm.util.AuthHeaderHelper
import spock.lang.Shared
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.dao.ScopeAccessDao
import org.joda.time.DateTime
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.unboundid.ldap.sdk.RDN
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.unboundid.ldap.sdk.Attribute
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.exception.NotFoundException

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/29/12
 * Time: 6:14 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultScopeAccessServiceGroovyTest extends Specification {

    @Autowired private Configuration config
    @Autowired private AuthHeaderHelper authHeaderHelper
    @Autowired private DefaultScopeAccessService service

    @Shared UserDao userDao
    @Shared TenantDao tenantDao
    @Shared EndpointDao endpointDao
    @Shared ApplicationDao applicationDao
    @Shared TenantRoleDao tenantRoleDao
    @Shared ScopeAccessDao scopeAccessDao

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def expiredDate
    @Shared def refreshDate
    @Shared def futureDate
    @Shared def dn = "accessToken=123456,cn=TOKENS,rsId=12345,ou=users,o=rackspace"
    @Shared def searchDn = "rsId=12345,ou=users,o=rackspace"

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        expiredDate = new DateTime().minusHours(config.getInt("token.refreshWindowHours")).minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).minusHours(2).toDate()
        futureDate = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).plusHours(2).toDate()
    }

    def "getOrCreatePassWordResetScopeAccessForUser adds new scopeAccess and deletes old"() {
        given:
        createMocks()
        def mockedUser = Mock(User)
        mockedUser.getUniqueId() >> "rsId=1,ou=users,o=rackspace"

        PasswordResetScopeAccess scopeAccessOne = new PasswordResetScopeAccess()
        PasswordResetScopeAccess scopeAccessTwo = new PasswordResetScopeAccess()

        scopeAccessOne.accessTokenString = "123456"
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessOne.accessTokenExp = new DateTime().minusHours(12).toDate()

        scopeAccessTwo.accessTokenString = "123456"
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessTwo.accessTokenExp = new DateTime().plusHours(12).toDate()

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [
                null,
                scopeAccessOne,
                scopeAccessTwo
        ]

        when:
        service.getOrCreatePasswordResetScopeAccessForUser(mockedUser)
        service.getOrCreatePasswordResetScopeAccessForUser(mockedUser)
        service.getOrCreatePasswordResetScopeAccessForUser(mockedUser)

        then:
        2 * scopeAccessDao.addDirectScopeAccess(_, _)

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
    }

    def "updateUserScopeAccessTokenForClientIdByUser deletes existing and adds new scopeAccess"() {
        given:
        createMocks()
        def mockedUser = Mock(User)
        def scopeAccessOne = new UserScopeAccess()

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccessOne
        applicationDao.getClientByClientId(_) >> new Application().with() {
            it.clientId = "clientId"
            it.RCN = "RCN"
            return it
        }

        scopeAccessOne.accessTokenString = "123456"
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessOne.accessTokenExp = new DateTime().minusHours(12).toDate()

        when:
        service.updateUserScopeAccessTokenForClientIdByUser(mockedUser, "clientId", "token", new DateTime().toDate())

        then:
        1 * scopeAccessDao.addDirectScopeAccess(_, _)

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(dn)
    }

    def "update expired user scope access adds new scope access entity to the directory"() {
        given:
        createMocks()
        def scopeAccess = Mock(UserScopeAccess)

        scopeAccess.isAccessTokenExpired(_) >>> [ true, false ]
        scopeAccess.isAccessTokenWithinRefreshWindow(_) >>> [ true, false ]
        scopeAccess.getUniqueId() >> dn

        when:
        service.updateExpiredUserScopeAccess(scopeAccess, false)
        service.updateExpiredUserScopeAccess(scopeAccess, false)
        service.updateExpiredUserScopeAccess(scopeAccess, false)

        then:
        2 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
    }

    def "update (different parameters) deletes expired"() {
        given:
        createMocks()
        def scopeAccessOne = new UserScopeAccess()
        def scopeAccessTwo = new UserScopeAccess()

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = expiredDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))
        scopeAccessTwo.accessTokenString = "1234"
        scopeAccessTwo.accessTokenExp = futureDate
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList() >> [].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccessTwo
        userDao.getUserByDn(_) >> new User().with {
            it.username = "username"
            it.id = "id"
            return it
        }

        when:
        service.updateExpiredUserScopeAccess("parentUniqueIdString", "clientId")
        service.updateExpiredUserScopeAccess(dn, sharedRandom)

        then:
        1 * scopeAccessDao.deleteScopeAccess(scopeAccessOne)

        1 * scopeAccessDao.addDirectScopeAccess(dn, _)
    }

    def "getParentDn returns the parentDn"() {
        given:
        createMocks()

        when:
        def one = service.getBaseDnAsString(dn)
        def two = service.getBaseDnAsString("blah")

        then:
        one.equals(searchDn)
        thrown(IllegalStateException)
    }

    def "getValidUserScopeAccessForClientId adds scopeAccess and deletes old"() {
        given:
        createMocks()
        def scopeAccessOne = new UserScopeAccess()
        def scopeAccessTwo = new UserScopeAccess()
        def scopeAccessThree = new UserScopeAccess()
        def scopeAccessFour = new UserScopeAccess()

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = expiredDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))

        scopeAccessTwo.accessTokenString = "1234"
        scopeAccessTwo.accessTokenExp = refreshDate
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))

        scopeAccessThree.accessTokenString = "1234"
        scopeAccessThree.accessTokenExp = expiredDate
        scopeAccessThree.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid3"))

        scopeAccessFour.accessTokenExp = futureDate
        scopeAccessFour.accessTokenString = "1234"
        scopeAccessFour.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid4"))

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [ scopeAccessTwo, scopeAccessThree, scopeAccessFour ]

        when:
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)
        2 * scopeAccessDao.addDirectScopeAccess(_, _)
        3 * scopeAccessDao.deleteScopeAccess(_)

    }

    def "getValidRackerScopeAccessForClientId adds and deletes scopeAccess as appropriate"() {
        given:
        createMocks()

        def rackerScopeAccessOne = new RackerScopeAccess()
        def rackerScopeAccessTwo = new RackerScopeAccess()
        def rackerScopeAccessThree = new RackerScopeAccess()

        rackerScopeAccessOne.accessTokenString = "12345"
        rackerScopeAccessOne.accessTokenExp = expiredDate
        rackerScopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))

        rackerScopeAccessTwo.accessTokenString = "12345"
        rackerScopeAccessTwo.accessTokenExp = refreshDate
        rackerScopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))

        rackerScopeAccessThree.accessTokenString = "12345"
        rackerScopeAccessThree.accessTokenExp = futureDate
        rackerScopeAccessThree.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid3"))

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> null >>> [ rackerScopeAccessOne, rackerScopeAccessTwo, rackerScopeAccessThree ]

        when:
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")

        then:
        3 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

    }

    def "test parameters for deleteScopeAccessByDn"() {
        given:
        createMocks()

        when:
        service.deleteScopeAccessByDn(dn)
        service.deleteScopeAccessByDn(null)

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(dn)

        then:
        thrown(IllegalArgumentException)
    }

    def "updateScopeAccess updates scopeAccess (no methods using modify accessToken)"() {
        given:
        createMocks()

        def mockedScopeAccess = Mock(ScopeAccess)

        when:
        service.updateScopeAccess(mockedScopeAccess)

        then:
        1 * scopeAccessDao.updateScopeAccess(_)
    }

    def "updateUserScopeAccessTokenForClientIdByUser adds new and deletes old"() {
        given:
        createMocks()

        UserScopeAccess scopeAccessOne = Mock(UserScopeAccess);
        scopeAccessOne.getAccessTokenString() >>> [ "notequal", "token" ]

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccessOne

        applicationDao.getClientByClientId(_) >> new Application().with() {
            it.clientId = "clientId"
            it.RCN = "RCN"
            return it
        }

        when:
        service.updateUserScopeAccessTokenForClientIdByUser(new User(), "clientId", "token", new DateTime().toDate())
        service.updateUserScopeAccessTokenForClientIdByUser(new User(), "clientId", "token", new DateTime().toDate())

        then:
        1 * scopeAccessDao.addDirectScopeAccess(_, _)
        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

    }


    def "provisionUserScopeAccess adds necessary values to scopeAccess"() {
        given:
        createMocks()

        userDao.getUserByDn(_) >> new User().with() {
            it.username = "username"
            it.id = "id"
            return it
        } >> null

        when:
        UserScopeAccess scopeAccessOne = service.provisionUserScopeAccess(dn, sharedRandom)
        UserScopeAccess scopeAccessTwo = service.provisionUserScopeAccess(dn, sharedRandom)

        then:
        scopeAccessOne.getUsername().equals("username")
        scopeAccessOne.getUserRsId().equals("id")
        scopeAccessOne.getClientId().equals(sharedRandom)
        scopeAccessOne.getAccessTokenString() != null
        scopeAccessOne.getAccessTokenExp() != null

        then:
        thrown(NotFoundException)
    }

    def createMocks() {
        userDao = Mock()
        tenantDao = Mock()
        endpointDao = Mock()
        applicationDao = Mock()
        tenantRoleDao = Mock()
        scopeAccessDao = Mock()

        service.userDao = userDao
        service.tenantDao = tenantDao
        service.endpointDao = endpointDao
        service.applicationDao = applicationDao
        service.tenantRoleDao = tenantRoleDao
        service.scopeAcessDao = scopeAccessDao
    }

    def createUserScopeAccess() {
        new UserScopeAccess().with {
            it.clientId = "clientId"
            it.userRsId = "userRsId"
            it.accessTokenString = "string"
            it.accessTokenExp = new Date()
            return it
        }
    }

    def attribute() {
        return new Attribute("attribute", "value")
    }
}
