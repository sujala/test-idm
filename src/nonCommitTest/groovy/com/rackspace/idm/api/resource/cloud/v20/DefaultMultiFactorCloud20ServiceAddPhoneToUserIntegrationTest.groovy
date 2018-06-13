package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static testHelpers.IdmAssert.*

/**
 * Tests the multifactor REST services
 */
class DefaultMultiFactorCloud20ServiceAddPhoneToUserIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    UserDao userRepository;

    @Autowired
    private Configuration globalConfig;

    /**
     * This tests linking a phone number to a user
     *
     * @return
     */
    @Unroll("Add a phone to a user-admin: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Add a phone to a user-admin"() {
        setup:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        when:
        def responseAddPhoneToUser = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone, requestContentMediaType, acceptMediaType)
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone = responseAddPhoneToUser.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
        MobilePhone ldapPhone = mobilePhoneRepository.getById(responsePhone.getId())

        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        responseAddPhoneToUser != null
        responseAddPhoneToUser.getStatus() == HttpStatus.SC_CREATED
        responsePhone != null
        responsePhone.getId() != null
        responsePhone.getNumber() == requestMobilePhone.getNumber() //sent in phone is in E.123 format, which is same as output

        //verify phone is in ldap (don't verify phone since format could theoretically be different)
        ldapPhone != null
        responsePhone.getId() == ldapPhone.getId()

        //verify user is linked to phone
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == responsePhone.getId()

        cleanup:
        deleteUserQuietly(userAdmin)
        deletePhoneQuietly(ldapPhone)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE
    }

    def "Adding a phone to a user-admin, then updating other information about the user, does not unlink the phone"() {
        setup:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)
        def responseAddPhoneToUser = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone, requestContentMediaType, acceptMediaType)
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone responsePhone = responseAddPhoneToUser.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)

        def userForUpdate = v2Factory.createUserForUpdate(userAdmin.getId(), userAdmin.getUsername(), "a new name", "asdf@rackspace.com", true, null, null)

        when:
        def updateUserResponse = cloud20.updateUser(userAdminToken, userAdmin.getId(), userForUpdate)

        MobilePhone ldapPhone = mobilePhoneRepository.getById(responsePhone.getId())
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        //verify user is still linked to phone
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == responsePhone.getId()

        cleanup:
        deleteUserQuietly(userAdmin)
        deletePhoneQuietly(ldapPhone)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE
    }


    /**
     * This tests linking an existing phone number to a user
     *
     * @return
     */
    @Unroll("Add an existing phone to a user-admin: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Add an existing phone to a user-admin"() {
        setup:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone();
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)
        def firstResponse = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone)
        assert firstResponse.status == HttpStatus.SC_CREATED
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone firstResponsePhone = firstResponse.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
        MobilePhone ldapPhone = mobilePhoneRepository.getById(firstResponsePhone.getId())

        def userAdmin2 = createUserAdmin()
        String userAdmin2Token = authenticate(userAdmin2.username)

        when:
        def addWhenExistingResponse = cloud20.addPhoneToUser(userAdmin2Token, userAdmin2.id, requestMobilePhone, requestContentMediaType, acceptMediaType)
        assert addWhenExistingResponse.status == HttpStatus.SC_CREATED

        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone linkedExistingPhone = addWhenExistingResponse.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)

        User finalUserAdmin = userRepository.getUserById(userAdmin2.getId())

        then:
        linkedExistingPhone != null
        linkedExistingPhone.getId() != null
        linkedExistingPhone.getId() == firstResponsePhone.getId()
        linkedExistingPhone.getNumber() == requestMobilePhone.getNumber()
        linkedExistingPhone.getNumber() == firstResponsePhone.getNumber()

        //verify user is linked to phone
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == linkedExistingPhone.getId()

        cleanup:
        deleteUserQuietly(userAdmin)
        deleteUserQuietly(userAdmin2)
        deletePhoneQuietly(ldapPhone)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE
    }

    /**
     * Test the failure response when providing an invalid phone number.
     * <p>
     * Note - Due to existing base json converter classes providing a NULL telephone number (as opposed to an empty string) will result in an invalid
     * JSON message rather than the more informative "invalid phone number". Also, if you provide a null phone number in a json request, but want an xml response, the resultant fault code
     * is NOT the standard openstack fault, but a rackspace. This is documented in the test case "Fails with rackspace fault when provide an invalid mobile phone number"
     * </p>
     * @return
     */
    @Unroll("Fails with openstack fault when do not provide a valid mobile phone number in mobile phone object: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType ; testPhoneNumber=#testPhoneNumber ; expectedMessage=#expectedMessage")
    def "Fails when do not provide a valid mobile phone number in mobile phone object"() {
        setup:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone requestMobilePhone = v2Factory.createMobilePhone(testPhoneNumber);
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        when:
        def response = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, requestMobilePhone, requestContentMediaType, acceptMediaType)

        then:
        assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, expectedMessage)

        cleanup:
        deleteUserQuietly(userAdmin)

        where:
        requestContentMediaType | acceptMediaType   | testPhoneNumber | expectedMessage
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE | ""    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE | ""    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE | ""    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE | ""    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE | "abcd"    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE | "abcd"    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PHONE_NUMBER
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE | "abcd"    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE | "abcd"    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_PHONE_NUMBER
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE | null    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE | null    |  DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE | null    | DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE | null    | DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_MISSING_PHONE_NUMBER
    }

    @Unroll("Fails when do not provide a mobile phone object in request: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType ; expectedMessage=#expectedMessage")
    def "Fails when do not provide any mobile phone object in request"() {
        setup:
        def userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        when:
        def response = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, null, requestContentMediaType, acceptMediaType)

        then:
        assertRackspaceCommonFaultResponse(response, com.rackspace.api.common.fault.v1.BadRequestFault, HttpStatus.SC_BAD_REQUEST, expectedMessage)

        cleanup:
        deleteUserQuietly(userAdmin)

        where:
        requestContentMediaType | acceptMediaType  | expectedMessage
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE   |  "Invalid XML"
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE  |  "Invalid json request body"
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE | "Invalid XML"
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE | "Invalid json request body"
    }

    def deletePhoneQuietly(MobilePhone mobilePhone) {
        if (mobilePhone != null && mobilePhone.id != null) {
            mobilePhoneRepository.deleteMobilePhone(mobilePhone)
        }
    }
}
