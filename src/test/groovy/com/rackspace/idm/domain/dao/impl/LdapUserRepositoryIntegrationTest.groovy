package com.rackspace.idm.domain.dao.impl
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.EncryptionService
import com.rackspace.idm.helpers.CloudTestUtils
import com.unboundid.ldap.sdk.LDAPException
import org.apache.commons.collections4.IteratorUtils
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapUserRepositoryIntegrationTest extends Specification{
    @Autowired
    LdapUserRepository ldapUserRepository;

    @Autowired
    EncryptionService encryptionService;

    @Autowired Configuration config

    @Autowired
    CloudTestUtils cloudTestUtils

    @Shared def random
    @Shared def username

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random
    }

    def "getNextId returns UUID"() {
        when:
        def success = false
        def id = ldapUserRepository.getNextId()
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        }

        then:
        success == true
    }

    def "user crud"() {
        given:
        String rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")

        when:
        ldapUserRepository.addUser(user)
        def retrievedUser = ldapUserRepository.getUserByUsername(username)
        def retrievedUser2 = ldapUserRepository.getUserByUsername(username)
        ldapUserRepository.deleteUser(retrievedUser);
        def deletedUser = ldapUserRepository.getUserByUsername(username)

        then:
        retrievedUser != null
        deletedUser == null

        retrievedUser.getPasswordLastUpdated() != null
        retrievedUser.getUserPassword() != null
        retrievedUser.isPasswordWasSelfUpdated() == false
        retrievedUser.getPasswordFailureDate() == null
        retrievedUser.getRsGroupDN() == null

        when: "delete object already deleted when using recursion delete algorithm"
        ldapUserRepository.deleteUser(retrievedUser2);

        then: "an illegal state exception is thrown"
        def e = thrown(IllegalStateException)
        e.message == "no such object"
    }

    def "retrieving users in a domain returns all users within the domain"() {
        given:
        def username1 = "enabledUse$random"
        def username2 = "disabledUse$random"
        def username3 = "use$random"
        def domain1 = "domain$random"
        def domain2 = "domain2$random"
        def user1 = createUser("1$random", username1, domain1, "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", username2, domain1, "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", username3, domain2, "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain(domain1).collect()
        ldapUserRepository.deleteUser(username1)
        ldapUserRepository.deleteUser(username2)
        ldapUserRepository.deleteUser(username3)

        then:
        userList != null
        userList.size() == 2
    }

    def "retrieving enabled users in a domain returns enabled users within the domain"() {
        given:
        def username1 = "enabledUse$random"
        def username2 = "disabledUse$random"
        def username3 = "use$random"
        def domain1 = "domain$random"
        def domain2 = "domain2$random"
        def user1 = createUser("1$random", username1, domain1, "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", username2, domain1, "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", username3, domain2, "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomainAndEnabledFlag(domain1, true).collect()
        ldapUserRepository.deleteUser(username1)
        ldapUserRepository.deleteUser(username2)
        ldapUserRepository.deleteUser(username3)

        then:
        userList != null
        userList.size() == 1
        userList.get(0).username == username1
    }


    def "retrieving disabled users in a domain returns disabled users within the domain"() {
        given:
        def username1 = "enabledUse$random"
        def username2 = "disabledUse$random"
        def username3 = "use$random"
        def domain1 = "domain$random"
        def domain2 = "domain2$random"
        def user1 = createUser("1$random", username1, domain1, "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", username2, domain1, "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", username3, domain2, "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomainAndEnabledFlag(domain1, false).collect()
        ldapUserRepository.deleteUser(username1)
        ldapUserRepository.deleteUser(username2)
        ldapUserRepository.deleteUser(username3)

        then:
        userList != null
        userList.size() == 1
        userList.get(0).username == username2
    }

    def "retrieving enabled users does not retrieve disabled users"() {
        given:
        String username1 = "enabledUse$random"
        String username2 = "disabledUse$random"
        def user1 = createUser("1$random", username1, "domain$random", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", username2, "domain$random", "email@email.com", false, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)

        when:
        def userList = ldapUserRepository.getEnabledUsers(0, 1000).valueList.collect()

        then:
        userList != null
        userList.username.contains(username1)
        !userList.username.contains(username2)

        cleanup:
        ldapUserRepository.deleteUser(username1)
        ldapUserRepository.deleteUser(username2)
    }

    def "getEnabledUsersByGroupId pagination - retrieving enabled users by groupId does not retrieve disabled users"() {
        given:
        String enabledUserName = cloudTestUtils.getRandomUUID()
        String disabledUsername =cloudTestUtils.getRandomUUID()
        String groupId =  cloudTestUtils.getRandomUUID()
        def user1 = createUser("1$random", enabledUserName, cloudTestUtils.getRandomUUID(), "email@email.com", true, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }
        def user2 = createUser("2$random", disabledUsername, cloudTestUtils.getRandomUUID(), "email@email.com", false, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)

        when:
        def userList = ldapUserRepository.getEnabledUsersByGroupId(groupId, 0, 1000).valueList

        then:
        userList != null
        userList.username.contains(enabledUserName)
        !userList.username.contains(disabledUsername)

        cleanup:
        ldapUserRepository.deleteUser(enabledUserName)
        ldapUserRepository.deleteUser(disabledUsername)
    }

    def "getEnabledUsersByGroupId - retrieving enabled users by groupId does not retrieve disabled users"() {
        given:
        String enabledUserName = cloudTestUtils.getRandomUUID()
        String disabledUsername =cloudTestUtils.getRandomUUID()
        String groupId =  cloudTestUtils.getRandomUUID()
        def user1 = createUser("1$random", enabledUserName, cloudTestUtils.getRandomUUID(), "email@email.com", true, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }
        def user2 = createUser("2$random", disabledUsername, cloudTestUtils.getRandomUUID(), "email@email.com", false, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)

        when:
        def userList = IteratorUtils.toList(ldapUserRepository.getEnabledUsersByGroupId(groupId).iterator())

        then:
        userList != null
        userList.username.contains(enabledUserName)
        !userList.username.contains(disabledUsername)

        cleanup:
        ldapUserRepository.deleteUser(enabledUserName)
        ldapUserRepository.deleteUser(disabledUsername)
    }

    def "getDisabledUsersByGroupId - retrieving disabled users by groupId does not retrieve enabled users"() {
        given:
        String enabledUserName = cloudTestUtils.getRandomUUID()
        String disabledUsername =cloudTestUtils.getRandomUUID()
        String groupId =  cloudTestUtils.getRandomUUID()
        def user1 = createUser("1$random", enabledUserName, cloudTestUtils.getRandomUUID(), "email@email.com", true, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }
        def user2 = createUser("2$random", disabledUsername, cloudTestUtils.getRandomUUID(), "email@email.com", false, "DFW", "Password1").with {
            it.rsGroupId = [groupId] as HashSet
            it
        }

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)

        when:
        def userList = IteratorUtils.toList(ldapUserRepository.getDisabledUsersByGroupId(groupId).iterator())

        then:
        userList != null
        !userList.username.contains(enabledUserName)
        userList.username.contains(disabledUsername)

        cleanup:
        ldapUserRepository.deleteUser(enabledUserName)
        ldapUserRepository.deleteUser(disabledUsername)
    }

    def "calling getUserByEmail returns the user"() {
        given:
        def email = "email$random@email.com"
        def user1 = createUser("1$random", "username$random", "1234567890", email, true, "DFW", "Password1")

        when:
        ldapUserRepository.addUser(user1)
        def user = ldapUserRepository.getUsersByEmail(email).collect()
        ldapUserRepository.deleteUser(user.get(0))

        then:
        user != null
        user1.email == user.get(0).email
    }
        
    def "create user with salt and encryption version id"() {
        given:
        def rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password").with {
            it.salt = "A1 B1"
            it.encryptionVersion = "0"
            return it
        }

        when:
        ldapUserRepository.addUser(user);
        User retrievedUser = ldapUserRepository.getUserById(rsId)
        ldapUserRepository.deleteUser(retrievedUser)

        then:
        retrievedUser.salt == user.salt
        retrievedUser.encryptionVersion == user.encryptionVersion
    }

    def "updateUserEncryption updates user encryption"() {
        given:
        def rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password").with {
            it.salt = "A1 B1"
            it.encryptionVersion = "0"
            return it
        }

        when:
        ldapUserRepository.addUser(user);
        ldapUserRepository.updateUserEncryption(rsId);
        User retrievedUser = ldapUserRepository.getUserById(rsId)

        then:
        retrievedUser.displayName == user.displayName
        retrievedUser.email == user.email
        retrievedUser.apiKey == user.apiKey
        retrievedUser.secretAnswer == user.secretAnswer
        retrievedUser.secretQuestion == user.secretQuestion
        retrievedUser.secretQuestionId == user.secretQuestionId
    }

    def "updateUserEncryption with no salt or encryptionversion updates user encryption"() {
        given:
        def rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")

        when:
        ldapUserRepository.addUser(user);
        ldapUserRepository.updateUserEncryption(rsId);
        User retrievedUser = ldapUserRepository.getUserById(rsId)

        then:
        retrievedUser.displayName == user.displayName
        retrievedUser.email == user.email
        retrievedUser.apiKey == user.apiKey
        retrievedUser.secretAnswer == user.secretAnswer
        retrievedUser.secretQuestion == user.secretQuestion
        retrievedUser.secretQuestionId == user.secretQuestionId
    }

    def "calling getUsersToReEncrypt gets the users that need to be re-encrypted"() {
        when:
        def users = ldapUserRepository.getUsersToReEncrypt(0, 50)

        then:
        users != null
        users.valueList != null
    }

    def "modified and created timestamps should not be considered by the persister" () {
        given:
        def rsId = "testTimeStamps$random"
        def username = "update$username"
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")

        when:
        ldapUserRepository.addUser(user)
        def created = new Date().minus(1)
        user.created = created
        def updated = new Date().plus(1)
        user.updated = updated
        def email = "someOtherEmail@rackspace.com"
        user.email = email
        ldapUserRepository.updateUser(user)
        User getUser = ldapUserRepository.getUserByUsername(username)

        then:
        getUser.created != created
        getUser.updated != updated
        getUser.email == email
        notThrown(LDAPException)

        cleanup:
        ldapUserRepository.deleteUser(user)
    }

    def "Password is not populated on get user call"() {
        given:
        def rsId = "testPassword$random"
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")
        ldapUserRepository.addUser(user)

        when:
        User getUser = ldapUserRepository.getUserByUsername(username)

        then:
        getUser.password == null

        cleanup:
        ldapUserRepository.deleteUser(user)
    }

    def createUser(String id, String username, String domainId, String email, boolean enabled, String region, String password) {
        new User().with {
            it.apiKey = "key"
            it.displayName = "displayName"
            it.mossoId = 0
            it.nastId = "nastId"
            it.secretAnswer = "answer"
            it.secretQuestion = "question"
            it.secretQuestionId = "id"

            it.id = id
            it.username = username
            it.domainId = domainId
            it.email = email
            it.enabled = enabled
            it.region = region
            it.password = password
            it.userPassword = password
            return it
        }
    }
}
