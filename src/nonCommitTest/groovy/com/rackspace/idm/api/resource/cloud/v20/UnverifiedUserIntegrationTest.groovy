package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Invite
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria
import com.rackspace.idm.validation.Validator20
import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.apache.http.HttpStatus
import org.codehaus.groovy.runtime.InvokerHelper
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType
import javax.xml.datatype.DatatypeFactory

import static org.apache.http.HttpStatus.*

class UnverifiedUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    UserDao userDao

    @Unroll
    def "The feature flag 'feature.enable.create.invites' must be enabled in order for unverified users to be created: featureEnabled == #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, featureEnabled)
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when:
        def response = cloud20.createUnverifiedUser(utils.getToken(userAdmin.username), user)

        then:
        if (featureEnabled) {
            assert response.status == HttpStatus.SC_CREATED
        } else {
            IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, DefaultCloud20Service.ERROR_CREATION_OF_INVITE_USERS_DISABLED)
        }

        cleanup:
        reloadableConfiguration.reset()

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "a domain must belong to an RCN authorized to create unverified users: domainRcnAuthorized = #domainRcnAuthorized"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        if (domainRcnAuthorized) {
            utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        } else {
            utils.domainRcnSwitch(userAdmin.domainId, testUtils.getRandomRCN())
        }

        when:
        def response = cloud20.createUnverifiedUser(utils.getToken(userAdmin.username), user)

        then:
        if (domainRcnAuthorized) {
            assert response.status == HttpStatus.SC_CREATED
        } else {
            IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, DefaultCloud20Service.ERROR_DOMAIN_NOT_IN_AUTHORIZED_RCN_FOR_INVITE_USERS)
        }

        where:
        domainRcnAuthorized << [true, false]
    }

    def "only users with the Identity user type of default user are not allowed to create invite users"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def userManage = utils.createUser(utils.getToken(userAdmin.username))
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username))
        def tokensAllowed = [utils.getServiceAdminToken(), utils.getIdentityAdminToken(), utils.getToken(userAdmin.username), utils.getToken(userManage.username)]
        def tokensNotAllowed = [utils.getToken(defaultUser.username)]

        when:
        def allowedResponses = []
        def notAllowedResponses = []
        for (allowedToken in tokensAllowed) {
            user.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            allowedResponses << cloud20.createUnverifiedUser(allowedToken, user)
        }
        for (notAllowedToken in tokensNotAllowed) {
            notAllowedResponses << cloud20.createUnverifiedUser(notAllowedToken, user)
        }

        then:
        for (allowedResponse in allowedResponses) {
            assert allowedResponse.status == 201
        }
        for (notAllowedResponse in notAllowedResponses) {
            assert notAllowedResponse.status == 403
        }
    }

    @Unroll
    def "user admins and user managers are only allowed to create unverified users in their domain: sameDomain = #sameDomain"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def user = new User()
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def userManage = utils.createUser(utils.getToken(userAdmin.username))
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def reqUsers = [userAdmin, userManage]
        def otherUserAdmin = utils.createCloudAccount()

        when:
        def responses = []
        for (reqUser in reqUsers) {
            if (sameDomain) {
                user.domainId = reqUser.domainId
            } else {
                user.domainId = otherUserAdmin.domainId
            }
            user.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            responses << cloud20.createUnverifiedUser(utils.getToken(reqUser.username), user)
        }

        then:
        for (response in responses) {
            if (sameDomain) {
                assert response.status == 201
            } else {
                IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, DefaultCloud20Service.ERROR_DOMAIN_USERS_RESTRICTED_TO_SAME_DOMAIN_FOR_INVITE_USERS)
            }
        }

        where:
        sameDomain << [true, false]
    }

    def "unverified users are required to have an email address"() {
        given :
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, DefaultCloud20Service.ERROR_UNVERIFIED_USERS_REQUIRE_EMAIL_ADDRESS)
    }

    def "unverified users are required to have an email address that meets the email address pattern"() {
        given :
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.domainId = userAdmin.domainId
            it.email = RandomStringUtils.randomAlphabetic(8)
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, DefaultCloud20Service.ERROR_UNVERIFIED_USERS_REQUIRED_VALID_EMAIL_ADDRESS)
    }

    def "if the domain is not specified for an unverified user the domain defaults to the callers domain"() {
        given :
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when:
        def response = cloud20.createUnverifiedUser(utils.getToken(userAdmin.username), user)

        then:
        response.status == 201
        User unverifiedUser = response.getEntity(User).value
        unverifiedUser.domainId == userAdmin.domainId
    }

    def "the domain for an unverified user must exist"() {
        given :
        def user = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultCloud20Service.ERROR_DOMAIN_MUST_EXIST_FOR_UNVERIFIED_USERS)
    }

    def "unverified users require an enabled domain"() {
        given :
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        utils.disableDomain(userAdmin.domainId)

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, DefaultCloud20Service.ERROR_DOMAIN_MUST_BE_ENABLED_FOR_UNVERIFIED_USERS)
    }

    def "unverified users must have a unique email address within their domain"() {
        given :
        def userAdmin = utils.createCloudAccount()
        def email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when: "try to create the user w/ a provisioned user in the same domain with the same email address"
        def otherUser = utils.createUser(utils.getToken(userAdmin.username))
        otherUser.email = email
        utils.updateUser(otherUser)
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, ErrorCodes.ERROR_CODE_INVALID_VALUE, DefaultCloud20Service.ERROR_UNVERIFIED_USERS_MUST_HAVE_UNIQUE_EMAIL_WITHIN_DOMAIN)

        when: "delete the provisioned user and try again"
        utils.deleteUser(otherUser)
        response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        response.status == 201

        when: "create an unverified user with the same email address as another unverified user"
        email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
        user.email = email
        def unverifiedUser = utils.createUnverifiedUser(userAdmin.domainId, email)
        response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, ErrorCodes.ERROR_CODE_INVALID_VALUE, DefaultCloud20Service.ERROR_UNVERIFIED_USERS_MUST_HAVE_UNIQUE_EMAIL_WITHIN_DOMAIN)

        when: "delete the unverified user and try again"
        utils.deleteUser(unverifiedUser)
        response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        response.status == 201
    }

    def "the creation of unverified users cannot exceed the max number of users in a domain"() {
        given:
        def maxNumUsers = 2
        staticIdmConfiguration.setProperty(IdentityConfig.MAX_NUM_USERS_IN_DOMAIN, maxNumUsers)
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        utils.createUser(utils.getToken(userAdmin.username))

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, String.format(DefaultUserService.ERROR_PATTERN_MAX_NUM_USERS_IN_DOMAIN, maxNumUsers))

        cleanup:
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "unverified users are created disabled and have defaults set based on user admin: contentType = #contentType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def group = utils.createGroup()
        utils.addUserToGroup(group, userAdmin)
        def propRole = utils.createPropagatingRole()
        utils.addRoleToUser(userAdmin, propRole.id)
        def userReq = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), userReq, contentType, contentType)

        then: "the user was created and is disabled and marked as unverified"
        assert response.status == 201
        def unverifiedUser;
        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            unverifiedUser = response.getEntity(User)
        } else {
            unverifiedUser = response.getEntity(User).value
        }
        !unverifiedUser.enabled
        unverifiedUser.unverified

        and: "has the correct default region"
        unverifiedUser.defaultRegion == userAdmin.defaultRegion

        and: "has the same groups as the user admin"
        def unverifiedUserGroups = utils.listGroupsForUser(unverifiedUser).group
        def userAdminGroups = utils.listGroupsForUser(userAdmin).group
        userAdminGroups.size() > 0
        userAdminGroups.size() == unverifiedUserGroups.size()
        userAdminGroups.id.intersect(unverifiedUserGroups.id).size() == userAdminGroups.size()

        and: "has the identity:default role"
        def roles = utils.listUserGlobalRoles(utils.getServiceAdminToken(), unverifiedUser.id)
        roles.role.id.find { it -> it == Constants.DEFAULT_USER_ROLE_ID} != null

        and: "has the prop role"
        roles.role.id.find { it -> it == propRole.id} != null

        and: "the user does not have a username"
        unverifiedUser.username == null

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "creation of unverified user ignores all attributes other than the email and domain"() {
        given:
        def userAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def userReq = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        userReq.metaPropertyValues.each { metaProp ->
            if (!["domainId", "email"].contains(metaProp.name) && userReq.hasProperty(metaProp.name)) {
                if (metaProp.type == String) {
                    userReq[metaProp.name] = RandomStringUtils.randomAlphabetic(8)
                } else if (metaProp.type == Boolean) {
                    userReq[metaProp.name] = RandomUtils.nextInt(0, 2) == 0
                } else if (metaProp.type == Integer) {
                    userReq[metaProp.name] = RandomUtils.nextInt(0, 20)
                }
            }
        }
        def expectedUnverifiedUserProperties = ['id', 'email', 'enabled', 'RAX-AUTH:defaultRegion', 'RAX-AUTH:domainId', 'RAX-AUTH:unverified']

        when:
        def response = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), userReq, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)

        then:
        response.status == 201
        def createdUser = new JsonSlurper().parseText(response.getEntity(String))
        createdUser['user'].each { prop ->
            assert expectedUnverifiedUserProperties.contains(prop.key)
        }
    }

    @Unroll
    def "send invite for unverified users: accept = #accept"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.UNVERIFIED_USER_REGISTRATION_CODE_SIZE_PROP, 32)
        reloadableConfiguration.setProperty(IdentityConfig.UNVERIFIED_USER_INVITES_TTL_HOURS_PROP, 48)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def userManager = utils.createUser(userAdminToken)
        def userManagerToken = utils.getToken(userManager.username)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        when: "using user admin token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id, accept)
        def inviteEntity = getInviteEntity(response)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        inviteEntity.userId == unverifiedUserEntity.id
        inviteEntity.email == unverifiedUserEntity.email
        inviteEntity.registrationCode != null
        inviteEntity.registrationCode.size() == 32
        inviteEntity.created != null
        inviteEntity.expires != null
        inviteEntity.created.toGregorianCalendar().getTime().equals(inviteEntity.expires.toGregorianCalendar().getTime().minus(2))

        and: "email sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        when: "using user manager token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userManagerToken, unverifiedUserEntity.id, accept)
        inviteEntity = getInviteEntity(response)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        inviteEntity.userId == unverifiedUserEntity.id
        inviteEntity.email == unverifiedUserEntity.email
        inviteEntity.registrationCode != null
        inviteEntity.registrationCode.size() == 32
        inviteEntity.created != null
        inviteEntity.expires != null
        inviteEntity.created.toGregorianCalendar().getTime().equals(inviteEntity.expires.toGregorianCalendar().getTime().minus(2))

        and: "email sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        when: "using identity admin token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(utils.getIdentityAdminToken(), unverifiedUserEntity.id, accept)
        inviteEntity = getInviteEntity(response)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        inviteEntity.userId == unverifiedUserEntity.id
        inviteEntity.email == unverifiedUserEntity.email
        inviteEntity.registrationCode != null
        inviteEntity.registrationCode.size() == 32
        inviteEntity.created != null
        inviteEntity.expires != null
        inviteEntity.created.toGregorianCalendar().getTime().equals(inviteEntity.expires.toGregorianCalendar().getTime().minus(2))

        and: "email sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        when: "using identity service admin token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(utils.getIdentityAdminToken(), unverifiedUserEntity.id, accept)
        inviteEntity = getInviteEntity(response)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        inviteEntity.userId == unverifiedUserEntity.id
        inviteEntity.email == unverifiedUserEntity.email
        inviteEntity.registrationCode != null
        inviteEntity.registrationCode.size() == 32
        inviteEntity.created != null
        inviteEntity.expires != null
        inviteEntity.created.toGregorianCalendar().getTime().equals(inviteEntity.expires.toGregorianCalendar().getTime().minus(2))

        and: "email sent"
        wiserWrapper.wiserServer.getMessages() != null
        wiserWrapper.wiserServer.getMessages().size() == 1

        cleanup:
        reloadableConfiguration.reset()
        clearEmailServerMessages()

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "unauthorized users can not send an unverified user invite: accept = #accept"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def userAdmin2 = utils.createCloudAccount()
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        def userManager2 = utils.createUser(userAdmin2Token)
        def userManager2Token = utils.getToken(userManager2.username)
        utils.addRoleToUser(userManager2, Constants.USER_MANAGE_ROLE_ID)
        def defaultUser = utils.createUser(userAdminToken)
        def defaultUserToken = utils.getToken(defaultUser.username)
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        when: "using default user's token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(defaultUserToken, unverifiedUserEntity.id, accept)

        then: "expect forbidden"
        response.status == HttpStatus.SC_FORBIDDEN

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        when: "using user admin's token from another domain"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdmin2Token, unverifiedUserEntity.id, accept)

        then: "expect forbidden"
        response.status == HttpStatus.SC_FORBIDDEN

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        when: "using user manage's token from another domain"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userManager2Token, unverifiedUserEntity.id, accept)

        then: "expect forbidden"
        response.status == HttpStatus.SC_FORBIDDEN

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        when: "using invalid token"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite("invalid", unverifiedUserEntity.id, accept)

        then: "expect forbidden"
        response.status == HttpStatus.SC_UNAUTHORIZED

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        cleanup:
        reloadableConfiguration.reset()
        clearEmailServerMessages()

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "error check: unverified user invite common errors: accept = #accept"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED

        when: "invalid user id"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, "invalid", accept)

        then: "expect not found"
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format("Unverified user with ID '%s' was not found.", "invalid"))

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        when: "not an unverified user id"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, defaultUser.id, accept)

        then: "expect not found"
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format("Unverified user with ID '%s' was not found.", defaultUser.id))

        and: "email is not sent"
        wiserWrapper.wiserServer.getMessages().isEmpty()

        cleanup:
        reloadableConfiguration.reset()
        clearEmailServerMessages()

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Verify User Invite"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        when: "send unverified user invite"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        when: "verify valid user invite"
        com.rackspace.idm.domain.entity.User daoUser = userDao.getUserById(unverifiedUserEntity.id)
        response = cloud20.verifyUserInvite(unverifiedUserEntity.id, daoUser.registrationCode)

        then: "returns valid response"
        response.status == HttpStatus.SC_OK

        when: "verify valid user invite invalid code"
        response = cloud20.verifyUserInvite(unverifiedUserEntity.id, "01234567890")

        then: "returns not found"
        response.status == HttpStatus.SC_NOT_FOUND

        when: "verify regular user"
        response = cloud20.verifyUserInvite(userAdmin.id, "01234567890")

        then: "returns not found"
        response.status == HttpStatus.SC_NOT_FOUND

        when: "verify invalid user invite invalid code"
        response = cloud20.verifyUserInvite("0123456789", "01234567890")

        then: "returns not found"
        response.status == HttpStatus.SC_NOT_FOUND

        when: "re-send unverified user invite"
        clearEmailServerMessages()
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)

        then: "assert valid response"
        response.status == HttpStatus.SC_OK

        when: "verify valid user invite with first registrationCode"
        response = cloud20.verifyUserInvite(unverifiedUserEntity.id, daoUser.registrationCode)

        then: "returns not found"
        response.status == HttpStatus.SC_NOT_FOUND

        when: "verify valid user invite with second registartionCode"
        com.rackspace.idm.domain.entity.User daoUser2 = userDao.getUserById(unverifiedUserEntity.id)
        response = cloud20.verifyUserInvite(unverifiedUserEntity.id, daoUser2.registrationCode)

        then: "returns valid response"
        response.status == HttpStatus.SC_OK

        when: "verify expired user invite"
        reloadableConfiguration
        reloadableConfiguration.setProperty(IdentityConfig.UNVERIFIED_USER_INVITES_TTL_HOURS_PROP, 0)
        response = cloud20.verifyUserInvite(unverifiedUserEntity.id, daoUser2.registrationCode)

        then: "returns not found"
        response.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        reloadableConfiguration.reset()
        clearEmailServerMessages()
    }

    def "Test listing of Verified and Unverified users in domain"(){
        given :
        def userAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        def unverifiedUser1 = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        def unverifiedUser2 = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        // Create Verified Users
        def verifiedUser1 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser2 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser3 = utils.createUser(utils.getToken(userAdmin.username))

        // Create unverifeid Users
        unverifiedUser1 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser1).getEntity(User).value
        unverifiedUser2 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser2).getEntity(User).value

        when: "list users in domain with filter - VERIFIED"
        def listOnlyVerifiedUsers = cloud20.listUsersInDomain(utils.getServiceAdminToken(), userAdmin.getDomainId(), "VERIFIED").getEntity(UserList).value

        then: "only verified users are listed"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)

        when: "list users in domain with filter - UNVERIFIED"
        def listOnlyUnverifiedUsers = cloud20.listUsersInDomain(utils.getServiceAdminToken(), userAdmin.getDomainId(), "UNVERIFIED").getEntity(UserList).value

        then: "only unverified users are listed"
        listOnlyUnverifiedUsers.user.id.contains(unverifiedUser1.id)
        listOnlyUnverifiedUsers.user.id.contains(unverifiedUser2.id)

        and: "unverified users are not listed"
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser1.id)
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser2.id)
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser3.id)

        when: "list users in domain with filter - ALL"
        def listAllUsers = cloud20.listUsersInDomain(utils.getServiceAdminToken(), userAdmin.getDomainId(), "ALL").getEntity(UserList).value

        then: "both unverified users and verified users are listed"
        listAllUsers.user.id.contains(unverifiedUser1.id)
        listAllUsers.user.id.contains(unverifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser1.id)
        listAllUsers.user.id.contains(verifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser3.id)

        when: "list users in domain with filter - unexpected value"
        listOnlyVerifiedUsers = cloud20.listUsersInDomain(utils.getServiceAdminToken(), userAdmin.getDomainId(), "unExpectedV&&**&").getEntity(UserList).value

        then: "only by default verified users are listed"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)

        when: "list users in domain with filter - null value"
        listOnlyVerifiedUsers = cloud20.listUsersInDomain(utils.getServiceAdminToken(), userAdmin.getDomainId(), "unExpectedV&&**&").getEntity(UserList).value

        then: "only by default verified users are listed"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)
    }

    def "Test listing of Verified and Unverified users in user group of a domain"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def domainId = userAdmin.domainId
        utils.domainRcnSwitch(domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        def userAdminToken = utils.getToken(userAdmin.username)

        def unverifiedUser1 = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        def unverifiedUser2 = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        // Create Verified Users
        def verifiedUser1 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser2 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser3 = utils.createUser(utils.getToken(userAdmin.username))

        // Create unverifeid Users
        unverifiedUser1 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser1).getEntity(User).value
        unverifiedUser2 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser2).getEntity(User).value

        when: "create user group for domain"
        UserGroup userGroup = new UserGroup().with {
            it.domainId = userAdmin.domainId
            it.name = testUtils.getRandomUUID('userGroup')
            it.description = "desc"
            it
        }
        def response = cloud20.createUserGroup(userAdminToken, userGroup)
        def userGroupEntity = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "users are added in the user group"
        def verifiedUser1Response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, verifiedUser1.id)
        def verifiedUser2Response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, verifiedUser2.id)
        def verifiedUser3Response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, verifiedUser3.id)
        def unverifiedUser1Response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, unverifiedUser1.id)
        def unverifiedUser2Response = cloud20.addUserToUserGroup(userAdminToken, domainId, userGroupEntity.id, unverifiedUser2.id)

        then: "all users should get added to user group"
        verifiedUser1Response.status == SC_NO_CONTENT
        verifiedUser2Response.status == SC_NO_CONTENT
        verifiedUser3Response.status == SC_NO_CONTENT
        unverifiedUser1Response.status == SC_NO_CONTENT
        unverifiedUser2Response.status == SC_NO_CONTENT

        when: "list all users in user group"
        UserSearchCriteria userSearchCriteria = new UserSearchCriteria()
        userSearchCriteria.setUserType(com.rackspace.idm.domain.entity.User.UserType.ALL)
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, userSearchCriteria)
        def listAllUsers = response.getEntity(UserList).value

        then: "All 5 users should be listed"
        response.status == SC_OK
        listAllUsers.user.size() == 5
        listAllUsers.user.id.contains(unverifiedUser1.id)
        listAllUsers.user.id.contains(unverifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser1.id)
        listAllUsers.user.id.contains(verifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser3.id)

        when: "filter only verified users in user group"
        userSearchCriteria.setUserType(com.rackspace.idm.domain.entity.User.UserType.VERIFIED)
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, userSearchCriteria)
        def onlyVerifiedUsers = response.getEntity(UserList).value

        then: "only 3 verified users should be listed"
        response.status == SC_OK
        onlyVerifiedUsers.user.size() == 3
        onlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        onlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        onlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        when: "filter only unverified users in user group"
        userSearchCriteria.setUserType(com.rackspace.idm.domain.entity.User.UserType.UNVERIFIED)
        response = cloud20.getUsersInUserGroup(userAdminToken, domainId, userGroupEntity.id, userSearchCriteria)
        def listOnlyUnVerifiedUsers = response.getEntity(UserList).value

        then: "only 2 unverified users should be listed"
        response.status == SC_OK
        listOnlyUnVerifiedUsers.user.size() == 2
        listOnlyUnVerifiedUsers.user.id.contains(unverifiedUser1.id)
        listOnlyUnVerifiedUsers.user.id.contains(unverifiedUser2.id)
    }

    def "Test that name and user type are mutually exclusive query params"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        // adding unverified and verified users
        def unverifiedUser = new User().with {
            it.email = "aseem.jain@rackspace.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }
        cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser)
        utils.createUser(userAdminToken)
        utils.createUser(userAdminToken)

        when: "list users without passing any param"
        def response = cloud20.listUsersWithFilterOptions(userAdminToken, "ALL", null, null, MediaType.APPLICATION_XML_TYPE)

        then:
        then: "status is 200 OK"
        response.status == 200

        when: "list users with param of only user type is passed"
        response = cloud20.listUsersWithFilterOptions(userAdminToken, "ALL", null, null, MediaType.APPLICATION_XML_TYPE)

        then: "status is 200 OK"
        response.status == 200

        when: "list users with param of user type and email is passed"
        response = cloud20.listUsersWithFilterOptions(userAdminToken, "ALL", null, "aseem@example.com", MediaType.APPLICATION_XML_TYPE)

        then: "status is 200 OK"
        response.status == 200

        when: "list users with param of user type and name is passed"
        response = cloud20.listUsersWithFilterOptions(userAdminToken, "VERIFIED", "aseem", null, MediaType.APPLICATION_XML_TYPE)

        then: "status is 400 Bad Request"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG)

        when: "list users with param of user type, name and email is passed"
        response = cloud20.listUsersWithFilterOptions(userAdminToken, "VERIFIED", "aseem", "aseem@example.com", MediaType.APPLICATION_XML_TYPE)

        then: "status is 400 Bad Request"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG)
    }

    def "Test listing of Verified and Unverified users while listing users"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        def unverifiedUser1 = new User().with {
            it.email = "aseem@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        def unverifiedUser2 = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it.unverified = true
            it
        }

        // Create Verified Users
        def verifiedUser1 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser2 = utils.createUser(utils.getToken(userAdmin.username))
        def verifiedUser3 = utils.createUser(utils.getToken(userAdmin.username))

        // Create Unverified Users
        unverifiedUser1 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser1).getEntity(User).value
        unverifiedUser2 = cloud20.createUnverifiedUser(utils.getServiceAdminToken(), unverifiedUser2).getEntity(User).value

        when: "list users in domain with filter - VERIFIED"
        def listOnlyVerifiedUsers = cloud20.listUsersWithFilterOptions(userAdminToken, "VERIFIED").getEntity(UserList).value

        then: "only verified users are listed"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)

        when: "list users in domain with filter - null value"
        listOnlyVerifiedUsers = cloud20.listUsersWithFilterOptions(userAdminToken, "unExpectedV&&**&").getEntity(UserList).value

        then: "only by default verified users are listed"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)

        when: "list users in domain with filter - ALL"
        def listAllUsers = cloud20.listUsersWithFilterOptions(userAdminToken, "ALL").getEntity(UserList).value

        then: "both unverified users and verified users are listed"
        listAllUsers.user.id.contains(unverifiedUser1.id)
        listAllUsers.user.id.contains(unverifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser1.id)
        listAllUsers.user.id.contains(verifiedUser2.id)
        listAllUsers.user.id.contains(verifiedUser3.id)

        when: "list users with filter - unexpected value"
        listOnlyVerifiedUsers = cloud20.listUsersWithFilterOptions(userAdminToken, "unExpectedV&&**&").getEntity(UserList).value

        then: "only verified users are listed (by default)"
        listOnlyVerifiedUsers.user.id.contains(verifiedUser1.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser2.id)
        listOnlyVerifiedUsers.user.id.contains(verifiedUser3.id)

        and: "unverified users are not listed"
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser1.id)
        !listOnlyVerifiedUsers.user.id.contains(unverifiedUser2.id)

        when: "list users with filter - UNVERIFIED"
        def listOnlyUnverifiedUsers = cloud20.listUsersWithFilterOptions(userAdminToken, "UNVERIFIED").getEntity(UserList).value

        then: "only unverified users are listed"
        listOnlyUnverifiedUsers.user.id.contains(unverifiedUser1.id)
        listOnlyUnverifiedUsers.user.id.contains(unverifiedUser2.id)

        and: "unverified users are not listed"
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser1.id)
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser2.id)
        !listOnlyUnverifiedUsers.user.id.contains(verifiedUser3.id)
    }

    @Unroll
    def "accept invite for unverified users: mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        // Send invite
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)
        assert response.status == HttpStatus.SC_OK
        def inviteEntity = getInviteEntity(response)

        def username = testUtils.getRandomUUID("user")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = inviteEntity.userId
            it.username = username
            it.password = Constants.DEFAULT_PASSWORD
            it.registrationCode = inviteEntity.registrationCode
            it.secretQA = v2Factory.createSecretQA()
            it
        }

        when: "accept invite"
        response = cloud20.acceptUnverifiedUserInvite(userForCreate, mediaType, mediaType)
        def userEntity = testUtils.getEntity(response, User)

        then:
        response.status == HttpStatus.SC_OK

        and: "expected attributes"
        userEntity.id == inviteEntity.userId
        userEntity.username == username
        userEntity.domainId == userAdmin.domainId
        userEntity.email == email
        !userEntity.unverified
        userEntity.enabled

        and: "assert password and registration code are not returned"
        userEntity.registrationCode == null
        userEntity.password == null

        and: "assert create event is sent when unverified user accept invite"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userEntity, EventType.CREATE),
                VerificationTimes.exactly(1)
        )

        when: "Pull the entity from backend"
        com.rackspace.idm.domain.entity.User persistedUser = userDao.getUserById(unverifiedUserEntity.id)

        then: "registration/invitesend attributes are now null"
        persistedUser.registrationCode == null
        persistedUser.inviteSendDate == null

        when: "retrieve secretQA"
        response = cloud20.getSecretQA(utils.identityAdminToken, userEntity.id)
        def secretQAEntity = response.getEntity(SecretQA)

        then:
        response.status == HttpStatus.SC_OK

        secretQAEntity.answer == Constants.DEFAULT_RAX_KSQA_SECRET_ANWSER
        secretQAEntity.question == Constants.DEFAULT_RAX_KSQA_SECRET_QUESTION

        when: "authenticate user"
        response = cloud20.authenticate(username, Constants.DEFAULT_PASSWORD)

        then:
        response.status == HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "error check: accept invite for unverified users: mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value
        // Create invite
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)
        assert response.status == HttpStatus.SC_OK
        def inviteEntity = getInviteEntity(response)

        def username = testUtils.getRandomUUID("user")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = inviteEntity.userId
            it.username = username
            it.password = Constants.DEFAULT_PASSWORD
            it.registrationCode = inviteEntity.registrationCode
            it.secretQA = v2Factory.createSecretQA()
            it
        }

        when: "invalid user id"
        def invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.id = "invalid"
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format(DefaultCloud20Service.UNVERIFIED_USER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "provisioned user id"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.id = userAdmin.id
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format(DefaultCloud20Service.UNVERIFIED_USER_NOT_FOUND_ERROR_MESSAGE, userAdmin.id))

        when: "invalid registration code"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.registrationCode = "invalid"
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format(DefaultCloud20Service.UNVERIFIED_USER_NOT_FOUND_ERROR_MESSAGE, inviteEntity.userId))

        when: "missing username"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.username = null
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, String.format(Validator20.REQUIRED_ATTR_MESSAGE, "username"))

        when: "invalid username"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.username = "invalid#&^%()"
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST,"Username must begin with an alphanumeric character, have no spaces, and only contain the following valid special characters: - _ . @")

        when: "existing username"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.username = userAdmin.username
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, "Username unavailable within Rackspace system. Please try another.")

        when: "missing password"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.password = null
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, String.format(Validator20.REQUIRED_ATTR_MESSAGE, "password"))

        when: "invalid password"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.password = "password"
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character.")

        when: "missing secretQA"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.secretQA = null
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Secret question and answer are required attributes.")

        when: "missing secret answer"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.secretQA = v2Factory.createSecretQA()
        invalidUserForCreate.secretQA.answer = null
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, String.format(Validator20.REQUIRED_ATTR_MESSAGE, "answer"))

        when: "missing secret question"
        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        invalidUserForCreate.secretQA = v2Factory.createSecretQA()
        invalidUserForCreate.secretQA.question = null
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, String.format(Validator20.REQUIRED_ATTR_MESSAGE, "question"))

        when: "expired registration code"
        reloadableConfiguration.setProperty(IdentityConfig.UNVERIFIED_USER_INVITES_TTL_HOURS_PROP, 0)

        invalidUserForCreate = new UserForCreate()
        InvokerHelper.setProperties(invalidUserForCreate, userForCreate.properties)
        response = cloud20.acceptUnverifiedUserInvite(invalidUserForCreate, mediaType, mediaType)
        reloadableConfiguration.setProperty(IdentityConfig.UNVERIFIED_USER_INVITES_TTL_HOURS_PROP, IdentityConfig.UNVERIFIED_USER_INVITES_TTL_HOURS_DEFAULT)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Your registration code has expired, please request a new invite.")

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Verify registration code on unverified user is replaced when new invite is sent: mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value
        // Create invite
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)
        assert response.status == HttpStatus.SC_OK
        def oldInviteEntity = getInviteEntity(response)

        def username = testUtils.getRandomUUID("user")
        UserForCreate userForCreate = new UserForCreate().with {
            it.id = oldInviteEntity.userId
            it.username = username
            it.password = Constants.DEFAULT_PASSWORD
            it.registrationCode = oldInviteEntity.registrationCode
            it.secretQA = v2Factory.createSecretQA()
            it
        }

        when: "send new invite"
        response = cloud20.sendUnverifiedUserInvite(userAdminToken, unverifiedUserEntity.id)
        def inviteEntity = getInviteEntity(response)

        then:
        response.status == HttpStatus.SC_OK

        when: "accepting invite with old registration code"
        response = cloud20.acceptUnverifiedUserInvite(userForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, String.format(DefaultCloud20Service.UNVERIFIED_USER_NOT_FOUND_ERROR_MESSAGE, inviteEntity.userId))

        when: "accepting invite with new registration code"
        userForCreate.registrationCode = inviteEntity.registrationCode
        response = cloud20.acceptUnverifiedUserInvite(userForCreate, mediaType, mediaType)

        then:
        response.status == HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "verify unverified users cannot be created in domain with no user admin"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        def identityAdmin = utils.createIdentityAdmin()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = identityAdmin.domainId
            it
        }
        utils.domainRcnSwitch(identityAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when: "providing domain id"
        def response = cloud20.createUnverifiedUser(utils.getToken(identityAdmin.username), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN_MESSAGE)

        when: "not providing domain id"
        user.domainId = null
        response = cloud20.createUnverifiedUser(utils.getToken(identityAdmin.username), user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN_MESSAGE)

        cleanup:
        reloadableConfiguration.reset()
    }

    def "verify unverified users cannot be created in domain with no enabled user admin"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Disable userAdmin
        utils.disableUser(userAdmin)

        when: "providing domain id"
        def response = cloud20.createUnverifiedUser(utils.identityAdminToken, user)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_DOMAIN_WITHOUT_ACCOUNT_ADMIN_MESSAGE)

        cleanup:
        reloadableConfiguration.reset()
    }

    @Unroll
    def "update unverified user's contactId: mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        def userForUpdate = new UserForCreate().with {
            it.contactId = testUtils.getRandomUUID("contactId")
            it
        }

        when: "get user"
        response = cloud20.getUserById(utils.getIdentityAdminToken(), unverifiedUserEntity.id)
        def userEntity = testUtils.getEntity(response, User)

        then:
        response.status == SC_OK

        and: "expected attributes"
        userEntity.id == unverifiedUserEntity.id
        userEntity.username == null
        userEntity.domainId == unverifiedUserEntity.domainId
        userEntity.email == unverifiedUserEntity.email
        userEntity.unverified
        !userEntity.enabled
        userEntity.contactId == null

        when: "update user"
        response = cloud20.updateUser(utils.getIdentityAdminToken(), unverifiedUserEntity.id, userForUpdate, mediaType, mediaType)
        userEntity = testUtils.getEntity(response, User)

        then:
        response.status == SC_OK

        and: "expected attributes"
        userEntity.id == unverifiedUserEntity.id
        userEntity.username == null
        userEntity.domainId == unverifiedUserEntity.domainId
        userEntity.email == unverifiedUserEntity.email
        userEntity.unverified
        !userEntity.enabled
        userEntity.contactId == userForUpdate.contactId

        when: "update unverified user ignores other attributes"
        userForUpdate.username = "otherUsername"
        userForUpdate.contactId = testUtils.getRandomUUID("contactId")
        userForUpdate.domainId = "otherDomainId"
        userForUpdate.enabled = true
        userForUpdate.unverified = false
        userForUpdate.email = "badEmail@rackspace.com"
        response = cloud20.updateUser(utils.getIdentityAdminToken(), unverifiedUserEntity.id, userForUpdate, mediaType, mediaType)
        userEntity = testUtils.getEntity(response, User)

        then:
        response.status == SC_OK

        and: "expected attributes"
        userEntity.id == unverifiedUserEntity.id
        userEntity.username == null
        userEntity.domainId == unverifiedUserEntity.domainId
        userEntity.email == unverifiedUserEntity.email
        userEntity.unverified
        !userEntity.enabled
        userEntity.contactId == userForUpdate.contactId

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "error check: update unverified user: mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CREATE_INVITES_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def email = "${RandomStringUtils.randomAlphabetic(8)}@rackspace.com"
        def user = new User().with {
            it.email = email
            it.domainId = userAdmin.domainId
            it
        }
        utils.domainRcnSwitch(userAdmin.domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        // Create unverified user
        def response = cloud20.createUnverifiedUser(userAdminToken, user)
        assert response.status == HttpStatus.SC_CREATED
        def unverifiedUserEntity = response.getEntity(User).value

        def userForUpdate = new UserForCreate().with {
            it.contactId = testUtils.getRandomUUID("contactId")
            it
        }

        when: "using non admin token"
        response = cloud20.updateUser(userAdminToken, unverifiedUserEntity.id, userForUpdate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, "Not Authorized")

        when: "contactId is an empty string"
        userForUpdate.contactId = ""
        response = cloud20.updateUser(utils.getIdentityAdminToken(), unverifiedUserEntity.id, userForUpdate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(Validator20.EMPTY_ATTR_MESSAGE, "contactId"))

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def getInviteEntity(ClientResponse response) {
        if (response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(Invite)
        }

        Invite invite = new Invite()
        def entity = new JsonSlurper().parseText(response.getEntity(String))
        invite.userId = entity["RAX-AUTH:invite"]["userId"]
        invite.registrationCode = entity["RAX-AUTH:invite"]["registrationCode"]
        invite.email = entity["RAX-AUTH:invite"]["email"]
        invite.created = DatatypeFactory.newInstance().newXMLGregorianCalendar(entity["RAX-AUTH:invite"]["created"])
        invite.expires = DatatypeFactory.newInstance().newXMLGregorianCalendar(entity["RAX-AUTH:invite"]["expires"])

        return invite
    }

}
