package com.rackspace.idm.helpers

import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpHeaders
import org.apache.http.client.utils.URLEncodedUtils
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.XPathBody
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.ws.rs.core.MediaType
import java.nio.charset.StandardCharsets

@Component
class CloudTestUtils {

    @Autowired
    private OTPHelper otpHelper

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

    def getRandomRCN() {
        return String.format("RCN-%d-%d-%d", random.nextInt(900) + 100, random.nextInt(900) + 100, random.nextInt(900) + 100)
    }

    def getOTPCode(String keyUri) {
        def secret = Base32.decode(URLEncodedUtils.parse(new URI(keyUri), "UTF-8").find { it.name == 'secret' }.value)
        return otpHelper.TOTP(secret)
    }

    def extractSessionIdFromWwwAuthenticateHeader(String sessionIdHeader) {
        def matcher = ( sessionIdHeader =~ DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE_VALUE_SESSIONID_REGEX )
        matcher[0][1]
    }

    def randomAlphaStringWithLengthInBytes(int bytes) {
        // English chars are 1 byte in UTF 8
        return RandomStringUtils.randomAlphabetic(bytes)
    }

    def randomAlphaStringWithLenghtInKilobytes(int kilobytes) {
        return randomAlphaStringWithLengthInBytes(kilobytes * 1024)
    }

    def getStringLenghtInBytes(String s) {
        assert s != null
        return s.getBytes(StandardCharsets.UTF_8).length
    }

    def getStringLengthInKilobytes(String s) {
        return getStringLenghtInBytes(s) / 1024
    }

    def String getFeedsXPathForUser(user, eventType) {
        return "//event[@id and @resourceName='$user.username' and @type='$eventType']/product[@displayName='$user.username' and @resourceType='USER' and @serviceCode='CloudIdentity']"
    }

    def String getFeedsXPathForUserTRR(user, AuthenticatedByMethodGroup authenticatedByMethodGroup) {
        def authBy = StringUtils.join(authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues(), " ")
        return "//event[@id and @resourceId='$user.id' and @type='DELETE']/product[@resourceType='TRR_USER' and @serviceCode='CloudIdentity']/tokenAuthenticatedBy[@values='$authBy']"
    }

    def String getFeedsXPathForIdp(idp, eventType) {
        return "//event[@id and @resourceId='$idp.providerId' and @type='$eventType']/product[@resourceType='IDP' and @serviceCode='CloudIdentity' and @issuer='$idp.uri']"
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

    def HttpRequest createUserTrrFeedsRequest(user, AuthenticatedByMethodGroup authenticatedByMethodGroup) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForUserTRR(user, authenticatedByMethodGroup)))
    }

    def HttpRequest createIdpFeedsRequest(idp, eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForIdp(idp, eventType)))
    }

}
