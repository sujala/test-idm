package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.GroupDao
import com.rackspace.idm.domain.dao.IdentityUserDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.helpers.CloudTestUtils
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections4.IteratorUtils
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
    private GroupDao groupDao

    @Autowired
    LdapConnectionPools ldapConnectionPools

    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    @Autowired
    CloudTestUtils utils

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
        verifyFederatedUser(fedUser, retrievedEndUser, expectedDn)

        when:  "search for federated user"
        FederatedUser retrievedFederatedUser = identityUserDao.getFederatedUserById(fedUser.id)

        then: "we get the federated user back"
        verifyFederatedUser(fedUser, retrievedFederatedUser, expectedDn)

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
        User user = entityFactory.createUser().with {
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }
        provisionedUserDao.addUser(user)
        String expectedDn =  getExpectedProvisionedUserDn(user)

        when: "search for end user"
        EndUser retrievedEndUser = identityUserDao.getEndUserById(user.id)

        then: "we get the provisioned user back"
        verifyProvisionedUser(user, retrievedEndUser, expectedDn)

        when: "search for provisioned user"
        User retrievedProvisionedUser = identityUserDao.getProvisionedUserById(user.id)

        then: "we get the provisioned user back"
        verifyProvisionedUser(user, retrievedProvisionedUser, expectedDn)

        when: "search for provisioned user via federated user search"
        User retrievedFederatedUser = identityUserDao.getFederatedUserById(user.id)

        then: "null is returned"
        retrievedFederatedUser == null


        cleanup:
        provisionedUserLdapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
        provisionedUserLdapHelper.deleteDirect(retrievedEndUser) //delete this in case the uniqueId is NOT the expected one
    }

    def "Verify retrieving a users by domain id"() {
        setup:
        def domainId = utils.getRandomUUID("domain")
        FederatedUser fedUser = entityFactory.createFederatedUser().with {
            it.domainId = domainId
            it
        }
        def expectedFederatedUserDn =  getExpectedFederatedUserDn(commonIdentityProvider, fedUser)
        def provisionedUser = entityFactory.createUser().with {
            it.domainId = domainId
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }
        provisionedUserDao.addUser(provisionedUser)
        def expectedProvisionedUserDn =  getExpectedProvisionedUserDn(provisionedUser)
        federatedUserDao.addUser(commonIdentityProvider, fedUser)

        when: "search for users by domain id"
        Iterable<EndUser> retrievedEndUsers = identityUserDao.getEndUsersByDomainId(domainId)
        def retrievedFederatedUser
        def retrievedProvisionedUser
        for(user in retrievedEndUsers) {
            if(user instanceof User) {
                retrievedProvisionedUser = user
            } else if(user instanceof FederatedUser) {
                retrievedFederatedUser = user
            }
        }

        then: "we get the federated user back"
        verifyFederatedUser(fedUser, retrievedFederatedUser, expectedFederatedUserDn)
        verifyProvisionedUser(provisionedUser, retrievedProvisionedUser, expectedProvisionedUserDn)

        cleanup:
        federatedUserLdapHelper.deleteDirect(expectedFederatedUserDn)
        provisionedUserLdapHelper.deleteDirect(expectedProvisionedUserDn)
    }

    def "getEnabledEndUsersByGroupId - Verify retrieving only enabled users by group id"() {
        setup:
        def random = ((String) UUID.randomUUID()).replace("-", "")
        def group = entityFactory.createGroup(random, "group$random", "this is a group")
        groupDao.addGroup(group)

        //add fed user with group
        FederatedUser fedUser = entityFactory.createFederatedUser().with {
            it.domainId = domainId
            it.getRsGroupId().add(group.getGroupId())
            it
        }

        def provisionedUser = entityFactory.createRandomUser().with {
            it.domainId = domainId
            it.getRsGroupId().add(group.getGroupId())
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }

        def provisionedDisabledUser = entityFactory.createRandomUser().with {
            it.domainId = domainId
            it.enabled = false
            it.getRsGroupId().add(group.getGroupId())
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }

        federatedUserDao.addUser(commonIdentityProvider, fedUser)
        provisionedUserDao.addUser(provisionedUser)
        provisionedUserDao.addUser(provisionedDisabledUser)

        when: "search for users by group id"
        List<EndUser> endUserList = IteratorUtils.toList(identityUserDao.getEnabledEndUsersByGroupId(group.getGroupId()).iterator())

        then: "we get the federated user back"
        endUserList.iterator().count {it.username == fedUser.username && it.getRsGroupId().contains(group.groupId)}
        endUserList.iterator().count {it.username == provisionedUser.username && it.getRsGroupId().contains(group.groupId)}
        endUserList.iterator().size() == 2

        cleanup:
        provisionedUserLdapHelper.deleteDirect(provisionedDisabledUser)
        endUserList.each {
            federatedUserLdapHelper.deleteDirect(it)
        }
        groupDao.deleteGroup(group.groupId)
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

    void verifyFederatedUser(originalUser, retrievedUser, expectedDn) {
        assert retrievedUser != null
        assert retrievedUser instanceof FederatedUser
        assert retrievedUser.username == originalUser.username
        assert retrievedUser.region == originalUser.region
        assert retrievedUser.domainId == originalUser.domainId
        assert CollectionUtils.isEqualCollection(retrievedUser.getRsGroupId(), originalUser.getRsGroupId())
        assert retrievedUser.email == originalUser.email
        assert retrievedUser.federatedIdpUri == originalUser.federatedIdpUri
        assert retrievedUser.getUniqueId() != null

        assert retrievedUser.getUniqueId() == expectedDn

        assert federatedUserLdapHelper.entryExists(retrievedUser)
    }

    void verifyProvisionedUser(originalUser, retrievedUser, expectedDn) {
        assert retrievedUser != null
        assert retrievedUser instanceof User
        assert retrievedUser.username == originalUser.username
        assert retrievedUser.region == originalUser.region
        assert retrievedUser.domainId == originalUser.domainId
        assert retrievedUser.email == originalUser.email
        assert retrievedUser.getUniqueId() != null

        assert retrievedUser.getUniqueId() == expectedDn

        assert provisionedUserLdapHelper.entryExists(retrievedUser)
    }

}
