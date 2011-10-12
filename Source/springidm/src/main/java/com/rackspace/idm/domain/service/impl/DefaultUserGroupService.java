package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.domain.service.UserGroupService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
@Component
public class DefaultUserGroupService implements UserGroupService{
    @Override
    public List<Group> getGroups(Integer mossoAccountId) {
        //TODO write me
        throw new UnsupportedOperationException("not written -- com.rackspace.idm.domain.service.impl.DefaultUserGroupService.getGroups");
    }
}
