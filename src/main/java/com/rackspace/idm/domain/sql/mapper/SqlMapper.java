package com.rackspace.idm.domain.sql.mapper;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public abstract class SqlMapper<Entity, SQLEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMapper.class);

    private static final String EXTRA_FIELD = "extra";

    final private Class<Entity> entityClass = (Class<Entity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    final private Class<SQLEntity> sqlEntityClass = (Class<SQLEntity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

    public SQLEntity toSQL(Entity entity) {
        if (entity == null) {
            return null;
        }

        SQLEntity sqlEntity = null;
        try {
            sqlEntity = sqlEntityClass.newInstance();

            final BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
            final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

            final Map<String, String> declaredFields = getDeclaredFields(sqlEntityClass);
            overrideFields(declaredFields);

            for (String field : declaredFields.keySet()) {
                try {
                    final Object value = entityWrapper.getPropertyValue(declaredFields.get(field));
                    sqlEntityWrapper.setPropertyValue(field, value);
                } catch (BeansException e) {
                    LOGGER.warn("Error mapping field '" + field + "'.", e);
                }
            }

            JSONObject extra = new JSONObject();
            for (String attribute : getExtraAttributes()) {
                try {
                    final Object value = entityWrapper.getPropertyValue(attribute);
                    if (value != null) {
                        extra.put(attribute, value);
                    }
                } catch (BeansException e) {
                    LOGGER.warn("Error mapping attribute '" + attribute + "'.", e);
                }
            }
            if (getExtraAttributes().size() > 0) {
                sqlEntityWrapper.setPropertyValue(EXTRA_FIELD, extra.toJSONString());
            }

        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error mapping data.", e);
        }

        return sqlEntity;
    }

    public Entity fromSQL(SQLEntity sqlEntity) {
        if (sqlEntity == null) {
            return null;
        }

        Entity entity = null;
        try {
            entity = entityClass.newInstance();

            final BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
            final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

            final Map<String, String> declaredFields = getDeclaredFields(sqlEntityClass);
            overrideFields(declaredFields);

            for (String field : declaredFields.keySet()) {
                try {
                    final Object value = sqlEntityWrapper.getPropertyValue(field);
                    entityWrapper.setPropertyValue(declaredFields.get(field), value);
                } catch (BeansException e) {
                    LOGGER.warn("Error mapping field '" + field + "'.", e);
                }
            }

            final List<String> extraAttributes = getExtraAttributes();

            if (extraAttributes.size() > 0) {
                JSONObject jsonObject = null;
                try {
                    final String jsonString = (String) sqlEntityWrapper.getPropertyValue(EXTRA_FIELD);
                    if (jsonString != null) {
                        final JSONParser jsonParser = new JSONParser();
                        jsonObject = (JSONObject) jsonParser.parse(jsonString);
                    }
                } catch (ParseException e) {
                    LOGGER.warn("Error mapping extras.", e);
                }

                if (jsonObject != null) {
                    for (String attribute : extraAttributes) {
                        try {
                            final Object value = jsonObject.get(attribute);
                            entityWrapper.setPropertyValue(attribute, value);
                        } catch (BeansException e) {
                            LOGGER.warn("Error mapping attribute '" + attribute + "'.", e);
                        }
                    }
                }
            }

        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error mapping data.", e);
        }

        return entity;
    }

    public List<Entity> fromSQL(Iterable<SQLEntity> sqlEntities) {
        final List<Entity> entities = new ArrayList<Entity>();
        for (SQLEntity sqlEntity : sqlEntities) {
            entities.add(fromSQL(sqlEntity));
        }
        return entities;
    }

    public List<String> getExtraAttributes() {
        return Collections.EMPTY_LIST;
    }

    public Map<String, String> getDeclaredFields(Class<?> entity){
        final Map<String, String> declaredFields = new HashMap<String, String>();
        for(Field field : entity.getDeclaredFields()){
            declaredFields.put(field.getName(), field.getName());
        }
        return declaredFields;
    }

    public void overrideFields(Map<String, String> map) {
    }

}
