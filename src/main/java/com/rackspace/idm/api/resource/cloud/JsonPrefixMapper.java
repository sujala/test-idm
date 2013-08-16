package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/12/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonPrefixMapper {

    public JSONObject addPrefix(JSONObject object, HashMap prefixValues){

        for(Object key : prefixValues.keySet()){
            String[] elements = ((String)key).split("\\.");

            replaceJsonObject(object, elements, prefixValues.get(key).toString());
        }

        return object;
    }

    private void replaceJsonObject(JSONObject object, String[] elements, String newValue){
        if(object != null && object.containsKey(elements[0])){
            if(elements.length > 1){
                replaceJsonObject((JSONObject) object.get(elements[0]), Arrays.copyOfRange(elements, 1, elements.length), newValue);
            } else{
                object.put(newValue, object.get(elements[0]));
                object.remove(elements[0]);
            }
        }
    }

}
