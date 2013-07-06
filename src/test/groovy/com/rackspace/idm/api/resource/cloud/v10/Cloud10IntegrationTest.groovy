package com.rackspace.idm.api.resource.cloud.v10

import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
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

    def setup() {
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")
        cloud10.authenticate("auth", "thisismykey")
    }

    def cleanupSpec(){
        cloud10.authenticate("auth", "thisismykey")
    }

    def "authenticate using username and apiKey"() {
        when:
        def response = cloud10.authenticate("auth", "thisismykey")
        then:
        response.status == 204
    }

    def "authenticate and verify token entropy"() {
        given:
        def response = cloud10.authenticate("auth", "thisismykey")
        String token = response.headers.get("X-Auth-Token")[0]
        cloud11.revokeToken(token)

        when:
        def startTime = new DateTime()
        def expirationOne = authAndExpire("auth", "thisismykey")
        sleep(10)
        def expirationTwo = authAndExpire("auth", "thisismykey")
        sleep(10)
        def expirationThree = authAndExpire("auth", "thisismykey")
        sleep(10)
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
        def validateResponseOne = cloud20.validateToken(token, token)
        cloud11.revokeToken(token)

        def expiration = validateResponseOne.getEntity(AuthenticateResponse).value.token.expires
        return expiration.toGregorianCalendar().getTime()
    }
}
