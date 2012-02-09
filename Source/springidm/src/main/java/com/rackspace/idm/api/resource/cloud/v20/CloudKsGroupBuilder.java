package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 8, 2012
 * Time: 11:26:42 AM
 * To change this template use File | Settings | File Templates.
 */
public interface CloudKsGroupBuilder {
    com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group build(Group group);
}
