package com.rackspace.idm.domain.entity;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;

import java.lang.reflect.ParameterizedType;

public class ObjectMapper<T> {
    private static Mapper mapper = new DozerBeanMapper();

    public T convert(Class<T> type) {
        return mapper.map(this, type);
    }

    public T convert() {
        return null;
    }
}
