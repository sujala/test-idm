package com.rackspace.idm.api.converter.cloudv20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.service.GroupService
import spock.lang.Shared
import testHelpers.RootServiceTest

class GroupConverterCloudV20Test extends RootServiceTest {
    @Shared GroupConverterCloudV20 converter
    @Shared GroupService mockGroupService

    def setupSpec() {
        converter = new GroupConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
    }

    def setup() {
        mockGroupService(converter)
    }

    def "convert groupIds to jaxb Groups"() {
        given:
        HashSet<String> groupIds = new HashSet<String>()
        groupIds.add("groupId");

        def group = entityFactory.createGroup("groupId", "groupName", "groupDescription");

        and:
        mockGroupService.getGroupById("groupId") >> group

        when:
        def result = converter.toGroupListJaxb(groupIds)

        then:
        result.getGroup().size() == 1

        def jaxbGroup = result.getGroup().get(0)
        jaxbGroup.id == group.groupId
        jaxbGroup.name == group.name
        jaxbGroup.description == group.description
    }

    def "convert groupIds to jaxb Groups adds null entry to returned list"() {
        given:
        HashSet<String> groupIds = new HashSet<String>()
        groupIds.add("groupId");

        and:
        mockGroupService.getGroupById("groupId") >> null

        when:
        def groups = converter.toGroupListJaxb(groupIds)

        then:
        groups.getGroup().size() == 1
        groups.getGroup().get(0) == null
    }

    def "convert jaxbGroups to groupIds"() {
        given:
        def group = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup()
        def groups = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups()

        group.name = "groupName"
        groups.getGroup().add(group)

        and:
        mockGroupService.getGroupByName(group.name) >> entityFactory.createGroup("groupId", group.name, "groupDescription");

        when:
        def result = converter.toSetOfGroupIds(groups)

        then:
        result.size() == 1
        result.contains("groupId")
    }

    def "convert jaxbGroups to groupIds adds group name to list"() {
        given:
        def group = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup()
        def groups = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups()

        group.name = "groupName"
        groups.getGroup().add(group)

        and:
        mockGroupService.getGroupByName(group.name) >> null

        when:
        def groupIds = converter.toSetOfGroupIds(groups)

        then:
        groupIds.size() == 1
        groupIds.contains(group.name)
    }

    def "convert jaxbGroups to groupIds when jaxbGroups is null"() {
        when:
        def result = converter.toSetOfGroupIds(null)

        then:
        result == null
    }

    def mockGroupService(service) {
        mockGroupService = Mock()
        service.groupService = mockGroupService
    }
}
