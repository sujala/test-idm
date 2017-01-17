package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.api.resource.cloud.email.EmailTemplateConstants
import com.rackspace.idm.helpers.CloudTestUtils
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.EmailUtils
import testHelpers.RootIntegrationTest

import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

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

    @Shared def userAdmin;

    @Shared def identityAdminToken

    def setupSpec() {
        def identityAdminAuthResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD).getEntity(AuthenticateResponse)
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        identityAdminToken = identityAdminAuthResponse.value.token.id

        //create single user-admin/domain to test with
        userAdmin = createUserAdmin()
    }

    def cleanupSpec() {
        deleteUserQuietly(userAdmin)
    }

    def cleanup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
        clearEmailServerMessages()
    }

    @Unroll
    def "get 400 response when send '#username' username"() {
        when:
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(username, null)
        def response = methods.forgotPassword(creds)

        then:
        response.status == SC_BAD_REQUEST

        where:
        username | _
        null | _
        "" | _
    }

    @Unroll
    def "get 204 response when sending request type: #requestType"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when:
        def response = methods.forgotPassword(creds, requestContentType)

        then:
        response.status == SC_NO_CONTENT

        and: "username returned in the headers"
        response.getHeaders().get(GlobalConstants.X_USER_NAME)[0] == userAdmin.username

        where:
        requestContentType | _
        MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_XML_TYPE | _
    }

    def "username returned in headers for forgot pw calls if user does not exist"() {
        given:
        def username = "doesNotExist" + RandomStringUtils.randomAlphanumeric(30)

        when:
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(username, null)
        def response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT
        response.getHeaders().get(GlobalConstants.X_USER_NAME)[0] == username
    }

    def "When user is identity:admin or service:admin, no email sent"() {
        when: "try to reset identity admin"
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(Constants.IDENTITY_ADMIN_USERNAME, null)
        def response = methods.forgotPassword(creds)

        then: "response is 204, but no email sent"
        response.status == SC_NO_CONTENT
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        and: "username returned in the headers"
        response.getHeaders().get(GlobalConstants.X_USER_NAME)[0] == Constants.IDENTITY_ADMIN_USERNAME

        when: "try to reset service admin"
        creds = v2Factory.createForgotPasswordCredentials(Constants.SERVICE_ADMIN_USERNAME, null)
        response = methods.forgotPassword(creds)

        then: "response is 204, but no email sent"
        response.status == SC_NO_CONTENT
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        and: "username returned in the headers"
        response.getHeaders().get(GlobalConstants.X_USER_NAME)[0] == Constants.SERVICE_ADMIN_USERNAME
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

        and: "username returned in the headers"
        response.getHeaders().get(GlobalConstants.X_USER_NAME)[0] == defaultUserUsername

        cleanup:
        deleteUserQuietly(defaultUser)
    }

    def "Sends an Email w/ defined dynamic properties"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when:
        def response = methods.forgotPassword(creds)

        then: "an email was sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        and: "email contains appropriate dynamic props"
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getFrom().length == 1
        message.getFrom()[0].toString() == "no-reply@rackspace.com"
        message.getSubject() == "Default Hosting Password Reset Instructions"

        verifyDynamicEmailProps(userAdmin, message)
    }

    def "Sends an Email w/ cloud control formatting"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, "cloud_control")

        when:
        def response = methods.forgotPassword(creds)

        then: "an email was sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        and: "email contains appropriate dynamic props"
        String emailContent = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage().getContent()
        emailContent.startsWith("User: " + userAdmin.username)
    }

    /**
     * The default email content template simply lists all the explicitly supported dynamic values for emails. This tests
     * that these are set appropriately.
     *
     * @param expectedUser
     * @param expectedScopeAccess
     * @param email
     */
    def void verifyDynamicEmailProps(User expectedUser, MimeMessage email) {
        Map<String, String> dynamicProps = EmailUtils.extractDynamicPropsFromDefaultEmail(email)

        //verify the username prop
        assert expectedUser.username == dynamicProps.get(EmailTemplateConstants.FORGOT_PASSWORD_USER_NAME_PROP)

        //verify the validity period prop
        assert String.valueOf(identityConfig.getReloadableConfig().getForgotPasswordTokenLifetime()/60) == dynamicProps.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_VALIDITY_PERIOD_PROP)

        //verify the token_str prop (and decrypt actual token)
        def accessTokenString = dynamicProps.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_STRING_PROP)
        assert accessTokenString != null
        def tokenEntity = aeTokenService.unmarshallToken(accessTokenString)
        assert tokenEntity != null

        //verify the expiration date prop
        DateTimeFormatter formatterForEmail = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ") //match the format specified in email

        String expStr = dynamicProps.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_EXPIRATION_PROP)
        DateTime expDateInEmail = formatterForEmail.parseDateTime(expStr);
        DateTime expectedExpirationDate = new DateTime(tokenEntity.accessTokenExp)
        assert expDateInEmail.equals(expectedExpirationDate)
    }

    def "Email contains an AE Token configured for user"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when:
        def response = methods.forgotPassword(creds)

        then: "the email contains an AE token"
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())
        def tokenEntity = aeTokenService.unmarshallToken(tokenStr)
        tokenEntity != null

        and: "the token was created appropriately"
        tokenEntity.scope == TokenScopeEnum.PWD_RESET.scope
        tokenEntity.authenticatedBy.size() == 1
        tokenEntity.authenticatedBy.get(0) == AuthenticatedByMethodEnum.EMAIL.value
        tokenEntity.accessTokenExp != null
        tokenEntity instanceof UserScopeAccess
        ((UserScopeAccess)tokenEntity).getIssuedToUserId() == userAdmin.id
    }

    def "Reset token expiration based on config property"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when: "expiration length set to 10 minutes"
        def expirationSeconds = 10 * 60
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, expirationSeconds)
        def response = methods.forgotPassword(creds)
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())
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
        tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(1).getMimeMessage()) //get the second email
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
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        then: "the token can not be validated as self token"
        cloud20.validateToken(tokenStr, tokenStr).status == SC_FORBIDDEN

        and: "the token can not be validated as the target token"
        cloud20.validateToken(identityAdminToken, tokenStr).status == SC_NOT_FOUND
    }

    def "Reset token can not be used to revoke a different token"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        def pwdToken = utils.getToken(userAdmin.username)
        methods.forgotPassword(creds)
        def fpToken = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        methods.forgotPassword(creds)
        def fpToken2 = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(1).getMimeMessage())

        when: "try to revoke pwd token"
        def response = cloud20.revokeUserToken(fpToken, pwdToken)

        then: "get 403"
        response.status == SC_FORBIDDEN

        when: "revoke own forgot password token"
        response = cloud20.revokeUserToken(fpToken, fpToken)

        then: "get 204"
        response.status == SC_NO_CONTENT

        when: "revoke forgot password token using pwd token"
        response = cloud20.revokeUserToken(pwdToken, fpToken2)

        then: "get 204"
        response.status == SC_NO_CONTENT
    }

    def "Reset token can not be validated in v1.1 validate"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)

        when: "get reset token"
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        then: "the token can not be validated"
        cloud11.validateToken(tokenStr).status == SC_NOT_FOUND
    }

    def "Reset token can not be used to get user"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        expect: "try to get my user using tokens results in 403"
        cloud20.getUserById(tokenStr, userAdmin.id).status == SC_FORBIDDEN
    }

    def "Reset token can not be used to auth w/ token"() {
        setup:
        def domainId = utils.createDomain()
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        methods.forgotPassword(creds)
        def tokenStr = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        when: "try to auth w/ token using restricted token session id"
        def authResponse = cloud20.authenticateTokenAndTenant(tokenStr, userAdmin.domainId)

        then: "get 403"
        assertOpenStackV2FaultResponse(authResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)
    }

    def "get 400 when specify non-existant portal"() {
        when:
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, "non-existant")
        def response = methods.forgotPassword(creds)

        then:
        response.status == SC_BAD_REQUEST
    }

    def "get 204 when specify acceptable portal"() {
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, "cloud_control")

        when: "exact match"
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_VALID_PORTALS_PROP_NAME, "cloud_control")
        def response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT

        when: "difference case"
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_VALID_PORTALS_PROP_NAME, "CLOUD_CONTROL")
        response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT

        when: "multiple "
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_VALID_PORTALS_PROP_NAME, "CLOUD_CONTROL, asdf")
        response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT
    }

    def "get 204 when do not specify portal"() {
        when: "empty string"
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, "")
        def response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT

        when: "null"
        creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        response = methods.forgotPassword(creds)

        then:
        response.status == SC_NO_CONTENT

    }

    def "list endpoints for password reset token returns 404"() {
        given:
        def creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        methods.forgotPassword(creds)
        def fpToken = extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())

        when:
        def response = cloud20.listEndpointsForToken(utils.getServiceAdminToken(), fpToken)

        then:
        response.status == 404
    }

    def extractTokenFromDefaultEmail(MimeMessage message) {
        def map = EmailUtils.extractDynamicPropsFromDefaultEmail(message)
        return StringUtils.trim(map.get(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_STRING_PROP));
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
