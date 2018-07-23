package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.DefaultUserService
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.User
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UnverifiedUserIntegrationTest extends RootIntegrationTest {

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
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, ErrorCodes.ERROR_CODE_INVALID_VALUE, DefaultCloud20Service.ERROR_UVERIFIED_USERS_MUST_HAVE_UNIQUE_EMAIL_WITHIN_DOMAIN)

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
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, ErrorCodes.ERROR_CODE_INVALID_VALUE, DefaultCloud20Service.ERROR_UVERIFIED_USERS_MUST_HAVE_UNIQUE_EMAIL_WITHIN_DOMAIN)

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

}
