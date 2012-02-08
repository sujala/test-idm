package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 8, 2012
 * Time: 10:41:26 AM
 * To change this template use File | Settings | File Templates.
 */
public interface CloudGroupBuilder {
    Group build(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group); 
}
