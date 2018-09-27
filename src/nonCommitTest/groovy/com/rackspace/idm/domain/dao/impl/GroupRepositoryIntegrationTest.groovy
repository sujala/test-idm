package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.GroupDao
import com.rackspace.idm.domain.entity.Group
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Specification

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class GroupRepositoryIntegrationTest extends Specification {
    @Shared
    def random

    @Autowired
    GroupDao groupDao

    @Autowired Configuration config

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "group crud"() {
        given:
        def groupId = "$random"
        def groupName = "group$random"
        def groupToCreate = getGroup(groupId, groupName, "description")
        def groupToUpdate = getGroup(groupId, "name2", "description2")

        when:
        groupDao.addGroup(groupToCreate)
        def createdGroup = groupDao.getGroupByName(groupName)


        groupDao.updateGroup(groupToUpdate)
        def updatedGroup = groupDao.getGroupById(groupId)

        def groupsWithGroup = groupDao.getGroups().collect()

        groupDao.deleteGroup(groupId)

        def groupsWithoutGroup = groupDao.getGroups().collect()

        then:
        groupToCreate == createdGroup
        groupToUpdate == updatedGroup

        groupsWithGroup.contains(updatedGroup)
        !groupsWithoutGroup.contains(updatedGroup)
    }

    def getGroup(id, name, description) {
        new Group().with {
            it.groupId = id
            it.name = name
            it.description = description
            return it
        }
    }
}
