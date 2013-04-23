package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.PropertyDao;
import com.rackspace.idm.domain.dao.impl.LdapPropertyRepository;
import com.rackspace.idm.domain.entity.Property;
import com.rackspace.idm.domain.service.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.stereotype.Component;

@Component
public class DefaultPropertiesService implements PropertiesService {
    @Autowired
    PropertyDao propertyDao;

    @Override
    public String getValue(String name) {
        Property property = propertyDao.getProperty(name);
        if (property != null) {
            if (property.getValue().size() > 0) {
                return property.getValue().get(0);
            }
        }
        return null;
    }
}
