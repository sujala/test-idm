package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.ExternalBeansConfiguration
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfig.ReloadableConfig
import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.ProvisionedUserDelegate
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.DomainSubUserDefaults
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.datatype.DatatypeFactory


class UserConverterCloudV20Test extends Specification {
    @Shared UserConverterCloudV20 converterCloudV20
    @Shared RoleConverterCloudV20 mockRoleConverterCloudV20
    @Shared SecretQAConverterCloudV20 mockSecretQAConverterCloudV20
    @Shared GroupConverterCloudV20 mockGroupConverterCloudV20
    @Shared AuthorizationService authorizationService
    @Shared Configuration mockConfig;
    @Shared Configuration mockReloadableConfig
    @Shared Configuration mockStaticConfig
    @Shared ReloadableConfig reloadableConfig
    @Shared BasicMultiFactorService basicMultiFactorService

    def setupSpec() {
        ExternalBeansConfiguration config = new ExternalBeansConfiguration()
        def mapper = config.getMapper()
        mockReloadableConfig = Mock()
        mockStaticConfig = Mock()
        reloadableConfig = Mock()
        converterCloudV20 = new UserConverterCloudV20().with {
            it.mapper = mapper
            it.authorizationService = Mock(AuthorizationService)
            it.identityConfig = new IdentityConfig(mockStaticConfig, mockReloadableConfig)
            it.identityConfig.reloadableConfig = reloadableConfig
            return it
        }
        reloadableConfig.getDomainDefaultSessionInactivityTimeout() >> IdentityConfig.DOMAIN_DEFAULT_SESSION_INACTIVITY_TIMEOUT_DEFAULT
    }

    def setup() {
        mockRoleConverterCloudV20()
        mockSecretQAConverterCloudV20()
        mockGroupConverterCloudV20()

        basicMultiFactorService = Mock(BasicMultiFactorService)
        converterCloudV20.basicMultiFactorService = basicMultiFactorService
        converterCloudV20.authenticationContext = Mock(AuthenticationContext)
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

    def mockConfig() {
        mockConfig = Mock()
        converterCloudV20.config = mockConfig
    }

    def "convert user from domain entity to jaxb object - roles and groups populated"() {
        given:
        User user = user(false)
        user.getRsGroupId().add("id")
        def role = new Role()
        role.id = "id"
        def roles = new RoleList()
        roles.role.add(role)
        def group = new Group()
        group.id = "id"
        def groups = new Groups()
        groups.group.add(group)
        def secretQA = new SecretQA()

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, true)

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
        jaxbUser.multiFactorEnabled == user.multifactorEnabled
    }

    def "convert user from domain entity to jaxb object - user roles and groups null"() {
        given:
        User user = userWithNoRolesOrGroup(false)
        def secretQA = new SecretQA()

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, true)

