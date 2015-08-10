package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.entity.SqlDomainRax;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;
import org.apache.xalan.xsltc.DOM;

import java.util.*;

@SQLComponent
public class DomainMapper extends SqlRaxMapper<Domain, SqlDomain, SqlDomainRax> {

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
    public SqlDomain toSQL(Domain entity) {
        SqlDomain sqlDomain = super.toSQL(entity);

        if (sqlDomain == null) {
            return null;
        }

        Set<SqlProject> sqlProjects = new HashSet<SqlProject>();

        for (String tenantId : entity.getTenantIds()){
            SqlProject sqlProject = new SqlProject();
            sqlProject.setTenantId(tenantId);
            sqlProjects.add(sqlProject);
        }

        sqlDomain.setSqlProject(sqlProjects);

        return sqlDomain;
    }

    public List<String> getExtraAttributes() {
        return Arrays.asList("description");
    }
}
