package com.rackspace.idm.domain.entity;

import java.util.List;

public class OpenstackEndpoint {
    private String tenantId = null;
    private String tenantName = null;
    private List<CloudBaseUrl> baseUrls = null;
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public List<CloudBaseUrl> getBaseUrls() {
        return baseUrls;
    }
    
    public void setBaseUrls(List<CloudBaseUrl> baseUrls) {
        this.baseUrls = baseUrls;
    }
}
