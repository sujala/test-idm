package com.rackspace.idm.domain.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class Customer implements Auditable {

    private String uniqueId = null;
    
    private String id = null;
    
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String rcn = null;

    private Boolean enabled = null;
    
    private Integer passwordRotationDuration; // this is in days
    private Boolean passwordRotationEnabled = null;

    public Customer() {
    }
    
    public Boolean getPasswordRotationEnabled() {
        return passwordRotationEnabled == null ? false : passwordRotationEnabled;
    }
    
    public void setPasswordRotationEnabled(Boolean passwordRotationEnabled) {
        this.passwordRotationEnabled = passwordRotationEnabled;
    }
    
    public Integer getPasswordRotationDuration() {
        return passwordRotationDuration;
    }
    
    public void setPasswordRotationDuration(Integer passwordRotationDuration) {
        this.passwordRotationDuration = passwordRotationDuration;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getRcn() {
        return rcn;
    }

    public void setRcn(String rcn) {
        if (rcn != null) {
            this.rcn = rcn;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public void setDefaults() {
        this.enabled = true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rcn == null) ? 0 : rcn.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime
            * result
            + ((passwordRotationDuration == null) ? 0
                : passwordRotationDuration.hashCode());
        result = prime
            * result
            + ((passwordRotationEnabled == null) ? 0 : passwordRotationEnabled
                .hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
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
        Customer other = (Customer) obj;
        if (rcn == null) {
            if (other.rcn != null) {
                return false;
            }
        } else if (!rcn.equals(other.rcn)) {
            return false;
        }
        if (enabled == null) {
            if (other.enabled != null) {
                return false;
            }
        } else if (!enabled.equals(other.enabled)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (passwordRotationDuration == null) {
            if (other.passwordRotationDuration != null) {
                return false;
            }
        } else if (!passwordRotationDuration
            .equals(other.passwordRotationDuration)) {
            return false;
        }
        if (passwordRotationEnabled == null) {
            if (other.passwordRotationEnabled != null) {
                return false;
            }
        } else if (!passwordRotationEnabled
            .equals(other.passwordRotationEnabled)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
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
        String format = "id=%s,rcn=%s";
        return String.format(format, getId(), rcn);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isEnabled() {
        return enabled;
    }
}
