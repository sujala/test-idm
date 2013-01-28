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

    def "Do not allow duplicate groups with the same name to be migrated" () {
        given:
        setMocks()
        Groups groups = new Groups()
        groups.group = [createGroup("1","groupName", "desc")].asList()
        config.getString("cloudAuth20url") >> "https://auth.staging.us.ccp.rackspace.net/v2.0/"
        config.getString("cloudAuth11url") >> "https://auth.staging.us.ccp.rackspace.net/v1.1/"
        client.getGroups(_) >> groups
        cloudGroupService.getGroupByName(_) >> createEntityGroup(1, "groupName", "new desc")

        when:
        cloudMigrationService.addOrUpdateGroups("someToken")

        then:
        1 * cloudGroupService.updateGroup(_)
    }

    def "Create new group if not found." () {
        given:
        setMocks()
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

    def createEntityGroup(Integer id, String name, String description) {
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

    def setMocks(){
        config = Mock()
        client = Mock()
        cloudGroupService = Mock()

        cloudMigrationService.config = config
        cloudMigrationService.client = client
        cloudMigrationService.cloudGroupService = cloudGroupService
    }
}
