package com.rackspace.idm.api.resource.cloud.email

import spock.lang.Shared
import testHelpers.RootServiceTest

class MailTransferAgentClientTest extends RootServiceTest {

    @Shared MailTransferAgentClient client

    def setupSpec() {
        client = new MailTransferAgentClient()
    }

    def setup() {
        mockEmailService(client)
        mockEmailConfigBuilder(client)
        mockIdentityConfig(client)
    }

    def "sendUnverifiedUserInviteMessage: calls correct services"() {
        given:
        def user = entityFactory.createUser().with {
            it.unverified = true
            it.registrationCode = "code"
            it.email = "test@rackspace.com"
            it
        }
        def emailConfig = Mock(EmailConfig)

        when:
        Boolean result = client.sendUnverifiedUserInviteMessage(user)

        then:
        result

        1 * emailConfigBuilder.buildEmailConfig(Arrays.asList(user.email), MailTransferAgentClient.UNVERIFIED_USER_EMAIL_BASE_DIR, MailTransferAgentClient.INVITE_PREFIX) >> emailConfig
        1 * emailService.sendTemplatedMultiPartMimeEmail(emailConfig, _ as Map) >> { args ->
            Map<String, Object> model = args[1]
            assert model.get(EmailTemplateConstants.INVITE_USER_ID_PROP) == user.id
            assert model.get(EmailTemplateConstants.INVITE_REGISTRATION_CODE_PROP) == user.registrationCode
        }
    }

    def "sendUnverifiedUserInviteMessage: error check"() {
        given:
        def user = entityFactory.createUser().with {
            it.unverified = true
            it.registrationCode = "code"
            it.email = "test@rackspace.com"
            it
        }
        def emailConfig = Mock(EmailConfig)

        when: "sending email throws exception"
        Boolean result = client.sendUnverifiedUserInviteMessage(user)

        then:
        !result

        1 * emailConfigBuilder.buildEmailConfig(Arrays.asList(user.email), MailTransferAgentClient.UNVERIFIED_USER_EMAIL_BASE_DIR, MailTransferAgentClient.INVITE_PREFIX) >> emailConfig
        1 * emailService.sendTemplatedMultiPartMimeEmail(emailConfig, _) >> {throw new Exception()}

        when: "user is null"
        client.sendUnverifiedUserInviteMessage(null)

        then:
        thrown(IllegalArgumentException)

        when: "user missing id"
        def invalidUser = entityFactory.createUser()
        invalidUser.setId(null)
        client.sendUnverifiedUserInviteMessage(invalidUser)

        then:
        thrown(IllegalArgumentException)

        when: "user missing email"
        invalidUser = entityFactory.createUser()
        client.sendUnverifiedUserInviteMessage(invalidUser)

        then:
        thrown(IllegalArgumentException)

        when: "user missing registration code"
        invalidUser = entityFactory.createUser().with {
            it.email = "test@rackspace.com"
            it
        }
        client.sendUnverifiedUserInviteMessage(invalidUser)

        then:
        thrown(IllegalArgumentException)

        when: "user not a unverified user"
        invalidUser = entityFactory.createUser().with {
            it.email = "test@rackspace.com"
            it.unverified = false
            it
        }
        client.sendUnverifiedUserInviteMessage(invalidUser)

        then:
        thrown(IllegalArgumentException)
    }
}
