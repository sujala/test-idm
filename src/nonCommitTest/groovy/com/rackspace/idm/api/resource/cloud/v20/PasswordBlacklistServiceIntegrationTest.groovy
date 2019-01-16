package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Invite
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.User
import org.subethamail.wiser.WiserMessage
import testHelpers.EmailUtils
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class PasswordBlacklistServiceIntegrationTest extends RootIntegrationTest{

    def "Create user - Verify that user creation is not allowed with blacklisted password" () {
        given:
        def domainId = utils.createDomain()

        when: "Password blacklist feature is enabled with a lower compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        def user = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, Constants.BLACKLISTED_PASSWORD)
        def response = cloud20.createUser(utils.getServiceAdminToken(), user)

        then: "User cannot be created and validation error is thrown"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)

        when: "Password blacklist feature is enabled with a higher compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 10000)
        user = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email2@email.com", true, null, domainId, Constants.BLACKLISTED_PASSWORD)
        response = cloud20.createUser(utils.getServiceAdminToken(), user)

        then: "User would be created"
        response.status == HttpStatus.SC_CREATED

        when: "Password blacklist feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)
        user = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email4@email.com", true, null, domainId, Constants.BLACKLISTED_PASSWORD)

        response = cloud20.createUser(utils.getServiceAdminToken(), user)

        then: "User would be created ignoring validation check for password"
        response.status == HttpStatus.SC_CREATED
    }

    def "Update user password - Verify that user update is not allowed with blacklisted password" () {
        given:
        def domainId = utils.createDomain()
        def serviceAdminToken = utils.getServiceAdminToken()

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        def userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def userCreateResponse = cloud20.createUser(utils.getServiceAdminToken(), userForCreate)
        def userEntity = userCreateResponse.getEntity(User).value

        when: "Password blacklist feature is enabled with a lower compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        def user = v2Factory.createUserForUpdate(null, null, null, null, true, null, Constants.BLACKLISTED_PASSWORD)
        def response = cloud20.updateUser(serviceAdminToken, userEntity.getId(), user)

        then: "User cannot be updated and validation error is thrown"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)

        when: "Password blacklist feature is enabled with a higher compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 10000)

        user = v2Factory.createUserForUpdate(null, null, null, null, true, null, Constants.BLACKLISTED_PASSWORD)
        response = cloud20.updateUser(serviceAdminToken, userEntity.getId(), user)

        then: "User can be updated"
        response.status == HttpStatus.SC_OK

        when: "Password blacklist feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        user = v2Factory.createUserForUpdate(null, null, null, null, true, null, Constants.BLACKLISTED_PASSWORD)
        response = cloud20.updateUser(serviceAdminToken, userEntity.getId(), user)

        then: "User would be updated ignoring validation check for password"
        response.status == HttpStatus.SC_OK
    }

    def "Reset password - Verify that reset password is not allowed with blacklisted password" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)

        User user = utils.createGenericUserAdmin()
        def emailToken = getPasswordResetToken(user)

        when: "Password blacklist feature is enabled with a lower compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        def response = cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(Constants.BLACKLISTED_PASSWORD), MediaType.APPLICATION_JSON_TYPE)

        then: "Password reset is not successful"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)


        when: "Password blacklist feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)

        response = cloud20.resetPassword(emailToken, v2Factory.createPasswordReset(Constants.BLACKLISTED_PASSWORD), MediaType.APPLICATION_JSON_TYPE)

        then: "Password reset is successful"
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "Add credential to user - Verify that user update is not allowed with blacklisted password" () {
        given:
        def domainId = utils.createDomain()
        def serviceAdminToken = utils.getServiceAdminToken()

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        def userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        def userEntity = cloud20.createUser(utils.getServiceAdminToken(), userForCreate).getEntity(User).value

        when: "Password blacklist feature is enabled with a lower compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        def response = cloud20.addCredential(serviceAdminToken, userEntity.getId(), v2Factory.createPasswordCredentialsBase(userEntity.getUsername(), Constants.BLACKLISTED_PASSWORD))

        then: "User credentials cannot be added"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)

        when: "Password blacklist feature is enabled with a higher compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 10000)

        response = cloud20.addCredential(serviceAdminToken, userEntity.getId(), v2Factory.createPasswordCredentialsBase(userEntity.getUsername(), Constants.BLACKLISTED_PASSWORD))

        then: "User credentials can be added"
        response.status == HttpStatus.SC_CREATED
    }

    def "Change password - Verify that change password is not allowed with blacklisted password"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        User user = utils.createGenericUserAdmin()

        when: "Password blacklist feature is enabled with a lower compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        def response = cloud20.changeUserPassword(user.username, Constants.DEFAULT_PASSWORD, Constants.BLACKLISTED_PASSWORD)

        then: "user gets the validation error message"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)

        when: "Password blacklist feature is enabled with a higher compromised count"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 10000)

        response = cloud20.changeUserPassword(user.username, Constants.DEFAULT_PASSWORD, Constants.BLACKLISTED_PASSWORD)

        then: "User password change is successful"
        response.status == HttpStatus.SC_NO_CONTENT

        when: "Password blacklist feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 1)

        response = cloud20.changeUserPassword(user.username, Constants.BLACKLISTED_PASSWORD, Constants.BLACKLISTED_PASSWORD_1)

        then: "User password change is successful"
        response.status == HttpStatus.SC_NO_CONTENT
    }

    def "Accept unverified user invite - Verify that unverified user is not allowed to accept invitation with blacklisted password"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        // Send invite
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)
        assert response.status == HttpStatus.SC_OK
        def inviteEntity = response.getEntity(Invite)

        def username = testUtils.getRandomUUID("user")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = inviteEntity.userId
            it.username = username
            it.password = Constants.BLACKLISTED_PASSWORD
            it.registrationCode = inviteEntity.registrationCode
            it.secretQA = v2Factory.createSecretQA()
            it
        }

        when: "accept invite with blacklisted password"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 25)
        def userResponse = cloud20.acceptUnverifiedUserInvite(userForCreate)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(userResponse, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)

        when: "accept invite with password black list feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.DYNAMO_DB_PASSWORD_BLACKLIST_COUNT_MAX_ALLOWED_PROP, 25)
        userResponse = cloud20.acceptUnverifiedUserInvite(userForCreate)

        then:
        userResponse.status == HttpStatus.SC_OK
    }

    def getPasswordResetToken(userAdmin) {
        def creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        cloud20.forgotPassword(creds)
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        EmailUtils.extractTokenFromDefaultForgotPasswordEmail(message.getMimeMessage())
    }

    def cleanup(){
        reloadableConfiguration.reset()
    }
}
