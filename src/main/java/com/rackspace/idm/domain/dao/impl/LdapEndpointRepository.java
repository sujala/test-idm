package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@LDAPComponent
public class LdapEndpointRepository extends LdapGenericRepository<CloudBaseUrl> implements EndpointDao {

    private static final String ENCODED_BLANK_SPACE = "<blank>";

    public String getBaseDn(){
        return BASEURL_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_BASEURL;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    /*
     * directoryString cannot be of size zero.  Encode empty string '' to a '<blank>'
     */
    @Override
    public void doPreEncode(CloudBaseUrl baseUrl) {
        String tenantAlias = baseUrl.getTenantAlias();
        if (StringUtils.isWhitespace(tenantAlias)) {
            baseUrl.setTenantAlias(ENCODED_BLANK_SPACE);
        }
    }

    /*
     * directoryString cannot be of size zero.  Decode '<blank>' to a empty string ''
     */
    @Override
    public void doPostEncode(CloudBaseUrl baseUrl) {
        String tenantAlias = baseUrl.getTenantAlias();
        if (ENCODED_BLANK_SPACE.equals(tenantAlias)) {
            baseUrl.setTenantAlias("");
        }
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
    public Iterable<CloudBaseUrl> getBaseUrlsByService(String service) {
        return getObjects(searchFilterGetBaseUrlByService(service));
    }

    @Override
    public Iterable<CloudBaseUrl> getDefaultBaseUrlsByBaseUrlTypeAndEnabled(String baseUrlType, boolean enabled) {
        return getObjects(searchFilterGetDefaultBaseUrlsByBaseUrlType(baseUrlType, enabled));
    }

    @Override
    public Iterable<CloudBaseUrl> getGlobalUSBaseUrlsByBaseUrlType(String baseUrlType) {
        return getObjects(searchFilterGetGlobalUSBaseurlsByBaseUrlType(baseUrlType));
    }

    @Override
    public Iterable<CloudBaseUrl> getGlobalUKBaseUrlsByBaseUrlType(String baseUrlType) {
        return getObjects(searchFilterGetGlobalUKBaseurlsByBaseUrlType(baseUrlType));
    }

    @Override
    public void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl) {
        updateObject(cloudBaseUrl);
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrls() {
        return getObjects(searchFilterGetBaseUrl());
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds) {
        return getObjects(searchFilterGetBaseUrlById(baseUrlIds));
    }

    @Override
    public int getBaseUrlCount() {
        return countObjects(searchFilterGetBaseUrl());
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

    private Filter searchFilterGetDefaultBaseUrlsByBaseUrlType(String baseUrlType, boolean enabled) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_BASEURL_TYPE, baseUrlType)
                .addEqualAttribute(ATTR_DEF, Boolean.toString(true).toUpperCase())
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(enabled).toUpperCase())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }

    private Filter searchFilterGetGlobalUSBaseurlsByBaseUrlType(String baseUrlType) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_BASEURL_TYPE, baseUrlType)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL)
                .addEqualAttribute(ATTR_GLOBAL, Boolean.toString(true).toUpperCase())
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
                .addNotEqualAttribute(ATTR_REGION, "LON").build();
    }

    private Filter searchFilterGetGlobalUKBaseurlsByBaseUrlType(String baseUrlType) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_BASEURL_TYPE, baseUrlType)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL)
                .addEqualAttribute(ATTR_GLOBAL, Boolean.toString(true).toUpperCase())
                .addEqualAttribute(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
                .addEqualAttribute(ATTR_REGION, "LON").build();
    }

    private Filter searchFilterGetBaseUrl() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL).build();
    }
}
