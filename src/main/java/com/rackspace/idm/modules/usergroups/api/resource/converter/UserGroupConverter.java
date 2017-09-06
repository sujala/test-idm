package com.rackspace.idm.modules.usergroups.api.resource.converter;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between the UserGroup "LDAP Entity" and the UserGroup "REST request/response" entity
 */
@Component
public class UserGroupConverter {
    @Autowired
    private Mapper mapper;

    /**
     * Converts from the request/response web service representation of a userGroup to the LDAP based representation.
     *
     * @param userGroupWeb
     * @return
     */
    public com.rackspace.idm.modules.usergroups.entity.UserGroup fromUserGroupWeb(UserGroup userGroupWeb) {
        com.rackspace.idm.modules.usergroups.entity.UserGroup userGroup = mapper.map(userGroupWeb, com.rackspace.idm.modules.usergroups.entity.UserGroup.class);
        return userGroup;
    }

    /**
     * Converts from the LDAP representation of a userGroup to the request/response web service based representation.
     *
     * @param userGroupEntity
     * @return
     */
    public UserGroup toUserGroupWeb(com.rackspace.idm.modules.usergroups.entity.UserGroup userGroupEntity) {
        UserGroup phone = mapper.map(userGroupEntity, UserGroup.class);
        return phone;
    }

     /**
     * Converts from the LDAP representation of a userGroup list to the request/response web service based representation.
     *
     * @param userGroupEntityList
     * @return
     */
    public UserGroups toUserGroupsWeb(List<com.rackspace.idm.modules.usergroups.entity.UserGroup> userGroupEntityList) {
        UserGroups userGroups = new UserGroups();
        for (com.rackspace.idm.modules.usergroups.entity.UserGroup userGroup : userGroupEntityList) {
            userGroups.getUserGroup().add(toUserGroupWeb(userGroup));
        }
        return userGroups;
    }

}
