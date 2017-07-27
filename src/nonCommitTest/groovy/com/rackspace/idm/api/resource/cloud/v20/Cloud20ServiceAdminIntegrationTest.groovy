package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import testHelpers.RootIntegrationTest

class Cloud20ServiceAdminIntegrationTest extends RootIntegrationTest {

    @Shared def serviceAdminToken1
    @Shared def serviceAdmin1
    @Shared def serviceAdminToken2
    @Shared def serviceAdmin2
    @Shared def identityAdminToken
    @Shared def identityAdmin
    @Shared def userAdminToken
    @Shared def userAdmin
    @Shared def userManageToken
    @Shared def userManage
    @Shared def defaultUserToken
    @Shared def defaultUser

    def "other users cannot list roles on another service admin"() {
        given:
        serviceAdminToken1 = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        serviceAdminToken2 = utils.getToken(Constants.SERVICE_ADMIN_2_USERNAME, Constants.SERVICE_ADMIN_2_PASSWORD)
        serviceAdmin1 = cloud20.getUserByName(serviceAdminToken1, Constants.SERVICE_ADMIN_USERNAME).getEntity(User).value
        serviceAdmin2 = cloud20.getUserByName(serviceAdminToken2, Constants.SERVICE_ADMIN_2_USERNAME).getEntity(User).value
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(utils.createDomain())
        identityAdminToken = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        userAdminToken = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        userManageToken = utils.getToken(userManage.username, Constants.DEFAULT_PASSWORD)
        defaultUserToken = utils.getToken(defaultUser.username, Constants.DEFAULT_PASSWORD)
        def tokens = [serviceAdminToken1, serviceAdminToken1, identityAdminToken, userAdminToken, userManageToken, defaultUserToken]
        def status = []

        when:
        for(def token : tokens) {
            status << cloud20.listUserGlobalRoles(token, serviceAdmin2.id).status == status
        }

        then:
        for(def curStatus : status) {
            curStatus == 403
        }
    }

    def "service admin should be able to list roles on self"() {
        given:
        serviceAdminToken1 = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        serviceAdmin1 = cloud20.getUserByName(serviceAdminToken1, Constants.SERVICE_ADMIN_USERNAME).getEntity(User).value

        when:
        def status = cloud20.listUserGlobalRoles(serviceAdminToken1, serviceAdmin1.id).status

        then:
        status == 200
    }

}
