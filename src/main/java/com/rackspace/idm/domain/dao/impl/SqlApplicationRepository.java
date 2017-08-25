package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.sql.dao.ServiceRepository;
import com.rackspace.idm.domain.sql.entity.SqlService;
import com.rackspace.idm.domain.sql.mapper.impl.ServiceMapper;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

@SQLComponent
public class SqlApplicationRepository implements ApplicationDao {

    @Autowired
    private ServiceMapper mapper;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private static final String APPLICATION_NAME_REGEX_QUERY = "\"name\":[[:space:]]*\":service_name\"";
    private static final String APPLICATION_NAME_REGEX_NAME_PARAM = ":service_name";

    @Override
    @Transactional
    public void addApplication(Application client) {
        final SqlService sqlService = serviceRepository.save(mapper.toSQL(client));

        // Save necessary LDIF for rollback
        final Application application = mapper.fromSQL(sqlService, client);
        final String dn = application.getUniqueId();
    }

    @Override
    @Transactional
    public void updateApplication(Application client) {
        final SqlService sqlService = serviceRepository.save(mapper.toSQL(client, serviceRepository.findOne(client.getClientId())));

        final Application application = mapper.fromSQL(sqlService, client);
    }

    @Override
    @Transactional
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
        List<SqlService> services = serviceRepository.findByServiceNameRegex(
                APPLICATION_NAME_REGEX_QUERY.replace(APPLICATION_NAME_REGEX_NAME_PARAM, clientName));

        if (services.size() == 0) {
            return null;
        }

        if(services.size() > 1) {
            throw new BadRequestException("More than one service exists for name " + clientName);
        }

        return mapper.fromSQL(services.get(0));
    }

    @Override
    public Iterable<Application> getApplicationByType(String type) {
        return mapper.fromSQL(serviceRepository.findByOpenStackType(type));
    }

    @Override
    public Iterable<Application> getOpenStackServices() {
        return getAllApplications();
    }

    @Override
    public PaginatorContext<Application> getOpenStackServices(int offset, int limit) {
        throw new NotImplementedException();
    }
}
