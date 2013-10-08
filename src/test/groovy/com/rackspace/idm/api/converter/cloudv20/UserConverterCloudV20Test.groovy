package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import org.dozer.DozerBeanMapper
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.datatype.DatatypeFactory

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/20/13
 * Time: 11:26 AM
 * To change this template use File | Settings | File Templates.
 */
class UserConverterCloudV20Test extends Specification {
    @Shared UserConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new UserConverterCloudV20().with {
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert user from ldap to jersey object"() {
        given:
        User user = user(false)

        when:
        org.openstack.docs.identity.api.v2.User userEntity = converterCloudV20.toUser(user)

        then:
        user.username == userEntity.username
        user.displayName == userEntity.displayName
        user.email == userEntity.email
        user.enabled == userEntity.enabled
        user.region == userEntity.defaultRegion
    }

    def "convert user from ldap to UserForCreate jersey object"() {
        given:
        User user = user(false)

        when:
        org.openstack.docs.identity.api.v2.User userEntity = converterCloudV20.toUserForCreate(user)

        then:
        user.username == userEntity.username
        user.displayName == userEntity.displayName
        user.email == userEntity.email
        user.enabled == userEntity.enabled
        user.region == userEntity.defaultRegion
    }

    def "convert user from ldap to UserForAuthenticateResponse jersey object"() {
        given:
        User user = user(false)

        when:
        org.openstack.docs.identity.api.v2.UserForAuthenticateResponse userEntity = converterCloudV20.toUserForAuthenticateResponse(user, null)

        then:
        user.id == userEntity.id
        user.username == userEntity.name
    }

    def "convert user from ldap to UserForAuthenticateResponse (racker) jersey object"() {
        given:
        Racker racker = racker()

        when:
        org.openstack.docs.identity.api.v2.UserForAuthenticateResponse userEntity = converterCloudV20.toRackerForAuthenticateResponse(racker, null)

        then:
        racker.username == userEntity.name
        racker.rackerId == userEntity.id
    }

    def "convert user from jersey object to ldap"() {
        given:
        org.openstack.docs.identity.api.v2.User userEntity = userEntity(false)

        when:
        User user = converterCloudV20.fromUser(userEntity)

        then:
        user.username == userEntity.username
        user.displayName == userEntity.displayName
        user.email == userEntity.email
        user.enabled == userEntity.enabled
        user.region == userEntity.defaultRegion
    }

    def "convert users to jersey object" () {
        given:
        User user = user()
        List<User> users = new ArrayList<User>()
        users.add(user)

        when:
        org.openstack.docs.identity.api.v2.UserList userList = converterCloudV20.toUserList(users)

        then:
        userList.getUser().size() == users.size()
        org.openstack.docs.identity.api.v2.User userEntity = userList.getUser().get(0)
        user.username == userEntity.username
        user.displayName == userEntity.displayName
        user.email == userEntity.email
        user.enabled == userEntity.enabled
        user.region == userEntity.defaultRegion
    }


    def user() {
        user(true)
    }

    def userEntity() {
        userEntity(true)
    }

    def userEntity(Boolean enabled) {
        new org.openstack.docs.identity.api.v2.User().with {
            it.id = "id"
            it.username = "username"
            it.email = "email@email.com"
            it.displayName = "display"
            it.enabled = enabled
            it.defaultRegion = "region"
            it.created = createdXML()
            return it
        }
    }

    def user(Boolean enabled) {
        new User().with {
            it.id = "id"
            it.username = "username"
            it.email = "email@email.com"
            it.displayName = "display"
            it.enabled = enabled
            it.region = "region"
            it.created = created()
            return it
        }
    }

    def created() {
        new Date(2013,1,1)
    }

    def createdXML() {
        GregorianCalendar gc = new GregorianCalendar()
        DateTime created = new DateTime(created())
        gc.setTime(created.toDate())
        DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
    }

    def racker() {
        new Racker().with {
            it.username = "racker"
            it.rackerId = "id"
            return it
        }
    }
}
