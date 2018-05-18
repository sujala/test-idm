package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.mockserver.verify.VerificationTimes
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class CredentialChangeFeedEventsIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "resetting a user's password creates a credential change event, featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_POST_CREDENTIAL_FEED_EVENTS_ENABLED_PROP, featureEnabled)
        def userAdmin = utils.createCloudAccount().with {
            it.email = "${it.username}@rackspace.com"
            it
        }
        utils.updateUser(userAdmin)

        when: "change password through change password API call"
        resetCloudFeedsMock()
        def newPassword = "Asdf!123"
        utils.changeUserPassword(userAdmin.username, Constants.DEFAULT_PASSWORD, newPassword)

        then:
        cloudFeedsMock.verify(
                testUtils.createUpdateUserPasswordRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(featureEnabled ? 1 : 0)
        )

        when: "change password through update user creds API call"
        resetCloudFeedsMock()
        newPassword = "Asdf!1234"
        utils.updateUserPasswordCredentials(userAdmin, newPassword)

        then:
        cloudFeedsMock.verify(
                testUtils.createUpdateUserPasswordRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(featureEnabled ? 1 : 0)
        )

        when: "change password through forgot password flow"
        resetCloudFeedsMock()
        newPassword = "Asdf!12345"
        ForgotPasswordCredentials creds = v2Factory.createForgotPasswordCredentials(userAdmin.username, null)
        def response = cloud20.forgotPassword(creds)
        assert response.status == 204
        def forgotPwdToken = testUtils.extractTokenFromDefaultEmail(wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage())
        cloud20.resetPassword(forgotPwdToken, v2Factory.createPasswordReset(newPassword))

        then:
        cloudFeedsMock.verify(
                testUtils.createUpdateUserPasswordRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(featureEnabled ? 1 : 0)
        )

        when: "update password through update user"
        resetCloudFeedsMock()
        userAdmin.password = "Asdf!123456"
        utils.updateUser(userAdmin)

        then:
        cloudFeedsMock.verify(
                testUtils.createUpdateUserPasswordRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(featureEnabled ? 1 : 0)
        )

        cleanup:
        reloadableConfiguration.reset()

        where:
        featureEnabled << [true, false]
    }

    def "the requestId in credential change events matches the requestId specified in the request"() {
        given:
        def userAdmin = utils.createCloudAccount()

        when: "update password through update user"
        resetCloudFeedsMock()
        userAdmin.password = "Asdf!123456"
        def requestId = UUID.randomUUID().toString()
        def response = cloud20.updateUser(utils.getServiceAdminToken(), userAdmin.id, userAdmin, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE, requestId)
        assert response.status == 200

        then:
        cloudFeedsMock.verify(
                testUtils.createUpdateUserPasswordRequestWithRequestId(userAdmin, EventType.UPDATE, requestId),
                VerificationTimes.exactly(1)
        )
    }

}
