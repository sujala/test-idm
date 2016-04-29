package com.rackspace.idm.api.resource.cloud.email

import com.rackspace.idm.domain.entity.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSenderImpl
import testHelpers.RootIntegrationTest

import javax.mail.internet.MimeMessage

class MfaVelocityEmailIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(MfaVelocityEmailIntegrationTest.class);

    @Autowired MailTransferAgentClient client

    @Autowired
    JavaMailSenderImpl javaMailSender;

    def setup() {
        clearEmailServerMessages()
    }

    def "Successfully send mfa disabled email using velocity framework"() {
        given:
        User user = new User()
        user.username = "velocity.disabled"
        user.email = "velocity.disabled@rackspace.com"

        when:
        def response = client.sendMultiFactorDisabledMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        message.getSubject().startsWith("(Velocity)")
    }

    def "Successfully send mfa enabled email using velocity framework"() {
        given:
        User user = new User()
        user.username = "velocity.enabled"
        user.email = "velocity.enabled@rackspace.com"

        when:
        def response = client.sendMultiFactorEnabledMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        message.getSubject().startsWith("(Velocity)")
    }

    def "Successfully send locked out email using velocity framework"() {
        given:
        User user = new User()
        user.username = "velocity.locked"
        user.email = "velocity.locked@rackspace.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        message.getSubject().startsWith("(Velocity)")
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
