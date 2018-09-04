package com.rackspace.idm.api.resource.cloud.email


import com.rackspace.idm.domain.entity.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSenderImpl
import testHelpers.RootIntegrationTest

class MailTransferAgentIntegrationTest  extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(MailTransferAgentIntegrationTest.class);

    @Autowired MailTransferAgentClient client

    @Autowired
    JavaMailSenderImpl javaMailSender

    @Autowired
    MailTransferAgentClient mailTransferAgentClient

    def cleanupSpec() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    def setup() {
        clearEmailServerMessages()
    }

    def "Cannot send email to external account if send only to internal email feature enabled"() {
        given:
        User user = new User();
        user.username = "fail.external.send"
        user.email = "fail.external.send@rackspace2.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)
        def response1 = client.sendMultiFactorEnabledMessage(user)
        def response2 = client.sendMultiFactorDisabledMessage(user)

        then:
        response == false
        response1 == false
        response2 == false
    }

    def "Exception thrown when no email address"() {
        given:
        User user = new User()
        user.username = "noEmailProvided"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)
        def response1 = client.sendMultiFactorEnabledMessage(user)
        def response2 = client.sendMultiFactorDisabledMessage(user)

        then:
        response == false
        response1 == false
        response2 == false
    }
}
