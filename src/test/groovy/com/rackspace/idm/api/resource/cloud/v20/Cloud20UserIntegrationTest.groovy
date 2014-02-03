package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.UserDao
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.springframework.beans.factory.annotation.Autowired
import org.openstack.docs.identity.api.v2.CredentialListType
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.Constants.DEFAULT_RAX_KSQA_SECRET_ANWSER
import static com.rackspace.idm.Constants.DEFAULT_RAX_KSQA_SECRET_QUESTION
import static com.rackspace.idm.Constants.DEFAULT_SECRET_ANWSER
import static com.rackspace.idm.Constants.MOSSO_ROLE_ID
import static com.rackspace.idm.Constants.SERVICE_ADMIN_USERNAME
import static com.rackspace.idm.Constants.USER_MANAGE_ROLE_ID

/**
 * Created with IntelliJ IDEA
 * User: jorge
 * Date: 11/19/13
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20UserIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo
    @Shared def identityAdminThree, userAdminThree, userManageThree, defaultUserThree
    @Shared def domainId

    @Autowired
    UserDao userDao


    def "Update user with password populated in request will expire all tokens" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def user = new UserForCreate().with {
            it.id = userAdmin.id
            it.password = DEFAULT_PASSWORD
            it
        }

        when:
        def token = utils.getToken(userAdmin.username)
        utils.validateToken(token)
        utils.updateUser(user)
        def response = cloud20.validateToken(utils.getServiceAdminToken(), token)

        then:
        response.status == 404

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Adding group to default/manage user should return 400" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def group = utils.createGroup()
        def responseDefaultUser = cloud20.addUserToGroup(utils.getServiceAdminToken(), group.id, defaultUser.id)
        def responseManageUser = cloud20.addUserToGroup(utils.getServiceAdminToken(), group.id, userManage.id)

        then:
        responseDefaultUser.status == 400
        responseManageUser.status == 400

        cleanup:
        utils.deleteGroup(group)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Adding group to default/manage user after adding it to user admin should return 400" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def group = utils.createGroup()
        def responseUserAdmin = cloud20.addUserToGroup(utils.getServiceAdminToken(), group.id, userAdmin.id)
        def responseManageUser = cloud20.addUserToGroup(utils.getServiceAdminToken(), group.id, userManage.id)
        def responseDefaultUser = cloud20.addUserToGroup(utils.getServiceAdminToken(), group.id, defaultUser.id)

        then:
        responseUserAdmin.status == 204
        responseManageUser.status == 400
        responseDefaultUser.status == 400

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteGroup(group)
        utils.deleteDomain(domainId)
    }

    def "Not Authorized request for adding a user to a group" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userList = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        def group = utils.createGroup()
        def defaultUserToken = utils.getToken(defaultUser.username)
        for(def user : userList) {
            def response = cloud20.addUserToGroup(defaultUserToken, group.id, user.id)
            assert (response.status == 403)
        }
        def userManageToken = utils.getToken(userManage.username)
        for(def user : userList) {
            def response = cloud20.addUserToGroup(userManageToken, group.id, user.id)
            assert (response.status == 403)
        }
        def userAdminToken = utils.getToken(userAdmin.username)
        for(def user : userList) {
            def response = cloud20.addUserToGroup(userAdminToken, group.id, user.id)
            assert (response.status == 403)
        }
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def serviceAdmin = utils.getUserByName(SERVICE_ADMIN_USERNAME)
        def response = cloud20.addUserToGroup(identityAdminToken, group.id, serviceAdmin.id)

        then:
        true
        response.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteGroup(group)
        utils.deleteDomain(domainId)
    }

    def "Verify users at same level cannot assign groups" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo) = utils.createUsers(domainId2)

        when:
        def group = utils.createGroup()
        def adminToken = utils.getToken(identityAdmin.username)
        def responseOne = cloud20.addUserToGroup(adminToken, group.id, identityAdminTwo.id)

        then:
        responseOne.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUserTwo, userManageTwo, userAdminTwo, identityAdminTwo)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)

    }

    def "Valid request for adding a user to a group" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo) = utils.createUsers(domainId2)
        def userList = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        def group = utils.createGroup()
        for(def user: userList) {
            if(!user.username.equals(userManage.username)
               && !user.username.equals(defaultUser.username)){
                utils.addUserToGroup(group, user)
            }
        }
        def groupTwo = utils.createGroup()
        utils.addUserToGroup(groupTwo, userAdmin, utils.getIdentityAdminToken())

        then:
        true

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUserTwo, userManageTwo, userAdminTwo, identityAdminTwo)
        utils.deleteGroup(group)
        utils.deleteGroup(groupTwo)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
    }

    def "User manage within the same domain should not be allowed to retrieve other user manage's apiKey" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo) = utils.createUsers(domainId2)

        def userAdminTwoToken = utils.getToken(userManageTwo.username)
        def userManageThree =  utils.createUser(userAdminTwoToken, testUtils.getRandomUUID("userManage"), domainId2)

        when:
        utils.addRoleToUser(userManageTwo, USER_MANAGE_ROLE_ID)
        utils.addRoleToUser(userManageThree, USER_MANAGE_ROLE_ID)
        String userManageThreeToken = utils.getToken(userManageThree.username)
        String token = utils.getToken(userManage.username)
        def credentials = utils.addApiKeyToUser(userManageTwo)
        def userManageThreeResponse = cloud20.getUserApiKey(userManageThreeToken, userManageTwo.id)
        def response = cloud20.getUserApiKey(token, userManageTwo.id)

        then:
        credentials.username == userManageTwo.username
        userManageThreeResponse.status == 403
        response.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUserTwo, userManageTwo, userManageThree, userAdminTwo, identityAdminTwo)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
    }

    def "Valid requests for get user's apiKey" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(def user : users){
            utils.addApiKeyToUser(user)
        }

        def identityAdminToken = utils.getToken(identityAdmin.username);
        for(def user : users){
            utils.getUserApiKey(user, identityAdminToken)
        }

        def userAdminToken = utils.getToken(userAdmin.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)){
                utils.getUserApiKey(user, userAdminToken)
            }
        }

        def userManageToken = utils.getToken(userManage.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)
            && !user.username.equals(userAdmin.username) ){
                utils.getUserApiKey(user, userManageToken)
            }
        }

        def defaultUserToken = utils.getToken(defaultUser.username)
        def response = utils.getUserApiKey(defaultUser, defaultUserToken)

        then:
        response != null
        response.apiKey != null
        response.username == defaultUser.username

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Invalid requests for get user's apiKey" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(def user : users){
            utils.addApiKeyToUser(user)
        }

        def defaultUserToken = utils.getToken(defaultUser.username)
        for(def user : users){
            if(!user.username.equals(defaultUser.username)){
                def response = cloud20.getUserApiKey(defaultUserToken, user.id)
                assert (response.status == 403)
            }
        }

        def userManageToken = utils.getToken(userManage.username);
        for(def user : users){
            if(!user.username.equals(defaultUser.username)
                    && !user.username.equals(userManage.username) ){
                def response = cloud20.getUserApiKey(userManageToken, user.id)
                assert (response.status == 403)
            }
        }

        def userAdminToken = utils.getToken(userAdmin.username);
        for(def user : users){
            if(!user.username.equals(defaultUser.username)
                    && !user.username.equals(userManage.username)
                    && !user.username.equals(userAdmin.username)){
                def response = cloud20.getUserApiKey(userAdminToken, user.id)
                assert (response.status == 403)
            }
        }

        def identityAdminToken = utils.getToken(identityAdmin.username);
        def serviceAdmin = utils.getUserByName(SERVICE_ADMIN_USERNAME)
        def response = cloud20.getUserApiKey(identityAdminToken, serviceAdmin.id)


        then:
        response.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Valid requests for list user's credentials" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(def user : users){
            utils.addApiKeyToUser(user)
        }

        def identityAdminToken = utils.getToken(identityAdmin.username);
        for(def user : users){
            def cred = utils.listUserCredentials(user, identityAdminToken)
            assert (cred != null)
        }

        def userAdminToken = utils.getToken(userAdmin.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)){
                utils.listUserCredentials(user, userAdminToken)
            }
        }

        def userManageToken = utils.getToken(userManage.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)
                    && !user.username.equals(userAdmin.username) ){
                utils.listUserCredentials(user, userManageToken)
            }
        }

        def defaultUserToken = utils.getToken(defaultUser.username)
        def response = utils.listUserCredentials(defaultUser, defaultUserToken)

        then:
        response != null

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Invalid requests for list user's credentials" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(def user : users){
            utils.addApiKeyToUser(user)
        }

        def defaultUserToken = utils.getToken(defaultUser.username)
        for(def user : users){
            if(!user.username.equals(defaultUser.username)){
                def response = cloud20.listCredentials(defaultUserToken, user.id)
                assert (response.status == 403)
            }
        }

        def userManageToken = utils.getToken(userManage.username);
        for(def user : users){
            if(!user.username.equals(defaultUser.username)
                    && !user.username.equals(userManage.username) ){
                def response = cloud20.listCredentials(userManageToken, user.id)
                assert (response.status == 403)
            }
        }

        def userAdminToken = utils.getToken(userAdmin.username);
        for(def user : users){
            if(!user.username.equals(defaultUser.username)
                    && !user.username.equals(userManage.username)
                    && !user.username.equals(userAdmin.username)){
                def response = cloud20.listCredentials(userAdminToken, user.id)
                assert (response.status == 403)
            }
        }

        def identityAdminToken = utils.getToken(identityAdmin.username);
        def serviceAdmin = utils.getUserByName(SERVICE_ADMIN_USERNAME)
        def response = cloud20.listCredentials(identityAdminToken, serviceAdmin.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Listing credentials - verify domain" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def username = testUtils.getRandomUUID("listCredentialsUser")

        when:
        def user = utils.createUser(utils.identityAdminToken, username, utils.createDomain())
        def token = utils.getToken(username)
        def response = cloud20.listCredentials(token, defaultUser.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, user)
        utils.deleteDomain(domainId)
    }

    def "Multi-factor status is exposed for get user calls"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def daoUser = userDao.getUserByUsername(defaultUser.username)
        daoUser.multifactorEnabled = multiFactorEnabled
        userDao.updateUser(daoUser)

        when:
        def userById = utils.getUserById(defaultUser.id)
        def userByUsername = utils.getUserByName(defaultUser.username)
        def userByEmail = utils.getUserByEmail(defaultUser.email)
        def users = utils.listUsers()

        then:
        userById.multiFactorEnabled == expectedResult
        userByUsername.multiFactorEnabled == expectedResult
        userByEmail.multiFactorEnabled == expectedResult
        for (def user : users) {
            if (user.id == defaultUser.id) {
                assert (user.multiFactorEnabled == expectedResult)
            }
        }

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        multiFactorEnabled  | expectedResult
        true                | true
        false               | false
    }

        def "Assigning user 'compute:default' global role should allow authentication" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        utils.addRoleToUser(userAdmin, MOSSO_ROLE_ID)
        def token = utils.getToken(userAdmin.username)

        then:
        token != null

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "update user does not set multiFactor enabled flag"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        userAdmin.multiFactorEnabled = true
        utils.updateUser(userAdmin)
        def retrievedUser = utils.getUserById(userAdmin.id)

        then:
        retrievedUser.multiFactorEnabled == false

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Update user's apikey - validate encryption" () {
        given:
        def domainId = utils.createDomain()
        def users
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        utils.addApiKeyToUser(defaultUser)
        def cred = utils.getUserApiKey(defaultUser)
        utils.authenticateApiKey(defaultUser, cred.apiKey)
        utils.resetApiKey(defaultUser)
        utils.addApiKeyToUser(defaultUser)
        def cred2 = utils.getUserApiKey(defaultUser)
        utils.authenticateApiKey(defaultUser, cred2.apiKey)
        def user = utils11.getUserByName(defaultUser.username)

        then:
        cred != null
        cred.apiKey != null
        cred2 != null
        cred2.apiKey != null
        user.key != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Update user's secretQA - validate encryption" () {
        given:
        def domainId = utils.createDomain()
        def users
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when:
        utils.createSecretQA(defaultUser)
        def secretQA = utils.getSecretQA(defaultUser)
        utils.authenticate(defaultUser)
        utils.updateSecretQA(defaultUser)
        def secretQA2 = utils.getSecretQA(defaultUser)
        utils.authenticate(defaultUser)

        then:
        secretQA != null
        secretQA.answer == DEFAULT_SECRET_ANWSER
        secretQA2 != null
        secretQA2.question == DEFAULT_RAX_KSQA_SECRET_QUESTION
        secretQA2.answer == DEFAULT_RAX_KSQA_SECRET_ANWSER

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
