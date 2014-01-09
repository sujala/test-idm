package com.rackspace.idm.multifactor.providers.duo.service

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.config.PropertyFileConfiguration
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import com.rackspace.idm.multifactor.providers.ProviderPhone
import com.rackspace.idm.multifactor.providers.ProviderUser
import com.rackspace.idm.multifactor.providers.duo.config.ApacheConfigAdminApiConfig
import com.rackspace.idm.multifactor.providers.duo.domain.DuoPhone
import com.rackspace.idm.multifactor.providers.duo.domain.DuoUser
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

/**
 * This is an integration test between the duo provider integration and Duo Security. It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 */
class DuoUserManagementUserPhoneIntegrationTest extends Specification {

    @Shared DuoUserManagement duoUserManagement;

    @Shared EntityFactory entityFactory = new EntityFactory()

    def setupSpec() {
        PropertyFileConfiguration pfConfig = new PropertyFileConfiguration();
        Configuration devConfiguration = pfConfig.getConfig();
        duoUserManagement = new DuoUserManagement()
        duoUserManagement.setAdminApiConfig(new ApacheConfigAdminApiConfig(devConfiguration))
        duoUserManagement.init()
    }

    /**
     * Tests attaching a phone to a newly created user and verifying the user lists the phone and the phone
     * lists the user.
     */
    def "associate new phone with user"() {
        User user = new User();
        user.setId("bob-test-" + UUID.randomUUID().toString());
        ProviderUser providerUser = duoUserManagement.createUser(user)
        assert providerUser.providerId != null
        MobilePhone fakePhone = entityFactory.createMobilePhoneWithId()

        when:
            DuoPhone providerPhone = duoUserManagement.linkMobilePhoneToUser(providerUser.providerId, fakePhone)
            DuoUser modifiedUser = duoUserManagement.getUserById(providerUser.providerId)

        then:
            providerPhone.providerId != null
            providerPhone.users.user_id.contains(providerUser.providerId)
            modifiedUser.getPhones().phone_id.contains(providerPhone.providerId) //the list should contain a phone with the specified id

        cleanup:
            if (providerPhone != null) duoUserManagement.deleteMobilePhone(providerPhone.providerId)
            if (providerUser != null) duoUserManagement.deleteUserById(providerUser.providerId)
    }

    /**
     * Test attaching a new phone to a new user, then deleting the phone to see if the association to the
     * user is also deleted..
     */
    def "delete phone that is linked to user removes link from user"() {
        User idmUser = new User();
        idmUser.setId("bob-test-" + UUID.randomUUID().toString());

        //create a new user from scratch
        ProviderUser providerUser = duoUserManagement.createUser(idmUser)
        assert providerUser.providerId != null

        //add a phone and verify the user has a phone added
        MobilePhone fakePhone = entityFactory.createMobilePhoneWithId()
        ProviderPhone providerPhone = duoUserManagement.linkMobilePhoneToUser(providerUser.providerId, fakePhone)
        assert providerPhone.providerId != null
        DuoUser duoUser = duoUserManagement.getUserById(providerUser.providerId)
        assert duoUser.phones.size() == 1

        when: "delete the phone linked to the user"
            duoUserManagement.deleteMobilePhone(providerPhone.providerId)
            DuoUser postPhoneDeleteUser = duoUserManagement.getUserById(providerUser.providerId)

        then: "the user is no longer linked to the phone"
            assert postPhoneDeleteUser.phones.size() == 0

        cleanup:
        if (providerUser != null) duoUserManagement.deleteUserById(providerUser.providerId)
    }

    /**
     * Test attaching a new phone to a new user, then deleting the user to see if the association to the
     * phone is also deleted..
     */
    def "delete user that is linked to phone removes link from phone"() {
        User idmUser = new User();
        idmUser.setId("bob-test-" + UUID.randomUUID().toString());
        MobilePhone fakePhone = entityFactory.createMobilePhoneWithId()

        //create a new user from scratch
        ProviderUser providerUser = duoUserManagement.createUser(idmUser)
        assert providerUser.providerId != null

        //add a phone and verify the phone lists the user
        ProviderPhone providerPhone = duoUserManagement.linkMobilePhoneToUser(providerUser.providerId, fakePhone)
        assert providerPhone.providerId != null
        assert providerPhone.users.user_id.contains(providerUser.providerId)

        when: "delete the user linked to the phone"
        duoUserManagement.deleteUserById(providerUser.providerId)
        DuoPhone postPhone = duoUserManagement.getPhoneById(providerPhone.getProviderId())

        then: "the user is no longer linked to the phone"
        assert postPhone.users.size() == 0

        cleanup:
        if (providerPhone != null) duoUserManagement.deleteMobilePhone(providerPhone.providerId)
    }
}
