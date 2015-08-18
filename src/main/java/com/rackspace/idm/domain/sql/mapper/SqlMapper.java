package com.rackspace.idm.domain.sql.mapper;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.entity.PaginatorContext;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.collection.spi.PersistentCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public abstract class SqlMapper<Entity, SQLEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMapper.class);

    protected static final String EXTRA_FIELD = "extra";
    protected static final Integer PAGE_SIZE = 10;

    final private Class<Entity> entityClass = (Class<Entity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    final private Class<SQLEntity> sqlEntityClass = (Class<SQLEntity>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

    protected Set<String> getIgnoredSetFields() {
        return Collections.singleton(EXTRA_FIELD);
    }

    public SQLEntity toSQL(Entity entity) {
        if (entity == null) {
            return null;
        }

        try {
            return toSQL(entity, sqlEntityClass.newInstance());
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error creating entity.", e);
            return null;
        }
    }

    public SQLEntity toSQL(Entity entity, SQLEntity sqlEntity) {
        return toSQL(entity, sqlEntity, true);
    }

    public SQLEntity toSQL(Entity entity, SQLEntity sqlEntity, boolean ignoreNulls) {
        if (entity == null || sqlEntity == null) {
            return sqlEntity;
        }

        final BeanWrapperImpl entityWrapper = new BeanWrapperImpl(entity);
        final BeanWrapperImpl sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

        final Map<String, String> declaredFields = getDeclaredFields(sqlEntityClass);
        overrideFields(declaredFields);
        setToSQL(declaredFields, entityWrapper, sqlEntityWrapper, sqlEntityClass, ignoreNulls);

        if (declaredFields.keySet().contains(EXTRA_FIELD)) {
            setExtraToSQL(entityWrapper, sqlEntityWrapper);
        }

        return sqlEntity;
    }

    protected final void setToSQL(Map<String, String> declaredFields, BeanWrapper entityWrapper, BeanWrapper sqlEntityWrapper, Class<?> entityClass, boolean ignoreNulls) {
        final Set<String> ignored = getIgnoredSetFields();
        for (String field : declaredFields.keySet()) {
            if (!ignored.contains(field) && sqlEntityWrapper.isWritableProperty(field)) {
                try {
                    final String propertyName = declaredFields.get(field);
                    Object value = entityWrapper.getPropertyValue(propertyName);
                    final boolean deleteNullValues = entityWrapper.getPropertyTypeDescriptor(propertyName).hasAnnotation(DeleteNullValues.class);
                    if (value != null || !ignoreNulls || deleteNullValues) {
                        value = convertBase64(value, field, entityClass);
                        value = convertPersistentCollection(value, field, sqlEntityWrapper);
                        sqlEntityWrapper.setPropertyValue(field, value);
                    }
                } catch (BeansException e) {
                    LOGGER.debug("Error mapping field '" + field + "'.", e);
                }
            }
        }
    }

    private Object convertPersistentCollection(Object value, String field, BeanWrapper sqlEntityWrapper) {
        Object original = sqlEntityWrapper.getPropertyValue(field);
        if(original instanceof PersistentCollection && original instanceof Collection && value instanceof Collection) {
            Collection persistentSet = (Collection) original;
            persistentSet.clear();
            persistentSet.addAll((Collection) value);
            return persistentSet;
        }
        return value;
    }

    private void setExtraToSQL(BeanWrapperImpl entityWrapper, BeanWrapperImpl sqlEntityWrapper) {
        JSONObject extra = getExtraObject(sqlEntityWrapper);
        if (extra == null) {
            extra = new JSONObject();
        }

        final List<String> extraAttributes = getExtraAttributes();
        if (extraAttributes.size() > 0) {
            for (String attribute : extraAttributes) {
                try {
                    final Object value = entityWrapper.getPropertyValue(attribute);
                    if (value != null) {
                        extra.put(attribute, value);
                    }
                } catch (BeansException e) {
                    LOGGER.debug("Error mapping attribute '" + attribute + "'.", e);
                }
            }
        }
        sqlEntityWrapper.setPropertyValue(EXTRA_FIELD, extra.toJSONString());
    }

    private JSONObject getExtraObject(BeanWrapper sqlEntityWrapper) {
        try {
            final String jsonString = (String) sqlEntityWrapper.getPropertyValue(EXTRA_FIELD);
            if (jsonString != null) {
                final JSONParser jsonParser = new JSONParser();
                return (JSONObject) jsonParser.parse(jsonString);
            }
        } catch (Exception e) {
            LOGGER.debug("Error parsing 'extra' attribute.", e);
        }
        return null;
    }

    public Entity fromSQL(SQLEntity sqlEntity) {
        return fromSQL(sqlEntity, true);
    }

    public Entity fromSQL(SQLEntity sqlEntity, boolean ignoreNulls) {
        if (sqlEntity == null) {
            return null;
        }

        Entity entity = null;
        try {
            entity = entityClass.newInstance();

            final BeanWrapper entityWrapper = new BeanWrapperImpl(entity);
            final BeanWrapper sqlEntityWrapper = new BeanWrapperImpl(sqlEntity);

            final Map<String, String> declaredFields = getDeclaredFields(sqlEntityClass);
            overrideFields(declaredFields);
            setFromSQL(declaredFields, entityWrapper, sqlEntityWrapper, entityClass, ignoreNulls);

            if (declaredFields.keySet().contains(EXTRA_FIELD)) {
                setExtraFromSQL(entityWrapper, sqlEntityWrapper);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error mapping data.", e);
        }

        return entity;
    }

    protected final void setFromSQL(Map<String, String> declaredFields, BeanWrapper entityWrapper, BeanWrapper sqlEntityWrapper, Class<?> entityType, boolean ignoreNulls) {
        final Set<String> ignored = getIgnoredSetFields();
        for (String field : declaredFields.keySet()) {
            if (!ignored.contains(field) && entityWrapper.isWritableProperty(declaredFields.get(field))) {
                try {
                    Object value = sqlEntityWrapper.getPropertyValue(field);
                    if (value != null || !ignoreNulls) {
                        value = convertBase64(value, field, entityType);
                        entityWrapper.setPropertyValue(declaredFields.get(field), value);
                    }
                } catch (BeansException e) {
                    LOGGER.debug("Error mapping field '" + field + "'.", e);
                }
            }
        }
    }

    private void setExtraFromSQL(BeanWrapper entityWrapper, BeanWrapper sqlEntityWrapper) {
        final List<String> extraAttributes = getExtraAttributes();
        if (extraAttributes.size() > 0) {
            final JSONObject jsonObject = getExtraObject(sqlEntityWrapper);
            if (jsonObject != null) {
                for (String attribute : extraAttributes) {
                    try {
                        final Object value = jsonObject.get(attribute);
                        entityWrapper.setPropertyValue(attribute, value);
                    } catch (BeansException e) {
                        LOGGER.debug("Error mapping attribute '" + attribute + "'.", e);
                    }
                }
            }
        }
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

    protected Object convertBase64(Object value, String field, Class<?> entityType) {
        Object newValue = value;
        if (value instanceof String || value instanceof byte[]) {
            try {
                final Type fieldType = entityType.getDeclaredField(field).getType();
                if (fieldType.equals(String.class) && value instanceof byte[]) {
                    newValue = Base64.encodeBase64String((byte[]) value);
                } else if (fieldType.equals(byte[].class) && value instanceof String) {
                    newValue = Base64.decodeBase64((String) value);
                }
            } catch (NoSuchFieldException e) {
            }
        }
        return newValue;
    }

    public PaginatorContext<Entity> getPageRequest(int offset, int limit) {
        final int page = offset / PAGE_SIZE;
        final int size = PAGE_SIZE;

        final PaginatorContext<Entity> context = new PaginatorContext<Entity>();
        context.setOffset(offset);
        context.setLimit(limit);
        context.setPageRequest(new PageRequest(page, size));

        return context;
    }

    public boolean fromSQL(Page<SQLEntity> sqlEntities, PaginatorContext<Entity> context) {
        if (sqlEntities.getTotalPages() < sqlEntities.getNumber()) {
            return false;
        }
        context.setTotalRecords(sqlEntities.getTotalElements());

        final List<Entity> entities = fromSQL(sqlEntities);

        if (context.getValueList().size() == 0) {
            context.setValueList(entities.subList(context.getOffset() % PAGE_SIZE, entities.size()));
        } else {
            context.getValueList().addAll(entities);
        }

        context.setPageRequest((PageRequest) context.getPageRequest().next());

        if (context.getValueList().size() >= context.getLimit()) {
            context.setValueList(context.getValueList().subList(0, context.getLimit()));
            return false;
        }

        if (context.getPageRequest() == null) {
            return false;
        }

        return true;
    }
}
