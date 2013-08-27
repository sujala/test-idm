package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LdapEndpointRepository extends LdapGenericRepository<CloudBaseUrl> implements EndpointDao {

    public String getBaseDn(){
        return BASEURL_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_BASEURL;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addBaseUrl(CloudBaseUrl baseUrl) {
        addObject(baseUrl);
    }

    @Override
    public void deleteBaseUrl(String baseUrlId) {
        deleteObject(searchFilterGetBaseUrlById(baseUrlId));
    }

    @Override
    public CloudBaseUrl getBaseUrlById(String baseUrlId) {
        return getObject(searchFilterGetBaseUrlById(baseUrlId));
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsByService(String service) {
        return getObjects(searchFilterGetBaseUrlByService(service));
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId) {
        return getObjects(searchFilterGetBaseUrlByPolicyId(policyId));
    }

    @Override
    public void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl) {
        updateObject(cloudBaseUrl);
    }

    @Override
    public List<CloudBaseUrl> getBaseUrls() {
        return getObjects(searchFilterGetBaseUrl());
    }

    @Override
    public List<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds) {
        return getObjects(searchFilterGetBaseUrlById(baseUrlIds));
    }

    private Filter searchFilterGetBaseUrlById(String baseUrlId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, baseUrlId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }

    private Filter searchFilterGetBaseUrlById(List<String> baseUrlIds) {
        List<Filter> orComponents = new ArrayList<Filter>();
        for (String baseUrlId : baseUrlIds) {
            orComponents.add(Filter.createEqualityFilter(ATTR_ID, baseUrlId));
        }

        return new LdapSearchBuilder()
                .addOrAttributes(orComponents)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }

    private Filter searchFilterGetBaseUrlByService(String service) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_SERVICE, service)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }

    private Filter searchFilterGetBaseUrlByPolicyId(String policyId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_POLICY_ID, policyId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }

    private Filter searchFilterGetBaseUrl() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }
}
