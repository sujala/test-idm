package com.rackspace.idm.api.resource.cloud.v10

import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/24/13
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */

class Cloud10IntegrationTest extends RootIntegrationTest {

    @Autowired Configuration config

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared String sharedRandom
    @Shared String username
    @Shared String adminToken
    @Shared User user

    def setupSpec(){
        adminToken = cloud10.authenticate("auth", "thisismykey").headers.get("X-Auth-Token")[0]

        sharedRandom = ("$sharedRandomness").replace('-',"")
        username = "auth10Test$sharedRandom"

        def response = cloud20.createUser(adminToken, v2Factory.createUserForCreate(username, "displayName", "testemail@rackspace.com", true, "ORD", "auth10Domain$sharedRandom", "Password1"))
        user = response.getEntity(User)
        cloud20.addApiKeyToUser(adminToken, user.id, v1Factory.createApiKeyCredentials(username, "thisismykey"))
    }

    def setup() {
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")
    }

    def cleanupSpec(){
        cloud20.destroyUser(adminToken, user.id)
    }

    def "authenticate using username and apiKey"() {
        when:
        def response = cloud10.authenticate(username, "thisismykey")
        then:
        response.status == 204
    }

    def "authenticate and verify token entropy"() {
        given:
        def response = cloud10.authenticate(username, "thisismykey")
        String token = response.headers.get("X-Auth-Token")[0]
        cloud11.revokeToken(token)

        when:
        def startTime = new DateTime()
        def expirationOne = authAndExpire(username, "thisismykey")
        def expirationTwo = authAndExpire(username, "thisismykey")
        def expirationThree = authAndExpire(username, "thisismykey")
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)

        then:
        expirationOne >= range.get("min")
        expirationOne <= range.get("max")
        expirationTwo >= range.get("min")
        expirationTwo <= range.get("max")
        expirationThree >= range.get("min")
        expirationThree <= range.get("max")
    }

    def authAndExpire(String username, String key) {
        def token = cloud10.authenticate(username, key).headers.get("X-Auth-Token")[0]
        def validateResponseOne = cloud20.validateToken(adminToken, token)
        cloud11.revokeToken(token)

        def expiration = validateResponseOne.getEntity(AuthenticateResponse).value.token.expires
        return expiration.toGregorianCalendar().getTime()
    }
}
