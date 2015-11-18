package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Our Json representation for arrays does not map one to one to the underlying jaxb object.
 * This class is responsible for transforming our representation to match with underlying jaxb
 * object represenation and vice versa
 *
 * For example,
 *
 * {
 *     "user" : {
 *         "firstname" : john,
 *         "roles" : [
 *             { "name" : managed },
 *             { "name" : moderator },
 *         ]
 *     }
 * }
 *
 * Does not map to jaxb object which is of form.
 *
 *      user.firstname = "john"
 *      user.roles.role = [ {"name" : managed}, {"name" : moderator} ]
 *
 * Jaxb object has an extra wrapper. The json payload that would have mapped directly would have been
 *
 *  {
 *     "user" : {
 *         "firstname" : john,
 *         "roles" : {
 *              "role" : [
 *                  { "name" : managed },
 *                  { "name" : moderator },
 *              ]
 *         }
 *     }
 * }
 */
public class JsonArrayTransformer {
    public static final JsonArrayTransformerHandler ALWAYS_PLURALIZE_HANDLER = new AlwaysPluralizeJsonArrayTransformerHandler();


    public JSONObject transformIncludeWrapper(JSONObject object) {
        return transformIncludeWrapper(object, ALWAYS_PLURALIZE_HANDLER);
    }

    public JSONObject transformIncludeWrapper(JSONObject object, JsonArrayTransformerHandler handler){
        for(Object key : object.keySet()){
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                value = transformIncludeWrapper((JSONObject) value, handler);
            }

            if (value instanceof JSONArray && handler.pluralizeJSONArrayWithName(key.toString())) {
                //create new wrapper element. following convention
                //{wrapperName}.{elementName}
                //what we get from json object is actually the wrapper name.
                //the wrapper name is just a plural form (s) of the element name
                JSONArray array = (JSONArray) value;
                String elementName = key.toString().substring(0, key.toString().length()-1);

                JSONObject wrapper = new JSONObject();
                wrapper.put(elementName, array);

                object.put(key, wrapper);
            }
        }

        return object;
    }

    public JSONObject transformRemoveWrapper(JSONObject object, JSONObject parent) {
        return transformRemoveWrapper(object, parent, ALWAYS_PLURALIZE_HANDLER);
    }

    public JSONObject transformRemoveWrapper(JSONObject object, JSONObject parent, JsonArrayTransformerHandler handler) {
        for(Object key : object.keySet()){
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                value = transformRemoveWrapper((JSONObject) value, object, handler);
            }

            if (value instanceof JSONArray && handler.pluralizeJSONArrayWithName(key.toString())) {

                //remove the wrapper element. following convention
                //e.g roles.role
                JSONArray array = (JSONArray) value;
                String elementName = key.toString() + "s";
                //object.put(elementName, array);
                parent.put(elementName, array);
            }
        }

        return object;
    }
}
