package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Users
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
class LdapUserRepositoryGroovyIntegrationTest extends Specification{
    @Autowired
    LdapUserRepository ldapUserRepository;

    @Shared def random
    @Shared def username

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random
    }

    def "GET - user w/ a list of filters" (){
        given:
        String rsId = "99999999"
        User user = createUser(rsId, username,"999999","someEmail@rackspace.com", true, "ORD", "password")
        List<Filter> filterList = new ArrayList<Filter>();
        filterList.add(Filter.createEqualityFilter("rsId", rsId))


        when:
        ldapUserRepository.addUser(user)
        Users users = ldapUserRepository.getUsers(filterList)
        ldapUserRepository.deleteUser(username)

        then:
        users.getUsers().size() == 1
    }

    def "GET - user w/ a list of filters - null" () {
        when:
        Users users = ldapUserRepository.getUsers(null)
        then:
        users.getUsers() == null
    }

    def "GET - user w/ a list of filters - list is zero" () {
        when:
        Users users = ldapUserRepository.getUsers(new ArrayList<Filter>())
        then:
        users.getUsers() == null
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
        ldapUserRepository.updateUserEncryption(rsId, "0");
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
