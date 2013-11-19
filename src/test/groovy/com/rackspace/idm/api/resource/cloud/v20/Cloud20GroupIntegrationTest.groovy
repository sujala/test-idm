package com.rackspace.idm.api.resource.cloud.v20

import testHelpers.RootIntegrationTest

class Cloud20GroupIntegrationTest extends RootIntegrationTest {


    def "Updating a group with invalid Id should return 400" () {
        given:
        def group = v1Factory.createGroup(getRandomUUID('group'), "description")
        group.id = "badId"

        when:
        def entity = utils.createGroup()
        def updateResponse = cloud20.updateGroup(utils.getServiceAdminToken(), entity.id, group)

        then:
        updateResponse != null
        updateResponse.status == 400

        cleanup:
        utils.deleteGroup(entity)
    }

    def "Update invalid group should return 404" () {
        given:
        def group = v1Factory.createGroup(getRandomUUID('group'), "description")

        when:
        def updateResponse = cloud20.updateGroup(utils.getServiceAdminToken(), testUtils.getRandomIntegerString(), group)

        then:
        updateResponse.status == 404
    }
}
