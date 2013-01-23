package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.api.resource.cloud.CloudClient
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Users
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.sun.grizzly.http.servlet.HttpServletRequestImpl
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/22/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
class DelegateCloud11ServiceGroovyTest extends Specification{
    @Shared DelegateCloud11Service delegateCloud11Service
    @Shared Configuration config
    @Shared DefaultUserService defaultUserService
    @Shared DefaultCloud11Service defaultCloud11Service
    @Shared CloudClient cloudClient

    def setupSpec(){
        delegateCloud11Service = new DelegateCloud11Service()
    }

    def setup(){
        createMocks()
    }

    def "GET - user by mossoId - returns a user" () {
        given:
        config.getBoolean("useCloudAuth") >> true
        defaultUserService.getUsersByTenantId(_) >> createUsers([createUser("1","someName",123,"1234")].asList())

        when:
        delegateCloud11Service.getUserFromMossoId(null,123,null)

        then:
        1 * delegateCloud11Service.defaultCloud11Service.getUserFromMossoId(_,_,_)
    }

    def "GET - user by mossoId - routing false" () {
        given:
        config.getBoolean("useCloudAuth") >> false
        defaultUserService.getUsersByTenantId(_) >> createUsers([createUser("1","someName",123,"1234")].asList())

        when:
        delegateCloud11Service.getUserFromMossoId(null,123,null)

        then:
        1 * delegateCloud11Service.defaultCloud11Service.getUserFromMossoId(_,_,_)
    }

    def "GET - user by mossoId - returns a user from cloud auth" () {
        given:
        config.getBoolean("useCloudAuth") >> true
        config.getString("cloudAuth11url") >> "someUrl"
        defaultUserService.getUsersByTenantId(_) >> new Users()

        when:
        delegateCloud11Service.getUserFromMossoId(null,123,null)

        then:
        1 * delegateCloud11Service.cloudClient.get(_,_)
    }

    def "GET - user by nastId - returns a user" () {
        given:
        config.getBoolean("useCloudAuth") >> true
        defaultUserService.getUsersByTenantId(_) >> createUsers([createUser("1","someName",123,"1234")].asList())

        when:
        delegateCloud11Service.getUserFromNastId(null,"123",null)

        then:
        1 * delegateCloud11Service.defaultCloud11Service.getUserFromNastId(_,_,_)
    }

    def "GET - user by nastId - routing false" () {
        given:
        config.getBoolean("useCloudAuth") >> false
        defaultUserService.getUsersByTenantId(_) >> createUsers([createUser("1","someName",123,"1234")].asList())

        when:
        delegateCloud11Service.getUserFromNastId(null,"123",null)

        then:
        1 * delegateCloud11Service.defaultCloud11Service.getUserFromNastId(_,_,_)
    }

    def "GET - user by nastId - returns a user from cloud auth" () {
        given:
        config.getBoolean("useCloudAuth") >> true
        config.getString("cloudAuth11url") >> "someUrl"
        defaultUserService.getUsersByTenantId(_) >> new Users()

        when:
        delegateCloud11Service.getUserFromNastId(null,"123",null)

        then:
        1 * delegateCloud11Service.cloudClient.get(_,_)
    }

    def createUsers(List<User> users) {
        new Users().with {
            it.users = new ArrayList<User>()
            for(User user : users){
               it.users.add(user)
            }
            return it
        }
    }

    def createUser(String id, String username, int mossoId, String nastId){
        new User().with {
            it.id = id
            it.username = username
            it.mossoId = mossoId
            it.nastId = nastId
            return it
        }
    }

    def createMocks(){
        config = Mock()
        defaultUserService = Mock()
        defaultCloud11Service = Mock()
        cloudClient = Mock()

        delegateCloud11Service.config = config
        delegateCloud11Service.defaultUserService = defaultUserService
        delegateCloud11Service.defaultCloud11Service = defaultCloud11Service
        delegateCloud11Service.cloudClient = cloudClient
    }
}
