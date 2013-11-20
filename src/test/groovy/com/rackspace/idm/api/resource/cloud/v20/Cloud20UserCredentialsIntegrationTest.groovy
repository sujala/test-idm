package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.User
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

    @Shared def serviceAdminToken

    def setup(){
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        serviceAdminToken = utils.getServiceAdminToken()
    }

    def cleanup(){
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "No one should be allowed retrieve user's password" () {
        given:
        def users = [identityAdmin, userAdmin, userManage, defaultUser].asList()

        when:
        for(User user : users) {
            String token = utils.getToken(user.username)
            def response = cloud20.getPasswordCredentials(token, defaultUser.id)
            assert (response.status == 403)
        }
        def serviceAdminResponse = cloud20.getPasswordCredentials(serviceAdminToken, defaultUser.id)

        then:
        serviceAdminResponse.status == 403
    }
}
