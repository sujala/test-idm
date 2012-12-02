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
    @Shared def dn = "accessToken=123456,cn=TOKENS,ou=users,o=rackspace"
    @Shared def parentDn = "cn=TOKENS,ou=users,o=rackspace"

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        expiredDate = new DateTime().minusHours(12).toDate()
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
        1 * scopeAccessDao.deleteScopeAccessByDn(dn)
    }

    def "updateUserScopeAccessTokenForClientIdByUser deletes existing and adds new scopeAccess"() {
        given:
        createMocks()
        def mockedUser = Mock(User)
        def scopeAccessOne = new UserScopeAccess()

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccessOne

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
        2 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
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

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> scopeAccessTwo

        when:
        service.updateExpiredUserScopeAccess("parentUniqueIdString", "clientId")

        then:
        1 * scopeAccessDao.deleteScopeAccess(scopeAccessOne)
    }

    def "getParentDn returns the parentDn"() {
        given:
        createMocks()

        when:
        def one = service.getParentDn(dn)
        def two = service.getParentDn("blah")

        then:
        one.equals(parentDn)
        two == null
    }

    def "create updated scope access returns scope access with new token"() {
        given:
        def scopeAccessOne = createUserScopeAccess()
        def scopeAccessTwo = createUserScopeAccess()
        def scopeAccess = createUserScopeAccess()

        when:
        scopeAccessOne = service.createUpdatedScopeAccess(scopeAccessOne, false)
        scopeAccessTwo = service.createUpdatedScopeAccess(scopeAccessTwo, true)

        then:
        scopeAccess.clientId == scopeAccessOne.clientId
        scopeAccess.clientId == scopeAccessTwo.clientId
        scopeAccess.userRsId == scopeAccessOne.userRsId
        scopeAccess.userRsId == scopeAccessTwo.userRsId
        scopeAccess.getAccessTokenExp() < scopeAccessOne.getAccessTokenExp()
        scopeAccess.getAccessTokenExp() < scopeAccessTwo.getAccessTokenExp()
        !scopeAccessOne.getAccessTokenString().equals(scopeAccessTwo.getAccessTokenString())
        !scopeAccessTwo.getAccessTokenString().equals(scopeAccess.getAccessTokenString())
    }

    def "getValidUserScopeAccessForClientId adds scopeAccess and deletes old"() {
        given:
        createMocks()
        def scopeAccessOne = new UserScopeAccess()
        def scopeAccessTwo = new UserScopeAccess()
        def scopeAccessThree = new UserScopeAccess()
        def scopeAccessFour = new UserScopeAccess()

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = new DateTime().minusHours(1).toDate()
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))
        scopeAccessTwo.accessTokenString = "1234"
        scopeAccessTwo.accessTokenExp = refreshDate
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))
        scopeAccessThree.accessTokenString = "1234"
        scopeAccessThree.accessTokenExp = expiredDate
        scopeAccessThree.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))
        scopeAccessFour.accessTokenExp = futureDate
        scopeAccessFour.accessTokenString = "1234"
        scopeAccessFour.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid2"))

        scopeAccessDao.getDirectScopeAccessForParentByClientId(_, _) >> [scopeAccessOne].asList()
        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [ scopeAccessTwo, scopeAccessThree, scopeAccessFour ]

        when:
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")
        service.getValidUserScopeAccessForClientId("userUniqueId", "clientId")

        then:
        4 * scopeAccessDao.deleteScopeAccess(_)
        2 * scopeAccessDao.addScopeAccess(_, _)

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
        rackerScopeAccessTwo.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))

        rackerScopeAccessThree.accessTokenString = "12345"
        rackerScopeAccessThree.accessTokenExp = futureDate
        rackerScopeAccessThree.ldapEntry = new ReadOnlyEntry(dn, new Attribute("uid", "uid1"))

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> null >>> [ rackerScopeAccessOne, rackerScopeAccessTwo, rackerScopeAccessThree ]

        when:
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")
        service.getValidRackerScopeAccessForClientId("12345", "12345", "12345")

        then:
        1 * scopeAccessDao.addDirectScopeAccess(_, _)
        2 * scopeAccessDao.addScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
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

    def "updateScopeAccess saves parentDn, then deletes existing and adds updated scopeAccess"() {
        given:
        createMocks()

        def mockedScopeAccess = Mock(ScopeAccess)

        mockedScopeAccess.getUniqueId() >>> [ dn, "asdf/asf" ]

        when:
        service.updateScopeAccess(mockedScopeAccess)
        service.updateScopeAccess(mockedScopeAccess)

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessDao.addDirectScopeAccess(_, mockedScopeAccess)

        then:
        thrown(IllegalStateException)
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
