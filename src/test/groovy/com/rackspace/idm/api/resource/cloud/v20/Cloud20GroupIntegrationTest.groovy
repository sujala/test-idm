package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
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

    def "group name on create is limited to 64 chars"() {
        given:
        def groupName63 = testUtils.getRandomUUIDOfLength("group", 63)
        def groupName64 = testUtils.getRandomUUIDOfLength("group", 64)
        def groupName65 = testUtils.getRandomUUIDOfLength("group", 65)
        def group63 = v1Factory.createGroup(groupName63, "description")
        def group64 = v1Factory.createGroup(groupName64, "description")
        def group65 = v1Factory.createGroup(groupName65, "description")

        when: "create a group with name length < max"
        def response = cloud20.createGroup(utils.getServiceAdminToken(), group63)

        then:
        response.status == 201
        def createdGroup63 = response.getEntity(Group).value
        createdGroup63.name == groupName63

        when: "create a group with name length == max"
        response = cloud20.createGroup(utils.getServiceAdminToken(), group64)

        then:
        response.status == 201
        def createdGroup64 = response.getEntity(Group).value
        createdGroup64.name == groupName64

        when: "create a group with name length > max"
        response = cloud20.createGroup(utils.getServiceAdminToken(), group65)

        then:
        response.status == 400

        cleanup:
        utils.deleteGroup(createdGroup63)
        utils.deleteGroup(createdGroup64)
    }

    def "group name on update is limited to 64 chars"() {
        given:
        def group = utils.createGroup()
        def groupName63 = testUtils.getRandomUUIDOfLength("group", 63)
        def groupName64 = testUtils.getRandomUUIDOfLength("group", 64)
        def groupName65 = testUtils.getRandomUUIDOfLength("group", 65)
        def group63 = v1Factory.createGroup(groupName63, group.id, "description")
        def group64 = v1Factory.createGroup(groupName64, group.id, "description")
        def group65 = v1Factory.createGroup(groupName65, group.id, "description")

        when: "create a group with name length < max"
        def response = cloud20.updateGroup(utils.getServiceAdminToken(), group.id, group63)

        then:
        response.status == 200
        def createdGroup63 = response.getEntity(Group).value
        createdGroup63.name == groupName63

        when: "create a group with name length == max"
        response = cloud20.updateGroup(utils.getServiceAdminToken(), group.id, group64)

        then:
        response.status == 200
        def createdGroup64 = response.getEntity(Group).value
        createdGroup64.name == groupName64

        when: "create a group with name length > max"
        response = cloud20.updateGroup(utils.getServiceAdminToken(), group.id, group65)

        then:
        response.status == 400

        cleanup:
        utils.deleteGroup(group)
    }

}
