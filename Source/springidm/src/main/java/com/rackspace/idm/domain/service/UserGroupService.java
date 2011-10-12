package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:26 PM
 */
public interface UserGroupService {
    List<Group> getGroups(Integer mossoAccountId);
}
