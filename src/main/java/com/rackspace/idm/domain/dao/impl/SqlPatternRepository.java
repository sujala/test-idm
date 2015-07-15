package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.PatternDao;
import com.rackspace.idm.domain.entity.Pattern;
import com.rackspace.idm.domain.sql.dao.PatternRepository;
import com.rackspace.idm.domain.sql.mapper.impl.PatternMapper;
import org.springframework.beans.factory.annotation.Autowired;

@SQLComponent
public class SqlPatternRepository implements PatternDao {

    @Autowired
    PatternMapper mapper;

    @Autowired
    PatternRepository patternRepository;

    @Override
    public Pattern getPattern(String name) {
        return mapper.fromSQL(patternRepository.findOne(name));
    }

}
