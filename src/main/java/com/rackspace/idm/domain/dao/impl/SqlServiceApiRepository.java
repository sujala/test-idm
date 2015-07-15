package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.ServiceApiDao;
import com.rackspace.idm.domain.entity.ServiceApi;
import com.rackspace.idm.domain.sql.dao.ServiceApiRepository;
import com.rackspace.idm.domain.sql.mapper.impl.ServiceApiMapper;
import org.springframework.beans.factory.annotation.Autowired;

@SQLComponent
public class SqlServiceApiRepository implements ServiceApiDao {

    @Autowired
    ServiceApiMapper mapper;

    @Autowired
    ServiceApiRepository repository;

    @Override
    public Iterable<ServiceApi> getServiceApis() {
        return mapper.fromSQL(repository.findByVersionNotNullAndTypeNotNull());
    }
}
