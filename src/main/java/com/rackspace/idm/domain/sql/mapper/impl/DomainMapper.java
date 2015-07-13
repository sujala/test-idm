package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.entity.SqlDomainRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

import java.util.Arrays;
import java.util.List;

@SQLComponent
public class DomainMapper extends SqlRaxMapper<Domain, SqlDomain, SqlDomainRax> {

    public List<String> getExtraAttributes() {
        return Arrays.asList("description");
    }
}
