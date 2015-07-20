package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.sql.entity.SqlService;
import com.rackspace.idm.domain.sql.entity.SqlServiceRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

import java.util.Arrays;
import java.util.List;

@SQLComponent
public class ServiceMapper extends SqlRaxMapper<Application, SqlService, SqlServiceRax> {

    @Override
    public List<String> getExtraAttributes() {
        return Arrays.asList("description", "name");
    }
}
