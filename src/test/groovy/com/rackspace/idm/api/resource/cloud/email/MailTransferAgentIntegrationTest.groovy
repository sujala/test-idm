package com.rackspace.idm.api.resource.cloud.email

import com.rackspace.idm.api.resource.cloud.email.impl.MailTransferAgentClient
import com.rackspace.idm.domain.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootIntegrationTest


@ContextConfiguration(locations = "classpath:app-config.xml")
class MailTransferAgentIntegrationTest  extends RootIntegrationTest {

    @Autowired MailTransferAgentClient client

    def "Successfully send locked out email"() {
        given:
        User user = new User();
        user.username = "testUser"
        user.email = "matt.kovacs@rackspace.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == true
    }

    def "Cannot send email to external account if send only to internal email feature enabled"() {
        given:
        User user = new User();
        user.username = "testUser"
        user.email = "matt.kovacs@rackspace2.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == false
    }

    def "Exception thrown for send locked out email email"() {
        given:
        User user = new User();
        user.username = "mkovacs"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == false
    }
}
