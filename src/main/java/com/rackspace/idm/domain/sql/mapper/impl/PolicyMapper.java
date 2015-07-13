package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.sql.entity.SqlPolicy;
import com.rackspace.idm.domain.sql.entity.SqlPolicyRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

import java.util.Arrays;
import java.util.List;

@SQLComponent
public class PolicyMapper extends SqlRaxMapper<Policy, SqlPolicy, SqlPolicyRax> {

    public List<String> getExtraAttributes() {
        return Arrays.asList("user_id", "project_id");
    }
}
