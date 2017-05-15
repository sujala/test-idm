package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.PasswordPolicy
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.subethamail.wiser.WiserMessage
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.EmailUtils
import testHelpers.RootIntegrationTest

import java.time.Duration

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED
import static org.apache.http.HttpStatus.SC_OK
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE

class PasswordPolicyIntegrationTest extends RootIntegrationTest {

    @Shared
    String serviceAdminToken

    @Shared
    String identityAdminToken

    void doSetupSpec() {
        def response = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        serviceAdminToken = response.getEntity(AuthenticateResponse).value.token.id

        response = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        identityAdminToken = response.getEntity(AuthenticateResponse).value.token.id
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def setup() {
        // Ensure all tests start with common setting for password policy features
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.PASSWORD_HISTORY_MAX_PROP, 10)
    }

    @Unroll
    def "Can not use password policy services when feature disabled - user type: #type"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)
        Domain domain = utils.createDomainEntity()
        def policy = new PasswordPolicy("PT1H", 2)

        expect: "404 when try to set policy"
        cloud20.updateDomainPasswordPolicy(token, domain.id, policy).status == SC_NOT_FOUND

        and: "404 when get policy"
        cloud20.getDomainPasswordPolicy(token, domain.id).status == SC_NOT_FOUND

        and: "404 when delete policy"
        cloud20.deleteDomainPasswordPolicy(token, domain.id).status == SC_NOT_FOUND

        where:
        type | token
        "serviceAdmin" | serviceAdminToken
        "identityAdmin" | identityAdminToken
    }

    @Unroll
    def "Identity/Service admins can use password policy services when feature enabled - user type: #type"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, true)
        Domain domain = utils.createDomainEntity()

        when: "set policy"
        def policy = new PasswordPolicy("PT1H", 2)
        def response = cloud20.updateDomainPasswordPolicy(token, domain.id, policy)

        then:
        response.status == SC_OK
        PasswordPolicy returnedPolicy = PasswordPolicy.fromJson(response.getEntity(String))
        returnedPolicy.passwordHistoryRestriction == policy.passwordHistoryRestriction
        returnedPolicy.passwordDuration.equals(policy.passwordDuration)
        returnedPolicy.toJson().equals(policy.toJson())

        when: "retrieve policy"
        def getResponse = cloud20.getDomainPasswordPolicy(token, domain.id)

        then:
        response.status == SC_OK
        PasswordPolicy getReturnedPolicy = PasswordPolicy.fromJson(getResponse.getEntity(String))
        getReturnedPolicy.passwordHistoryRestriction == policy.passwordHistoryRestriction
        getReturnedPolicy.passwordDuration.equals(policy.passwordDuration)
        getReturnedPolicy.toJson().equals(policy.toJson())

        when: "delete policy"
        response = cloud20.deleteDomainPasswordPolicy(token, domain.id)

        then:
        response.status == SC_NO_CONTENT

        when: "retrieve policy"
        response = cloud20.getDomainPasswordPolicy(token, domain.id)

        then:
        response.status == SC_NOT_FOUND

        where:
        type | token
        "serviceAdmin" | serviceAdminToken
        "identityAdmin" | identityAdminToken
    }

    def "User admins can use password policy services on own domain; subusers can't on any domain"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, true)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUserToken = utils.getToken(defaultUser.username)
        def userManageToken = utils.getToken(userManage.username)


        def otherDomain = utils.createDomainEntity()
        def policy = new PasswordPolicy("PT1H", 2)

        expect: "200 when user-admin tries to set policy on own domain"
        cloud20.updateDomainPasswordPolicy(userAdminToken, domainId, policy).status == SC_OK

        and: "403 when try to set policy on own (subuser), non-existant (all), or other domain (all)"
        cloud20.updateDomainPasswordPolicy(userAdminToken, "blahnonexistant", policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(userAdminToken, otherDomain.id, policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(userManageToken, domainId, policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(userManageToken, "blahnonexistant", policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(userManageToken, otherDomain.id, policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(defaultUserToken, domainId, policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(defaultUserToken, "blahnonexistant", policy).status == SC_FORBIDDEN
        cloud20.updateDomainPasswordPolicy(defaultUserToken, otherDomain.id, policy).status == SC_FORBIDDEN

        and: "200 when user-admin tries to get policy on own domain"
        cloud20.getDomainPasswordPolicy(userAdminToken, domainId).status == SC_OK

        and: "403 when try to get policy on own (subusers), non-existant (all), or other domain (all)"
        cloud20.getDomainPasswordPolicy(userAdminToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(userAdminToken, otherDomain.id).status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(userManageToken, domainId).status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(userManageToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(userManageToken, otherDomain.id).status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(defaultUserToken, domainId).status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(defaultUserToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.getDomainPasswordPolicy(defaultUserToken, otherDomain.id).status == SC_FORBIDDEN

        and: "204 when user-admin tries to delete policy on own domain"
        cloud20.deleteDomainPasswordPolicy(userAdminToken, domainId).status == SC_NO_CONTENT

        and: "403 when try to delete policy on own (subusers), non-existant (all), or other domain (all)"
        cloud20.deleteDomainPasswordPolicy(userAdminToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(userAdminToken, otherDomain.id).status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(userManageToken, domainId).status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(userManageToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(userManageToken, otherDomain.id).status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(defaultUserToken, domainId).status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(defaultUserToken, "blahnonexistant").status == SC_FORBIDDEN
        cloud20.deleteDomainPasswordPolicy(defaultUserToken, otherDomain.id).status == SC_FORBIDDEN

        cleanup:
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        utils.deleteUsersQuietly(users)
    }

    def "Change user password availability controlled by feature flag"() {
        given:
        User user = utils.createGenericUserAdmin()
        def passwordOriginal = Constants.DEFAULT_PASSWORD
        def passworda = Constants.DEFAULT_PASSWORD + "a"

        when: "Feature disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)

        then: "Service not available"
        cloud20.changeUserPassword(user.username, passwordOriginal, passworda).status == SC_NOT_FOUND

        when: "Feature enabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, true)

        then: "Can't change password if provide invalid current password"
        cloud20.changeUserPassword(user.username, passworda, "irrelevant").status == SC_UNAUTHORIZED

        and: "Can change password if provide valid current password"
        cloud20.changeUserPassword(user.username, passwordOriginal, passworda).status == SC_NO_CONTENT
        cloud20.authenticatePassword(user.username, passworda).status == SC_OK

        cleanup:
        utils.deleteUsers(user)
    }

    def "password.history.max config property controls max history value for policy"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, true)
        Domain domain = utils.createDomainEntity()

        when: "Set history storage to 8"
        reloadableConfiguration.setProperty(IdentityConfig.PASSWORD_HISTORY_MAX_PROP, 8)

        then: "can set policy to 8"
        cloud20.updateDomainPasswordPolicy(serviceAdminToken, domain.id, new PasswordPolicy(null, 8)).status == SC_OK

        and: "can't set to 9"
        cloud20.updateDomainPasswordPolicy(serviceAdminToken, domain.id, new PasswordPolicy(null, 9)).status == SC_BAD_REQUEST

        when: "Set history storage to 2"
        reloadableConfiguration.setProperty(IdentityConfig.PASSWORD_HISTORY_MAX_PROP, 2)

        then: "can set policy to 2"
        cloud20.updateDomainPasswordPolicy(serviceAdminToken, domain.id, new PasswordPolicy(null, 2)).status == SC_OK

        and: "can't set to 8"
        cloud20.updateDomainPasswordPolicy(serviceAdminToken, domain.id, new PasswordPolicy(null, 8)).status == SC_BAD_REQUEST
    }

    def "Password policy controls validity period for password"() {
        given:
        DateTime now = new DateTime()
        User user = utils.createGenericUserAdmin()

        when: "No password policy"
        def response = cloud20.authenticatePassword(user.username)

        then: "Can auth"
        response.status == SC_OK

        and: "No expiration header"
        response.getHeaders().getFirst(GlobalConstants.X_PASSWORD_EXPIRATION) == null

        when: "Set password expiration to 5 minutes and reauth"
        PasswordPolicy policy = createPasswordPolicy("PT5M", 0)
        utils.updateDomainPasswordPolicy(user.domainId, policy)
        response = cloud20.authenticatePassword(user.username)

        then:
        response.status == SC_OK

        and: "Header set for password expiration"
        def expStr = response.getHeaders().getFirst(GlobalConstants.X_PASSWORD_EXPIRATION)
        assert expStr != null
        def exp = new DateTime(expStr)
        exp.isAfter(now.plusMinutes(5).minusMillis(1)) //subtract 1 ms just in case now and user creation were at same time

        when: "Set password expiration to a nanosecond and reauth"
        policy = createPasswordPolicy("PT0.000000001S", 0)
        utils.updateDomainPasswordPolicy(user.domainId, policy)

        then:
        cloud20.authenticatePassword(user.username).status == SC_UNAUTHORIZED

        cleanup:
        utils.deleteUsers(user)
        }

    def "Password policy can be removed or not enforced"() {
        given:
        DateTime now = new DateTime()
        User user = utils.createGenericUserAdmin()

        when: "Set password expiration to 1 nano and reauth"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy("PT0.000000001S", 0))
        def response = cloud20.authenticatePassword(user.username)

        then: "auth denied"
        response.status == SC_UNAUTHORIZED

        when: "unenforce password policy"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, false)

        then: "can now authenticate"
        cloud20.authenticatePassword(user.username).status == SC_OK

        when: "re-enforce password policy"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, true)

        then: "can no longer authenticate"
        cloud20.authenticatePassword(user.username).status == SC_UNAUTHORIZED

        when: "null out password policy"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy("PT0M", 0))
        response = cloud20.authenticatePassword(user.username)

        then: "can authenticate again"
        response.status == SC_OK

        and: "No expiration header"
        response.getHeaders().getFirst(GlobalConstants.X_PASSWORD_EXPIRATION) == null

        when: "Delete empty password policy"
        utils.deleteDomainPasswordPolicy(user.domainId)
        response = cloud20.authenticatePassword(user.username)

        then: "Can auth"
        response.status == SC_OK

        and: "No expiration header"
        response.getHeaders().getFirst(GlobalConstants.X_PASSWORD_EXPIRATION) == null

        cleanup:
        utils.deleteUsers(user)
    }

    def "Password policy controls password history restriction for update user"() {
        given:
        User user = utils.createGenericUserAdmin()
        def originalPassword = Constants.DEFAULT_PASSWORD
        def passworda = Constants.DEFAULT_PASSWORD + "a"
        def passwordb = Constants.DEFAULT_PASSWORD + "b"
        def passwordc = Constants.DEFAULT_PASSWORD + "c"

        def userUpdateOriginal = new User().with {
            it.password = originalPassword
            it
        }
        def userUpdateA = new User().with {
            it.password = passworda
            it
        }
        def userUpdateB = new User().with {
            it.password = passwordb
            it
        }
        def userUpdateC = new User().with {
            it.password = passwordc
            it
        }

        when: "When no password policy"
        def response = cloud20.updateUser(identityAdminToken, user.id, userUpdateOriginal, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE)

        then: "Can change password to same value"
        response.status == SC_OK

        when: "When history set to 0"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 0))

        then: "Can not change password to same value once a policy is set - even to 0"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateOriginal, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateA, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_OK

        when: "When history set to 1"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 1))

        then: "Can not change password to same value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateA, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can not change password to previous value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateOriginal, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateB, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_OK

        when: "When history set to 2"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 2))

        then: "Can not change password to same value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateB, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can not change password to previous value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateA, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can not change password to 2 values prior"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateOriginal, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateC, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_OK

        when: "When history set back to 1"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 1))

        then: "Can not change password to previous value"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateB, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        and: "Can change password back to 2 passwords ago"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateA, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_OK

        when: "unenforce password history"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, false)

        then: "can now change pwd to previous password 'C'"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateC, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_OK

        when: "re-enforce password history"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, true)

        then: "can not change pwd to previous password 'A'"
        cloud20.updateUser(identityAdminToken, user.id, userUpdateA, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE).status == SC_BAD_REQUEST

        cleanup:
        utils.deleteUsers(user)
    }

    def "Password policy controls password history restriction for change password"() {
        given:
        User user = utils.createGenericUserAdmin()
        def passwordOriginal = Constants.DEFAULT_PASSWORD
        def passworda = Constants.DEFAULT_PASSWORD + "a"
        def passwordb = Constants.DEFAULT_PASSWORD + "b"
        def passwordc = Constants.DEFAULT_PASSWORD + "c"

        when: "When no password policy"
        def response = cloud20.changeUserPassword(user.username, passwordOriginal, passwordOriginal)

        then: "Can not change password to same value"
        response.status == SC_BAD_REQUEST

        when: "When history set to 0"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 0))

        then: "Can not change password to same value"
        cloud20.changeUserPassword(user.username, passwordOriginal, passwordOriginal).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.changeUserPassword(user.username, passwordOriginal, passworda).status == SC_NO_CONTENT

        when: "When history set to 1"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 1))

        then: "Can not change password to same value"
        cloud20.changeUserPassword(user.username, passworda, passworda).status == SC_BAD_REQUEST

        and: "Can not change password to previous value"
        cloud20.changeUserPassword(user.username, passworda, passwordOriginal).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.changeUserPassword(user.username, passworda, passwordb).status == SC_NO_CONTENT

        when: "When history set to 2"
        // Note history at this point is O -> A -> B
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 2))

        then: "Can not change password to same value"
        cloud20.changeUserPassword(user.username, passwordb, passwordb).status == SC_BAD_REQUEST

        and: "Can not change password to previous value"
        cloud20.changeUserPassword(user.username, passwordb, passworda).status == SC_BAD_REQUEST

        and: "Can not change password to 2 values prior"
        cloud20.changeUserPassword(user.username, passwordb, passwordOriginal).status == SC_BAD_REQUEST

        and: "Can change to new value"
        cloud20.changeUserPassword(user.username, passwordb, passwordc).status == SC_NO_CONTENT

        when: "When history set back to 1"
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 1))

        then: "Can not change password to previous value"
        cloud20.changeUserPassword(user.username, passwordc, passwordb).status == SC_BAD_REQUEST

        and: "Can change password back to 2 passwords ago"
        cloud20.changeUserPassword(user.username, passwordc, passworda).status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(user)
    }

    def "Reset user password uses password policy"() {
        given:
        User user = utils.createGenericUserAdmin()
        utils.updateDomainPasswordPolicy(user.domainId, createPasswordPolicy(null, 1))

        def passwordOriginal = Constants.DEFAULT_PASSWORD
        def passworda = Constants.DEFAULT_PASSWORD + "a"

        when: "reset the password with current password"
        def emailToken = getPasswordResetToken(user)
        def response = cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(passwordOriginal))

        then: "denied"
        response.status == SC_BAD_REQUEST

        and: "can reset to new pwd"
        cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(passworda)).status == SC_NO_CONTENT

        when: "reset with previous password"
        //delay so new token will not be revoked due to User TRR generated when identity admin disabled
        System.sleep(1001-new DateTime().getMillisOfSecond())
        def emailToken2 = getPasswordResetToken(user)
        response = cloud20.resetPassword(emailToken2, v2Factory.createPasswordReset(passwordOriginal))

        then: "can not reset to previous password"
        response.status == SC_BAD_REQUEST

        cleanup:
        utils.deleteUsers(user)
    }

    def getPasswordResetToken(userAdmin) {
        clearEmailServerMessages()
        def creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        cloud20.forgotPassword(creds)
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())
    }

    String createPasswordPolicyJson(String durationStr, Integer historyRestriction) {
        return createPasswordPolicy(durationStr, historyRestriction).toJson()
    }

    PasswordPolicy createPasswordPolicy(String durationStr, Integer historyRestriction) {
        return new PasswordPolicy(durationStr, historyRestriction)
    }
}