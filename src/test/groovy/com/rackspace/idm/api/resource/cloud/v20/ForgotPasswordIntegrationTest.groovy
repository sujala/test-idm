package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.helpers.WiserWrapper
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.RootIntegrationTest

import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT

class ForgotPasswordIntegrationTest extends RootIntegrationTest {
    @Autowired(required = false)
    LdapScopeAccessRepository ldapScopeAccessRepository

    @Autowired
    IdentityConfig config;

    @Autowired
    Cloud20Methods methods

    @Autowired
    AETokenService aeTokenService

    @Autowired
    IdentityConfig identityConfig

    @Shared def WiserWrapper wiserWrapper;

    @Shared def userAdmin;

    @Shared def identityAdminToken

    def setupSpec() {
        def identityAdminAuthResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD).getEntity(AuthenticateResponse)
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        identityAdminToken = identityAdminAuthResponse.value.token.id

        //create single user-admin/domain to test with
        userAdmin = createUserAdmin()
        wiserWrapper = WiserWrapper.startWiser(2525)
    }

    def cleanupSpec() {
        deleteUserQuietly(userAdmin)
        wiserWrapper.wiserServer.stop()
    }

    def setup() {
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_HOST, wiserWrapper.getHost())
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_PORT, String.valueOf(wiserWrapper.getPort()))
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_CREATE_EMAIL_SESSION_PER_EMAIL_PROP, true)

        wiserWrapper.wiserServer.getMessages().clear()
    }

    def cleanup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "get 204 response when sending request type: #requestType"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when:
        def response = methods.forgotPassword(creds, requestContentType)

        then:
        response.status == SC_NO_CONTENT

        where:
        requestContentType | _
        MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_XML_TYPE | _
    }

    def "When user is identity:admin or service:admin, no email sent"() {
        when: "try to reset identity admin"
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(Constants.IDENTITY_ADMIN_USERNAME, null)
        def response = methods.forgotPassword(creds)

        then: "response is 204, but no email sent"
        response.status == SC_NO_CONTENT
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        when: "try to reset service admin"
        creds = v2Factory.createForgotPasswordCredentials(Constants.SERVICE_ADMIN_USERNAME, null)
        response = methods.forgotPassword(creds)

        then: "response is 204, but no email sent"
        response.status == SC_NO_CONTENT
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())
    }

    def "When user is disabled, no email sent"() {
        //create disabled user
        String userAdminToken = utils.getToken(userAdmin.username)
        String defaultUserUsername = testUtils.getRandomUUID()
        User defaultUser = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(defaultUserUsername, "display", "test@rackspace.com", false, null, null, Constants.DEFAULT_PASSWORD)).getEntity(User).value

        when: "try to reset disabled default user"
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(defaultUserUsername, null)
        def response = methods.forgotPassword(creds)

        then: "response is 204, but no email sent"
        response.status == SC_NO_CONTENT
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        cleanup:
        deleteUserQuietly(defaultUser)
    }

    def "Sends an Email"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        when:
        def response = methods.forgotPassword(creds)

        then: "an email was sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1
    }

    def "Email contains an AE Token"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when:
        def response = methods.forgotPassword(creds)

        then: "the email contains an AE token"
        def tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())
        def tokenEntity = aeTokenService.unmarshallToken(tokenStr)
        tokenEntity != null

        and: "the token was created appropriately"
        tokenEntity.scope == TokenScopeEnum.PWD_RESET.scope
        tokenEntity.authenticatedBy.size() == 1
        tokenEntity.authenticatedBy.get(0) == AuthenticatedByMethodEnum.EMAIL.value
        tokenEntity.accessTokenExp != null
    }

    def "Reset token expiration based on config property"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when: "expiration length set to 10 minutes"
        def expirationSeconds = 10 * 60
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, expirationSeconds)
        def response = methods.forgotPassword(creds)
        def tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())
        def tokenEntity = aeTokenService.unmarshallToken(tokenStr)
        DateTime expectedTokenExpireOnOrBefore = new DateTime().plusSeconds(expirationSeconds)
        DateTime expectedTokenExpireAfter = new DateTime().plusSeconds(expirationSeconds).minusSeconds(20) //20 seconds of padding in case method takes a while

        then: "the token expires in approximately 10 minutes"
        DateTime tokenExpiration = new DateTime(tokenEntity.accessTokenExp)
        assertDateOnOrBefore(tokenExpiration, expectedTokenExpireOnOrBefore)
        tokenExpiration.isAfter(expectedTokenExpireAfter)

        when: "expiration length set to 100 minutes"
        expirationSeconds = 100 * 60
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, expirationSeconds)
        methods.forgotPassword(creds)
        tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(1).getMimeMessage()) //get the second email
        tokenEntity = aeTokenService.unmarshallToken(tokenStr)
        expectedTokenExpireOnOrBefore = new DateTime().plusSeconds(expirationSeconds)
        expectedTokenExpireAfter = new DateTime().plusSeconds(expirationSeconds).minusSeconds(20) //20 seconds of padding in case method takes a while

        then: "the token expires in approximately 100 minutes"
        DateTime token2Expiration = new DateTime(tokenEntity.accessTokenExp)
        assertDateOnOrBefore(token2Expiration, expectedTokenExpireOnOrBefore)
        token2Expiration.isAfter(expectedTokenExpireAfter)
    }

    def "Reset token can not be validated in v2.0 validate"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when: "get reset token"
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        then: "the token can not be validated as self token"
        cloud20.validateToken(tokenStr, tokenStr).status == SC_NOT_FOUND

        and: "the token can not be validated as the target token"
        cloud20.validateToken(identityAdminToken, tokenStr).status == SC_NOT_FOUND
    }

    def "Reset token can not be validated in v1.1 validate"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when: "get reset token"
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        then: "the token can not be validated"
        cloud11.validateToken(tokenStr).status == SC_NOT_FOUND
    }

    def "Reset token can not be used to get user"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        expect: "try to get my user using tokens results in 403"
        cloud20.getUserById(tokenStr, userAdmin.id).status == SC_FORBIDDEN
    }

    def extractTokenFromEmail(MimeMessage message) {
        return StringUtils.trim(message.content);
    }

    def void assertDateOnOrBefore(DateTime date1, DateTime date2) {
        assert date1.isBefore(date2) || date1.isEqual(date2)
    }

    def createUserAdmin(String adminUsername = RandomStringUtils.randomAlphanumeric(20), String domainId = RandomStringUtils.randomAlphanumeric(20)) {
        cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername, "display", adminUsername + "@rackspace.com", true, null, domainId, Constants.DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(identityAdminToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User).value
        return userAdmin;
    }

    def deleteUserQuietly(user) {
        if (user != null) {
            try {
                cloud20.destroyUser(identityAdminToken, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }
}
