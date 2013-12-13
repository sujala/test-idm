package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class GroupConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private GroupService groupService;

    public Groups toGroupListJaxb(HashSet<String> groupIds) {
        Groups jaxbGroups = objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups();
        if (groupIds == null) {
            return jaxbGroups;
        }

        for (String groupId : groupIds) {
            com.rackspace.idm.domain.entity.Group groupEntity = groupService.getGroupById(groupId);

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = null;
            if (groupEntity != null) {
                group = objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup();
                group.setId(groupEntity.getGroupId());
                group.setName(groupEntity.getName());
                group.setDescription(groupEntity.getDescription());
            }

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
            com.rackspace.idm.domain.entity.Group groupEntity = groupService.getGroupByName(group.getName());

            if (groupEntity != null) {
                groupIds.add(groupEntity.getGroupId());
            }
            else {
                groupIds.add(group.getName());
            }
        }

        return groupIds;
    }
}
