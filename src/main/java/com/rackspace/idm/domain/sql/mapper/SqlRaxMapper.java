package com.rackspace.idm.domain.sql.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class SqlRaxMapper<Entity, SQLEntity, SQLRaxEntity> extends SqlMapper<Entity, SQLEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRaxMapper.class);

    public static final String RAX_FIELD = "rax";

    final private Class<Entity> entityClass = (Class<Entity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    final private Class<SQLRaxEntity> sqlRaxEntityClass = (Class<SQLRaxEntity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];

    @Override
    protected Set<String> getIgnoredSetFields() {
        return new HashSet<String>(Arrays.asList(EXTRA_FIELD, RAX_FIELD));
    }

    @Override
    public SQLEntity toSQL(Entity entity, SQLEntity sqlEntity) {
        final SQLEntity finalEntity = super.toSQL(entity, sqlEntity);

        final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);
        SQLRaxEntity sqlRaxEntity;
        try {
            sqlRaxEntity = (SQLRaxEntity) sqlEntityWrapper.getPropertyValue(RAX_FIELD);
        } catch (Exception ignored) {
            try {
                sqlRaxEntity = sqlRaxEntityClass.newInstance();
            } catch (ReflectiveOperationException e) {
                LOGGER.error("Cannot create RAX entity.", e);
                sqlRaxEntity = null;
            }
        }

        return modifyRaxToSQL(entity, finalEntity, sqlRaxEntity);
    }

    private SQLEntity modifyRaxToSQL(Entity entity, SQLEntity sqlEntity, SQLRaxEntity sqlRaxEntity) {
        if (entity == null || sqlRaxEntity == null) {
            return sqlEntity;
        }

        final BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
        final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);
        final BeanWrapperImpl sqlRaxEntityWrapper = new BeanWrapperImpl(sqlRaxEntity);

        final Map<String, String> declaredRaxFields = getDeclaredFields(sqlRaxEntityClass);
        overrideRaxFields(declaredRaxFields);
        setToSQL(declaredRaxFields, entityWrapper, sqlRaxEntityWrapper, sqlRaxEntityClass);

        sqlEntityWrapper.setPropertyValue(RAX_FIELD, sqlRaxEntity);
        return sqlEntity;
    }

    public Entity fromSQL(SQLEntity sqlEntity) {
        if (sqlEntity == null) {
            return null;
        }

        Entity entity = null;
        SQLRaxEntity sqlRaxEntity = null;
        entity = super.fromSQL(sqlEntity);

        final BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
        final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

        sqlRaxEntity = (SQLRaxEntity) sqlEntityWrapper.getPropertyValue(RAX_FIELD);
        if (sqlRaxEntity != null) {
            final BeanWrapperImpl sqlRaxEntityWrapper = new BeanWrapperImpl(sqlRaxEntity);

            final Map<String, String> declaredRaxFields = getDeclaredFields(sqlRaxEntityClass);
            overrideRaxFields(declaredRaxFields);
            setFromSQL(declaredRaxFields, entityWrapper, sqlRaxEntityWrapper, entityClass);
        }

        return entity;
    }

    public void overrideRaxFields(Map<String, String> map){
    }

}
