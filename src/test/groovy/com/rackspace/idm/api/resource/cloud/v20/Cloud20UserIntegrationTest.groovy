package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
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

        when:
        def userAdminTwo = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin2"), domainId2)
        def userAdminThree = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin3"), domainId2)
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
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, userAdminTwo, userAdminThree)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
    }

    def "User manage within the same domain should not be allowed to retrieve other user manage's apiKey" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def userAdminTwo = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin2"), domainId2)
        def userAdminTwoToken = utils.getToken(userAdminTwo.username)
        def userManageTwo = utils.createUser(userAdminTwoToken, testUtils.getRandomUUID("userManage2"), domainId2)
        def userManageThree = utils.createUser(userAdminTwoToken, testUtils.getRandomUUID("userManage3"), domainId2)
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
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, userManageTwo, userManageThree, userAdminTwo)
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
            def response = cloud20.getUserApiKey(identityAdminToken, user.id)
            assert (response.status == 200)
        }

        def userAdminToken = utils.getToken(userAdmin.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)){
                def response = cloud20.getUserApiKey(userAdminToken, user.id)
                assert (response.status == 200)
            }
        }

        def userManageToken = utils.getToken(userManage.username);
        for(def user : users){
            if(!user.username.equals(identityAdmin.username)
            && !user.username.equals(userAdmin.username) ){
                def response = cloud20.getUserApiKey(userManageToken, user.id)
                assert (response.status == 200)
            }
        }

        def defaultUserToken = utils.getToken(defaultUser.username)
        def response = cloud20.getUserApiKey(defaultUserToken, defaultUser.id)

        then:
        response.status == 200

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }
}
