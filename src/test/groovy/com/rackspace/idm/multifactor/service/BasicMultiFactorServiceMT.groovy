package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest

import com.rackspace.idm.multifactor.PhoneNumberGenerator
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

/**
 * This is a manual integration test between the idm DefaultMultiFactorService, ldap, and Duo Security that must not be part of any automated run. It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 *
 * The test is a Manual Test (MT) because it causes a REAL SMS message to be sent to a REAL phone which costs REAL money for rackspace.
 */
class BasicMultiFactorServiceMT extends RootConcurrentIntegrationTest {

    @Shared Phonenumber.PhoneNumber phoneToText = PhoneNumberUtil.getInstance().parse("5127759583", "US")

    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    /**
     * This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "Add a phone to user-admin and verify"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = phoneToText;
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        //STEP 1: Add phone to user
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)

        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify phone
        finalPhone.getId() != null
        finalPhone.getTelephoneNumber() == canonTelephoneNumber

        //verify user state
        finalUserAdmin.getMultiFactorMobilePhoneRsId() == finalPhone.getId()

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }

}
