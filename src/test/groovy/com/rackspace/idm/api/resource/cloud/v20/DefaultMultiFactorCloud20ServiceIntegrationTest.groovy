package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.helpers.Cloud20Utils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.APPLICATION_XML

@ContextConfiguration(locations = ["classpath:app-config.xml"])
class DefaultMultiFactorCloud20ServiceIntegrationTest extends RootIntegrationTest {

    @Autowired
    Cloud20Utils utils

    @Autowired
    LdapMobilePhoneRepository mobilePhoneRepository

    @Autowired
    LdapUserRepository userRepository

    @Unroll
    def "Get devices for user returns devices accept: #accept contentType: #contentType"() {
        given:
        useMediaType(accept, contentType)

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def phone = utils.addPhone(userAdminToken, userAdmin.id)

        when:
        MobilePhones phoneList = utils.listDevices(userAdmin)

        then:
        phoneList != null
        phoneList.mobilePhone.size() == 1
        phoneList.mobilePhone[0].isVerified() == false
        phoneList.mobilePhone[0].id == phone.id
        phoneList.mobilePhone[0].number == phone.number

        when:
        def user = userRepository.getUserById(userAdmin.id)
        user.multiFactorDeviceVerified = true
        userRepository.updateUser(user)
        MobilePhones phoneList2 = utils.listDevices(userAdmin)

        then:
        phoneList2 != null
        phoneList2.mobilePhone.size() == 1
        phoneList2.mobilePhone[0].isVerified() == true
        phoneList2.mobilePhone[0].id == phone.id
        phoneList2.mobilePhone[0].number == phone.number

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(phone.id))

        where:
        [ accept, contentType ] << contentTypePermutations()
    }

    @Unroll
    def "Get devices for user returns empty devices accept: #accept contentType: #contentType"() {
        given:
        useMediaType(accept, contentType)

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        MobilePhones phoneList = utils.listDevices(userAdmin)

        then:
        phoneList != null
        phoneList.mobilePhone.size() == 0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        [ accept, contentType ] << contentTypePermutations()
    }

}
