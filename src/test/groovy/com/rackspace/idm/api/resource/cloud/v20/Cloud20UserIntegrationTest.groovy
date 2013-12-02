package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
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

    def "User admin within the same domain should not be allowed to retrieve other user admin's apiKey" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo) = utils.createUsers(domainId2)
        (identityAdminThree, userAdminThree, userManageThree, defaultUserThree) = utils.createUsers(domainId2)

        when:
        String token = utils.getToken(userAdmin.username)
        String userAdminThreeToken = utils.getToken(userAdminThree.username)
        def credentials = utils.addApiKeyToUser(userAdminTwo)
        def userAdminThreeResponse = cloud20.getUserApiKey(userAdminThreeToken, userAdminTwo.id)
        def response = cloud20.getUserApiKey(token, userAdminTwo.id)

        then:
        credentials.username == userAdminTwo.username
        userAdminThreeResponse.status == 403
        response.status == 403


        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUserTwo, defaultUserThree, userManageTwo, userManageThree, userAdminTwo, userAdminThree, identityAdminTwo, identityAdminThree)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
    }

    def "User manage within the same domain should not be allowed to retrieve other user manage's apiKey" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdminTwo, userAdminTwo, userManageTwo, defaultUserTwo) = utils.createUsers(domainId2)
        (identityAdminThree, userAdminThree, userManageThree, defaultUserThree) = utils.createUsers(domainId2)

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
        utils.deleteUsers(defaultUserTwo, defaultUserThree, userManageTwo, userManageThree, userAdminTwo, userAdminThree, identityAdminTwo, identityAdminThree)
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

}
