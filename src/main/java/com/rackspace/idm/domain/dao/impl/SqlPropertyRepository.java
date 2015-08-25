package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.PropertyDao;
import com.rackspace.idm.domain.entity.Property;
import com.rackspace.idm.domain.sql.dao.PropertyRepository;
import com.rackspace.idm.domain.sql.mapper.impl.PropertyMapper;
import org.springframework.beans.factory.annotation.Autowired;

@SQLComponent
public class SqlPropertyRepository implements PropertyDao {

    @Autowired
    private PropertyMapper mapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public Property getProperty(String name) {
        return mapper.fromSQL(propertyRepository.findOne(name));
    }

}
