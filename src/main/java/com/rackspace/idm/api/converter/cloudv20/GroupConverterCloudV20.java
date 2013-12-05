package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class GroupConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private GroupDao groupDao;

    public Groups toGroupListJaxb(HashSet<String> groupIds) {
        Groups jaxbGroups = objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups();
        if (groupIds == null) {
            return jaxbGroups;
        }

        for (String groupId : groupIds) {
            com.rackspace.idm.domain.entity.Group groupEntity = groupDao.getGroupById(groupId);

            if (groupEntity == null) {
                throw new BadRequestException(groupId + " not found.");
            }

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup();
            group.setId(groupEntity.getGroupId());
            group.setName(groupEntity.getName());
            group.setDescription(groupEntity.getDescription());
            jaxbGroups.getGroup().add(group);
        }

        return jaxbGroups;
    }

    public HashSet<String> toSetOfGroupIds(Groups groups) {
        if (groups == null) {
            return null;
        }

        HashSet<String> groupIds = new HashSet<String> ();
        for (Group group : groups.getGroup()) {
            com.rackspace.idm.domain.entity.Group groupEntity = groupDao.getGroupByName(group.getName());

            if (groupEntity == null) {
                throw new BadRequestException("group '" + group.getName() + "' does not exist.");
            }

            groupIds.add(groupEntity.getGroupId());
        }

        return groupIds;
    }
}
