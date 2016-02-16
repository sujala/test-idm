package com.rackspace.idm.api.resource.cloud;

import org.json.simple.JSONObject;

public interface JsonArrayEntryTransformer {

    void transform(JSONObject arrayEntry);

}
