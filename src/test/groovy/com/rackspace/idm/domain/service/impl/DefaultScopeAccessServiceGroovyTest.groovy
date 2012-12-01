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
        def scopeAccess = createUserScopeAccess()
        when:
        def c1 = service.createUpdatedScopeAccess(scopeAccess, false)
        def c2 = service.createUpdatedScopeAccess(scopeAccess, true)

        then:
        c1.clientId == scopeAccess.clientId
        c1.clientId == c2.clientId
        c2.userRsId == c1.userRsId
        c2.userRsId == scopeAccess.userRsId
        c1.getAccessTokenExp() > scopeAccess.getAccessTokenExp()
        c2.getAccessTokenExp() > scopeAccess.getAccessTokenExp()
        !c1.getAccessTokenString().equals(scopeAccess.getAccessTokenString())
        !c2.getAccessTokenString().equals(scopeAccess.getAccessTokenString())
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
