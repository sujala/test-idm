package com.rackspace.idm.domain.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class CloudBaseUrl {
    private Integer baseUrlId = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String baseUrlType = null;
    private String region = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String service = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String publicUrl = null;
    private String internalUrl = null;
    private String adminUrl = null;
    private Boolean def = null;
    private Boolean enabled = null;
    
    public Integer getBaseUrlId() {
        return baseUrlId;
    }
    
    public void setBaseUrlId(Integer baseUrlId) {
        this.baseUrlId = baseUrlId;
    }
    
    public String getBaseUrlType() {
        return baseUrlType;
    }
    
    public void setBaseUrlType(String baseUrlType) {
        this.baseUrlType = baseUrlType;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
    
    public String getPublicUrl() {
        return publicUrl;
    }
    
    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }
    
    public String getInternalUrl() {
        return internalUrl;
    }
    
    public void setInternalUrl(String internalUrl) {
        this.internalUrl = internalUrl;
    }
    
    public String getAdminUrl() {
        return adminUrl;
    }
    
    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }
    
    public Boolean getDef() {
        return def;
    }
    
    public void setDef(Boolean def) {
        this.def = def;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((adminUrl == null) ? 0 : adminUrl.hashCode());
        result = prime * result
            + ((baseUrlId == null) ? 0 : baseUrlId.hashCode());
        result = prime * result
            + ((baseUrlType == null) ? 0 : baseUrlType.hashCode());
        result = prime * result + ((def == null) ? 0 : def.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result
            + ((internalUrl == null) ? 0 : internalUrl.hashCode());
        result = prime * result
            + ((publicUrl == null) ? 0 : publicUrl.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudBaseUrl other = (CloudBaseUrl) obj;
        if (adminUrl == null) {
            if (other.adminUrl != null) {
                return false;
            }
        } else if (!adminUrl.equals(other.adminUrl)) {
            return false;
        }
        if (baseUrlId == null) {
            if (other.baseUrlId != null) {
                return false;
            }
        } else if (!baseUrlId.equals(other.baseUrlId)) {
            return false;
        }
        if (baseUrlType == null) {
            if (other.baseUrlType != null) {
                return false;
            }
        } else if (!baseUrlType.equals(other.baseUrlType)) {
            return false;
        }
        if (def == null) {
            if (other.def != null) {
                return false;
            }
        } else if (!def.equals(other.def)) {
            return false;
        }
        if (enabled == null) {
            if (other.enabled != null) {
                return false;
            }
        } else if (!enabled.equals(other.enabled)) {
            return false;
        }
        if (internalUrl == null) {
            if (other.internalUrl != null) {
                return false;
            }
        } else if (!internalUrl.equals(other.internalUrl)) {
            return false;
        }
        if (publicUrl == null) {
            if (other.publicUrl != null) {
                return false;
            }
        } else if (!publicUrl.equals(other.publicUrl)) {
            return false;
        }
        if (region == null) {
            if (other.region != null) {
                return false;
            }
        } else if (!region.equals(other.region)) {
            return false;
        }
        if (service == null) {
            if (other.service != null) {
                return false;
            }
        } else if (!service.equals(other.service)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CloudBaseUrl [baseUrlId=" + baseUrlId + ", baseUrlType="
            + baseUrlType + ", region=" + region + ", service=" + service
            + ", publicUrl=" + publicUrl + ", internalUrl=" + internalUrl
            + ", adminUrl=" + adminUrl + ", def=" + def + ", enabled="
            + enabled + "]";
    }
}
