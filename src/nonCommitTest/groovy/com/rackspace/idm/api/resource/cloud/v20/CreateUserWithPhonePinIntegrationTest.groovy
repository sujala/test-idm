package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.service.UserService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class CreateUserWithPhonePinIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser
    @Shared
    def domainId

    @Autowired
    UserService userService

    def "Create identityAdmin, userAdmin, userManage, defaultUser with phone PIN" () {
        given:
        def domainId = utils.createDomain()

        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def identityAdminUserEntity = userService.getUserById(identityAdmin.id)
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def userManageUserEntity = userService.getUserById(userManage.id)
        def defaultUserEntity = userService.getUserById(defaultUser.id)

        then:
        IdmAssert.assertPhonePin(identityAdminUserEntity)
        IdmAssert.assertPhonePin(userAdminUserEntity)
        IdmAssert.assertPhonePin(userManageUserEntity)
        IdmAssert.assertPhonePin(defaultUserEntity)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "When a user's token is validated - return Phone PIN" () {
        given:
        def user = utils.createCloudAccount()
        AuthenticateResponse federatedAuthResponse = utils.createFederatedUserForAuthResponse(user.domainId)

        when: "authenticate"
        def response = cloud20.authenticate(user.username, Constants.DEFAULT_PASSWORD, contentType)
        AuthenticateResponse authResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "returns phone pin"
        authResponse.user.phonePin != null

        when: "validate"
        response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id, contentType)
        AuthenticateResponse validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "returns phone pin"
        validateResponse.user.phonePin != null

        when: "validate impersonated token"
        ImpersonationResponse impersonationResponse = utils.impersonateWithRacker(user)
        response = cloud20.validateToken(utils.getServiceAdminToken(), impersonationResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not return phone pin"
        validateResponse.user.phonePin == null

        when: "validate federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedAuthResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "return phone pin"
        validateResponse.user.phonePin != null

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Get user by userId - return Phone PIN" () {
        given:
        def user = utils.createCloudAccount()

        AuthenticateResponse federatedAuthResponse = utils.createFederatedUserForAuthResponse(user.domainId)

        def token = utils.authenticate(user.username, Constants.DEFAULT_PASSWORD).token.id
        def impersonatedToken = utils.impersonateWithRacker(user).token.id

        when: "when caller is making request"
        def response = cloud20.getUserById(token, user.id, contentType)
        def responseUser = testUtils.getEntity(response, User)

        then: "returns phone pin"
        responseUser.phonePin != null

        when: "when caller is not self"
        response = cloud20.getUserById(utils.getIdentityAdminToken(), user.id, contentType)
        responseUser = testUtils.getEntity(response, User)

        then: "does not return phone pin"
        responseUser.phonePin == null

        when: "when caller is being impersonated"
        response = cloud20.getUserById(impersonatedToken, user.id, contentType)
        responseUser = testUtils.getEntity(response, User)

        then: "does not return phone pin"
        responseUser.phonePin == null

        when: "when caller is federated user"
        response = cloud20.getUserById(federatedAuthResponse.token.id, federatedAuthResponse.user.id, contentType)
        responseUser = testUtils.getEntity(response, User)

        then: "returns phone pin"
        responseUser.phonePin != null

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Other user services - do not return Phone PIN" () {
        given:
        def user = utils.createCloudAccount()
        def token = utils.authenticate(user.username, Constants.DEFAULT_PASSWORD).token.id

        when: "list users"
        def response = cloud20.listUsers(token, "0", "10", contentType)
        def responseUsers = testUtils.getEntity(response, UserList)

        then: "does not return phone pin"
        responseUsers.user.find{ it.id == user.id}.phonePin == null

        when: "get user by name"
        response = cloud20.getUserByName(token, user.username, contentType)
        def responseUser = testUtils.getEntity(response, User)

        then: "does not return phone pin"
        responseUser.phonePin == null

        when: "get users in domain"
        def usersByDomain = utils.getUsersByDomainId(user.domainId, utils.getServiceAdminToken(), contentType)

        then: "does not return phone pin"
        if (contentType == MediaType.APPLICATION_XML_TYPE) {
            usersByDomain.find {it.id == user.id}.phonePin == null
        } else {
            usersByDomain["users"].find {it['id'] == user.id}.phonePin == null
        }

        when: "get admins for user"
        response = cloud20.getAdminsForUser(token, user.id)
        responseUsers = testUtils.getEntity(response, UserList)

        then: "does not return phone pin"
        responseUsers.user.find{ it.id == user.id}.phonePin == null

        cleanup:
        utils.deleteUser(user)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "only users with the 'identity:phone-pin-admin' role can set a phone pin on user creation - mediaType = #mediaType"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def domainId = utils.createDomain()
        def phonePin = "236972"
        def userForCreate = v2Factory.userForCreate(testUtils.getRandomUUID('userWithPin'), null, "test@racksapce.com", true, "ORD", domainId, Constants.DEFAULT_PASSWORD).with {
            it.phonePin = phonePin
            it
        }

        when: "identity admin without 'identity:phone-pin-admin' role"
        def response = cloud20.createUser(identityAdminToken, userForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Not authorized to set phone pin.")

        when: "identity admin with 'identity:phone-pin-admin' role"
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        identityAdminToken = utils.getToken(identityAdmin.username)
        response = cloud20.createUser(identityAdminToken, userForCreate, mediaType, mediaType)
        User userAdmin = testUtils.getEntity(response, User)

        then: "assert user got created"
        response.status == HttpStatus.SC_CREATED
        userAdmin.phonePin == null

        when: "get self"
        def userToken = utils.getToken(userAdmin.username)
        response = cloud20.getUserById(userToken, userAdmin.id, mediaType)
        userAdmin = testUtils.getEntity(response, User)

        then: "assert phone pin set on user"
        userAdmin.phonePin == phonePin

        when: "userAdmin without 'identity:phone-pin-admin' role"
        userForCreate.username = testUtils.getRandomUUID('userManager')
        def userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.createUser(userAdminToken, userForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Not authorized to set phone pin.")

        when: "userAdmin with 'identity:phone-pin-admin' role"
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.createUser(userAdminToken, userForCreate, mediaType, mediaType)
        User defaultUser = testUtils.getEntity(response, User)

        then: "assert user got created"
        response.status == HttpStatus.SC_CREATED
        defaultUser.phonePin == null

        when: "get self"
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getUserById(defaultUserToken, defaultUser.id, mediaType)
        defaultUser = testUtils.getEntity(response, User)

        then: "assert phone pin set on user"
        defaultUser.phonePin == phonePin

        cleanup:
        utils.deleteUsersQuietly([identityAdmin, userAdmin, defaultUser])

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "userManager with the 'identity:phone-pin-admin' role can set a phone pin on user creation - mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userManageToken = utils.getToken(userManage.username)
        def phonePin = "236972"
        def userForCreate = v2Factory.userForCreate(testUtils.getRandomUUID('userWithPin'), null, "test@racksapce.com", true, "ORD", domainId, Constants.DEFAULT_PASSWORD).with {
            it.phonePin = phonePin
            it
        }

        when: "userManage without 'identity:phone-pin-admin' role"
        def response = cloud20.createUser(userManageToken, userForCreate, mediaType, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Not authorized to set phone pin.")

        when: "userManage with 'identity:phone-pin-admin' role"
        utils.addRoleToUser(userManage, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        userManageToken = utils.getToken(userManage.username)
        response = cloud20.createUser(userManageToken, userForCreate, mediaType, mediaType)
        User defaultUser2 = testUtils.getEntity(response, User)

        then: "assert user got created"
        response.status == HttpStatus.SC_CREATED
        defaultUser.phonePin == null

        when: "get self"
        def defaultUserToken = utils.getToken(defaultUser2.username)
        response = cloud20.getUserById(defaultUserToken, defaultUser2.id, mediaType)
        defaultUser2 = testUtils.getEntity(response, User)

        then: "assert phone pin set on user"
        defaultUser2.phonePin == phonePin

        cleanup:
        utils.deleteUsersQuietly([defaultUser2, defaultUser, userManage, userAdmin, identityAdmin])

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "error check: create user with phone pin"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def domainId = utils.createDomain()
        def userForCreate = v2Factory.userForCreate(testUtils.getRandomUUID('userWithPin'), null, "test@racksapce.com", true, "ORD", domainId, Constants.DEFAULT_PASSWORD)

        def domainId2 = utils.createDomain()
        def users, defaultUser
        (defaultUser, users) = utils.createDefaultUser(domainId2)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)

        when: "invalid phone pin"
        userForCreate.phonePin = "111122"
        def response = cloud20.createUser(identityAdminToken, userForCreate)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_PHONE_PIN_BAD_REQUEST, ErrorCodes.ERROR_MESSAGE_PHONE_PIN_BAD_REQUEST)

        when: "default user with 'identity:phone-pin-admin' role"
        userForCreate.domainId = domainId2
        response = cloud20.createUser(defaultUserToken, userForCreate)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, null, "Not Authorized")

        cleanup:
        utils.deleteUserQuietly(identityAdmin)
        utils.deleteUsersQuietly(users)
    }

}

