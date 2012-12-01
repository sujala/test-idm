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
    @Shared def futureDate = new DateTime().plusHours(12).toDate()
    @Shared def expiredDate = new DateTime().minusHours(12).toDate()
    @Shared def refreshDate = new DateTime().minusHours(1).toDate()

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def "update expired user scope access adds new scope access entity to the directory"() {
        given:
        createMocks()
        def scopeAccess = Mock(UserScopeAccess)

        scopeAccess.isAccessTokenExpired(_) >>> [ true, false ]
        scopeAccess.isAccessTokenWithinRefreshWindow(_) >>> [ true, false ]
        scopeAccess.getUniqueId() >> "accessToken=12345,cn=TOKENS,ou=Users"

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
        scopeAccessOne.accessTokenExp = new DateTime().minusHours(1).toDate()
        scopeAccessOne.ldapEntry = new ReadOnlyEntry("cn=12345,ou=users", new Attribute("uid", "uid1"))
        scopeAccessTwo.accessTokenString = "1234"
        scopeAccessTwo.accessTokenExp = new DateTime().plusHours(1).toDate()
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry("cn=1234,ou=users", new Attribute("uid", "uid2"))

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
        def one = service.getParentDn("accessToken=12345,cn=TOKENS,ou=users")
        def two = service.getParentDn("blah")

        then:
        one.equals("cn=TOKENS,ou=users")
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
        scopeAccessOne.ldapEntry = new ReadOnlyEntry("cn=12345,ou=users", new Attribute("uid", "uid1"))
        scopeAccessTwo.accessTokenString = "1234"
        scopeAccessTwo.accessTokenExp = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).minusHours(1).toDate()
        scopeAccessTwo.ldapEntry = new ReadOnlyEntry("cn=1234,ou=users", new Attribute("uid", "uid2"))
        scopeAccessThree.accessTokenString = "1234"
        scopeAccessThree.accessTokenExp = new DateTime().minusHours(1).toDate()
        scopeAccessThree.ldapEntry = new ReadOnlyEntry("cn=1234,ou=users", new Attribute("uid", "uid2"))
        scopeAccessFour.accessTokenExp = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).plusHours(2).toDate()
        scopeAccessFour.accessTokenString = "1234"
        scopeAccessFour.ldapEntry = new ReadOnlyEntry("cn=1234,ou=users", new Attribute("uid", "uid2"))

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
        rackerScopeAccessOne.accessTokenExp = new DateTime().minusHours(1).toDate()
        rackerScopeAccessOne.ldapEntry = new ReadOnlyEntry("cn=12345,ou=users,dn=com", new Attribute("uid", "uid1"))

        rackerScopeAccessTwo.accessTokenString = "12345"
        rackerScopeAccessTwo.accessTokenExp = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).minusHours(2).toDate()
        rackerScopeAccessTwo.ldapEntry = new ReadOnlyEntry("cn=12345,ou=users,dn=com", new Attribute("uid", "uid1"))

        rackerScopeAccessThree.accessTokenString = "12345"
        rackerScopeAccessThree.accessTokenExp = new DateTime().plusHours(config.getInt("token.refreshWindowHours")).plusHours(2).toDate()
        rackerScopeAccessThree.ldapEntry = new ReadOnlyEntry("cn=12345,ou=users,dn=com", new Attribute("uid", "uid1"))

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
}
