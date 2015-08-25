package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Property;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlProperty;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class PropertyMapper extends SqlMapper<Property, SqlProperty> {

    private static final String FORMAT = "cn=%s,ou=properties,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlProperty sqlProperty) {
        return new String[] {sqlProperty.getName()};
    }

}