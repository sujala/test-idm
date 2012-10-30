package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.impl.LdapScopeAccessPeristenceRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/29/12
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultCloud20ServiceTestGroovy extends Specification {

    @Autowired DefaultUserService userService
    @Autowired Configuration configuration
    @Autowired DefaultCloud20Service cloud20Service

    @Shared DefaultScopeAccessService scopeAccessService
    @Shared DefaultAuthorizationService authorizationService
    @Shared LdapUserRepository userDao
    @Shared LdapScopeAccessPeristenceRepository scopeAccessDao
    @Shared DefaultCloudRegionService cloudRegionService

    @Shared def authToken = "token"
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
    }

    def cleanupSpec() {
    }

    def "create a user sets the default region"() {
        given:
        createMocks()

        ScopeAccess accessToken = scopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(_) >> accessToken

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> region()

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        user.region == "DFW"
    }

    def "create user without default region throws bad request"() {
        given:
        createMocks()

        ScopeAccess accessToken = scopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(_) >> accessToken

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> null

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        thrown(BadRequestException)
    }

    def createMocks() {
        scopeAccessDao = Mock()

        scopeAccessService = Mock()
        cloud20Service.scopeAccessService = scopeAccessService

        authorizationService = Mock()
        cloud20Service.authorizationService = authorizationService

        userDao = Mock()
        userService.userDao = userDao
        userService.scopeAccesss = scopeAccessDao

        cloudRegionService = Mock()
        userService.cloudRegionService = cloudRegionService
    }

    def user(String username, String email, Boolean enabled, String displayName) {
        new User().with {
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            return it
        }
    }

    def region() {
        new Region().with {
            it.cloud = "US"
            it.name = "DFW"
            it.isEnabled = true
            it.isDefault = true
            return it
        }
    }

    def scopeAccess() {
        new ClientScopeAccess().with {
            Calendar calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            it.accessTokenExp = calendar.getTime()
            it.accessTokenString = calendar.getTime().toString()
            return it
        }
    }
}
