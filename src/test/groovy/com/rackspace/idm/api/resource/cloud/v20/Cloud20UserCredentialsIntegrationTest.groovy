package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 11/19/13
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20UserCredentialsIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    @Shared def serviceAdminToken, identityAdminToken, userAdminToken, userManageToken, defaultUserToken

    def setup(){
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        //Tokens
        serviceAdminToken = utils.getServiceAdminToken()
        identityAdminToken = utils.getToken(identityAdmin.username)
        userAdminToken = utils.getToken(userAdmin.username)
        userManageToken = utils.getToken(userManage.username)
        defaultUserToken = utils.getToken(defaultUser.username)
    }

    def cleanup(){
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "No one should be allowed retrieve user's password" () {
        when:
        def serviceAdminResponse = cloud20.getPasswordCredentials(serviceAdminToken, defaultUser.id)
        def identityAdminResponse = cloud20.getPasswordCredentials(identityAdminToken, defaultUser.id)
        def userAdminResponse = cloud20.getPasswordCredentials(userAdminToken, defaultUser.id)
        def userManageResponse = cloud20.getPasswordCredentials(userManageToken, defaultUser.id)
        def defaultUserResponse = cloud20.getPasswordCredentials(defaultUserToken, defaultUser.id)

        then:
        serviceAdminResponse.status == 403
        identityAdminResponse.status == 403
        userAdminResponse.status == 403
        userManageResponse.status == 403
        defaultUserResponse.status == 403
    }
}
