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
class LdapUserRepositoryIntegrationTest extends Specification{
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

    def "retrieving users in a domain returns all users within the domain"() {
        given:
        def user1 = createUser("1$random", "enabledUse", "1234567890", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", "disabledUse", "1234567890", "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", "use", "0123456789", "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain("1234567890")
        ldapUserRepository.deleteUser("enabledUse")
        ldapUserRepository.deleteUser("disabledUse")
        ldapUserRepository.deleteUser("use")

        then:
        userList != null
        userList.size() == 2
    }

    def "retrieving enabled users in a domain returns enabled users within the domain"() {
        given:
        def user1 = createUser("1$random", "enabledUse", "1234567890", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", "disabledUse", "1234567890", "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", "use", "0123456789", "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain("1234567890", true)
        ldapUserRepository.deleteUser("enabledUse")
        ldapUserRepository.deleteUser("disabledUse")
        ldapUserRepository.deleteUser("use")

        then:
        userList != null
        userList.size() == 1
        userList.get(0).username.equals("enabledUse")
    }


    def "retrieving disabled users in a domain returns disabled users within the domain"() {
        given:
        def user1 = createUser("1$random", "enabledUse", "1234567890", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", "disabledUse", "1234567890", "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", "use", "0123456789", "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain("1234567890", false)
        ldapUserRepository.deleteUser("enabledUse")
        ldapUserRepository.deleteUser("disabledUse")
        ldapUserRepository.deleteUser("use")

        then:
        userList != null
        userList.size() == 1
        userList.get(0).username.equals("disabledUse")
    }

    def "retrieving users in a domain with null domainId filter returns no users"() {
        def user1 = createUser("1$random", "enabledUse", "1234567890", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", "disabledUse", "1234567890", "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", "use", "0123456789", "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain(null, false)
        ldapUserRepository.deleteUser("enabledUse")
        ldapUserRepository.deleteUser("disabledUse")
        ldapUserRepository.deleteUser("use")

        then:
        userList != null
        userList.size() == 0
    }

    def "retrieving users in a domain with null domainId filter and no domainId filter returns no users"() {
        def user1 = createUser("1$random", "enabledUse", "1234567890", "email@email.com", true, "DFW", "Password1")
        def user2 = createUser("2$random", "disabledUse", "1234567890", "email@email.com", false, "DFW", "Password1")
        def user3 = createUser("3$random", "use", "0123456789", "email@email.com", true, "DFW", "Password1")

        ldapUserRepository.addUser(user1)
        ldapUserRepository.addUser(user2)
        ldapUserRepository.addUser(user3)

        when:
        def userList = ldapUserRepository.getUsersByDomain(null)
        ldapUserRepository.deleteUser("enabledUse")
        ldapUserRepository.deleteUser("disabledUse")
        ldapUserRepository.deleteUser("use")

        then:
        userList != null
        userList.size() == 0
    }

    def "calling getUserByEmail returns the user"() {
        given:
        def email = "email$random@email.com"
        def user1 = createUser("1$random", "username$random", "1234567890", email, true, "DFW", "Password1")

        when:
        ldapUserRepository.addUser(user1)
        def user = ldapUserRepository.getUsersByEmail(email)
        ldapUserRepository.deleteUser(user.users.get(0))

        then:
        user != null
        user1.email == user.users.get(0).email
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
