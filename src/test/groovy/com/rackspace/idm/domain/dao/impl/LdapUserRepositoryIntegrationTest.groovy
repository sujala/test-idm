package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Users
import com.rackspace.idm.domain.service.EncryptionService
import com.rackspace.idm.exception.StalePasswordException
import com.unboundid.ldap.sdk.Filter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/22/13
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapUserRepositoryIntegrationTest extends Specification{
    @Autowired
    LdapUserRepository ldapUserRepository;

    @Autowired
    EncryptionService encryptionService;

    @Shared def random
    @Shared def username

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random
    }

    def "user crud"() {
        given:
        String rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")

        when:
        ldapUserRepository.addUser(user)
        def retrivedUser = ldapUserRepository.getUserByUsername(username)
        ldapUserRepository.deleteUser(retrivedUser);
        def deletedUser = ldapUserRepository.getUserByUsername(username)

        then:
        retrivedUser != null
        deletedUser == null
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
        def userList = ldapUserRepository.getUsersByDomain(domain1)
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
        def userList = ldapUserRepository.getUsersByDomainAndEnabledFlag(domain1, true)
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
        def userList = ldapUserRepository.getUsersByDomainAndEnabledFlag(domain1, false)
        ldapUserRepository.deleteUser(username1)
        ldapUserRepository.deleteUser(username2)
        ldapUserRepository.deleteUser(username3)

        then:
        userList != null
        userList.size() == 1
        userList.get(0).username == username2
    }

    def "calling getUserByEmail returns the user"() {
        given:
        def email = "email$random@email.com"
        def user1 = createUser("1$random", "username$random", "1234567890", email, true, "DFW", "Password1")

        when:
        ldapUserRepository.addUser(user1)
        def user = ldapUserRepository.getUsersByEmail(email)
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
        retrievedUser.firstname == user.firstname
        retrievedUser.email == user.email
        retrievedUser.apiKey == user.apiKey
        retrievedUser.secretAnswer == user.secretAnswer
        retrievedUser.secretQuestion == user.secretQuestion
        retrievedUser.secretQuestionId == user.secretQuestionId
        retrievedUser.lastname == user.lastname
        retrievedUser.passwordObj.value == user.passwordObj.value
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
        retrievedUser.firstname == user.firstname
        retrievedUser.email == user.email
        retrievedUser.apiKey == user.apiKey
        retrievedUser.secretAnswer == user.secretAnswer
        retrievedUser.secretQuestion == user.secretQuestion
        retrievedUser.secretQuestionId == user.secretQuestionId
        retrievedUser.lastname == user.lastname
        retrievedUser.passwordObj.value == user.passwordObj.value
    }

    def "calling getUsersToReEncrypt gets the users that need to be re-encrypted"() {
        when:
        def users = ldapUserRepository.getUsersToReEncrypt(0, 50)

        then:
        users != null
        users.valueList != null
    }

    def "updateUserPassword thows a StalePasswordException"() {
        given:
        def rsId = random
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")
        User updateUser = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")

        when:
        ldapUserRepository.addUser(user);
        ldapUserRepository.updateUser(updateUser, user, false)

        then:
        thrown(StalePasswordException.class)
        ldapUserRepository.deleteUser(user)
    }


    def createUser(String id, String username, String domainId, String email, boolean enabled, String region, String password) {
        new User().with {
            it.id = id
            it.username = username
            it.domainId = domainId
            it.email = email
            it.enabled = enabled
            it.region = region
            it.password = password
            return it
        }
    }
}
