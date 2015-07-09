package com.rackspace.idm.domain.sql.mapper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.lang.reflect.ParameterizedType;
import java.util.*;

public class SqlRaxMapper<Entity, SQLEntity, SQLRaxEntity> extends SqlMapper<Entity, SQLEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMapper.class);

    public static final String RAX_FIELD = "rax";

    final private Class<SQLRaxEntity> sqlRaxEntityClass = (Class<SQLRaxEntity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];

    public SQLEntity toSQL(Entity entity) {
        if (entity == null) {
            return null;
        }

        SQLEntity sqlEntity = null;
        SQLRaxEntity sqlRaxEntity = null;
        try {
            sqlEntity = super.toSQL(entity);
            sqlRaxEntity = sqlRaxEntityClass.newInstance();

            BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
            BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);
            BeanWrapperImpl sqlRaxEntityWrapper = new BeanWrapperImpl(sqlRaxEntity);

            Map<String, String> declaredRaxFields = getDeclaredFields(sqlRaxEntityClass);
            overrideRaxFields(declaredRaxFields);

            for (String field : declaredRaxFields.keySet()) {
                try {
                    Object value = entityWrapper.getPropertyValue(declaredRaxFields.get(field));
                    sqlRaxEntityWrapper.setPropertyValue(field, value);
                } catch (BeansException e) {
                    LOGGER.warn("Error mapping field '" + field + "'.", e);
                }
            }

            sqlEntityWrapper.setPropertyValue(RAX_FIELD, sqlRaxEntity);

        } catch (InstantiationException e) {
            LOGGER.error("Error mapping data.", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Error mapping data.", e);
        }

        return sqlEntity;
    }

    public Entity fromSQL(SQLEntity sqlEntity) {
        if (sqlEntity == null) {
            return null;
        }

        Entity entity = null;
        SQLRaxEntity sqlRaxEntity = null;

        entity = super.fromSQL(sqlEntity);
        BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);

        BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

        sqlRaxEntity = (SQLRaxEntity) sqlEntityWrapper.getPropertyValue(RAX_FIELD);
        if (sqlRaxEntity != null) {
            BeanWrapperImpl sqlRaxEntityWrapper = new BeanWrapperImpl(sqlRaxEntity);

            Map<String, String> declaredRaxFields = getDeclaredFields(sqlRaxEntityClass);
            overrideRaxFields(declaredRaxFields);

            for (String field : declaredRaxFields.keySet()) {
                try {
                    Object value = sqlRaxEntityWrapper.getPropertyValue(field);
                    entityWrapper.setPropertyValue(declaredRaxFields.get(field), value);
                } catch (BeansException e) {
                    LOGGER.warn("Error mapping field '" + field + "'.", e);
                }
            }
        }

        return entity;
    }

    public void overrideRaxFields(Map<String, String> map){
    }
}
