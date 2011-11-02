package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
public interface UserGroupService {
    Groups getGroups(Integer mossoAccountId);
    GroupsList getGroupList(Integer mossoAccountId);
}
