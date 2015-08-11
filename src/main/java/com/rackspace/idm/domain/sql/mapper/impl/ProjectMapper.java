package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.EndpointRepository;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

@SQLComponent
public class ProjectMapper extends SqlMapper<Tenant, SqlProject> {

    @Autowired
    IdentityConfig config;

    @Autowired
    EndpointRepository endpointRepository;

    public Tenant fromSQL(SqlProject entity) {
        Tenant tenant = super.fromSQL(entity);

        if (tenant == null || entity.getTenantId().equalsIgnoreCase(config.getReloadableConfig().getIdentityRoleDefaultTenant())) {
            return null;
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

        if (sqlProject == null || sqlProject.getTenantId().equalsIgnoreCase(config.getReloadableConfig().getIdentityRoleDefaultTenant())) {
            return null;
        }

        LinkedHashSet<String> endpointIds = new LinkedHashSet<String>();
        endpointIds.addAll(tenant.getBaseUrlIds());
        endpointIds.addAll(tenant.getV1Defaults());



        HashMap<String, List<SqlEndpoint>> endpointMap = new HashMap<String, List<SqlEndpoint>>();
        for(SqlEndpoint endpoint : endpointRepository.findByLegacyEndpointIdIn(endpointIds)){
            String legacyEndpointId = endpoint.getLegacyEndpointId();
            if(endpointMap.containsKey(legacyEndpointId)){
                endpointMap.get(legacyEndpointId).add(endpoint);
            } else {
                List<SqlEndpoint> endpointList = new ArrayList<SqlEndpoint>();
                endpointList.add(endpoint);
                endpointMap.put(legacyEndpointId, endpointList);
            }
        }

        for (String endpointId : tenant.getBaseUrlIds()) {
            sqlProject.getBaseUrlIds().addAll(endpointMap.get(endpointId));
        }

        for (String endpointId : tenant.getV1Defaults()) {
            sqlProject.getV1Defaults().addAll(endpointMap.get(endpointId));
        }

        return sqlProject;
    }
}
