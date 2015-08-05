package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

/**
 * Tests the multifactor delete multifactor REST service
 */
class DefaultMultiFactorCloud20ServiceDeleteMultiFactorIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    UserDao userRepository;

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    UserScopeAccess userScopeAccess;
    org.openstack.docs.identity.api.v2.User userAdmin;
    String userAdminToken;
    VerificationCode constantVerificationCode;

    /**
     * Sets up a new user
     *
     * @return
     */
    def setup() {
        userAdmin = createUserAdmin()
        userAdminToken = authenticate(userAdmin.username)
        userScopeAccess = (UserScopeAccess)scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
    }

    def cleanup() {
        if (userAdmin != null) {
            if (multiFactorService.removeMultiFactorForUser(userAdmin.id))  //remove duo profile
            deleteUserQuietly(userAdmin)
        }
    }

    def "Verify that enabling multifactor expires existing token"() {
        setup:
        def token = scopeAccessRepository.getScopeAccessByAccessToken(userScopeAccess.accessTokenString)
        assert(!token.isAccessTokenExpired(new DateTime()))

        when:
        setUpAndEnableMultiFactor()
        def expiredToken = scopeAccessRepository.getScopeAccessByAccessToken(userScopeAccess.accessTokenString)

        then:
        expiredToken.isAccessTokenExpired(new DateTime())

        cleanup:
        resetTokenExpiration()
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)
    }

    /**
     * This tests deleting multi-factor on an account that is fully enabled with multifactor
     *
     * @return
     */
    @Unroll("Successfully delete multifactor: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor"() {
        setup:
        setUpAndEnableMultiFactor()
        resetTokenExpiration()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests enabling multi-factor on an account that is fully setup for multifactor, but not enabled.
     *
     * @return
     */
    @Unroll("Successfully delete multifactor when not enabled: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor when not enabled"() {
        setup:
        setUpMultiFactorWithoutEnable()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests enabling multi-factor on an account that is fully setup for multifactor, but not enabled.
     *
     * @return
     */
    @Unroll("Successfully delete multifactor when partially set up with just a phone: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully delete multifactor when partially set up with just a phone"() {
        setup:
        setUpMultiFactorWithUnverifiedPhone()
        resetTokenExpiration()

        when:
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())

        then:
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        verifyFinalUserState(finalUserAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests removing multifactor from an account that was set up for it, then setting it back up again
     * @return
     */
    @Unroll("Successfully reset up multifactor after removing it wth same phone number: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully reset up multifactor after removing it wth same phone number"() {
        setup:
        def responsePhone = setUpAndEnableMultiFactor()
        resetTokenExpiration()
        User retrievedUserAdmin = userRepository.getUserById(userAdmin.getId())
        MobilePhone originalPhone = mobilePhoneRepository.getById(retrievedUserAdmin.getMultiFactorMobilePhoneRsId())
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone newXmlPhone = v2Factory.createMobilePhone(originalPhone.getTelephoneNumber())
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        when:
        //setup multifactor again using same phone as before
        com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone returnedXmlPhone = utils.addPhone(userAdminToken, userAdmin.id, newXmlPhone)
        MobilePhone newPhone = mobilePhoneRepository.getById(returnedXmlPhone.getId())

        utils.sendVerificationCodeToPhone(userAdminToken, userAdmin.id, returnedXmlPhone.id)
        constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
        utils.verifyPhone(userAdminToken, userAdmin.id, returnedXmlPhone.id, constantVerificationCode)
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(true))
        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        resetTokenExpiration()

        then:
        newPhone.externalMultiFactorPhoneId != originalPhone.externalMultiFactorPhoneId //the phones ARE deleted in duo
        newPhone.id != originalPhone.id //the phones ARE deleted in ldap

        finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        finalUserAdmin.getMultiFactorDevicePin() == null
        finalUserAdmin.isMultiFactorDeviceVerified()
        finalUserAdmin.isMultiFactorEnabled()

        cleanup:
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    /**
     * This tests removing multifactor from an account that was set up for it, then setting it back up again
     * @return
     */
    @Unroll("Successfully reset up multifactor after removing it: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType")
    def "Successfully reset up multifactor after removing it"() {
        setup:
        setUpAndEnableMultiFactor()
        resetTokenExpiration()
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        expect:
        setUpAndEnableMultiFactor() //"test" is that no errors reported from assertions made while enabling multi-factor again
        resetTokenExpiration()

        cleanup:
        utils.deleteMultiFactor(userAdminToken, userAdmin.id)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "phone is deleted when last link to phone is removed: #requestContentMediaType, #acceptMediaType"() {
        setup:
        def phone = setUpAndEnableMultiFactor(userAdminToken, userAdmin)
        resetTokenExpiration(userScopeAccess)
        def userAdmin2 = createUserAdmin()
        def userAdminToken2 = authenticate(userAdmin2.username)
        setUpAndEnableMultiFactor(userAdminToken2, userAdmin2, phone.number)
        def userScopeAccess2 = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken2)
        resetTokenExpiration(userScopeAccess2)

        when: "delete mfa from the first user"
        def response = cloud20.deleteMultiFactor(userAdminToken, userAdmin.id, requestContentMediaType, acceptMediaType)

        then: "phone was not deleted"
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        mobilePhoneRepository.getById(phone.id) != null

        when: "delete mfa from the other user"
        response = cloud20.deleteMultiFactor(userAdminToken2, userAdmin2.id, requestContentMediaType, acceptMediaType)

        then: "the phone was deleted"
        response.getStatus() == HttpStatus.SC_NO_CONTENT
        mobilePhoneRepository.getById(phone.id) == null

        cleanup:
        deleteUserQuietly(userAdmin2)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "phone is deleted when last link to phone is removed through user deletion: #requestContentMediaType, #acceptMediaType"() {
        setup:
        def phone = setUpAndEnableMultiFactor(userAdminToken, userAdmin)
        resetTokenExpiration(userScopeAccess)
        def userAdmin2 = createUserAdmin()
        def userAdminToken2 = authenticate(userAdmin2.username)
        setUpAndEnableMultiFactor(userAdminToken2, userAdmin2, phone.number)
        def userScopeAccess2 = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken2)
        resetTokenExpiration(userScopeAccess2)

        when: "delete mfa from the first user"
        utils.deleteUser(userAdmin)

        then: "phone was not deleted"
        mobilePhoneRepository.getById(phone.id) != null

        when: "delete other mfa user"
        utils.deleteUser(userAdmin2)

        then: "the phone was deleted"
        mobilePhoneRepository.getById(phone.id) == null

        cleanup:
        userAdmin = null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    def void verifyFinalUserState(User finalUserAdmin) {
        assert finalUserAdmin.getMultiFactorDevicePinExpiration() == null
        assert finalUserAdmin.getMultiFactorDevicePin() == null
        assert !finalUserAdmin.isMultiFactorDeviceVerified()
        assert !finalUserAdmin.isMultiFactorEnabled()
        assert finalUserAdmin.getExternalMultiFactorUserId() == null
        assert finalUserAdmin.getMultiFactorMobilePhoneRsId() == null
    }


    def setUpAndEnableMultiFactor(token = userAdminToken, user = userAdmin, phoneNumber = null) {
        def phone = setUpMultiFactorWithoutEnable(token, user, phoneNumber)
        utils.updateMultiFactor(token, user.id, v2Factory.createMultiFactorSettings(true))
        return phone
    }

    def setUpMultiFactorWithoutEnable(token = userAdminToken, user = userAdmin, phoneNumber = null) {
        def phone = setUpMultiFactorWithUnverifiedPhone(token, user, phoneNumber)
        utils.sendVerificationCodeToPhone(token, user.id, phone.id)
        constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
        utils.verifyPhone(token, user.id, phone.id, constantVerificationCode)
        return phone
    }

    def setUpMultiFactorWithUnverifiedPhone(token = userAdminToken, user = userAdmin, phoneNumber = null) {
        def phone
        if(phoneNumber != null) {
            phone = utils.addPhone(token, user.id, v2Factory.createMobilePhone(phoneNumber))
        } else {
            phone = utils.addPhone(token, user.id)
        }
        return phone
    }

    def void resetTokenExpiration(scopeAccessToReset = userScopeAccess) {
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        scopeAccessToReset.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(scopeAccessToReset)
    }
}
