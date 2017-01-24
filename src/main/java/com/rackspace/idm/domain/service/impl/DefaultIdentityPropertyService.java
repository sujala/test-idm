package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.IdentityPropertyDao;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.service.IdentityPropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class DefaultIdentityPropertyService implements IdentityPropertyService {

    @Autowired
    IdentityPropertyDao identityPropertyDao;


    @Override
    public void addIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.addIdentityProperty(identityProperty);
    }

    @Override
    public void updateIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.updateIdentityProperty(identityProperty);
    }

    @Override
    public void deleteIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.deleteIdentityProperty(identityProperty);
    }

    @Override
    public IdentityProperty getIdentityPropertyById(String id) {
        return identityPropertyDao.getIdentityPropertyById(id);
    }

    @Override
    public IdentityProperty getIdentityPropertyByName(String name) {
        return identityPropertyDao.getIdentityPropertyByName(name);
    }

    @Override
    public Iterable<IdentityProperty> getIdentityPropertyByNameAndVersions(String name, Collection<String> idmVersions) {
        return identityPropertyDao.getIdentityPropertyByNameAndVersions(name, idmVersions);
    }
}
