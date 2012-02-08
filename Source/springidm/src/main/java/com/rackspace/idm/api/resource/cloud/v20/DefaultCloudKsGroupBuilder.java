package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 8, 2012
 * Time: 11:30:58 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultCloudKsGroupBuilder implements CloudKsGroupBuilder{
    public com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group build(Group group){
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groupKs.setName(group.getName());
        groupKs.setDescription(group.getDescription());
        groupKs.setId(group.getGroupId().toString());
        return groupKs;
    }
}
