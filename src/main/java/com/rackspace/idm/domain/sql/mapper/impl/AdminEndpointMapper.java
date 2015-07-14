package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlEndpointRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

import java.util.Map;

@SQLComponent
public class AdminEndpointMapper extends SqlRaxMapper<CloudBaseUrl, SqlEndpoint, SqlEndpointRax> {

    @Override
    public void overrideFields(Map<String, String> map){
        map.put("id", "adminUrlId");
        map.put("url", "adminUrl");
        map.put("legacyEndpointId", "baseUrlId");
    }

    @Override
    public void overrideRaxFields(Map<String, String> map){
        map.put("id", "adminUrlId");
    }
}
