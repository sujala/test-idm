package com.rackspace.idm.api.resource.cloud.migration

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.api.resource.cloud.MigrationClient
import com.rackspace.idm.domain.service.GroupService
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/28/13
 * Time: 1:56 PM
 * To change this template use File | Settings | File Templates.
 */
class CloudMigrationServiceTest extends Specification{
    @Shared CloudMigrationService cloudMigrationService
    @Shared Configuration config
    @Shared  MigrationClient client
    @Shared GroupService cloudGroupService

    def setupSpec(){
        cloudMigrationService = new CloudMigrationService()
    }

    def setup(){
        config = Mock()
        client = Mock()
        cloudGroupService = Mock()

        cloudMigrationService.config = config
        cloudMigrationService.client = client
        cloudMigrationService.cloudGroupService = cloudGroupService
    }

    def "Do not allow duplicate groups with the same name to be migrated" () {
        given:
        Groups groups = new Groups()
        groups.group = [createGroup("1","groupName", "desc")].asList()
        config.getString("cloudAuth20url") >> "https://auth.staging.us.ccp.rackspace.net/v2.0/"
        config.getString("cloudAuth11url") >> "https://auth.staging.us.ccp.rackspace.net/v1.1/"
        client.getGroups(_) >> groups
        cloudGroupService.getGroupByName(_) >> createEntityGroup("1", "groupName", "new desc")

        when:
        cloudMigrationService.addOrUpdateGroups("someToken")

        then:
        1 * cloudGroupService.updateGroup(_)
    }

    def "Create new group if not found." () {
        given:
        Groups groups = new Groups()
        groups.group = [createGroup("1","groupName", "desc")].asList()
        config.getString("cloudAuth20url") >> "https://auth.staging.us.ccp.rackspace.net/v2.0/"
        config.getString("cloudAuth11url") >> "https://auth.staging.us.ccp.rackspace.net/v1.1/"
        client.getGroups(_) >> groups
        cloudGroupService.getGroupByName(_) >> {throw new NotFoundException()}

        when:
        cloudMigrationService.addOrUpdateGroups("someToken")

        then:
        1 * cloudGroupService.insertGroup(_)
    }

    def "addUserGroups adds group based on name"() {
        given:
        String userId = "userId"
        Group group = createGroup("id", "name", "desc")
        Groups groups = new Groups().with {
            it.group = [group].asList()
            return it
        }
        def groupId = "2"

        when:
        cloudMigrationService.addUserGroups(userId, groups)

        then:
        1 * config.getString("cloud.region") >> "US"
        1 * cloudGroupService.getGroupByName("name") >> {
            createEntityGroup(groupId, "name", "desc")
        }
        1 * cloudGroupService.addGroupToUser(groupId, userId)
    }

    def "addUserGroups does not add user to group if group is missing"() {
        given:
        String userId = "userId"
        Group group = createGroup("id", "name", "desc")
        Groups groups = new Groups().with {
            it.group = [group].asList()
            return it
        }

        when:
        cloudMigrationService.addUserGroups(userId, groups)

        then:
        1 * config.getString("cloud.region") >> "US"
        1 * cloudGroupService.getGroupByName("name") >> {
            throw new NotFoundException()
        }
        0 * cloudGroupService.addGroupToUser(_, _)

        then:
        notThrown(NotFoundException)
    }

    def createEntityGroup(String id, String name, String description) {
        new com.rackspace.idm.domain.entity.Group().with {
            it.groupId = id
            it.name = name
            it.description = description
            return it
        }
    }

    def createGroup(String id, String name, String description) {
        new Group().with {
            it.id = id
            it.name = name
            it.description = description
            return it
        }
    }


}
