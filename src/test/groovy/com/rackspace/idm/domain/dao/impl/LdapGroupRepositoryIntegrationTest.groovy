package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Group
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapGroupRepositoryIntegrationTest extends Specification {
    @Shared
    def random

    @Autowired
    LdapGroupRepository groupRepository

    @Autowired Configuration config

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "getNextId returns UUID"() {
        given:
        def success = false
        groupRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",true)

        when:
        def id = groupRepository.getNextId(LdapRepository.NEXT_GROUP_ID)
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "getNextId returns Long"() {
        given:
        def success = false
        groupRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",false)

        when:
        def id = groupRepository.getNextId(LdapRepository.NEXT_GROUP_ID)
        try {
            Long.parseLong(id)
            success = true
        } catch (Exception) {
            //no-op
        }

        then:
        success == true


        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "group crud"() {
        given:
        def groupId = "$random"
        def groupName = "group$random"
        def groupToCreate = getGroup(groupId, groupName, "description")
        def groupToUpdate = getGroup(groupId, "name2", "description2")

        when:
        groupRepository.addGroup(groupToCreate)
        def createdGroup = groupRepository.getGroupByName(groupName)


        groupRepository.updateGroup(groupToUpdate)
        def updatedGroup = groupRepository.getGroupById(groupId)

        def groupsWithGroup = groupRepository.getGroups().collect()

        groupRepository.deleteGroup(groupId)

        def groupsWithoutGroup = groupRepository.getGroups().collect()

        then:
        groupToCreate == createdGroup
        groupToUpdate == updatedGroup

        groupsWithGroup.contains(updatedGroup)
        !groupsWithoutGroup.contains(updatedGroup)
    }

    def "can get group with duplicate name"() {
        given:
        def groupName = "group$random"

        when:
        groupRepository.addGroup(getGroup("1010101", groupName, ""))
        groupRepository.addGroup(getGroup("1010102", groupName, ""))

        def group = null
        try {
            group = groupRepository.getGroupByName(groupName)
        } catch (e) {
        }

        groupRepository.deleteGroup("1010101")
        groupRepository.deleteGroup("1010102")

        then:
        group != null
        group.name == groupName
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
