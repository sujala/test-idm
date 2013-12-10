package com.rackspace.idm.multifactor.providers.duo.service

import com.rackspace.idm.domain.config.PropertyFileConfiguration
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.multifactor.providers.duo.domain.DuoUser
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.Cloud20Utils
import testHelpers.EntityFactory

/**
 * This is an integration test between the duo provider integration and Duo Security. It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 */
class DuoUserManagementUserIntegrationTest extends Specification {

    @Shared DuoUserManagement duoUserManagement;

    def setupSpec() {
        PropertyFileConfiguration pfConfig = new PropertyFileConfiguration();
        Configuration devConfiguration = pfConfig.getConfig();
        duoUserManagement = new DuoUserManagement(devConfiguration)
    }

    def "successfully create duo user"() {
        User user = new User();
        user.setId(Cloud20Utils.createRandomString());

        when:
        DuoUser providerUser = duoUserManagement.createUser(user)

        then:
        providerUser.providerId != null

        cleanup:
        //depends on delete working, which is tested in another test.
        duoUserManagement.deleteUserById(providerUser.providerId)
    }

    def "successfully find duo user"() {
        User user = new User();
        user.setId(Cloud20Utils.createRandomString());
        DuoUser providerUser = duoUserManagement.createUser(user)

        when:
        DuoUser retrievedUser = duoUserManagement.getUserById(providerUser.providerId)

        then:
        providerUser.providerId.equals(retrievedUser.providerId)

        cleanup:
        //depends on delete working, which is tested in another test.
        duoUserManagement.deleteUserById(providerUser.providerId)
    }


    def "successfully delete duo user"() {
        User user = new User();
        user.setId(Cloud20Utils.createRandomString());
        DuoUser providerUser = duoUserManagement.createUser(user)
        DuoUser retrievedUser = duoUserManagement.getUserById(providerUser.providerId)
        assert providerUser.providerId.equals(retrievedUser.providerId)

        when:
        duoUserManagement.deleteUserById(providerUser.providerId)

        then:
        duoUserManagement.getUserById(providerUser.providerId) == null
    }

    def "create duo user when duo user already exists for idm user returns existing duo user"() {
        User user = new User();
        user.setId(Cloud20Utils.createRandomString());
        DuoUser originalProviderUser = duoUserManagement.createUser(user)

        when:
        DuoUser repeatProviderUser = duoUserManagement.createUser(user)

        then:
        repeatProviderUser.providerId.equals(originalProviderUser.providerId)

        cleanup:
        duoUserManagement.deleteUserById(originalProviderUser.providerId)
    }
}
