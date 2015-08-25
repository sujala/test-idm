package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.entity.SqlEndpointRax;
import com.rackspace.idm.domain.sql.entity.SqlPolicy;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SQLComponent
public class PublicEndpointMapper extends SqlRaxMapper<CloudBaseUrl, SqlEndpoint, SqlEndpointRax> {

    private static final String FORMAT = "rsId=%s,ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlEndpoint sqlEndpoint) {
        return new String[] {sqlEndpoint.getLegacyEndpointId()};
    }

    @Override
    public void overrideFields(Map<String, String> map){
        map.put("id", "publicUrlId");
        map.put("url", "publicUrl");
        map.put("legacyEndpointId", "baseUrlId");
    }

    @Override
    public void overrideRaxFields(Map<String, String> map){
        map.put("id", "publicUrlId");
    }

    public CloudBaseUrl fromSQL(SqlEndpoint entity) {
        CloudBaseUrl cloudBaseUrl = super.fromSQL(entity);

        if (cloudBaseUrl == null) {
            return cloudBaseUrl;
        }

        for (SqlPolicy sqlPolicy : entity.getRax().getSqlPolicy()) {
            cloudBaseUrl.getPolicyList().add(sqlPolicy.getPolicyId());
        }

        return cloudBaseUrl;
    }

    public SqlEndpoint toSQL(CloudBaseUrl entity) {
        SqlEndpoint sqlEndpoint = super.toSQL(entity);

        if (sqlEndpoint == null) {
            return sqlEndpoint;
        }

        for (String policyId : entity.getPolicyList()) {
            SqlPolicy sqlPolicy = new SqlPolicy();
            sqlPolicy.setPolicyId(policyId);
            sqlEndpoint.getRax().getSqlPolicy().add(sqlPolicy);
        }

        return sqlEndpoint;
    }

    @Override
    public List<String> validEmptyAttributes(){
        return new ArrayList<String>(Arrays.asList("tenantAlias"));
    }
}
