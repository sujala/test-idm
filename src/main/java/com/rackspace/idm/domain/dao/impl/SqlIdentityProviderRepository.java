package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.dao.IdentityProviderRepository;
import com.rackspace.idm.domain.sql.mapper.impl.IdentityProviderMapper;
import org.springframework.beans.factory.annotation.Autowired;

@SQLComponent
public class SqlIdentityProviderRepository implements IdentityProviderDao {

    @Autowired
    private IdentityProviderMapper mapper;

    @Autowired
    private IdentityProviderRepository repository;

    @Override
    public IdentityProvider getIdentityProviderByUri(String uri) {
        return mapper.fromSQL(repository.findByUri(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return mapper.fromSQL(repository.findOne(name));
    }

}
