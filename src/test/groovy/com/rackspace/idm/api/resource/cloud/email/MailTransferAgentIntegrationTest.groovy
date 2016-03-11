package com.rackspace.idm.api.resource.cloud.email

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.helpers.WiserWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import javax.mail.Session
import javax.mail.internet.MimeMessage

/**
 * This test dirties the context by modifying the port the MailTransferAgentClient will send mail to in order to match that
 * used by wiser
 */
@DirtiesContext
@ContextConfiguration(locations = "classpath:app-config.xml")
class MailTransferAgentIntegrationTest  extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(MailTransferAgentIntegrationTest.class);

    @Autowired MailTransferAgentClient client

    @Shared def WiserWrapper wiserWrapper = WiserWrapper.startWiser(10030)

    @Autowired
    JavaMailSenderImpl javaMailSender;

    @Autowired
    MailTransferAgentClient mailTransferAgentClient;

    def setupSpec() {
        //start up wiser and set the properties BEFORE making first cloud20 call (which starts grizzly)
        logger.warn("Wiser started on " + wiserWrapper.getPort())
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_HOST, wiserWrapper.getHost())
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_PORT, String.valueOf(wiserWrapper.getPort()))
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_USE_VELOCITY_FOR_MFA_EMAILS_PROP, false)
    }

    def cleanupSpec() {
        wiserWrapper.wiserServer.stop()
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    def setup() {
        mailTransferAgentClient.setSession(Session.getInstance(mailTransferAgentClient.getSessionProperties()))
        wiserWrapper.wiserServer.getMessages().clear()
    }

    def "Successfully send locked out email using legacy embedded framework"() {
        logger.warn("WiserWrapper on port " + wiserWrapper.getPort())
        logger.warn("javaMailSender on port " + javaMailSender.getPort())

        given:
        User user = new User()
        user.username = "embedded.locked"
        user.email = "embedded.locked@rackspace.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        !message.getSubject().startsWith("(Velocity)")
    }

    def "Successfully send mfa enabled email using legacy embedded framework"() {
        given:
        User user = new User()
        user.username = "embedded.enabled"
        user.email = "embedded.enabled@rackspace.com"

        when:
        def response = client.sendMultiFactorEnabledMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        !message.getSubject().startsWith("(Velocity)")
    }

    def "Successfully send mfa disabled email using legacy embedded framework"() {
        given:
        User user = new User()
        user.username = "embedded.disabled"
        user.email = "embedded.disabled@rackspace.com"

        when:
        def response = client.sendMultiFactorDisabledMessage(user)

        then:
        response == true

        and:
        wiserWrapper.wiserServer.getMessages().size() == 1
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getContent().contains(user.username)
        !message.getSubject().startsWith("(Velocity)")
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
