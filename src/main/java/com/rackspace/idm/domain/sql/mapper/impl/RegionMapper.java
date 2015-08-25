package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.sql.entity.SqlRegion;
import com.rackspace.idm.domain.sql.entity.SqlRegionRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

@SQLComponent
public class RegionMapper extends SqlRaxMapper<Region, SqlRegion, SqlRegionRax> {

    private static final String FORMAT = "cn=%s,ou=regions,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlRegion sqlRegion) {
        return new Object[] {sqlRegion.getName()};
    }

}
