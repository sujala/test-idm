package com.rackspace.idm.api.resource.cloud.v10

import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/24/13
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud10IntegrationTest extends RootIntegrationTest {

    @Autowired Configuration config

    def setupSpec() {
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
    }

    def setup() {
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")
    }

    def "authenticate using username and apiKey"() {
        when:
        def response = authenticate10("auth", "thisismykey")
        then:
        response.status == 204
    }

    def "authenticate and verify token entropy"() {
        given:
        def response = authenticate10("auth", "thisismykey")
        String token = response.headers.get("X-Auth-Token")[0]
        revokeToken11("auth", "auth123", token)

        when:
        def startTime = new DateTime()
        def expirationOne = authAndExpire("auth", "thisismykey")
        def expirationTwo = authAndExpire("auth", "thisismykey")
        def expirationThree = authAndExpire("auth", "thisismykey")
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)

        then:
        expirationOne >= range.get("min")
        expirationOne <= range.get("max")
        expirationTwo >= range.get("min")
        expirationTwo >= range.get("min")
        expirationThree <= range.get("max")
        expirationThree <= range.get("max")
    }

    def authAndExpire(String username, String key) {
        def token = authenticate10(username, key).headers.get("X-Auth-Token")[0]
        def validateResponseOne = validateToken20(token, token)
        revokeToken11("auth", "auth123", token)

        def expiration = validateResponseOne.getEntity(AuthenticateResponse).value.token.expires
        return expiration.toGregorianCalendar().getTime()
    }
}