        then:
        mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA == secretQA
        jaxbUser.roles == null
        jaxbUser.groups == null
        jaxbUser.password == user.password
    }

    def "convert user from domain entity to jaxb object without secret QA (user had secret QA)"() {
        given:
        User user = userWithNoRolesOrGroup(false)
        def secretQA = new SecretQA()

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, false)

        then:
        mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA == null
        jaxbUser.roles == null
        jaxbUser.groups == null
        jaxbUser.password == user.password
    }

    def "convert user from domain entity to jaxb object without secret QA (user had secret QA, roles, and groups)"() {
        given:
        User user = userWithNoRolesOrGroup(false)
        def secretQA = new SecretQA()
        def roles = new RoleList()
        def groups = new Groups()


        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, false)

        then:
        mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        mockGroupConverterCloudV20.toGroupListJaxb(user.getRsGroupId()) >> groups
        mockRoleConverterCloudV20.toRoleListJaxb(user.getRoles()) >> roles

        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA == null
        jaxbUser.roles == null
        jaxbUser.groups == null
        jaxbUser.password == user.password
    }

    def "convert user from domain entity to jaxb object with secret QA"() {
        given:
        User user = userWithNoRolesOrGroup(false)
        user.roles = new ArrayList<TenantRole>()
        user.roles.add(new TenantRole())
        user.rsGroupId = new ArrayList<String>()
        user.rsGroupId.add("0")

        def secretQA = new SecretQA()
        def role = new Role()
        def roles = new RoleList().with {
            it.role.add(role)
            it
        }
        def group = new Group()
        def groups = new Groups().with {
            it.group.add(group)
            it
        }

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, true)

        then:
        mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        mockGroupConverterCloudV20.toGroupListJaxb(user.getRsGroupId()) >> groups
        mockRoleConverterCloudV20.toRoleListJaxb(user.getRoles()) >> roles

        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA != null
        jaxbUser.roles != null
        jaxbUser.groups != null
        jaxbUser.password == user.password
    }

    def "convert user from domain entity to jaxb object - user roles and groups empty"() {
        given:
        User user = userWithNoRolesOrGroup(false)
        user.setRoles(Collections.EMPTY_LIST)
        user.setRsGroupId(new HashSet<String>())
        def secretQA = new SecretQA()

        when:
        org.openstack.docs.identity.api.v2.User jaxbUser = converterCloudV20.toUser(user, true)

        then:
        mockSecretQAConverterCloudV20.toSecretQA(user.secretQuestion, user.secretAnswer) >> secretQA
        jaxbUser.username == user.username
        jaxbUser.displayName == user.displayName
        jaxbUser.email == user.email
        jaxbUser.enabled == user.enabled
        jaxbUser.defaultRegion == user.region
        jaxbUser.secretQA == secretQA
        jaxbUser.roles == null
        jaxbUser.groups == null
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
        mockConfig.getBoolean("createUser.fullPayload.enabled") >> true

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

    def "convert delegate user to jersey object"() {
        given:
        def provisionedUserDelegate = provisionedUserDelegate(true)

        when:
        org.openstack.docs.identity.api.v2.User user = converterCloudV20.toUser(provisionedUserDelegate)

        then:
        provisionedUserDelegate.domainId == user.domainId
        provisionedUserDelegate.region == user.defaultRegion
        provisionedUserDelegate.delegationAgreement.id == user.delegationAgreementId
    }

    def "convert delegate user to jersey object illegal user"() {
        given:
        def provisionedUserDelegate = provisionedUserDelegate(true).with {
            it.originalEndUser = new ProvisionedUserDelegate(null, null, null)
            it
        }

        when:
        converterCloudV20.toUser(provisionedUserDelegate)

        then:
        thrown(IllegalArgumentException)
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
            it.multifactorEnabled = true
            it.roles = [ new TenantRole() ].asList()
            it.rsGroupId = new HashSet<String> ()
            it.secretQuestion = "question"
            it.secretAnswer = "answer"
            it.password = "password"
            return it
        }
    }

    def provisionedUserDelegate(Boolean enabled) {
        def user = new User().with {
            it.id = "id"
            it.username = "username"
            it.email = "email@email.com"
            it.displayName = "display"
            it.enabled = enabled
            it.region = "region"
            it.created = created()
            it.multifactorEnabled = true
            it.roles = [ new TenantRole() ].asList()
            it.rsGroupId = new HashSet<String> ()
            it.secretQuestion = "question"
            it.secretAnswer = "answer"
            it.password = "password"
            return it
        }

        def delegationAgreement = new DelegationAgreement().with {
            it.id = "id"
            return it
        }

        def domainSubUserDefaults = Mock(DomainSubUserDefaults)

        new ProvisionedUserDelegate(domainSubUserDefaults, delegationAgreement, user)
    }

    def userWithNoRolesOrGroup(Boolean enabled) {
        new User().with {
            it.id = "id"
            it.username = "username"
            it.email = "email@email.com"
            it.displayName = "display"
            it.enabled = enabled
            it.region = "region"
            it.created = created()
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
