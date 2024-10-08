package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.ListUsersSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.dao.*
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.helpers.CloudTestUtils
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections4.IteratorUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

@WebAppConfiguration
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
    private DomainDao domainDao

    @Autowired(required = false)
    LdapConnectionPools ldapConnectionPools

    @Autowired
    IdentityProviderDao ldapIdentityProviderRepository

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
        commonIdentityProvider = ldapIdentityProviderRepository.getIdentityProviderById(Constants.DEFAULT_IDP_ID)

        if (ldapConnectionPools != null) {
            con = ldapConnectionPools.getAppConnPoolInterface()
            applicationPersister = LDAPPersister.getInstance(Application.class)
            federatedUserLdapHelper = new LdapIntegrationTestSupport(con, FederatedUser.class)
            provisionedUserLdapHelper = new LdapIntegrationTestSupport(con, User.class)
        }
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
        if (ldapConnectionPools != null) {
            federatedUserLdapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
            federatedUserLdapHelper.deleteDirect(retrievedEndUser)
            //delete this in case the uniqueId is NOT the expected one
        }
    }

    def "Verify retrieving a provisioned user"() {
        setup:
        User user = entityFactory.createUser().with {
            it.id = null
            it.readOnlyEntry = null
            it.uniqueId = null
            it.passwordHistory = new ArrayList<>(Arrays.asList("newPassword1", "test124"))
            it
        }
        provisionedUserDao.addUser(user)
        String expectedDn =  getExpectedProvisionedUserDn(user)

        when: "search for end user"
        EndUser retrievedEndUser = identityUserDao.getEndUserById(user.id)

        then: "we get the provisioned user back"
        verifyProvisionedUser(user, retrievedEndUser, expectedDn)
        ((User) retrievedEndUser).getPasswordHistory() == null

        when: "search for provisioned user"
        User retrievedProvisionedUser = identityUserDao.getProvisionedUserByIdWithPwdHis(user.id)
        List<String> pwdHist = retrievedProvisionedUser.getPasswordHistory()

        then: "we get the provisioned user back"
        verifyProvisionedUser(user, retrievedProvisionedUser, expectedDn)
        pwdHist.size() == 2

        when: "search for provisioned user via federated user search"
        User retrievedFederatedUser = identityUserDao.getFederatedUserById(user.id)

        then: "null is returned"
        retrievedFederatedUser == null


        cleanup:
        if (ldapConnectionPools != null) {
            provisionedUserLdapHelper.deleteDirect(expectedDn) //delete this in case the ldap entry is not set correctly
            provisionedUserLdapHelper.deleteDirect(retrievedEndUser)
            //delete this in case the uniqueId is NOT the expected one
        }
    }

    def "Verify retrieving a users by domain id"() {
        setup:
        def domainId = utils.getRandomUUID("domain")
        domainDao.addDomain(entityFactory.createDomain(domainId))
        FederatedUser fedUser = entityFactory.createFederatedUser().with {
            it.domainId = domainId
            it
        }
        def expectedFederatedUserDn =  getExpectedFederatedUserDn(commonIdentityProvider, fedUser)
        def provisionedUser = entityFactory.createUser().with {
            it.id = null
            it.domainId = domainId
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }
        provisionedUserDao.addUser(provisionedUser)
        def expectedProvisionedUserDn =  getExpectedProvisionedUserDn(provisionedUser)
        federatedUserDao.addUser(commonIdentityProvider, fedUser)

        when: "search for users by domain id"
        Iterable<EndUser> retrievedEndUsers = identityUserDao.getEndUsersByDomainId(domainId, User.UserType.VERIFIED)
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
        if (ldapConnectionPools != null) {
            federatedUserLdapHelper.deleteDirect(expectedFederatedUserDn)
            provisionedUserLdapHelper.deleteDirect(expectedProvisionedUserDn)
        }
        domainDao.deleteDomain(domainId)
    }

    def "getEnabledEndUsersByGroupId - Verify retrieving only enabled users by group id"() {
        setup:
        def domainId = utils.getRandomUUID("domain")
        domainDao.addDomain(entityFactory.createDomain(domainId))
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
        if (ldapConnectionPools != null) {
            provisionedUserLdapHelper.deleteDirect(provisionedDisabledUser)
        }
        endUserList.each {
            if (ldapConnectionPools != null) {
                federatedUserLdapHelper.deleteDirect(it)
            }
        }
        groupDao.deleteGroup(group.groupId)
        domainDao.deleteDomain(domainId)
    }

    def "get users by search params"() {
        given:
        def username = "testUser" + utils.getRandomUUID()
        def domainId = utils.getRandomIntegerString()
        def email = "email@rackspace.com"
        def contactId = utils.getRandomUUID("contactId")
        def unverified = false
        User user = entityFactory.createUser().with {
            it.id = null
            it.username = username
            it.domainId = domainId
            it.email = email
            it.unverified = unverified
            it.contactId = contactId
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }
        provisionedUserDao.addUser(user)
        List<ListUsersSearchParams> listUsersSearchParams = [
                new ListUsersSearchParams(username, null, null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(username, email, null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, email, null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, null, contactId, new PaginationParams()),
                new ListUsersSearchParams(username, null, null, null, false, null, contactId, new PaginationParams()),
                new ListUsersSearchParams(null, email, null, null, false, null, contactId, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, null, contactId, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, User.UserType.ALL.name(), contactId, new PaginationParams()),
        ]
        List<PaginatorContext> responses = []

        when: "filter by search params"
        listUsersSearchParams.each { params ->
            responses << identityUserDao.getEndUsersPaged(params)
        }

        then:
        responses.each { response ->
            assert response.valueList.size() == 1
            EndUser retrievedUser = response.valueList.get(0)
            assert retrievedUser.username ==  username
            assert retrievedUser.email == email
            assert retrievedUser.domainId == domainId
        }
    }

    def "empty list cases - get users by search params"() {
        given:
        def username = "testUser" + utils.getRandomUUID()
        def domainId = utils.getRandomIntegerString()
        def email = "email@rackspace.com"
        def unverified = false
        def contactId = utils.getRandomUUID("contactId")
        User user = entityFactory.createUser().with {
            it.id = null
            it.username = username
            it.domainId = domainId
            it.email = email
            it.unverified = unverified
            it.readOnlyEntry = null
            it.uniqueId = null
            it
        }
        provisionedUserDao.addUser(user)
        List<ListUsersSearchParams> listUsersSearchParams = [
                new ListUsersSearchParams("badName", null, null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(username, email, null, "badDomain", false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, "badEmail", null, domainId, false, User.UserType.ALL.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, User.UserType.UNVERIFIED.name(), null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, null, "badContact", new PaginationParams()),
        ]
        List<PaginatorContext> responses = []

        when: "filter by search params"
        listUsersSearchParams.each { params ->
            responses << identityUserDao.getEndUsersPaged(params)
        }

        then:
        responses.each { response ->
            assert response.valueList.size() == 0
        }
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
        if (ldapConnectionPools != null) {
            return new LdapRepository.LdapDnBuilder(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN).addAttribute(LdapRepository.ATTR_UID, fedUser.username).addAttribute(LdapRepository.ATTR_OU, LdapRepository.EXTERNAL_PROVIDERS_USER_CONTAINER_NAME).addAttribute(LdapRepository.ATTR_OU, provider.providerId).build()
        }
        return null;
    }

    String getExpectedProvisionedUserDn(User user) {
        if (ldapConnectionPools != null) {
            return new LdapRepository.LdapDnBuilder(LdapRepository.USERS_BASE_DN).addAttribute(LdapRepository.ATTR_ID, user.id).build()
        }
        return null;
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

        if (ldapConnectionPools != null) {
            assert retrievedUser.getUniqueId() != null
            assert retrievedUser.getUniqueId() == expectedDn
            assert federatedUserLdapHelper.entryExists(retrievedUser)
        }
    }

    void verifyProvisionedUser(originalUser, retrievedUser, expectedDn) {
        assert retrievedUser != null
        assert retrievedUser instanceof User
        assert retrievedUser.username == originalUser.username
        assert retrievedUser.region == originalUser.region
        assert retrievedUser.domainId == originalUser.domainId
        assert retrievedUser.email == originalUser.email

        if (ldapConnectionPools != null) {
            assert retrievedUser.getUniqueId() != null
            assert retrievedUser.getUniqueId() == expectedDn
            assert provisionedUserLdapHelper.entryExists(retrievedUser)
        }
    }

}
