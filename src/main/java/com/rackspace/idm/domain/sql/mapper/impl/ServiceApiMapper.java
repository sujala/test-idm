package com.rackspace.idm.domain.sql.mapper.impl;


import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.ServiceApi;
import com.rackspace.idm.domain.sql.entity.SqlServiceApi;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class ServiceApiMapper extends SqlMapper<ServiceApi, SqlServiceApi> {

    private static final String FORMAT = "rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlServiceApi sqlServiceApi) {
        return new Object[] {sqlServiceApi.getId()};
    }

}
