package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.SqlDomainRepository;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.DomainRepository;
import com.rackspace.idm.domain.sql.dao.EndpointRepository;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import com.rackspace.idm.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;

@SQLComponent
public class ProjectMapper extends SqlMapper<Tenant, SqlProject> {

    private static final String FORMAT = "rsId=%s,ou=tenants,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    private static final String V1_DEFAULTS_FIELD = "v1Defaults";
    private static final String BASE_URL_IDS_FIELD = "baseUrlIds";

    @Autowired
    private IdentityConfig config;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EndpointRepository endpointRepository;

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlProject sqlProject) {
        return new Object[] {sqlProject.getTenantId()};
    }

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
            if (!tenant.getV1Defaults().contains(legacyId)) {
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

        mapDomain(entity, sqlProject);
        return mapEndpointIdsToProject(entity, sqlProject);
    }

    @Override
    public SqlProject toSQL(Tenant entity, SqlProject sqlEntity) {
        SqlProject sqlProject = super.toSQL(entity, sqlEntity);

        if (sqlProject == null) {
            return null;
        }

        mapDomain(entity, sqlProject);
        return mapEndpointIdsToProject(entity, sqlEntity);
    }

    private void mapDomain(Tenant tenant, SqlProject project) {
        if (tenant.getDomainId() != null) {
            SqlDomain domain = domainRepository.findOne(tenant.getDomainId());
            if (domain == null) {
                throw new NotFoundException("Domain does not exist");
            }
            project.setDomain(domain);
        }
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

    @Override
    protected Set<String> getIgnoredSetFields() {
        return new HashSet<String>(Arrays.asList(V1_DEFAULTS_FIELD, BASE_URL_IDS_FIELD));
    }

}
