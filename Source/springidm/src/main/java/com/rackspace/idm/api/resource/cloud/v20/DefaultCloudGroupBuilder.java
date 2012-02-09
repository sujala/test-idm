package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 8, 2012
 * Time: 10:52:57 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultCloudGroupBuilder implements CloudGroupBuilder{
    public Group build(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group){
        Group groupDO = new Group();
        groupDO.setDescription(group.getDescription());
        groupDO.setName(group.getName());
        if(group.getId() != null){
            groupDO.setGroupId(Integer.parseInt(group.getId()));
        }

        return groupDO;
    }
}
