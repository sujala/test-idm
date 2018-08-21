package com.rackspace.idm.helpers

import com.rackspace.docs.core.event.EventType
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.email.EmailTemplateConstants
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpHeaders
import org.apache.http.client.utils.URLEncodedUtils
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.XPathBody
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.EmailUtils

import javax.annotation.PostConstruct
import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

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

    def invertStringCase(String str) {
        if (StringUtils.isEmpty(str)) return

        def chars = str.toCharArray()
        for (int i : 0..<chars.size()) {
            chars[i] = chars[i].isUpperCase() ? chars[i].toLowerCase() : chars[i].toUpperCase()
        }

        new String(chars)
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

    String getFeedsXPathForUser(user, eventType) {
        def username = user instanceof com.rackspacecloud.docs.auth.api.v1.User ? user.id : user.username
        return "//event[@id and @resourceName='${username}' and @type='$eventType']/product[@displayName='${username}' and @resourceType='USER' and @serviceCode='CloudIdentity']"
    }

    String getFeedsXPathForFedUser(user, eventType) {
        def federatedIdp = user instanceof User ? user.federatedIdp : user?.federatedIdpUri
        return "//event[@id and @resourceName='${user?.username}@${federatedIdp}' and @type='$eventType']/product[@displayName='${user?.username}@${federatedIdp}' and @resourceType='USER' and @serviceCode='CloudIdentity']"
    }

    String getFeedsXPathForUserTRR(user, AuthenticatedByMethodGroup authenticatedByMethodGroup) {
        def authBy = StringUtils.join(authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues(), " ")
        return "//event[@id and @resourceId='$user.id' and @type='DELETE']/product[@resourceType='TRR_USER' and @serviceCode='CloudIdentity']/tokenAuthenticatedBy[@values='$authBy']"
    }

    String getFeedsXPathForUserTRR(user) {
        return "//event[@id and @resourceId='$user.id' and @type='DELETE']/product[@resourceType='TRR_USER' and @serviceCode='CloudIdentity']"
    }

    String getFeedsXPathForTokenTRR(String token) {
        return "//event[@id and @resourceId='$token' and @type='DELETE']/product[@resourceType='TOKEN' and @serviceCode='CloudIdentity']"
    }

    String getFeedsXPathForIdp(idp, eventType) {
        return "//event[@id and @resourceId='$idp.providerId' and @type='$eventType']/product[@resourceType='IDP' and @serviceCode='CloudIdentity' and @issuer='$idp.uri']"
    }

    String getFeedsXPathForPasswordCredentialChange(User user, EventType eventType) {
        return "//event[@id and @resourceId='${user.id}' and @type='${eventType.name()}']/" +
                "product[@resourceType='USER' and @serviceCode='CloudIdentity' and @userId='${user.id}' and @username='${user.username}' and " +
                         "@email='${user.email}' and @domainId='${user.domainId}' and @credentialType='PASSWORD' and @credentialUpdateDateTime and " +
                         "@requestId]"
    }

    String getFeedsXPathForPasswordCredentialChangeWithRequestId(User user, EventType eventType, String requestId) {
        return "//event[@id and @resourceId='${user.id}' and @type='${eventType.name()}']/" +
                "product[@resourceType='USER' and @serviceCode='CloudIdentity' and @userId='${user.id}' and @username='${user.username}' and " +
                "@email='${user.email}' and @domainId='${user.domainId}' and @credentialType='PASSWORD' and @credentialUpdateDateTime and " +
                "@requestId='${requestId}']"
    }

    HttpRequest createFeedsRequest() {
        return new HttpRequest()
                .withMethod(Constants.POST)
                .withPath(Constants.TEST_MOCK_FEEDS_PATH)
                .withHeaders(new Header(Constants.X_AUTH_TOKEN, ".*"),
                    new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML))
    }

    void assertStringPattern(String pattern, String value) {
        Pattern stringPattern = Pattern.compile(pattern)
        assert stringPattern.matcher(value).matches()
    }

    HttpRequest createUserFeedsRequest(user, eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForUser(user, eventType)))
    }

    HttpRequest createFedUserFeedsRequest(user, eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForFedUser(user, eventType)))
    }

    HttpRequest createUserTrrFeedsRequest(user, AuthenticatedByMethodGroup authenticatedByMethodGroup = null) {
        if (authenticatedByMethodGroup) {
            return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForUserTRR(user, authenticatedByMethodGroup)))
        } else {
            return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForUserTRR(user)))
        }
    }

    HttpRequest createIdpFeedsRequest(IdentityProvider idp, eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForIdp(idp, eventType)))
    }

    HttpRequest createTokenFeedsRequest(String token) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForTokenTRR(token)))
    }

    HttpRequest createUpdateUserPasswordRequest(User user, EventType eventType) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForPasswordCredentialChange(user, eventType)))
    }

    HttpRequest createUpdateUserPasswordRequestWithRequestId(User user, EventType eventType, String requestId) {
        return createFeedsRequest().withBody(new XPathBody(getFeedsXPathForPasswordCredentialChangeWithRequestId(user, eventType, requestId)))
    }

    def getEntity(response, type) {
        def entity = response.getEntity(type)

        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            entity = entity.value
        }
        return entity
    }

    def extractTokenFromDefaultEmail(MimeMessage message) {
        def map = EmailUtils.extractDynamicPropsFromDefaultEmail(message)
        return StringUtils.trim(map.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_STRING_PROP));
    }

}
