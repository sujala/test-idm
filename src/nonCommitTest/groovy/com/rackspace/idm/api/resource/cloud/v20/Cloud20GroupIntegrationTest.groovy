package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import spock.lang.Unroll
import testHelpers.IdmAssert
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

    def "Test add group to user"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        Group group = v2Factory.createGroup(testUtils.getRandomUUID()).with{
            it.description = "Description"
            it
        }

        when: "Create group"
        def response = cloud20.createGroup(utils.getServiceAdminToken(), group)
        def groupEntity = response.getEntity(Group).value

        then: "Assert group id"
        response.status == HttpStatus.SC_CREATED
        // 32 length UUID without dashes
        testUtils.assertStringPattern("[a-zA-Z0-9]{32}", groupEntity.id)

        when: "Add group to user"
        utils.addUserToGroup(groupEntity, userAdmin)
        def groupsResponse = cloud20.listGroupsForUser(utils.getServiceAdminToken(), userAdmin.id)
        Groups groupsEntity = groupsResponse.getEntity(Groups).value

        then: "Assert group correctly added to user"
        groupsEntity.group.find{it.id == groupEntity.id} != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteGroup(groupEntity)
    }

    def "do not allow removing legacy group from sub users"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        // Add group to userAdmin
        def group = utils.createGroup()
        utils.addUserToGroup(group, userAdmin)

        when: "delete group from default user"
        def response = cloud20.removeUserFromGroup(utils.identityAdminToken, group.id, defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Cannot remove Sub-Users directly from a Group, must remove their Parent User.")

        when: "delete group from user manage"
        response = cloud20.removeUserFromGroup(utils.identityAdminToken, group.id, userManage.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Cannot remove Sub-Users directly from a Group, must remove their Parent User.")

        cleanup:
        utils.deleteUsersQuietly(users)
    }

}
