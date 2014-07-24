package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.IdentityUserDao
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.MobilePhone
import com.rackspace.idm.domain.entity.User
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapIdentityUserRepositoryIntegrationTest extends Specification {

    @Shared def entityFactory = new EntityFactory()

    @Autowired
    private IdentityUserDao identityUserDao

    @Autowired
    private UserDao provisionedUserDao

    @Autowired
    private FederatedUserDao federatedUserDao

    @Autowired
    LdapConnectionPools ldapConnectionPools

    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    LDAPInterface con
    LDAPPersister<Application> applicationPersister

    LdapIntegrationTestSupport federatedUserLdapHelper;
    LdapIntegrationTestSupport provisionedUserLdapHelper;

    private IdentityProvider commonIdentityProvider;

    def setupSpec() {
    }

    def setup() {
        commonIdentityProvider = ldapIdentityProviderRepository.getIdentityProviderByName(Constants.DEFAULT_IDP_NAME)

        con = ldapConnectionPools.getAppConnPoolInterface()
        applicationPersister = LDAPPersister.getInstance(Application.class)
        federatedUserLdapHelper = new LdapIntegrationTestSupport(con, FederatedUser.class)
        provisionedUserLdapHelper = new LdapIntegrationTestSupport(con, User.class)
    }

    def "Verify retrieving a federated user"() {
        setup:
        FederatedUser fedUser = entityFactory.createFederatedUser()
        String expectedDn =  getExpectedFederatedUserDn(commonIdentityProvider, fedUser)
        federatedUserDao.addUser(commonIdentityProvider, fedUser)

        when: "search for end user"
        EndUser retrievedEndUser = identityUserDao.getEndUserById(fedUser.id)

        then: "we get the federated user back"
        retrievedEndUser != null
        retrievedEndUser instanceof FederatedUser
        ((FederatedUser)retrievedEndUser).username == fedUser.username
        ((FederatedUser)retrievedEndUser).region == fedUser.region
        ((FederatedUser)retrievedEndUser).domainId == fedUser.domainId
        ((FederatedUser)retrievedEndUser).email == fedUser.email
        ((FederatedUser)retrievedEndUser).federatedIdpUri == fedUser.federatedIdpUri
        ((FederatedUser)retrievedEndUser).getLdapEntry() != null

        retrievedEndUser.getUniqueId() == expectedDn

        federatedUserLdapHelper.entryExists(retrievedEndUser)

        when:  "search for federated user"
        FederatedUser retrievedFederatedUser = identityUserDao.getFederatedUserById(fedUser.id)

        then: "we get the federated user back"
        retrievedFederatedUser != null
        retrievedFederatedUser.region == fedUser.region
        retrievedFederatedUser.domainId == fedUser.domainId
        retrievedFederatedUser.email == fedUser.email
        retrievedFederatedUser.federatedIdpUri == fedUser.federatedIdpUri
        retrievedFederatedUser.getLdapEntry() != null

        retrievedFederatedUser.getUniqueId() == expectedDn

        federatedUserLdapHelper.entryExists(retrievedFederatedUser)

        when: "search for federated user via provisioned user method"
        User retrievedProvisionedUser = identityUserDao.getProvisionedUserById(fedUser.id)

        then: "null is returned"
        retrievedProvisionedUser == null

        cleanup:
        federatedUserLdapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        federatedUserLdapHelper.deleteDirect(retrievedEndUser) //delete this in case the uniqueId is NOT the expected one
    }

    def "Verify retrieving a provisioned user"() {
        setup:
        User user = entityFactory.createUser()
        provisionedUserDao.addUser(user)
        String expectedDn =  getExpectedProvisionedUserDn(user)

        when: "search for end user"
        EndUser retrievedEndUser = identityUserDao.getEndUserById(user.id)

        then: "we get the provisioned user back"
        retrievedEndUser != null
        retrievedEndUser instanceof User
        ((User)retrievedEndUser).username == user.username
        ((User)retrievedEndUser).region == user.region
        ((User)retrievedEndUser).domainId == user.domainId
        ((User)retrievedEndUser).email == user.email
        ((User)retrievedEndUser).getLdapEntry() != null

        retrievedEndUser.getUniqueId() == expectedDn

        provisionedUserLdapHelper.entryExists(retrievedEndUser)

        when: "search for provisioned user"
        User retrievedProvisionedUser = identityUserDao.getProvisionedUserById(user.id)

        then: "we get the provisioned user back"
        retrievedProvisionedUser != null
        retrievedProvisionedUser.username == user.username
        retrievedProvisionedUser.region == user.region
        retrievedProvisionedUser.domainId == user.domainId
        retrievedProvisionedUser.email == user.email
        retrievedProvisionedUser.getLdapEntry() != null

        retrievedProvisionedUser.getUniqueId() == expectedDn

        provisionedUserLdapHelper.entryExists(retrievedProvisionedUser)

        when: "search for provisioned user via federated user search"
        User retrievedFederatedUser = identityUserDao.getFederatedUserById(user.id)

        then: "null is returned"
        retrievedFederatedUser == null


        cleanup:
        provisionedUserLdapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        provisionedUserLdapHelper.deleteDirect(retrievedEndUser) //delete this in case the uniqueId is NOT the expected one
    }

    def "Verify searches returns null when id does not exist at all"() {
        expect:
        identityUserDao.getEndUserById(UUID.randomUUID().toString()) == null
        identityUserDao.getProvisionedUserById(UUID.randomUUID().toString()) == null
        identityUserDao.getFederatedUserById(UUID.randomUUID().toString()) == null
    }

    /* ###############################################################################
                        TEST HELPER METHODS
    ############################################################################### */

    String getExpectedFederatedUserDn(IdentityProvider provider, FederatedUser fedUser) {
        new LdapRepository.LdapDnBuilder(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN).addAttribute(LdapRepository.ATTR_UID, fedUser.username).addAttribute(LdapRepository.ATTR_OU, LdapRepository.EXTERNAL_PROVIDERS_USER_CONTAINER_NAME).addAttribute(LdapRepository.ATTR_OU, provider.name).build()
    }

    String getExpectedProvisionedUserDn(User user) {
        new LdapRepository.LdapDnBuilder(LdapRepository.USERS_BASE_DN).addAttribute(LdapRepository.ATTR_ID, user.id).build()
    }
}
