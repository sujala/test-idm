package com.rackspace.idm.api.resource.cloud.email

import com.rackspace.idm.domain.config.IdentityConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.subethamail.wiser.WiserMessage
import testHelpers.RootIntegrationTest

class EmailServiceIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceIntegrationTest.class);

    private static final String TEST_EMAIL_DIR = MailTransferAgentClient.TEMPLATE_DIR + File.separator + "test";

    @Autowired
    EmailService emailService

    @Autowired
    EmailConfigBuilder emailConfigBuilder

    def setup() {
        clearEmailServerMessages()
    }

    def "Use hardcoded 'no-reply@rackspace.com' sender if no configured reloadable property or configured sender in email template"() {
        given:
        def expectedSender = "no-reply@rackspace.com"

        reloadableConfiguration.setProperty(IdentityConfig.EMAIL_FROM_EMAIL_ADDRESS, null)
        def emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList("no-recip@rackspace.com"), TEST_EMAIL_DIR, "no_sender")

        when:
        emailService.sendTemplatedTextEmail(emailConfig, Collections.EMPTY_MAP)

        then:
        wiserWrapper.wiserServer.getMessages().size() == 1
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        message.getEnvelopeSender().toString().equals(expectedSender)

        //when reply-to not specified, uses sender
        message.getMimeMessage().getReplyTo().length == 1
        message.getMimeMessage().getReplyTo()[0].toString() == expectedSender
    }


    def "Use sender from reloadable properties when no configured sender in email template"() {
        given:
        def expectedSender = "sender@rackspace.com"
        reloadableConfiguration.setProperty(IdentityConfig.EMAIL_FROM_EMAIL_ADDRESS, expectedSender)
        def emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList("no-recip@rackspace.com"), TEST_EMAIL_DIR, "no_sender")

        when:
        emailService.sendTemplatedTextEmail(emailConfig, Collections.EMPTY_MAP)

        then:
        wiserWrapper.wiserServer.getMessages().size() == 1
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        message.getEnvelopeSender().toString().equals(expectedSender)

        //when reply-to not specified, uses sender
        message.getMimeMessage().getReplyTo().length == 1
        message.getMimeMessage().getReplyTo()[0].toString() == expectedSender
    }

    def "Use sender and reply-to from email template if configured"() {
        given:
        def expectedSender = "template-sender@rackspace.com"
        def expectedReplyTo = "template-replyto@rackspace.com"

        reloadableConfiguration.setProperty(IdentityConfig.EMAIL_FROM_EMAIL_ADDRESS, "property-sender@rackspace.com")
        def emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList("no-recip@rackspace.com"), TEST_EMAIL_DIR, "with_sender")

        when:
        emailService.sendTemplatedTextEmail(emailConfig, Collections.EMPTY_MAP)

        then:
        wiserWrapper.wiserServer.getMessages().size() == 1
        WiserMessage message = wiserWrapper.wiserServer.getMessages().get(0)
        message.getEnvelopeSender().toString().equals(expectedSender)
        message.getMimeMessage().getReplyTo().length == 1
        message.getMimeMessage().getReplyTo()[0].toString() == expectedReplyTo
    }

}
