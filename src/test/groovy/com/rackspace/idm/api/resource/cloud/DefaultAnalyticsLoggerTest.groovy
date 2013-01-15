package com.rackspace.idm.api.resource.cloud

import com.google.gson.GsonBuilder
import com.rackspace.idm.domain.service.impl.DefaultUserService
import org.apache.commons.configuration.Configuration
import org.apache.ws.commons.util.Base64
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

        config.getString("cloud.region") >> "US"
    }

    def "logger returns valid uri"() {
        when:
        def result = analyticsLogger.getUri("https://localhost/", "/somepath")
        result = result.replace("https://", "")

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
        analyticsLogger.log(new Date().getTime(), "authToken", null, null, "host", "userAgent", "POST", "users/userId", 200, null, null, null, null)

        then:
        1 * userService.getUserById(_) >> mockedUser
        1 * mockedUser.getUsername()
        1 * mockedUser.getDomainId()
    }

    def "log calls the log handler"() {
        given:
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        analyticsLogger.log(new Date().getTime(), "authToken", null, null, "host", "userAgent", "POST", "users/userId", 200, null, null, null, null)

        then:
        1 * userService.getUserById(_) >> mockedUser
        1 * analyticsLogHandler.log(_)
    }

    def "log sets resource response status, method, and uri"() {
        given:
        userService.getUserById(_) >> Mock(com.rackspace.idm.domain.entity.User)
        def status = 200
        def method = "POST"

        when:
        analyticsLogger.log(new Date().getTime(), "authToken", null, null, "host", "userAgent", method, "users/userId", status, null, null, null, null)

        then:
        analyticsLogHandler.log(_) >> { String json ->
            def message = gsonBuilder().fromJson(json, DefaultAnalyticsLogger.Message.class)
            def resource = message.getAt("resource")
            assert(resource != null)
            assert(resource.getAt("uri") != null)
            assert(resource.getAt("method") == method)
            assert(resource.getAt("responseStatus") == status)
        }
    }

    def "log sets duration and timestamp"() {
        given:
        userService.getUserById(_) >> Mock(com.rackspace.idm.domain.entity.User)

        when:
        analyticsLogger.log(new Date().getTime(), "authToken", null, null, "host", "userAgent", "POST", "users/userId", 200, null, null, null, null)

        then:
        analyticsLogHandler.log(_) >> { String json ->
            def message = gsonBuilder().fromJson(json, DefaultAnalyticsLogger.Message.class)
            assert(message.getAt("duration") != null)
            assert(message.getAt("timestamp") != null)
        }
    }

    def "user is retrieved from basic auth header"() {
        given:
        def userId = "userId"
        def username = "username"
        def basicAuth = createBasicAuth(username, "Password1")
        def mockedUser = Mock(com.rackspace.idm.domain.entity.User)

        when:
        def result = analyticsLogger.getUserIdFromBasicAuth(basicAuth)

        then:
        1 * userService.getUser(username) >> mockedUser
        1 * mockedUser.getId() >> userId
        result == userId
    }

    def "token is removed from path"() {
        expect:
        input.replace("tokenId", analyticsLogger.hashToken("tokenId")) == analyticsLogger.getPathWithoutToken(input)

        where:
        input << [
                "tokens/tokenId/endpoints",
                "tokens/tokenId",
                "tokens",
        ]
    }

    def "remove path is called when logging and token is hashed"() {
        given:
        userService.getUserById(_) >> Mock(com.rackspace.idm.domain.entity.User)

        when:
        analyticsLogger.log(new Date().getTime(), "authToken", null, "localhost", "host", "userAgent", "POST", "tokens/tokenId", 200, "", "", null, null)

        then:
        analyticsLogHandler.log(_) >> { String json ->
            def message = gsonBuilder().fromJson(json, DefaultAnalyticsLogger.Message.class)
            def resource = message.getAt("resource")
            assert(resource != null)
            assert(resource.getAt("uri") == "https://localhost/tokens/" + analyticsLogger.hashToken("tokenId"))
        }
    }

    def "should not log user if the user does not exist"() {
        given:
        userService.getUserById(_) >> null

        when:
        analyticsLogger.log(new Date().getTime(), "authToken", null, null, "host", "userAgent", "POST", "users/userId", 200, "", "", null, null)

        then:
        analyticsLogHandler.log(_) >> { String json ->
            DefaultAnalyticsLogger.Message message = gsonBuilder().fromJson(json, DefaultAnalyticsLogger.Message.class)
            assert message != null
            assert message.user.username == null
        }
    }

    def "can get username from requestBody"() {
        expect:
        analyticsLogger.getUsernameFromRequestBody(requestBody, contentType) == 'myuser'

        where:
        requestBody | contentType
        '{"user": {"username" : "myuser","email": "cmarin1-sub@example.com","enabled": true,"OS-KSADM:password":"Password1"}}' | 'application/json'
        '{"auth": {"RAX-KSKEY:apiKeyCredentials": {"username":"myuser","apiKey": "key"}}}' | 'application/json'
        '{"auth":{"passwordCredentials":{"UserName"  :  \t"myuser" , "password":"theUsersPassword"}}}' | 'application/json'
        '<?xml version="1.0" encoding="UTF-8"?><user xmlns="http://docs.openstack.org/identity/api/v2.0" enabled="true" email="john.smith@example.org" username="myuser"/>' | 'application/xml'
        '<?xml version="1.0" encoding="UTF-8"?><auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"xmlns="http://docs.openstack.org/identity/api/v2.0"><passwordCredentials username = "myuser" password="Password1"/></auth>' | 'application/xml'
    }

    def "hashToken method does token hash"() {
        given:
        def token = "token"

        when:
        def result1 = analyticsLogger.hashToken(token)
        def result2 = analyticsLogger.hashToken(token)

        then:
        result1 == result2
    }

    def "can get the token from responseBody"() {
        expect:
        def tokenParam = analyticsLogger.getTokenFromResponseBody(responseBody, contentType)
        tokenParam.token == "tokenId"
        tokenParam.tokenExp == 1334340900000

        where:
        responseBody | contentType
        '{ "access": { "token": { "expires": "2012-04-13T13:15:00.000-05:00", "id": "tokenId" }, "user": { "RAX-AUTH:defaultRegion": "DFW", "id": "161418", "name": "demoauthor", "roles": [ { "description": "User Admin Role.", "id": "3", "name": "identity:user-    admin" } ] } } }' | 'application/json'
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?> <access xmlns:atom="http://www.w3.org/2005/Atom"> <token id="tokenId" expires="2012-04-13T13:15:00.000-05:00"/> <user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" id=    "161418" name="demoauthor" rax-auth:defaultRegion="DFW"> <roles> <role id="3" name="identity:user-admin" description="User Admin Role."/> </roles> </user> </access>' | 'application/xml'
    }

    private String createBasicAuth(String username, String password) {
        String usernamePassword = (new StringBuffer(username).append(":").append(password)).toString();
        byte[] base = usernamePassword.getBytes();
        return (new StringBuffer("Basic ").append(Base64.encode(base))).toString();
    }

    def gsonBuilder() {
        new GsonBuilder().create()
    }
}
