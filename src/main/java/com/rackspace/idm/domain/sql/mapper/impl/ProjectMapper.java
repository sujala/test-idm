package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.DomainRepository;
import com.rackspace.idm.domain.sql.dao.EndpointRepository;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import com.rackspace.idm.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

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

        tenant.setDomainId(entity.getDomainId());

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
            if (!domainRepository.exists(tenant.getDomainId())) {
                throw new NotFoundException("Domain does not exist");
            }
            project.setDomainId(tenant.getDomainId());
        }
    }

    private SqlProject mapEndpointIdsToProject(Tenant entity, SqlProject sqlEntity) {
        final Set<String> endpointIds = new LinkedHashSet<String>();
        endpointIds.addAll(entity.getBaseUrlIds());
        endpointIds.addAll(entity.getV1Defaults());

        sqlEntity.setDomainId(entity.getDomainId());

        HashMap<String, List<SqlEndpoint>> endpointMap = new HashMap<String, List<SqlEndpoint>>();
        for(SqlEndpoint endpoint : endpointRepository.findByLegacyEndpointIdIn(endpointIds)){
            final String legacyEndpointId = endpoint.getLegacyEndpointId();
            if(endpointMap.containsKey(legacyEndpointId)){
                endpointMap.get(legacyEndpointId).add(endpoint);
            } else {
                final List<SqlEndpoint> endpointList = new ArrayList<SqlEndpoint>();
                endpointList.add(endpoint);
                endpointMap.put(legacyEndpointId, endpointList);
            }
        }

        // Add all baseURLs to project
        for (String endpointId : entity.getBaseUrlIds()) {
            if (endpointMap.containsKey(endpointId)) {
                sqlEntity.getBaseUrlIds().addAll(endpointMap.get(endpointId));
            }
        }

        // Remove baseURls not associated project
        List<SqlEndpoint> removeBaseUrls = new ArrayList<SqlEndpoint>();
        for (SqlEndpoint endpoint : sqlEntity.getBaseUrlIds()) {
            if(!entity.getBaseUrlIds().contains(endpoint.getLegacyEndpointId())) {
                removeBaseUrls.add(endpoint);
            }
        }
        sqlEntity.getBaseUrlIds().removeAll(removeBaseUrls);

        // Add all v1Defaults to project
        for (String endpointId : entity.getV1Defaults()) {
            if (endpointMap.containsKey(endpointId)) {
                sqlEntity.getV1Defaults().addAll(endpointMap.get(endpointId));
            }
        }

        // Remove v1Defaults not associated to project
        List<SqlEndpoint> removeV1Defaults = new ArrayList<SqlEndpoint>();
        for (SqlEndpoint endpoint : sqlEntity.getV1Defaults()) {
            if(!entity.getV1Defaults().contains(endpoint.getLegacyEndpointId())) {
                removeV1Defaults.add(endpoint);
            }
        }
        sqlEntity.getV1Defaults().removeAll(removeV1Defaults);

        return sqlEntity;
    }

    @Override
    protected Set<String> getIgnoredSetFields() {
        return new HashSet<String>(Arrays.asList(V1_DEFAULTS_FIELD, BASE_URL_IDS_FIELD));
    }

}
