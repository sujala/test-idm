package com.rackspace.idm.api.resource.cloud.v10

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.Base64
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/24/13
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud10IntegrationTest extends Specification {

    @Autowired Configuration config

    @Shared WebResource resource
    @Shared String pathv10 = "cloud/v1.0"
    @Shared String pathv11 = "cloud/v1.1"
    @Shared String pathv20 = "cloud/v2.0"
    @Shared String auth = "auth"
    @Shared String authPwd = "auth123"

    @Shared int defaultExpirationSeconds
    @Shared double entropy


    def setupSpec() {
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")

    }

    def setup() {
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")
    }

    def "authenticate using username and apiKey"() {
        when:
        def response = authenticateV10("auth", "thisismykey")
        then:
        response.status == 204
    }

    def "authenticate and verify token entropy"() {
        given:
        def response = authenticateV10("auth", "thisismykey")
        String token = response.headers.get("X-Auth-Token")[0]
        revokeTokenV11(token)

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
        def token = authenticateV10(username, key).headers.get("X-Auth-Token")[0]
        def validateResponseOne = validateTokenV20(token, token)
        revokeTokenV11(token)

        def expiration = validateResponseOne.getEntity(AuthenticateResponse).value.token.expires
        return expiration.toGregorianCalendar().getTime()
    }

    def getRange(seconds) {
        HashMap<String, Date> range = new HashMap<>()
        def min = new DateTime().plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = new DateTime().plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def getRange(seconds, start, end) {
        HashMap<String, Date> range = new HashMap<>()
        def min = start.plusSeconds((int)Math.floor(seconds * (1 - entropy))).toDate()
        def max = end.plusSeconds((int)Math.ceil(seconds * (1 + entropy))).toDate()
        range.put("min", min)
        range.put("max", max)
        return range
    }

    def authenticateV10(String username, String apiKey) {
        resource.path(pathv10).header("X-Auth-User", username).header("X-Auth-Key", apiKey).get(ClientResponse)
    }

    def revokeTokenV11(String token) {
        resource.path(pathv11).path("token").path(token).header("Authorization", "Basic " + new String(baseEncoding(auth,authPwd))).delete(ClientResponse)
    }

    def validateTokenV20(authToken, token) {
        resource.path(pathv20).path("tokens").path(token).header("X-Auth-Token", authToken).accept(MediaType.APPLICATION_XML).get(ClientResponse)
    }

    def baseEncoding(String username, String password) {
        return Base64.encode(username + ":" + password);
    }
}
