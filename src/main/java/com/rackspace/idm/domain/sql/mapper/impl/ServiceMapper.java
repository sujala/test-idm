package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.sql.entity.SqlService;
import com.rackspace.idm.domain.sql.entity.SqlServiceRax;
import com.rackspace.idm.domain.sql.entity.SqlUser;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;
import org.springframework.data.domain.Page;

import java.util.Arrays;
import java.util.List;

@SQLComponent
public class ServiceMapper extends SqlRaxMapper<Application, SqlService, SqlServiceRax> {

    private static final String FORMAT = "clientId=%s,ou=applications,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlService sqlService) {
        return new String[] {sqlService.getClientId()};
    }

    @Override
    public List<String> getExtraAttributes() {
        return Arrays.asList("description", "name");
    }

}
