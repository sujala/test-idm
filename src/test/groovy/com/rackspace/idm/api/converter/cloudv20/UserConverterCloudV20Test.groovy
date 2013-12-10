package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.domain.config.ExternalBeansConfiguration
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.RoleList
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
    @Shared RoleConverterCloudV20 mockRoleConverterCloudV20
    @Shared SecretQAConverterCloudV20 mockSecretQAConverterCloudV20
    @Shared GroupConverterCloudV20 mockGroupConverterCloudV20

    def setupSpec() {
        ExternalBeansConfiguration config = new ExternalBeansConfiguration()
        def mapper = config.getMapper()

        converterCloudV20 = new UserConverterCloudV20().with {
            it.mapper = mapper
            return it
        }
    }

    def setup() {
        mockRoleConverterCloudV20()
        mockSecretQAConverterCloudV20()
        mockGroupConverterCloudV20()
    }

    def mockRoleConverterCloudV20() {
        mockRoleConverterCloudV20 = Mock()
        converterCloudV20.roleConverterCloudV20 = mockRoleConverterCloudV20
    }

    def mockSecretQAConverterCloudV20() {
        mockSecretQAConverterCloudV20 = Mock()
        converterCloudV20.secretQAConverterCloudV20 = mockSecretQAConverterCloudV20
    }

    def mockGroupConverterCloudV20() {
        mockGroupConverterCloudV20 = Mock()
        converterCloudV20.groupConverterCloudV20 = mockGroupConverterCloudV20
    }

    def "convert user from domain entity to jaxb object"() {
        given:
        User user = user(false)
        def roles = new RoleList()
        def groups = new Groups()
        def secretQA = new SecretQA()

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user)

        then:
        1 * mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        1 * mockRoleConverterCloudV20.toRoleListJaxb(user.roles) >>  roles
        1 * mockGroupConverterCloudV20.toGroupListJaxb(user.rsGroupId) >> groups

        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA == secretQA
        jaxbUser.roles == roles
        jaxbUser.groups == groups
        jaxbUser.password == user.password
    }

    def "convert user from entity to UserForCreate jaxb object"() {
        given:
        User user = user(false)

        when:
        org.openstack.docs.identity.api.v2.User jaxbObject = converterCloudV20.toUserForCreate(user)

        then:
        user.username == jaxbObject.username
        user.displayName == jaxbObject.displayName
        user.email == jaxbObject.email
        user.enabled == jaxbObject.enabled
        user.region == jaxbObject.defaultRegion
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

    def "convert user from jaxb to domain entity"() {
        given:
        org.openstack.docs.identity.api.v2.User userJaxb = userJaxb(false)
        def tenantRoles = [ new TenantRole() ].asList()
        def rsGroupsIds = new HashSet<String>()

        when:
        User user = converterCloudV20.fromUser(userJaxb)

        then:
        1 * mockRoleConverterCloudV20.toTenantRoles(_) >> tenantRoles
        1 * mockGroupConverterCloudV20.toSetOfGroupIds(_) >> rsGroupsIds

        user.username == userJaxb.username
        user.displayName == userJaxb.displayName
        user.email == userJaxb.email
        user.enabled == userJaxb.enabled
        user.region == userJaxb.defaultRegion
        user.secretQuestion == userJaxb.getSecretQA().question
        user.secretAnswer == userJaxb.getSecretQA().answer
        user.getRsGroupId() == rsGroupsIds
        user.getRoles() == tenantRoles
        user.userPassword == userJaxb.password
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

    def userJaxb() {
        userJaxb(true)
    }

    def userJaxb(Boolean enabled) {
        new org.openstack.docs.identity.api.v2.User().with {
            it.id = "id"
            it.username = "username"
            it.email = "email@email.com"
            it.displayName = "display"
            it.enabled = enabled
            it.defaultRegion = "region"
            it.created = createdXML()
            it.secretQA = secretQA()
            it.roles = new RoleList()
            it.groups = new Groups()
            it.password = "password"
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
            it.roles = [ new TenantRole() ].asList()
            it.rsGroupId = new HashSet<String> ()
            it.secretQuestion = "question"
            it.secretAnswer = "answer"
            it.password = "password"
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

    def secretQA() {
        new SecretQA().with {
            it.question = "question"
            it.answer = "answer"
            return it
        }
    }
}
