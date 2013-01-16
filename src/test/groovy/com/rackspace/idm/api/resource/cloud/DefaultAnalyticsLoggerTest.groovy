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
                analyticsLogger.parseUserTokenFromPath("token/tokenId"),
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
                "token/tokenId/endpoints",
                "token/tokenId",
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
            assert message.user == null
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
        result1 != token
        result1 == result2
    }

    def "can get the token from responseBody"() {
        expect:
        def tokenParam = analyticsLogger.getTokenFromResponseBody(responseBody, contentType)
        tokenParam.token == "tokenId"
        tokenParam.tokenExp == "2012-04-13T13:15:00.000-05:00"

        where:
        responseBody | contentType
        '{ "access": { "token": { "expires": "2012-04-13T13:15:00.000-05:00", "id": "tokenId" }, "user": { "RAX-AUTH:defaultRegion": "DFW", "id": "161418", "name": "demoauthor", "roles": [ { "description": "User Admin Role.", "id": "3", "name": "identity:user-    admin" } ] } } }' | 'application/json'
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?> <access xmlns:atom="http://www.w3.org/2005/Atom"> <token id="tokenId" expires="2012-04-13T13:15:00.000-05:00"/> <user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" id=    "161418" name="demoauthor" rax-auth:defaultRegion="DFW"> <roles> <role id="3" name="identity:user-admin" description="User Admin Role."/> </roles> </user> </access>' | 'application/xml'
        '{ "auth": { "access_token": { "expires": "2012-04-13T13:15:00.000-05:00", "id": "tokenId" }, "user": { "RAX-AUTH:defaultRegion": "DFW", "id": "161418", "name": "demoauthor", "roles": [ { "description": "User Admin Role.", "id": "3", "name": "identity:user-    admin" } ] } } }' | 'application/json'
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns3:auth xmlns:ns2="http://www.w3.org/2005/Atom" xmlns:ns3="http://idm.api.rackspace.com/v1.0" xmlns:ns4="http://docs.rackspacecloud.com/auth/api/v1.1" xmlns:ns5="http://docs.openstack.org/common/api/v1.0" xmlns:ns6="http://docs.openstack.org/compute/api/v1.1" xmlns:ns7="http://docs.openstack.org/identity/api/v2.0" xmlns:ns8="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0" xmlns:ns9="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0" xmlns:ns10="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0" xmlns:ns11="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0" xmlns:ns12="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" xmlns:ns13="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0" xmlns:ns14="http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0" xmlns:ns15="http://docs.rackspace.com/core/event" xmlns:ns16="http://fault.common.api.rackspace.com/v1.0" xmlns:ns17="http://migration.api.rackspace.com/v1.0/MigrateUserResponse" daysUntilPasswordExpiration="0" isPasswordResetOnlyToken="false"><ns3:access_token expires="2012-04-13T13:15:00.000-05:00" id="tokenId"/><ns3:user username="auth"><ns3:roles limit="10" offset="0" totalRecords="10"><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="role9531471caa394714a64a13310741d63a" id="10021849"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="rolecf58559257e9402d91742d26e59e5436" id="10021724"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="rolec0df00f0c82f43099948bf9e97bb1b92" id="10021720"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="role72b7a300296f4e238ed2f0228861ae6d" id="10021429"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="roleecd421e088dd472f87900c09320e5ed1" id="10021388"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="rolec92de065423a4cab98c24a0d822bdf2a" id="10021368"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="role5b30fd85368e4fa7abf79e9f363d1bd5" id="10020583"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="role37a106c7e0ac4bbea5ee11e07faaba36" id="10020547"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="role8e278b39ff414130bcc187b0c06e5a83" id="10020027"/><ns3:role applicationId="bde1268ebabeeabb70a0e702a4626977c331d5c4" name="identity:admin" id="1"/></ns3:roles></ns3:user></ns3:auth>' | "application/xml"
    }

    def "calling setusertoken adds token source"() {
        expect:
        analyticsLogger.getTokenSource(token) == tokenSrc

        where:
        token                 | tokenSrc
        "0000000000000000"    | DefaultAnalyticsLogger.IDENTITY
        "0000-0000-0000-0000" | DefaultAnalyticsLogger.CLOUD_AUTH
        null                  | null

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
