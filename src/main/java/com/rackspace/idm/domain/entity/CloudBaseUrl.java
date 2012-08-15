package com.rackspace.idm.domain.entity;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class CloudBaseUrl implements Auditable {
    private String uniqueId = null;
    private Integer baseUrlId = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String baseUrlType = null;
    private String region = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String serviceName = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String publicUrl = null;
    private String internalUrl = null;
    private String adminUrl = null;
    private Boolean def = null;
    private Boolean enabled = null;
    
    private String openstackType = null;
    //private String name = null;
    private Boolean global = null;
    
    private String versionId = null;
    private String versionInfo = null;
    private String versionList = null;
    private Boolean v1Default;

    public String getUniqueId() {
        return uniqueId;
    }
    
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    
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
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Boolean isV1Default() {
        return v1Default;
    }

    public Boolean setV1Default(Boolean v1Default) {
        return this.v1Default = v1Default;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getOpenstackType() {
        return openstackType;
    }

    public void setOpenstackType(String openstackType) {
        this.openstackType = openstackType;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public String getVersionList() {
        return versionList;
    }

    public void setVersionList(String versionList) {
        this.versionList = versionList;
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
        result = prime * result + ((global == null) ? 0 : global.hashCode());
        result = prime * result
            + ((internalUrl == null) ? 0 : internalUrl.hashCode());
        result = prime * result
            + ((openstackType == null) ? 0 : openstackType.hashCode());
        result = prime * result
            + ((publicUrl == null) ? 0 : publicUrl.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result
            + ((versionId == null) ? 0 : versionId.hashCode());
        result = prime * result
            + ((versionInfo == null) ? 0 : versionInfo.hashCode());
        result = prime * result
            + ((versionList == null) ? 0 : versionList.hashCode());
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
        if (global == null) {
            if (other.global != null) {
                return false;
            }
        } else if (!global.equals(other.global)) {
            return false;
        }
        if (internalUrl == null) {
            if (other.internalUrl != null) {
                return false;
            }
        } else if (!internalUrl.equals(other.internalUrl)) {
            return false;
        }
        if (openstackType == null) {
            if (other.openstackType != null) {
                return false;
            }
        } else if (!openstackType.equals(other.openstackType)) {
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
        if (serviceName == null) {
            if (other.serviceName != null) {
                return false;
            }
        } else if (!serviceName.equals(other.serviceName)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (versionId == null) {
            if (other.versionId != null) {
                return false;
            }
        } else if (!versionId.equals(other.versionId)) {
            return false;
        }
        if (versionInfo == null) {
            if (other.versionInfo != null) {
                return false;
            }
        } else if (!versionInfo.equals(other.versionInfo)) {
            return false;
        }
        if (versionList == null) {
            if (other.versionList != null) {
                return false;
            }
        } else if (!versionList.equals(other.versionList)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
    
    @Override
    public String getAuditContext() {
        String format = "baseUrl=%s";
        return String.format(format, getBaseUrlId());
    }
}
