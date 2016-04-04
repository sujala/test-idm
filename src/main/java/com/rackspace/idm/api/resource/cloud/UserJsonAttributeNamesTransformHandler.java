package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.JSONConstants;

public class UserJsonAttributeNamesTransformHandler extends AlwaysPluralizeJsonArrayTransformerHandler {

    @Override
    public String getPluralizedNamed(String elementName) {
        if (JSONConstants.GROUP.equals(elementName)) {
            return JSONConstants.RAX_KSGRP_GROUPS;
        } else {
            return elementName + "s";
        }
    }

}
