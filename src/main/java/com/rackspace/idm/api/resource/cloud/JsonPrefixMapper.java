package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/12/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonPrefixMapper {
    /*
     The HashMap is create by assigning the key and value in the format as :: FROM -> TO
     Key = path to the element :: value = what the element gets updated to.

     replaceJsonObject method travels the path to the element and updates it.
     */

    public JSONObject mapPrefix(JSONObject object, Map prefixValues) {
        return mapPrefix(object, prefixValues, null);
    }

    public JSONObject mapPrefix(JSONObject object, Map prefixValues, JsonArrayEntryTransformer entryTransformer){

        for(Object key : prefixValues.keySet()){
            String[] elements = ((String)key).split("\\.");

            replaceJsonObject(object, elements, prefixValues.get(key).toString(), entryTransformer);
        }

        return object;
    }

    private void replaceJsonObject(JSONObject object, String[] elements, String newValue, JsonArrayEntryTransformer entryTransformer){
        if(object != null && object.containsKey(elements[0])){
            if(elements.length > 1){
                replaceJsonObject((JSONObject) object.get(elements[0]), Arrays.copyOfRange(elements, 1, elements.length), newValue, entryTransformer);
            } else{
                object.put(newValue, object.get(elements[0]));
                object.remove(elements[0]);

                if (entryTransformer != null && object instanceof JSONObject) {
                    entryTransformer.transform((JSONObject) object);
                }
            }
        }
    }

}
