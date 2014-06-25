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
        User user = new User()
        user.username = "testUser"
        user.email = "test.user@rackspace.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)

        then:
        response == true
    }

    def "Successfully send mfa enabled email"() {
        given:
        User user = new User()
        user.username = "testUser"
        user.email = "test.user@rackspace.com"

        when:
        def response = client.sendMultiFactorEnabledMessage(user)

        then:
        response == true
    }

    def "Successfully send mfa disabled email"() {
        given:
        User user = new User()
        user.username = "testUser"
        user.email = "test.user@rackspace.com"

        when:
        def response = client.sendMultiFactorDisabledMessage(user)

        then:
        response == true
    }

    def "Cannot send email to external account if send only to internal email feature enabled"() {
        given:
        User user = new User();
        user.username = "testUser"
        user.email = "test.user@rackspace2.com"

        when:
        def response = client.sendMultiFactorLockoutOutMessage(user)
        def response1 = client.sendMultiFactorEnabledMessage(user)
        def response2 = client.sendMultiFactorDisabledMessage(user)

        then:
        response == false
        response1 == false
        response2 == false
    }

    def "Exception thrown for send locked out email email"() {
        given:
        User user = new User()
        user.username = "testUser"

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
