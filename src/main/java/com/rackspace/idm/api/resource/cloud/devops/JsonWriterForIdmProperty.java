package com.rackspace.idm.api.resource.cloud.devops;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.IdmProperty;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class JsonWriterForIdmProperty {
private static final Logger logger = LoggerFactory.getLogger(JsonWriterForIdmProperty.class);

    public static final String JSON_PROP_CONFIG_PATH = "configPath";
    public static final String JSON_PROP_PROPERTIES = "properties";
    public static final String JSON_PROP_NAME = "name";
    public static final String JSON_PROP_DESCRIPTION = "description";
    public static final String JSON_PROP_VERSION_ADDED = "versionAdded";
    public static final String JSON_PROP_VALUE = "value";
    public static final String JSON_PROP_VALUE_TYPE = "valueType";
    public static final String JSON_PROP_DEFAULT_VALUE = "defaultValue";
    public static final String JSON_PROP_RELOADABLE = "reloadable";
    public static final String JSON_PROP_SOURCE = "source";
    public static final String JSON_PROP_ID = "id";

    @Autowired
    IdentityConfig identityConfig;

    public String toJsonString(Collection<IdmProperty> idmProperties) {
        JSONObject props = new JSONObject();
        props.put(JSON_PROP_CONFIG_PATH, identityConfig.getConfigRoot());
        props.put(JSON_PROP_PROPERTIES, toJSONObject(idmProperties));
        return props.toJSONString();
    }

    /**
     * This method creates a JSONArray object from a collection of IdmProperty objects. The JSONArray object
     * is intended to be used in building a response from the GET IDM Properties API call.
     */
    private JSONArray toJSONObject(Collection<IdmProperty> idmProperties) {
        JSONArray propArr = new JSONArray();

        for (IdmProperty idmProperty : idmProperties) {
            JSONObject prop = new JSONObject();
            try {
                prop.put(JSON_PROP_NAME, idmProperty.getName());
                prop.put(JSON_PROP_DESCRIPTION, idmProperty.getDescription());
                prop.put(JSON_PROP_VERSION_ADDED, idmProperty.getVersionAdded());
                prop.put(JSON_PROP_RELOADABLE, idmProperty.isReloadable());
                prop.put(JSON_PROP_SOURCE, idmProperty.getSource());

                if (idmProperty.getDefaultValue() != null) {
                    Object convertedDefaultValue = valueToAddToJSON(idmProperty.getDefaultValue());
                    prop.put(JSON_PROP_DEFAULT_VALUE, convertedDefaultValue);
                }

                Object convertedValue = valueToAddToJSON(idmProperty.getValue());
                prop.put(JSON_PROP_VALUE, convertedValue);

                if (idmProperty.getValueType() != null) {
                    prop.put(JSON_PROP_VALUE_TYPE, idmProperty.getValueType());
                }

                if (idmProperty.getId() != null) {
                    prop.put(JSON_PROP_ID, idmProperty.getId());
                }

                if (idmProperty.getValueType() != null) {
                    prop.put(JSON_PROP_VALUE_TYPE, idmProperty.getValueType());
                }

                propArr.add(prop);
            } catch (Exception e) {
                logger.error(String.format("error retrieving property '%s'", idmProperty.getName()), e);
            }
        }

        return propArr;
    }

    private Object valueToAddToJSON(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        } else if (value instanceof String[]) {
            JSONArray valueArray = new JSONArray();
            for (String val : (String[])value) {
                valueArray.add(val);
            }
            return valueArray;
        } else if (value instanceof Enum) {
            return ((Enum)value).name();
        } else {
            return value.toString();
        }
    }

}
