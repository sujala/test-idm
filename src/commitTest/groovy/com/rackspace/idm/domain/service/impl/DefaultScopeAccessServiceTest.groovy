package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.*
import org.joda.time.DateTime
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultScopeAccessServiceTest extends RootServiceTest {

    @Shared DefaultScopeAccessService service = new DefaultScopeAccessService()
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def dn = "accessToken=123456,cn=TOKENS,rsId=12345,ou=users,o=rackspace"
    @Shared def searchDn = "rsId=12345,ou=users,o=rackspace"

    @Shared def expiredDate
    @Shared def refreshDate
    @Shared def futureDate

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")

        expiredDate = new DateTime().minusHours(1).toDate()
        refreshDate = new DateTime().plusHours(defaultRefreshHours - 1).toDate()
        futureDate = new DateTime().plusHours(defaultRefreshHours + 1).toDate()
    }

    def setup() {
        mockConfiguration(service)
        mockScopeAccessDao(service)
        mockUserService(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockApplicationService(service)
        mockIdentityUserService(service)
        mockTokenFormatSelector(service)
        mockIdentityConfig(service)

        config.getInt("token.cloudAuthExpirationSeconds", _) >>  defaultCloudAuthExpirationSeconds
        config.getInt("token.cloudAuthRackerExpirationSeconds", _) >>  defaultCloudAuthRackerExpirationSeconds

    }

    def "getValidUserScopeAccessForClientId adds scopeAccess"() {
        given:
        def user = entityFactory.createUser()
        def scopeAccess = createUserScopeAccess("expiredOne", "userRsId", "clientId", expiredDate)

        when:
        service.addScopeAccess(user, "clientId", null)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _)
    }

    def "provisionUserScopeAccess adds necessary values to scopeAccess"() {
        given:
        def user = new User().with() {
            it.username = "username"
            it.id = "id"
            return it
        }

        when:
        UserScopeAccess scopeAccessOne = service.provisionUserScopeAccess(user, sharedRandom)

        then:
        scopeAccessOne.getUserRsId().equals("id")
        scopeAccessOne.getClientId().equals(sharedRandom)
        scopeAccessOne.getAccessTokenString() != null
        scopeAccessOne.getAccessTokenExp() != null
    }

    def "can get endpoints from getOpenstackEndpointsForUser"() {
        given:
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant("tenantId", "tenantName")
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = "roleRsId"
            it.tenantIds = [ "tenantId" ].asList()
            return it
        }
        def tenantRoles = [ tenantRole ].asList()
        def endpoint = entityFactory.createOpenstackEndpoint("tenantId", "tenantName").with {
            it.baseUrls = [ entityFactory.createCloudBaseUrl() ].asList()
            return it
        }

        when:
        def endpoints = service.getOpenstackEndpointsForUser(user)

        then:
        endpoints != null
        endpoints.size() == 1
        tenantService.getTenantRolesForUser(user) >> tenantRoles
        tenantService.getTenant(_) >> tenant
        endpointService.getOpenStackEndpointForTenant(_, _, _,_) >> endpoint
        applicationService.getById(_) >> new Application().with {
            it.openStackType = "compute"
            it
        }
    }

    def "isScopeAccessExpired returns true when scopeAccess is null"() {
        when:
        def result = service.isScopeAccessExpired(null)

        then:
        result == true
    }

    def "isScopeAccessExpired returns true when scopeAccess is expired"() {
        given:
        def scopeAccess = expireScopeAccess(createUserScopeAccess())

        when:
        def result = service.isScopeAccessExpired(scopeAccess)

        then:
        result == true
    }

    def "isScopeAccessExpired returns false when scopeAccess is not expired"() {
        given:
        def scopeAccess = createUserScopeAccess()

        when:
        def result = service.isScopeAccessExpired(scopeAccess)

        then:
        result == false
    }

    def "calling getValidRackerScopeAccessForClientId sets authenticatedBy"() {
        given:
        def authenticatedBy = ["RSA"].asList()

        when:
        service.getValidRackerScopeAccessForClientId(entityFactory.createRacker(), "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling addScopeAccess sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()
        def authenticatedBy = ["RSA"].asList()

        when:
        service.addScopeAccess(user, "clientId", authenticatedBy)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == authenticatedBy as Set)
        }
    }

    def "calling getUserScopeAccessForClientIdByUsernameAndPassword sets authenticatedBy"() {
        given:
        def user = entityFactory.createUser()

        when:
        service.getUserScopeAccessForClientIdByUsernameAndPassword("username", "password", "clientId")

        then:
        1 * userService.authenticate(_, _) >> new UserAuthenticationResult(user, true)

        then:
        1 * scopeAccessDao.addScopeAccess(_, _) >> { arg1, ScopeAccess scopeAccess ->
            assert (scopeAccess.authenticatedBy as Set == [GlobalConstants.AUTHENTICATED_BY_PASSWORD] as Set)
        }
    }

    def "Verify provision user scope access adds token expiration entropy"() {
        when:
        def range = getRange(exSeconds, entropy)
        UserScopeAccess scopeAccess = service.provisionUserScopeAccess(entityFactory.createUser(), "clientId")

        then:
        1 * config.getInt("token.cloudAuthExpirationSeconds", _) >> exSeconds
        1 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= ((Date)range.get("max"))
        scopeAccess.accessTokenExp >= range.get("min")

        where:
        exSeconds | entropy
        2600      | 0.01

    }

    def "Set entropy"() {
        given:
        def seconds = 2600
        def max = seconds * 1.01
        def min = seconds * 0.99

        when:
        def result = service.getTokenExpirationSeconds(seconds)

        then:
        1 * config.getDouble(_) >> 0.01
        result <= max
        result >= min
    }

    def "calling getValidRackerScopeAccessForClientId sets expiration with entropy"() {
        given:
        def entropy = 0.01
        def clientId = "clientId"
        def authedBy = ["PASSWORD"].asList()
        def range = getRange(defaultCloudAuthRackerExpirationSeconds, entropy)

        when:
        def scopeAccess = service.getValidRackerScopeAccessForClientId(entityFactory.createRacker(), clientId, authedBy)

        then:
        1 * config.getDouble("token.entropy") >> entropy
        scopeAccess.accessTokenExp <= range.get("max")
        scopeAccess.accessTokenExp >= range.get("min")
    }

    def "authenticateAccessToken returns true when token is valid" () {
        given:
        def sa = entityFactory.createScopeAccess()
        sa.accessTokenExp = new Date()
        sa.accessTokenExp.hours += 24

        when:
        def result = service.authenticateAccessToken("abc123")

        then:
        1 * scopeAccessDao.getScopeAccessByAccessToken(_) >> sa
        result == true
    }

    def "authenticateAccessToken with null token returns false" () {
        when:
        def result = service.authenticateAccessToken("abc123")

        then:
        1 * scopeAccessDao.getScopeAccessByAccessToken(_) >> null
        result == false
    }

    def "calling getOpenStackType returns the correct value"() {
        given:
        def role = entityFactory.createTenantRole()
        def application = entityFactory.createApplication().with {
            it.openStackType = applicationType
            it
        }

        when:
        def result = service.getOpenStackType(role)

        then:
        1 * applicationService.getById(_) >> application
        if(result){
            result.getName() == expectedResult
        } else {
            result == expectedResult
        }

        where:
        applicationType | expectedResult
        "object-store"  | "NAST"
        "compute"       | "MOSSO"
        null            | null
        ""              | null
    }

    def getRange(seconds, entropy) {
        /*
        to account for processing time, add +- the fudgeSeconds to the calculated range.

        - We add to the max in case this range is calculated before the call to generate the token - which means as long
        as it doesn't take longer than the fudgeSeconds between the time we calculate the range and when the code generates
        the token we're guaranteed that the token expiration will fall before the max of the range

        - We subtract from the min in case this range is calculated after the call to generate the token - which means as long
        as it doesn't take longer than the fudgeSeconds between the time the code generates the token and we calculate this
        range we're guaranteed that the token expiration will fall after the min of the range
         */
        int fudgeSeconds = 30
        HashMap<String, Date> range = new HashMap<>()
        range.put("min", new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))-fudgeSeconds).toDate())
        range.put("max", new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))+fudgeSeconds).toDate())
        return range
    }
}
