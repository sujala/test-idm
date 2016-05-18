package com.rackspace.idm.helpers

import com.rackspace.idm.Constants
import org.apache.http.HttpHeaders
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.XPathBody
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.ws.rs.core.MediaType

@Component
class CloudTestUtils {

    private Random random

    @PostConstruct
    def init() {
        random = new Random()
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    def getRandomUUIDOfLength(prefix='', length=50) {
        def uuid = getRandomUUID(prefix)

        while (uuid.length() < length) {
            uuid = uuid + getRandomUUID()
        }

        return uuid[0..<length]
    }

    def getRandomInteger() {
        ((Integer)(100000 + random.nextFloat() * 900000))
    }

    def getRandomIntegerString() {
        String.valueOf(getRandomInteger())
    }

    def String getFeedsXPathForUser(user, eventType) {
        return "//event[@id and @resourceName='$user.username' and @type='$eventType']/product[@displayName='$user.username' and @resourceType='USER' and @serviceCode='CloudIdentity']"
    }

    def HttpRequest createFeedsRequest() {
        return new HttpRequest()
                .withMethod(Constants.POST)
                .withPath(Constants.TEST_MOCK_FEEDS_PATH)
                .withHeaders(new Header(Constants.X_AUTH_TOKEN, ".*"),
                    new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML))
    }

    def HttpRequest createUpdateUserFeedsRequest(user, eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForUser(user, eventType)))
    }

}
