package com.rackspace.idm.api.converter.cloudv20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.dao.GroupDao
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import testHelpers.RootServiceTest

class GroupConverterCloudV20Test extends RootServiceTest {
    @Shared GroupConverterCloudV20 converter
    @Shared GroupDao mockGroupDao

    def setupSpec() {
        converter = new GroupConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
    }

    def setup() {
        mockGroupDao(converter)
    }

    def "convert groupIds to jaxb Groups"() {
        given:
        HashSet<String> groupIds = new HashSet<String>()
        groupIds.add("groupId");

        def group = entityFactory.createGroup("groupId", "groupName", "groupDescription");

        and:
        mockGroupDao.getGroupById("groupId") >> group

        when:
        def result = converter.toGroupListJaxb(groupIds)

        then:
        result.getGroup().size() == 1

        def jaxbGroup = result.getGroup().get(0)
        jaxbGroup.id == group.groupId
        jaxbGroup.name == group.name
        jaxbGroup.description == group.description
    }

    def "convert groupIds to jaxb Groups throws exception when group id does not exist"() {
        given:
        HashSet<String> groupIds = new HashSet<String>()
        groupIds.add("groupId");

        and:
        mockGroupDao.getGroupById("groupId") >> null

        when:
        converter.toGroupListJaxb(groupIds)

        then:
        thrown(BadRequestException)
    }

    def "convert jaxbGroups to groupIds"() {
        given:
        def group = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup()
        def groups = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups()

        group.name = "groupName"
        groups.getGroup().add(group)

        and:
        mockGroupDao.getGroupByName(group.name) >> entityFactory.createGroup("groupId", group.name, "groupDescription");

        when:
        def result = converter.toSetOfGroupIds(groups)

        then:
        result.size() == 1
        result.contains("groupId")
    }

    def "convert jaxbGroups to groupIds throws exception when group does not exist"() {
        given:
        def group = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup()
        def groups = converter.objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups()

        group.name = "groupName"
        groups.getGroup().add(group)

        and:
        mockGroupDao.getGroupByName(group.name) >> null

        when:
        converter.toSetOfGroupIds(groups)

        then:
        thrown(BadRequestException)
    }

    def "convert jaxbGroups to groupIds when jaxbGroups is null"() {
        when:
        def result = converter.toSetOfGroupIds(null)

        then:
        result == null
    }

    def mockGroupDao(service) {
        mockGroupDao = Mock()
        service.groupDao = mockGroupDao
    }
}
