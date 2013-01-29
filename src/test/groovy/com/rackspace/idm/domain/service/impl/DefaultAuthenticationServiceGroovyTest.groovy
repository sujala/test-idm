package com.rackspace.idm.domain.service.impl

import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.impl.*
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.util.RSAClient
import com.rackspace.idm.validation.InputValidator
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.apache.commons.configuration.Configuration
import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Credentials
import com.rackspace.idm.domain.entity.ClientAuthenticationResult
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/1/12
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultAuthenticationServiceTest extends Specification {

    @Autowired private DefaultAuthenticationService service
    @Autowired private TenantService tenantService
    @Autowired private ScopeAccessService scopeAccessService
    @Autowired private InputValidator inputValidator
    @Autowired private Configuration configuration
    @Autowired private RSAClient rsaClient

    @Shared LdapApplicationRepository applicationDao
    @Shared LdapAuthRepository authDao
    @Shared LdapUserRepository userDao
    @Shared LdapCustomerRepository customerDao
    @Shared LdapScopeAccessPeristenceRepository scopeAccessDao
    @Shared LdapTenantRoleRepository tenantRoleDao

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,ou=users,o=rackspace"
    @Shared def parentDn = "cn=TOKENS,ou=users,o=rackspace"
    @Shared def expiredDate
    @Shared def refreshDate
    @Shared def futureDate

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        expiredDate = new DateTime().minusHours(configuration.getInt("token.refreshWindowHours")).minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(configuration.getInt("token.refreshWindowHours")).minusHours(2).toDate()
        futureDate = new DateTime().plusHours(configuration.getInt("token.refreshWindowHours")).plusHours(2).toDate()
    }

    def "getAndUpdateUserScopeAccessForClientId adds new scope access and deletes old"() {
        given:
        createMocks()
        def mockedUserOne = Mock(User)
        def mockedUserTwo = Mock(User)
        def mockedApplication = Mock(Application)
        def scopeAccessOne = new UserScopeAccess()
        def stableScopeAccess = new UserScopeAccess()

        mockedUserOne.getUniqueId() >> "1"
        mockedUserTwo.getUniqueId() >> "2"

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = expiredDate
        scopeAccessOne.refreshTokenExp = expiredDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())

        stableScopeAccess.accessTokenString = "12345"
        stableScopeAccess.accessTokenExp = expiredDate
        stableScopeAccess.refreshTokenExp = expiredDate
        stableScopeAccess.ldapEntry = new ReadOnlyEntry(dn, attribute())


        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [
                null,
                scopeAccessOne
        ]

        when:
        service.getAndUpdateUserScopeAccessForClientId(mockedUserOne, mockedApplication)
        service.getAndUpdateUserScopeAccessForClientId(mockedUserTwo, mockedApplication)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('1', _) >> { arg1, arg2 ->
            assert(arg2.refreshTokenExp != null)
            assert(arg2.refreshTokenString != null)
            assert(arg2.accessTokenExp != null)
            assert(arg2.accessTokenString != null)
        }

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('2', _) >> { arg1, arg2 ->
            assert(! stableScopeAccess.accessTokenString.equals(arg2.accessTokenString))
            assert(! stableScopeAccess.refreshTokenString.equals(arg2.refreshTokenString))
            assert(stableScopeAccess.accessTokenExp.before(arg2.accessTokenExp))
            assert(stableScopeAccess.refreshTokenExp.before(arg2.refreshTokenExp))
        }
    }

    def "getAndUpdateClientScopeAccessForClientId adds new scopeAccess and deletes old"() {
        given:
        createMocks()
        def mockedAppOne = Mock(Application)
        def mockedAppTwo = Mock(Application)
        def scopeAccessOne = new ClientScopeAccess()
        def stableScopeAccess = new ClientScopeAccess()

        mockedAppOne.getUniqueId() >> "1"
        mockedAppOne.getRCN() >> "rcn"
        mockedAppOne.getClientId() >> "clientId"

        mockedAppTwo.getUniqueId() >> "2"
        mockedAppTwo.getRCN() >> "rcn"
        mockedAppTwo.getClientId() >> "clientId"

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = expiredDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())

        stableScopeAccess.accessTokenString = "12345"
        stableScopeAccess.accessTokenExp = expiredDate
        stableScopeAccess.ldapEntry = new ReadOnlyEntry(dn, attribute())

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [
                null,
                scopeAccessOne
        ]

        when:
        service.getAndUpdateClientScopeAccessForClientId(mockedAppOne)
        service.getAndUpdateClientScopeAccessForClientId(mockedAppTwo)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('1', _) >> { arg1, arg2 ->
            assert(arg2.clientId != null)
            assert(arg2.clientRCN != null)
            assert(arg2.accessTokenExp != null)
            assert(arg2.accessTokenString != null)
        }

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('2', _) >> { arg1, arg2 ->
            assert(! stableScopeAccess.accessTokenString.equals(arg2.accessTokenString))
            assert(stableScopeAccess.accessTokenExp.before(arg2.accessTokenExp))
        }
    }

    def "getAndUpdateRackerScopeAccessForClientId deletes old and adds new"() {
        given:
        createMocks()
        def mockedAppOne = Mock(Application)
        def mockedRackerOne = Mock(Racker)
        def mockedRackerTwo = Mock(Racker)
        def mockedTenantRole = Mock(TenantRole)
        def mockedClientRole = Mock(ClientRole)

        def scopeAccessOne = new RackerScopeAccess()
        def stableScopeAccess = new RackerScopeAccess()
        def tenantRoleList = new ArrayList<TenantRole>()

        mockedAppOne.getUniqueId() >> "1"
        mockedAppOne.getRCN() >> "rcn"
        mockedAppOne.getClientId() >> "clientId"

        mockedRackerOne.getRackerId() >> "rackerId"
        mockedRackerOne.getUniqueId() >> "1"

        mockedRackerTwo.getRackerId() >> "rackerId"
        mockedRackerTwo.getUniqueId() >> "2"

        mockedTenantRole.getName() >> "Racker"
        mockedTenantRole.getClientId() >> configuration.getString("idm.clientId")
        mockedTenantRole.getRoleRsId() >> "roleRsId"

        tenantRoleList.add(mockedTenantRole)

        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = expiredDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())

        stableScopeAccess.accessTokenString = "12345"
        stableScopeAccess.accessTokenExp = expiredDate
        stableScopeAccess.ldapEntry = new ReadOnlyEntry(dn, attribute())

        scopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(_, _) >>> [
                null,
                scopeAccessOne
        ]

        tenantService.getTenantRolesForUser(_) >> new ArrayList<TenantRole>()

        mockedClientRole.getName() >> "Racker"

        tenantRoleDao.getTenantRolesForScopeAccess(_) >> tenantRoleList
        applicationDao.getClientRoleById(_) >> mockedClientRole
        applicationDao.getClientRolesByClientId(_) >> [ mockedClientRole ].asList()

        applicationDao.getClientByClientId(_) >> Mock(Application)
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> Mock(ClientRole)

        when:
        service.getAndUpdateRackerScopeAccessForClientId(mockedRackerOne, mockedAppOne)
        service.getAndUpdateRackerScopeAccessForClientId(mockedRackerTwo, mockedAppOne)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('1', _) >> { arg1, arg2 ->
            assert(arg2.clientId != null)
            assert(arg2.clientRCN != null)
            assert(arg2.rackerId != null)
            assert(arg2.accessTokenExp != null)
            assert(arg2.accessTokenString != null)
        }

        then:
        1 * scopeAccessDao.deleteScopeAccessByDn(_)

        then:
        1 * scopeAccessDao.addDirectScopeAccess('2', _) >> { arg1, arg2 ->
            assert(! stableScopeAccess.accessTokenString.equals(arg2.accessTokenString))
            assert(stableScopeAccess.accessTokenExp.before(arg2.accessTokenExp))
        }
    }

    def "getTokens with refreshtoken credentials adds new and deletes old scopeaccess"() {
        given:
        createMocks()

        def scopeAccessOne = new UserScopeAccess();
        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = refreshDate
        scopeAccessOne.refreshTokenString = "12345refresh"
        scopeAccessOne.refreshTokenExp = refreshDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessOne.userRsId = "userRsId"
        scopeAccessOne.username = "username"
        scopeAccessOne.clientId = "clientId"


        def credentials = Mock(Credentials)
        credentials.getOAuthGrantType() >> "REFRESH_TOKEN"
        credentials.getGrantType() >> "REFRESH_TOKEN"
        credentials.getClientId() >> "12345"

        def authResult = Mock(ClientAuthenticationResult)
        authResult.isAuthenticated() >> true
        authResult.getClient() >> new Application().with() {
            it.clientId = "clientId"
            return it
        }

        userDao.getUserById(_) >> new User().with() {
            it.enabled = true
            return it
        }

        applicationDao.authenticate(_, _) >> authResult

        scopeAccessDao.getScopeAccessByRefreshToken(_) >> scopeAccessOne

        when:
        ScopeAccess returned = service.getTokens(credentials, new DateTime())

        then:
        returned.clientId == "clientId"
        1 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def "getTokens with Authorization Code credentials adds new and deletes old scopeaccess"() {
        given:
        createMocks()
        def scopeAccessOne = new DelegatedClientScopeAccess();
        scopeAccessOne.accessTokenString = "12345"
        scopeAccessOne.accessTokenExp = refreshDate
        scopeAccessOne.authCode = "authCode12345"
        scopeAccessOne.authCodeExp = refreshDate
        scopeAccessOne.ldapEntry = new ReadOnlyEntry(dn, attribute())
        scopeAccessOne.clientId = "clientId"

        def credentials = Mock(Credentials)
        credentials.getOAuthGrantType() >> "AUTHORIZATION_CODE"
        credentials.getGrantType() >> "AUTHORIZATION_CODE"
        credentials.getClientId() >> "12345"

        def authResult = Mock(ClientAuthenticationResult)
        authResult.isAuthenticated() >> true
        authResult.getClient() >> new Application().with() {
            it.clientId = "clientId"
            return it
        }

        applicationDao.authenticate(_, _) >> authResult

        scopeAccessDao.getScopeAccessByAuthorizationCode(_) >> scopeAccessOne

        when:
        ScopeAccess returned = service.getTokens(credentials, new DateTime())

        then:
        1 * scopeAccessDao.addDirectScopeAccess(_, _)
        1 * scopeAccessDao.deleteScopeAccess(_)
    }

    def createMocks() {
        applicationDao = Mock()
        authDao = Mock()
        userDao = Mock()
        customerDao = Mock()
        scopeAccessDao = Mock()
        tenantRoleDao = Mock()

        service.applicationDao = applicationDao
        service.userDao = userDao

        scopeAccessService.scopeAcessDao = scopeAccessDao

        tenantService.tenantRoleDao = tenantRoleDao
        tenantService.applicationDao = applicationDao

        service.tenantService = tenantService

    }

    def attribute() {
            return new Attribute("attribute", "value")
        }

    }
