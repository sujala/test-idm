package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

class DefaultMultiFactorCloud20ServiceGetMobilePhoneFromUserIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    private IdentityConfig identityConfig;

    @Unroll
    def "Get phone from user - requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        setup:
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)
        def addedPhone
        if (verified) {
            addedPhone = utils.addVerifiedMobilePhoneToUser(userAdminToken, userAdmin)
        } else {
            addedPhone = utils.addMobilePhoneToUser(userAdminToken, userAdmin)
        }

        when: "get mobile phone off my account"
        def retrievedPhone = utils.getPhoneFromUser(userAdminToken, userAdmin.id, addedPhone.id)

        then:
        retrievedPhone.isVerified() == verified
        retrievedPhone.number == addedPhone.number
        retrievedPhone.id == addedPhone.id

        cleanup:
        deleteUserQuietly(userAdmin)

        where:
        verified | requestContentMediaType | acceptMediaType
        true | MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE
        false | MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        false | MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        true | MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE
    }
}
