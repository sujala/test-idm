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
