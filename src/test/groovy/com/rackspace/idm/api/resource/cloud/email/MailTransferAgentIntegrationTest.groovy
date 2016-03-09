package com.rackspace.idm.api.resource.cloud.email

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.helpers.WiserWrapper
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly


@ContextConfiguration(locations = "classpath:app-config.xml")
class MailTransferAgentIntegrationTest  extends RootIntegrationTest {

    @Autowired MailTransferAgentClient client

    @Shared def WiserWrapper wiserWrapper;

    def setupSpec() {
        //start up wiser and set the properties BEFORE making first cloud20 call (which starts grizzly)
        wiserWrapper = WiserWrapper.startWiser(10025)
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_HOST, wiserWrapper.getHost())
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_PORT, String.valueOf(wiserWrapper.getPort()))

        this.resource = startOrRestartGrizzly("classpath:app-config.xml") //to pick up wiser changes
    }

    def cleanupSpec() {
        wiserWrapper.wiserServer.stop()
    }

    def setup() {
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_HOST, wiserWrapper.getHost())
        staticIdmConfiguration.setProperty(IdentityConfig.EMAIL_PORT, String.valueOf(wiserWrapper.getPort()))

        wiserWrapper.wiserServer.getMessages().clear()
    }

    def cleanup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

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
