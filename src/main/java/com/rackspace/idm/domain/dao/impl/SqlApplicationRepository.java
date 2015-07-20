package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.ServiceRepository;
import com.rackspace.idm.domain.sql.entity.SqlService;
import com.rackspace.idm.domain.sql.mapper.impl.ServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;


@SQLComponent
public class SqlApplicationRepository implements ApplicationDao {

    @Autowired
    ServiceMapper mapper;

    @Autowired
    ServiceRepository serviceRepository;

    @Override
    public void addApplication(Application client) {
        SqlService sqlService = mapper.toSQL(client);
        serviceRepository.save(sqlService);
    }

    @Override
    public void deleteApplication(Application client) {
        serviceRepository.delete(client.getClientId());
    }

    @Override
    public Iterable<Application> getAllApplications() {
        return mapper.fromSQL(serviceRepository.findAll());
    }

    @Override
    public Application getApplicationByClientId(String clientId) {
        return mapper.fromSQL(serviceRepository.findOne(clientId));
    }

    @Override
    public Application getApplicationByName(String clientName) {
        return mapper.fromSQL(serviceRepository.findOne(clientName));
    }

    @Override
    public Iterable<Application> getApplicationByType(String type) {
        return mapper.fromSQL(serviceRepository.findByOpenStackType(type));
    }

    @Override
    public void updateApplication(Application client) {
        serviceRepository.save(mapper.toSQL(client));
    }

    @Override
    public Iterable<Application> getOpenStackServices() {
        return getAllApplications();
    }
}
