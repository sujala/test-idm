package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.SqlDomainRepository;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.DomainRepository;
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
    DomainRepository domainRepository;

    @Autowired
    EndpointRepository endpointRepository;

    @Override
    public Tenant fromSQL(SqlProject entity) {
        Tenant tenant = super.fromSQL(entity);

        if (tenant == null) {
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

        tenant.setDomainId(entity.getDomain().getDomainId());

        return tenant;
    }

    @Override
    public SqlProject toSQL(Tenant entity) {
        SqlProject sqlProject = super.toSQL(entity);

        if (sqlProject == null) {
            return null;
        }

        return mapEndpointIdsToProject(entity, sqlProject);
    }

    @Override
    public SqlProject toSQL(Tenant entity, SqlProject sqlEntity) {
        SqlProject sqlProject = super.toSQL(entity, sqlEntity);

        if (sqlProject == null) {
            return null;
        }

        return mapEndpointIdsToProject(entity, sqlEntity);
    }

    private SqlProject mapEndpointIdsToProject(Tenant entity, SqlProject sqlEntity) {
        LinkedHashSet<String> endpointIds = new LinkedHashSet<String>();
        endpointIds.addAll(entity.getBaseUrlIds());
        endpointIds.addAll(entity.getV1Defaults());

        sqlEntity.setDomain(domainRepository.findOne(entity.getDomainId()));

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

        for (String endpointId : entity.getBaseUrlIds()) {
            sqlEntity.getBaseUrlIds().addAll(endpointMap.get(endpointId));
        }

        for (String endpointId : entity.getV1Defaults()) {
            sqlEntity.getV1Defaults().addAll(endpointMap.get(endpointId));
        }

        return sqlEntity;
    }
}
