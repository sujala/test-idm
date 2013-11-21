package com.rackspace.idm.api.resource.cloud.v20

import testHelpers.RootIntegrationTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 11/21/13
 * Time: 10:34 AM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20GroupIntegrationTest extends RootIntegrationTest {


    def "Updating a group with invalid Id should return 400" () {
        given:
        def group = v1Factory.createGroup(getRandomUUID('group'), "description")

        when:
        def entity = utils.createGroup(group)
        group.id = "badId"
        def updateResponse = cloud20.updateGroup(utils.getServiceAdminToken(), entity.id, group)

        then:
        updateResponse != null
        updateResponse.status == 400
    }

    def "Update invalid group should return 404" () {
        given:
        def group = v1Factory.createGroup(getRandomUUID('group'), "description")

        when:
        def updateResponse = cloud20.updateGroup(utils.getServiceAdminToken(), utils.getRandomIntegerString(), group)

        then:
        updateResponse.status == 404
    }
}
