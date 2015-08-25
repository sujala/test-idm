package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.sql.entity.SqlCapability;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.Map;

@SQLComponent
public class CapabilityMapper extends SqlMapper<Capability, SqlCapability> {

    private static String FORMAT = "rsId=%s,ou=capabilities,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    public void overrideFields(Map<String, String> map) {
        map.put("id", "rsId");
        map.put("capabilityId", "id");
    }

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlCapability sqlCapability) {
        return new Object[] {sqlCapability.getId()};
    }

}
