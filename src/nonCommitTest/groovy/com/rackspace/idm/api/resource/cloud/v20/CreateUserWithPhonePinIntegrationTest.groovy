package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.UserService
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.print.attribute.standard.Media
import javax.ws.rs.core.MediaType

class CreateUserWithPhonePinIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser
    @Shared
    def domainId

    @Autowired
    UserService userService

    @Unroll
    def "Create identityAdmin, userAdmin, userManage, defaultUser with phone PIN - featureEnabled == #featureEnabled" () {
        given:
        def domainId = utils.createDomain()

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, featureEnabled)

        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def identityAdminUserEntity = userService.getUserById(identityAdmin.id)
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def userManageUserEntity = userService.getUserById(userManage.id)
        def defaultUserEntity = userService.getUserById(defaultUser.id)

        then:

        if (featureEnabled) {
            IdmAssert.assertPhonePin(identityAdminUserEntity)
            IdmAssert.assertPhonePin(userAdminUserEntity)
            IdmAssert.assertPhonePin(userManageUserEntity)
            IdmAssert.assertPhonePin(defaultUserEntity)

        } else {
            assert identityAdminUserEntity.phonePin == null
            assert userAdminUserEntity.phonePin == null
            assert userManageUserEntity.phonePin == null
            assert defaultUserEntity.phonePin == null
        }

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        featureEnabled << [true, false]
    }

    def "When a user's token is validated - return Phone PIN" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)
        def userWithoutPhonePin = utils.createCloudAccount()
        AuthenticateResponse federatedAuthResponseWithoutPhonePin = utils.createFederatedUserForAuthResponse(userWithoutPhonePin.domainId)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
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

        when: "authenticate"
        response = cloud20.authenticate(userWithoutPhonePin.username, Constants.DEFAULT_PASSWORD, contentType)
        authResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not returns phone pin"
        authResponse.user.phonePin == null

        when: "validate"
        response = cloud20.validateToken(utils.getServiceAdminToken(), authResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not returns phone pin"
        validateResponse.user.phonePin == null

        when: "validate impersonated token"
        ImpersonationResponse impersonationResponse = utils.impersonateWithRacker(user)
        response = cloud20.validateToken(utils.getServiceAdminToken(), impersonationResponse.token.id, contentType)
        validateResponse = testUtils.getEntity(response, AuthenticateResponse)

        then: "does not return phone pin"
        validateResponse.user.phonePin == null

        when: "validate federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedAuthResponseWithoutPhonePin.token.id, contentType)
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
        utils.deleteUser(userWithoutPhonePin)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Get user by userId - return Phone PIN" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)
        def userWithoutPhonePin = utils.createCloudAccount()

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
        def user = utils.createCloudAccount()

        AuthenticateResponse federatedAuthResponse = utils.createFederatedUserForAuthResponse(user.domainId)

        def tokenWithoutPhonePin = utils.authenticate(userWithoutPhonePin.username, Constants.DEFAULT_PASSWORD).token.id
        def token = utils.authenticate(user.username, Constants.DEFAULT_PASSWORD).token.id
        def impersonatedToken = utils.impersonateWithRacker(user).token.id

        when: "when caller is making request"
        def response = cloud20.getUserById(tokenWithoutPhonePin, userWithoutPhonePin.id, contentType)
        User responseUser = testUtils.getEntity(response, User)

        then: "does not return phone pin"
        responseUser.phonePin == null

        when: "when caller is making request"
        response = cloud20.getUserById(token, user.id, contentType)
        responseUser = testUtils.getEntity(response, User)

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
        utils.deleteUser(userWithoutPhonePin)

        where:
        contentType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Other user services - do not return Phone PIN" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, true)
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

}

