package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

class MultiFactorStateIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired BasicMultiFactorService multiFactorService;

    @Autowired Configuration config

    @Autowired ScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired UserDao userDao

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    def void doSetupSpec() {
        startOrRestartGrizzly("classpath:app-config.xml ")
    }

    def void doCleanupSpec() {
        stopGrizzly()
    }

    def "when enabling mfa for user multiFactorState is updated to 'ACTIVE'"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = utils.authenticate(user).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)

        when: "loading the user through the api"
        def userById = utils.getUserById(user.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "loading the user from the directory"
        def directoryUser = userDao.getUserById(user.id)

        then: "the user's mfa state is 'ACTIVE'"
        directoryUser.multiFactorState == BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)
    }

    def "when disabling mfa for user multiFactorState is updated to null"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)

        when: "the mfa user's mfa state is initially 'ACTIVE'"
        def userById = utils.getUserById(user.id)

        then:
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "disabling multifactor on the user"
        settings = v2Factory.createMultiFactorSettings(false)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        userById = utils.getUserById(user.id)

        then: "removes the multifactor state attribute when loading the user through the api"
        userById.multiFactorState == null

        when: "loading a non-multifactor user from the directory"
        def directoryUser = userDao.getUserById(user.id)

        then: "the user does not have the multifactor state attribute"
        directoryUser.multiFactorState == null

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)
    }

    def "when deleting mfa for user multiFactorState is updated to null"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)

        when: "the mfa user's mfa state is initially 'ACTIVE'"
        def userById = utils.getUserById(user.id)

        then:
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "deleting multifactor on the user"
        cloud20.deleteMultiFactor(token, user.id)
        userById = utils.getUserById(user.id)

        then: "removes the multifactor state attribute when loading the user through the api"
        userById.multiFactorState == null

        when: "loading a non-multifactor user from the directory"
        def directoryUser = userDao.getUserById(user.id)

        then: "the user does not have the multifactor state attribute"
        directoryUser.multiFactorState == null

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)
    }

    def "mfa user with null mfa state will show as 'ACTIVE' in api calls"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def directoryUser = userDao.getUserById(user.id)
        directoryUser.multiFactorState = null
        userDao.updateUserAsIs(directoryUser)

        when: "user by ID call"
        def userById = cloud20.getUserById(utils.getServiceAdminToken(), user.id, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userById, acceptContentType, BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE)

        when: "get users by domain id call"
        def usersByDomainId = cloud20.getUsersByDomainId(utils.getServiceAdminToken(), user.domainId, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByDomainId, acceptContentType, BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE)

        when: "get user by username call"
        def userByUsername = cloud20.getUserByName(utils.getServiceAdminToken(), user.username, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userByUsername, acceptContentType, BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE)

        when: "get user by email address call"
        def usersByEmail = cloud20.getUsersByEmail(utils.getServiceAdminToken(), user.email, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByEmail, acceptContentType, BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE)

        when: "list users call"
        def userList = cloud20.listUsers(token, acceptContentType)

        then:
        verifyMfaStateForUsers(userList, acceptContentType, BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE)

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)

        where:
        acceptContentType | _
        MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_XML_TYPE | _
    }

    def "non-mfa user will not have mfa state attribute in api calls"() {
        given:
        def user = createUserAdmin()
        def token = utils.authenticate(user).token.id

        when: "user by ID call"
        def userById = cloud20.getUserById(utils.getServiceAdminToken(), user.id, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userById, acceptContentType, null)

        when: "get users by domain id call"
        def usersByDomainId = cloud20.getUsersByDomainId(utils.getServiceAdminToken(), user.domainId, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByDomainId, acceptContentType, null)

        when: "get user by username call"
        def userByUsername = cloud20.getUserByName(utils.getServiceAdminToken(), user.username, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userByUsername, acceptContentType, null)

        when: "get user by email address call"
        def usersByEmail = cloud20.getUsersByEmail(utils.getServiceAdminToken(), user.email, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByEmail, acceptContentType, null)

        when: "list users call"
        def userList = cloud20.listUsers(token, acceptContentType)

        then:
        verifyMfaStateForUsers(userList, acceptContentType, null)

        cleanup:
        deleteUserQuietly(user)

        where:
        acceptContentType | _
        MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_XML_TYPE | _
    }

    def "user with invalid mfa state in directory gets 500 on get user calls"() {
         given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def directoryUser = userDao.getUserById(user.id)
        directoryUser.multiFactorState = 'INVALID'
        userDao.updateUserAsIs(directoryUser)

        when: "user by ID call"
        def userByIdRequest = cloud20.getUserById(token, user.id)

        then:
        userByIdRequest.status == 500

        when: "get users by domain id call"
        def usersByDomainIdRequest = cloud20.getUsersByDomainId(utils.getServiceAdminToken(), user.domainId)

        then:
        usersByDomainIdRequest.status == 500

        when: "get user by username call"
        def userByUsernameRequest = cloud20.getUserByName(utils.getServiceAdminToken(), user.username)

        then:
        userByUsernameRequest.status == 500

        when: "get user by email address call"
        def userByEmailAddressRequest = cloud20.getUsersByEmail(utils.getServiceAdminToken(), user.email)

        then:
        userByEmailAddressRequest.status == 500

        when: "list users call"
        def userListRequest = cloud20.listUsers(token)

        then:
        userListRequest.status == 500

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)
    }

    def "user with mfaEnabled == false and a non-null mfa state in directory do not show mfa state on get user calls"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def directoryUser = userDao.getUserById(user.id)
        directoryUser.multifactorEnabled = Boolean.FALSE
        userDao.updateUserAsIs(directoryUser)

        when: "user by ID call"
        def userById = cloud20.getUserById(utils.getServiceAdminToken(), user.id, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userById, acceptContentType, null)

        when: "get users by domain id call"
        def usersByDomainId = cloud20.getUsersByDomainId(utils.getServiceAdminToken(), user.domainId, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByDomainId, acceptContentType, null)

        when: "get user by username call"
        def userByUsername = cloud20.getUserByName(utils.getServiceAdminToken(), user.username, acceptContentType).getEntity(User)

        then:
        verifyMfaStateForUser(userByUsername, acceptContentType, null)

        when: "get user by email address call"
        def usersByEmail = cloud20.getUsersByEmail(utils.getServiceAdminToken(), user.email, acceptContentType)

        then:
        verifyMfaStateForUsers(usersByEmail, acceptContentType, null)

        when: "list users call"
        def userList = cloud20.listUsers(token, acceptContentType)

        then:
        verifyMfaStateForUsers(userList, acceptContentType, null)

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)

        where:
        acceptContentType | _
        MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_XML_TYPE | _
    }

    def "locked mfa user should have dynamic mfa state changed to 'ACTIVE' when unlock mfa call is made"() {
        given:
        reloadableConfiguration.reset()
        def maxAttempts = 3
        def autoUnlockSeconds = 1800
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, maxAttempts)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, autoUnlockSeconds)

        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        utils.addApiKeyToUser(user)
        def token = utils.authenticateApiKey(user.username).token.id
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)

        when: "loading the user through the api"
        def userById = utils.getUserById(user.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "user mfa state is updated to 'LOCKED'"
        def directoryUser = userDao.getUserById(user.id)
        directoryUser.setMultiFactorFailedAttemptCount(maxAttempts)
        directoryUser.setMultiFactorLastFailedTimestamp(new Date())
        userDao.updateUserAsIs(directoryUser)
        userById = utils.getUserById(user.id)

        then: "the user's mfa state is 'LOCKED'"
        userById.multiFactorState == MultiFactorStateEnum.LOCKED

        when: "unlock the mfa user"
        settings = v2Factory.createMultiFactorSettings(null, true)
        cloud20.updateMultiFactorSettings(utils.getServiceAdminToken(), user.id, settings)
        userById = utils.getUserById(user.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        cleanup:
        multiFactorService.removeMultiFactorForUser(user.id)  //remove duo profile
        deleteUserQuietly(user)
    }

    def "user-admin and user-manage should be able to mfa unlock user's in their domain"() {
        given:
        def maxAttempts = 3
        def autoUnlockSeconds = 1800
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_ATTEMPTS_MAX_PROP, maxAttempts)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MULTIFACTOR_LOCKING_LOGIN_FAILURE_TTL_PROP, autoUnlockSeconds)

        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)
        def userManageToken = utils.getToken(userManage.username, DEFAULT_PASSWORD)
        def defaultUserToken = utils.getToken(defaultUser.username, DEFAULT_PASSWORD)

        def settings = v2Factory.createMultiFactorSettings(true)
        def responsePhone = addPhone(defaultUserToken, defaultUser)
        utils.addApiKeyToUser(defaultUser)
        defaultUserToken = utils.authenticateApiKey(defaultUser.username).token.id
        cloud20.updateMultiFactorSettings(defaultUserToken, defaultUser.id, settings)

        when: "loading the user through the api"
        def userById = utils.getUserById(defaultUser.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "loading the user from the directory"
        def directoryUser = userDao.getUserById(defaultUser.id)

        then: "the user's mfa state is 'ACTIVE'"
        directoryUser.multiFactorState == BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE

        when: "user mfa state is updated to 'LOCKED'"
        directoryUser = userDao.getUserById(defaultUser.id)
        directoryUser.setMultiFactorFailedAttemptCount(maxAttempts)
        directoryUser.setMultiFactorLastFailedTimestamp(new Date())
        directoryUser.multiFactorState = BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED
        userDao.updateUserAsIs(directoryUser)
        userById = utils.getUserById(defaultUser.id)

        then: "the user's mfa state is 'LOCKED'"
        userById.multiFactorState == MultiFactorStateEnum.LOCKED

        when: "the user admin unlocks the user"
        settings = v2Factory.createMultiFactorSettings(null, true)
        cloud20.updateMultiFactorSettings(userAdminToken, defaultUser.id, settings)
        userById = utils.getUserById(defaultUser.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        when: "user mfa stat is updated back to 'LOCKED'"
        directoryUser = userDao.getUserById(defaultUser.id)
        directoryUser.setMultiFactorFailedAttemptCount(maxAttempts)
        directoryUser.setMultiFactorLastFailedTimestamp(new Date())
        directoryUser.multiFactorState = BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED
        userDao.updateUserAsIs(directoryUser)
        userById = utils.getUserById(defaultUser.id)

        then: "the user's mfa state is 'LOCKED'"
        userById.multiFactorState == MultiFactorStateEnum.LOCKED

        when: "the user manager unlocks the user"
        settings = v2Factory.createMultiFactorSettings(null, true)
        cloud20.updateMultiFactorSettings(userManageToken, defaultUser.id, settings)
        userById = utils.getUserById(defaultUser.id)

        then: "the user's mfa state is 'ACTIVE'"
        userById.multiFactorState == MultiFactorStateEnum.ACTIVE

        cleanup:
        multiFactorService.removeMultiFactorForUser(defaultUser.id)  //remove duo profile
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def addPhone(token, user, verify=true) {
        def responsePhone = utils.addPhone(token, user.id)
        utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
        if(verify) {
            def constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
            utils.verifyPhone(token, user.id, responsePhone.id, constantVerificationCode)
        }
        responsePhone
    }

    def void verifyMfaStateForUser(user, mediaType, expectedResult) {
        if(mediaType == MediaType.APPLICATION_XML_TYPE) {
            user = user.value
        }
        user.multiFactorState == expectedResult
    }

    def void verifyMfaStateForUsers(users, mediaType, expectedResult) {
        if(mediaType == MediaType.APPLICATION_XML_TYPE) {
            users = users.getEntity(UserList).value
            if(expectedResult == null) {
                assert users.user[0].multiFactorState == expectedResult
            } else {
                assert users.user[0].multiFactorState.toString() == expectedResult
            }
        } else {
            users = new JsonSlurper().parseText(users.getEntity(String))
            if(expectedResult == null) {
                assert users.users[0].containsKey(JSONConstants.RAX_AUTH_MULTI_FACTOR_STATE) == false
            } else {
                assert users.users[0][JSONConstants.RAX_AUTH_MULTI_FACTOR_STATE] == expectedResult
            }
        }
    }

}
