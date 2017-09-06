package com.rackspace.idm.modules.usergroups.api.resource.converter

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups
import com.rackspace.idm.domain.config.ExternalBeansConfiguration
import org.dozer.Mapper
import spock.lang.Shared
import spock.lang.Specification

class UserGroupConverterComponentTest extends Specification {
    @Shared Mapper mapper
    @Shared UserGroupConverter userGroupConverter

    void setupSpec() {
        ExternalBeansConfiguration config = new ExternalBeansConfiguration()
        mapper = config.getMapper()
        userGroupConverter = new UserGroupConverter()
        userGroupConverter.mapper = mapper
    }

    def "converts group from web to entity"() {
        UserGroup webgroup = new UserGroup().with {
            it.id = "id"
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = userGroupConverter.fromUserGroupWeb(webgroup)

        then:
        entityGroup.id == webgroup.id
        entityGroup.domainId == webgroup.domainId
        entityGroup.name == webgroup.name
        entityGroup.description == webgroup.description
    }

    def "converts group from entity to web"() {
        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = "id"
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        UserGroup webgroup = userGroupConverter.toUserGroupWeb(entityGroup)

        then:
        entityGroup.id == webgroup.id
        entityGroup.domainId == webgroup.domainId
        entityGroup.name == webgroup.name
        entityGroup.description == webgroup.description
    }

    def "converts group list from entity to web"() {
        List<com.rackspace.idm.modules.usergroups.entity.UserGroup> entityGroups = new ArrayList<>()
        4.times { index ->
            com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
                it.id = index
                it.domainId = "domainId"
                it.name = "name_$index"
                it.description = "description_$index"
                it
            }
            entityGroups.add(entityGroup)
        }

        when:
        UserGroups webgroups = userGroupConverter.toUserGroupsWeb(entityGroups)

        then:
        webgroups.userGroup.size() == entityGroups.size()
        webgroups.userGroup.eachWithIndex {webgroup, index ->
            def entityGroup = entityGroups.get(index)
            webgroup.id == entityGroup.id
            webgroup.domainId == entityGroup.domainId
            webgroup.name == entityGroup.name
            webgroup.description == entityGroup.description
        }
    }
}
