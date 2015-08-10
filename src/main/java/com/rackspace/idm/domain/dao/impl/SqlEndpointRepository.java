package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.sql.dao.EndpointRepository;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import com.rackspace.idm.domain.sql.mapper.impl.AdminEndpointMapper;
import com.rackspace.idm.domain.sql.mapper.impl.InternalEndpointMapper;
import com.rackspace.idm.domain.sql.mapper.impl.PublicEndpointMapper;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@SQLComponent
public class SqlEndpointRepository implements EndpointDao {

    private static final String PUBLIC_INTERFACE = "public";
    private static final String INTERNAL_INTERFACE = "internal";
    private static final String ADMIN_INTERFACE = "admin";
    private static final String LON_REGION = "LON";

    @Autowired
    PublicEndpointMapper publicEndpointMapper;

    @Autowired
    InternalEndpointMapper internalEndpointMapper;

    @Autowired
    AdminEndpointMapper adminEndpointMapper;

    @Autowired
    EndpointRepository endpointRepository;

    @Override
    public void addBaseUrl(CloudBaseUrl baseUrl) {
        SqlEndpoint publicEndpoint = publicEndpointMapper.toSQL(baseUrl);
        if(publicEndpoint.getUrl() != null){
            publicEndpoint.setInterface1(PUBLIC_INTERFACE);
            endpointRepository.save(publicEndpoint);
        }

        SqlEndpoint internalEndpoint = internalEndpointMapper.toSQL(baseUrl);
        if(internalEndpoint.getUrl() != null){
            internalEndpoint.setInterface1(INTERNAL_INTERFACE);
            endpointRepository.save(internalEndpoint);
        }

        SqlEndpoint adminEndpoint = adminEndpointMapper.toSQL(baseUrl);
        if(adminEndpoint.getUrl() != null){
            adminEndpoint.setInterface1(ADMIN_INTERFACE);
            endpointRepository.save(adminEndpoint);
        }
    }

    @Override
    public void deleteBaseUrl(String baseUrlId) {
        List<SqlEndpoint> endpoints = endpointRepository.findByLegacyEndpointId(baseUrlId);

        endpointRepository.delete(endpoints);
    }

    @Override
    public CloudBaseUrl getBaseUrlById(String baseUrlId) {
        List<SqlEndpoint> endpoints = endpointRepository.findByLegacyEndpointId(baseUrlId);

        return buildCloudBaseUrl(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrlsByService(String service) {
        List<SqlEndpoint> endpoints = endpointRepository.findByRaxServiceName(service);

        return getCloudBaseUrls(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getDefaultBaseUrlsByBaseUrlTypeAndEnabled(String baseUrlType, boolean enabled) {
        List<SqlEndpoint> endpoints = endpointRepository.findByRaxBaseUrlTypeAndEnabledAndRaxDefTrue(baseUrlType, enabled);

        return getCloudBaseUrls(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getGlobalUSBaseUrlsByBaseUrlType(String baseUrlType) {
        List<SqlEndpoint> endpoints = endpointRepository.findByRegionNotAndRaxBaseUrlType(LON_REGION, baseUrlType);

        return getCloudBaseUrls(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getGlobalUKBaseUrlsByBaseUrlType(String baseUrlType) {
        List<SqlEndpoint> endpoints = endpointRepository.findByRegionAndRaxBaseUrlType(LON_REGION, baseUrlType);

        return getCloudBaseUrls(endpoints);

    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrlsWithPolicyId(String policyId) {
        List<SqlEndpoint> endpoints = endpointRepository.findByRaxSqlPolicyPolicyId(policyId);

        return getCloudBaseUrls(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrls() {
        List<SqlEndpoint> endpoints = endpointRepository.findAll();

        return getCloudBaseUrls(endpoints);
    }

    @Override
    public Iterable<CloudBaseUrl> getBaseUrlsById(List<String> baseUrlIds) {
        if(baseUrlIds.isEmpty()){
            return new ArrayList<CloudBaseUrl>();
        }
        List<SqlEndpoint> endpoints = endpointRepository.findByLegacyEndpointIdIn(baseUrlIds);
        return getCloudBaseUrls(endpoints);
    }

    @Override
    public void updateCloudBaseUrl(CloudBaseUrl cloudBaseUrl) {
        SqlEndpoint publicEndpoint = publicEndpointMapper.toSQL(cloudBaseUrl);
        publicEndpoint.setInterface1(PUBLIC_INTERFACE);
        endpointRepository.save(publicEndpoint);

        if(!StringUtils.isEmpty(cloudBaseUrl.getInternalUrl())){
            SqlEndpoint internalEndpoint = internalEndpointMapper.toSQL(cloudBaseUrl);
            internalEndpoint.setInterface1(INTERNAL_INTERFACE);
            endpointRepository.save(internalEndpoint);
        }

        if(!StringUtils.isEmpty(cloudBaseUrl.getAdminUrl())){
            SqlEndpoint adminEndpoint = adminEndpointMapper.toSQL(cloudBaseUrl);
            adminEndpoint.setInterface1(ADMIN_INTERFACE);
            endpointRepository.save(adminEndpoint);
        }

    }

    private CloudBaseUrl buildCloudBaseUrl(List<SqlEndpoint> endpoints){
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();

        for(SqlEndpoint endpoint : endpoints){
            if(PUBLIC_INTERFACE.equalsIgnoreCase(endpoint.getInterface1())){
                cloudBaseUrl = publicEndpointMapper.fromSQL(endpoint);
                break;
            }
        }

        if(cloudBaseUrl.getPublicUrl() == null){
            return null;
        }

        for(SqlEndpoint endpoint : endpoints){
            if(INTERNAL_INTERFACE.equalsIgnoreCase(endpoint.getInterface1())){
                cloudBaseUrl.setInternalUrlId(endpoint.getId());
                cloudBaseUrl.setInternalUrl(endpoint.getUrl());
            }
            if(ADMIN_INTERFACE.equalsIgnoreCase(endpoint.getInterface1())){
                cloudBaseUrl.setAdminUrlId(endpoint.getId());
                cloudBaseUrl.setAdminUrl(endpoint.getUrl());
            }
        }
        return cloudBaseUrl;
    }

    private List<CloudBaseUrl> getCloudBaseUrls(List<SqlEndpoint> endpoints) {
        HashMap<String, List<SqlEndpoint>> cloudBaseUrlMap = new HashMap<String, List<SqlEndpoint>>();
        for(SqlEndpoint endpoint : endpoints){
            String legacyEndpointId = endpoint.getLegacyEndpointId();
            if(legacyEndpointId == null){
                continue;
            }
            if(cloudBaseUrlMap.containsKey(legacyEndpointId)){
                cloudBaseUrlMap.get(legacyEndpointId).add(endpoint);
            } else {
                List<SqlEndpoint> endpointList = new ArrayList<SqlEndpoint>();
                endpointList.add(endpoint);
                cloudBaseUrlMap.put(legacyEndpointId, endpointList);
            }
        }

        List<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        for(String key : cloudBaseUrlMap.keySet()){
            CloudBaseUrl cloudBaseUrl = buildCloudBaseUrl(cloudBaseUrlMap.get(key));
            if(cloudBaseUrl != null){
                cloudBaseUrls.add(cloudBaseUrl);
            }
        }
        return cloudBaseUrls;
    }
}
