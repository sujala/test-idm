package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

/**
 * Tests the multifactor REST services
 */
class DefaultMultiFactorCloud20ServiceDeleteMobilePhoneFromUserIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private IdentityConfig identityConfig;

    def "Can only delete phone from user when MFA disabled if using SMS MFA"() {
        setup:
        def userAdmin = createUserAdmin()
        User entityUserAdmin = userRepository.getUserById(userAdmin.getId())
        String userAdminToken = utils.getTokenFromApiKeyAuth(entityUserAdmin.username, entityUserAdmin.apiKey) //use api since won't get revoked on mfa enablement
        def addedPhone = utils.setUpAndEnableUserForMultiFactorSMS(userAdminToken, userAdmin)

        when: "user tries to delete mobile phone when MFA SMS enabled"
        def response = cloud20.deletePhoneFromUser(userAdminToken, userAdmin.id, addedPhone.id)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        when: "user disables MFA can delete mobile phone"
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(false))
        response = cloud20.deletePhoneFromUser(userAdminToken, userAdmin.id, addedPhone.id)

        then: "phone is removed from user"
        response.status == HttpStatus.SC_NO_CONTENT

        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        finalUserAdmin.multiFactorDevicePinExpiration == null
        finalUserAdmin.multiFactorDevicePin == null
        !finalUserAdmin.multiFactorDeviceVerified
        finalUserAdmin.multiFactorMobilePhoneRsId == null

        cleanup:
        deleteUserQuietly(userAdmin)
    }

    def "Can delete phone from user when user users OTP MFA"() {
        setup:
        def userAdmin = createUserAdmin()
        User entityUserAdmin = userRepository.getUserById(userAdmin.getId())
        String userAdminToken = utils.getTokenFromApiKeyAuth(entityUserAdmin.username, entityUserAdmin.apiKey) //use api since won't get revoked on mfa enablement
        def addedOtp = utils.setUpAndEnableUserForMultiFactorOTP(userAdminToken, userAdmin)
        def addedPhone = utils.addVerifiedMobilePhoneToUser(userAdminToken, userAdmin)

        when: "user tries to delete mobile phone when MFA OTP enabled"
        def response = cloud20.deletePhoneFromUser(userAdminToken, userAdmin.id, addedPhone.id)

        then: "phone is removed from user"
        response.status == HttpStatus.SC_NO_CONTENT

        User finalUserAdmin = userRepository.getUserById(userAdmin.getId())
        finalUserAdmin.multiFactorDevicePinExpiration == null
        finalUserAdmin.multiFactorDevicePin == null
        !finalUserAdmin.multiFactorDeviceVerified
        finalUserAdmin.multiFactorMobilePhoneRsId == null

        cleanup:
        deleteUserQuietly(userAdmin)
    }
}
