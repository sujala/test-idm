package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.sql.entity.SqlRegionRax;
import com.rackspace.idm.domain.sql.entity.SqlRegion;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

@SQLComponent
public class RegionMapper extends SqlRaxMapper<Region, SqlRegion, SqlRegionRax> {
}
