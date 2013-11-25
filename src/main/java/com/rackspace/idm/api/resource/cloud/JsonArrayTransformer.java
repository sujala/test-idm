package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Our Json representation for arrays does not map one to one to the underlying jaxb object.
 * This class is responsible for transforming our representation to match with underlying jaxb object represenation..
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
 *
 *
 */
public class JsonArrayTransformer {

    public JSONObject transform(JSONObject object){
        for(Object key : object.keySet()){
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                value = transform((JSONObject) value);
            }

            if (value instanceof JSONArray) {
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
}
