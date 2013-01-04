package com.rackspace.idm.api.resource.cloud

import com.rackspace.idm.domain.service.impl.DefaultUserService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Specification

class DefaultAnalyticsLoggerTest extends Specification {

    @Shared DefaultUserService userService
    @Shared DefaultAnalyticsLogger analyticsLogger
    @Shared Configuration config
    @Shared DefaultAnalyticsLogHandler analyticsLogHandler

    def setupSpec() {
        analyticsLogger = new DefaultAnalyticsLogger()
    }

    def setup() {
        config = Mock()
        analyticsLogger.config = config

        userService = Mock()
        analyticsLogger.userService = userService

        analyticsLogHandler = Mock()
        analyticsLogger.analyticsLogHandler = analyticsLogHandler

        config.getString("ga.endpoint") >> "http://localhost"
        config.getString("cloud.region") >> "US"
    }

    def "logger returns valid uri"() {
        when:
        def result = analyticsLogger.getUri("http://localhost/", "/somepath")
        result = result.replace("http://", "")

        then:
        ! result.contains("//")
    }

    def "userService is called when getting userId"() {
        given:
        def userId = "id"
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        def result = analyticsLogger.getUserIdFromAuthToken("auth-token")

        then:
        1 * userService.getUserByAuthToken(_) >> mockedUser
        1 * mockedUser.getId() >> userId
        result == userId
    }

    def "userService is called and returns null when getting userId"() {
        when:
        def result = analyticsLogger.getUserIdFromAuthToken("auth-token")

        then:
        1 * userService.getUserByAuthToken(_) >> null
        result == null
    }

    def "port is removed from the host"() {
        expect:
        result == "hostname"

        where:
        result << [
                analyticsLogger.getHost("hostname"),
                analyticsLogger.getHost("hostname:8000")
        ]
    }

    def "can parse userId from path"() {
        expect:
        result == "userId"

        where:
        result << [
                analyticsLogger.parseUserIdFromPath("users/userId"),
                analyticsLogger.parseUserIdFromPath("users/userId/1"),
                analyticsLogger.parseUserIdFromPath("users/userId/RAX-AUTH/domains"),
                analyticsLogger.parseUserIdFromPath("tenants/1/users/userId/roles"),
        ]
    }

    def "parse userId from the path returns null"() {
        expect:
        result == null

        where:
        result << [
                analyticsLogger.parseUserIdFromPath("users"),
                analyticsLogger.parseUserIdFromPath("tenants"),
        ]
    }

    def "can parse token from path"() {
        expect:
        result == "tokenId"

        where:
        result << [
                analyticsLogger.parseUserTokenFromPath("tokens/tokenId"),
                analyticsLogger.parseUserTokenFromPath("tokens/tokenId/endpoints"),
        ]
    }

    def "parse token from the path returns null"() {
        expect:
        result == null

        where:
        result << [
                analyticsLogger.parseUserTokenFromPath("tenants"),
                analyticsLogger.parseUserTokenFromPath("tokens"),
                analyticsLogger.parseUserTokenFromPath("RAX-AUTH/impersonation-tokens"),
        ]
    }

    def "has token calls getUserByAuthToken"() {
        given:
        def userId = "id"
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        def result = analyticsLogger.getUserIdFromPath("tokens/tokenId")

        then:
        1 * userService.getUserByAuthToken(_) >> mockedUser
        1 * mockedUser.getId() >> userId
        result == userId
    }

    def "has userId returns the userId"() {
        expect:
        result == "userId"

        where:
        result << [
                analyticsLogger.getUserIdFromPath("users/userId")
        ]
    }

    def "has no userId or token returns null"() {
        expect:
        result == null

        where:
        result << [
                analyticsLogger.getUserIdFromPath("nothing/here")
        ]
    }

    def "log gets the user and sets the username and domain"() {
        given:
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        analyticsLogger.log("authToken", "host", "userAgent", "POST", "users/userId")

        then:
        1 * userService.getUserById(_) >> mockedUser
        1 * mockedUser.getUsername()
        1 * mockedUser.getDomainId()
    }

    def "log calls the log handler"() {
        given:
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        analyticsLogger.log("authToken", "host", "userAgent", "POST", "users/userId")

        then:
        1 * userService.getUserById(_) >> mockedUser
        1 * analyticsLogHandler.log(_)
    }
}
