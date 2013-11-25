package com.rackspace.idm.multifactor.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

/**
 * Tests the multifactor service.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"])
class BasicMultiFactorServiceIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private BasicMultiFactorService multiFactorService;

    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    /**
     * This tests linking a phone number to a user
     *
     * @return
     */
    def "Add a phone to a user-admin"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdminOpenStack.getId(), telephoneNumber)
        MobilePhone retrievedPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap

        User finalUserAdmin = userRepository.getUserById(userAdminOpenStack.getId())
        then:
        //verify passed in object is updated with id, and phone number still as expected
        phone.getId() != null
        phone.getTelephoneNumber() == canonTelephoneNumber

        phone.getId() == retrievedPhone.getId()
        retrievedPhone.getTelephoneNumber() == canonTelephoneNumber

        finalUserAdmin.getMultiFactorMobilePhoneRsId() == phone.getId()

        cleanup:
        userRepository.deleteObject(finalUserAdmin)
        mobilePhoneRepository.deleteObject(retrievedPhone)
    }

    /**
     * TODO: This test performs the entire golden use case of adding a mobile phone to a user-admin, sending sms, and verifying the code. The
     * end state of the user is verified for appropriate behavior.
     *
     * @return
     */
    def "Golden use case of adding phone, send pin, verify pin works in harmony"() {
        setup:
        org.openstack.docs.identity.api.v2.User userAdminOpenStack = createUserAdmin()
        User userAdmin = userRepository.getUserById(userAdminOpenStack.getId())

        Phonenumber.PhoneNumber telephoneNumber = PhoneNumberGenerator.randomUSNumber();
        String canonTelephoneNumber = PhoneNumberGenerator.canonicalizePhoneNumberToString(telephoneNumber)

        when:
        //STEP 1: Add phone to user
        MobilePhone phone = multiFactorService.addPhoneToUser(userAdmin.getId(), telephoneNumber)

        MobilePhone finalPhone = mobilePhoneRepository.getById(phone.getId())  //retrieve the phone from ldap
        User finalUser = userRepository.getUserById(userAdminOpenStack.getId())

        then:
        //verify phone
        finalPhone.getId() != null
        finalPhone.getTelephoneNumber() == canonTelephoneNumber

        //verify user state
        finalUser.getMultiFactorMobilePhoneRsId() == finalPhone.getId()

        cleanup:
        userRepository.deleteObject(userAdmin)
        mobilePhoneRepository.deleteObject(phone)
    }
}
