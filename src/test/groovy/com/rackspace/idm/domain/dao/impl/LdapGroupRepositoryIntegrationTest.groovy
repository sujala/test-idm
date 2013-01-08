package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Group
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

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
    }

    def "can get group with duplicate name"() {
        given:
        def groupName = "group$random"

        when:
        groupRepository.addGroup(getGroup(1010101, groupName, ""))
        groupRepository.addGroup(getGroup(1010102, groupName, ""))

        def group = null
        try {
            group = groupRepository.getGroupByName(groupName)
        } catch (e) {
        }

        groupRepository.deleteGroup(1010101)
        groupRepository.deleteGroup(1010102)

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
