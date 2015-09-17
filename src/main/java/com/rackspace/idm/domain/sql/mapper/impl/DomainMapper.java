package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.sql.dao.ProjectRepository;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.entity.SqlDomainRax;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;
import org.apache.xalan.xsltc.DOM;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@SQLComponent
public class DomainMapper extends SqlRaxMapper<Domain, SqlDomain, SqlDomainRax> {

    private static final String FORMAT = "rsId=%s,ou=domains,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlDomain sqlDomain) {
        return new Object[] {sqlDomain.getDomainId()};
    }

    @Override
    public Domain fromSQL(SqlDomain entity) {
        Domain domain = super.fromSQL(entity);

        if (domain == null){
            return null;
        }

        List<String> tenantIds = new ArrayList<String>();

        for (SqlProject sqlProject : entity.getSqlProject()) {
            tenantIds.add(sqlProject.getTenantId());
        }

        domain.setTenantIds(tenantIds.toArray((new String[tenantIds.size()])));

        return domain;
    }

    @Override
    public SqlDomain toSQL(Domain entity, SqlDomain sqlDomainOrig) {
        SqlDomain sqlDomain = super.toSQL(entity, sqlDomainOrig);

        if (sqlDomain == null) {
            return null;
        }

        Set<SqlProject> sqlProjects = new HashSet<SqlProject>();

        List<String> tenantIds = new ArrayList<String>();
        if(entity.getTenantIds() != null) {
            tenantIds.addAll(Arrays.asList(entity.getTenantIds()));
        }

        for (String tenantId : tenantIds) {
            SqlProject sqlProject = projectRepository.findOne(tenantId);
            sqlProject.setDomainId(sqlDomain.getDomainId());
            sqlProjects.add(sqlProject);
        }

        sqlDomain.setSqlProject(sqlProjects);

        return sqlDomain;
    }

    public List<String> getExtraAttributes() {
        return Arrays.asList("description");
    }
}
