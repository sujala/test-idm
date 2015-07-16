package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class ProjectMapper extends SqlMapper<Tenant, SqlProject> {

    public Tenant fromSQL(SqlProject entity) {
        Tenant tenant = super.fromSQL(entity);

        if (tenant == null) {
            return tenant;
        }

        for (SqlEndpoint sqlEndpoint : entity.getBaseUrlIds()) {
            String legacyId = sqlEndpoint.getLegacyEndpointId();
            if (!tenant.getBaseUrlIds().contains(legacyId)){
                tenant.getBaseUrlIds().add(legacyId);
            }
        }
        for (SqlEndpoint sqlEndpoint: entity.getV1Defaults()) {
            String legacyId = sqlEndpoint.getLegacyEndpointId();
            if (!tenant.getBaseUrlIds().contains(legacyId)) {
                tenant.getV1Defaults().add(legacyId);
            }
        }

        return tenant;
    }

    public SqlProject toSQL(Tenant tenant) {
        SqlProject sqlProject = super.toSQL(tenant);

        if (sqlProject == null) {
            return sqlProject;
        }

        //TODO: does saving or updating project_endpoint tables require all fields to be set?
        for (String endpointId : tenant.getBaseUrlIds()) {
            SqlEndpoint sqlEndpoint = new SqlEndpoint();
            sqlEndpoint.setId(endpointId);
            sqlProject.getBaseUrlIds().add(sqlEndpoint);
        }

        for (String endpointId : tenant.getV1Defaults()) {
            SqlEndpoint sqlEndpoint = new SqlEndpoint();
            sqlEndpoint.setId(endpointId);
            sqlProject.getV1Defaults().add(sqlEndpoint);
        }

        return sqlProject;
    }
}
