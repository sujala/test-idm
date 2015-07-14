package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.sql.entity.SqlCapability;
import com.rackspace.idm.domain.sql.entity.SqlCapabilityResource;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.ArrayList;

@SQLComponent
public class CapabilityMapper extends SqlMapper<Capability, SqlCapability> {

    public Capability fromSQL(SqlCapability sqlEntity) {
        if (sqlEntity == null) {
            return null;
        }

        Capability capability = super.fromSQL(sqlEntity);
        capability.setResources(new ArrayList<String>());

        if (sqlEntity.getResources() != null) {
            for (SqlCapabilityResource userCertificate : sqlEntity.getResources()) {
                capability.getResources().add(userCertificate.getResource());
            }
        }

        return capability;
    }

    public SqlCapability toSQL(Capability entity) {
        if (entity == null) {
            return null;
        }

        SqlCapability capability = super.toSQL(entity);
        capability.setResources(new ArrayList<SqlCapabilityResource>());

        if (entity.getResources() != null) {
            for (String resource : entity.getResources()) {
                SqlCapabilityResource sqlResource = new SqlCapabilityResource();
                sqlResource.setId(capability.getId());
                sqlResource.setResource(resource);
                capability.getResources().add(sqlResource);
            }
        }

        return capability;
    }
}
