package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserAuthenticationResult
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.helpers.WiserWrapper
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.subethamail.wiser.WiserMessage
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.EmailUtils
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.validation.entity.Constants.PASSWORD_MIN;
import static com.rackspace.idm.validation.entity.Constants.MAX;

class PasswordResetIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    UserService userService

    @Autowired
    IdentityUserService identityUserService

    def cleanup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
        clearEmailServerMessages()
    }

    @Unroll
    def "reset user password using password reset call, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def emailToken = getPasswordResetToken(userAdmin)
        def emailToken2 = getPasswordResetToken(userAdmin)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()
        def passwordToken = utils.getToken(userAdmin.username)

        when: "reset the password"
        def response = cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(newPassword), request)

        then: "success"
        response.status == 204

        and: "password token is no longer valid"
        cloud20.validateToken(utils.getServiceAdminToken(), passwordToken).status == 404

        and: "used password reset token is invalid"
        cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(newPassword), request).status == 401

        and: "unused password reset token is invalid"
        cloud20.resetPassword(emailToken2, v2Factory.createPasswordReset(newPassword), request).status == 401

        when: "auth with the old password"
        def authWithOldPwResponse = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "error"
        authWithOldPwResponse.status == 401

        when: "auth with the new password"
        def authWithNewPwResponse = cloud20.authenticate(userAdmin.username, newPassword)

        then: "success"
        authWithNewPwResponse.status == 200

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "calling password reset with non-password reset scoped token results in 403, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()

        when: "reset the password"
        def response = cloud20.resetPassword(utils.getToken(userAdmin.username), v2Factory.createPasswordReset(newPassword), request)

        then: "error"
        response.status == 403

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "calling password reset with expired password reset scoped token results in 401, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()
        def expirationSeconds = 1
        reloadableConfiguration.setProperty(IdentityConfig.FORGOT_PWD_SCOPED_TOKEN_VALIDITY_LENGTH_SECONDS_PROP_NAME, expirationSeconds)
        def expiredToken = getPasswordResetToken(userAdmin)

        when: "wait for token to expire and then reset the password"
        sleep(1000)
        def response = cloud20.resetPassword(expiredToken, v2Factory.createPasswordReset(newPassword), request)

        then: "error"
        response.status == 401

        cleanup:
        utils.deleteUsers(users)
        reloadableConfiguration.reset()

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "calling password reset with password reset scoped token for deleted user results in 404, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()
        def pwdResetToken = getPasswordResetToken(userAdmin)
        utils.deleteUser(userAdmin)
        users.remove(userAdmin)

        when: "reset the password"
        def response = cloud20.resetPassword(pwdResetToken, v2Factory.createPasswordReset(newPassword), request)

        then: "error"
        response.status == 404

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "calling password reset with invalid token results in 401, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()

        when: "reset the password"
        def response = cloud20.resetPassword("notAValidToken", v2Factory.createPasswordReset(newPassword), request)

        then: "error"
        response.status == 401

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "calling password reset with invalid password results in 400, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def pwdResetToken = getPasswordResetToken(userAdmin)

        when: "reset the password"
        def response = cloud20.resetPassword(pwdResetToken, v2Factory.createPasswordReset("password"), request)

        then: "error"
        response.status == 400

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "test feature flag works for reset password call"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def pwdResetToken = getPasswordResetToken(userAdmin)
        def newPassword = Constants.DEFAULT_PASSWORD + testUtils.getRandomUUID()
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_FORGOT_PWD_ENABLED_PROP_NAME, false)

        when: "reset the password"
        def response = cloud20.resetPassword(pwdResetToken, v2Factory.createPasswordReset(newPassword))

        then: "not found"
        response.status == 404

        cleanup:
        utils.deleteUsers(users)
        reloadableConfiguration.reset()
    }

    @Unroll
    def "reset user password using invalid password, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def emailToken = getPasswordResetToken(userAdmin)
        def newPassword = 'P' * 101

        when: "reset the password"
        def response = cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(newPassword), request)

        then: "bad request"
        response.status == 400
        def fault = response.getEntity(BadRequestFault)
        fault.message == "password: size must be between " + PASSWORD_MIN + " and " + MAX

        cleanup:
        utils.deleteUsers(users)

        where:
        request | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def getPasswordResetToken(userAdmin) {
        def creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        cloud20.forgotPassword(creds)
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())
    }

}
