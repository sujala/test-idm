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
    private String RCN = null;
    private CustomerStatus status = null;

    private Boolean locked = null;
    private Boolean softDeleted = null;
    
    private Integer passwordRotationDuration; // this is in days
    private Boolean passwordRotationEnabled = null;

    public Customer() {
    }

    public Customer(String customerId,
        CustomerStatus status) {
        this.RCN = customerId;
        this.status = status;
    }
    
    public Boolean getPasswordRotationEnabled() {
        return passwordRotationEnabled;
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

    public Boolean getSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getRCN() {
        return RCN;
    }

    public void setRCN(String rcn) {
        if (rcn != null) {
            this.RCN = rcn;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean isLocked() {
        return this.locked;
    }

    public void setLocked(Boolean isLocked) {
        this.locked = isLocked;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void setDefaults() {
        this.status = CustomerStatus.ACTIVE;
        this.locked = false;
        this.softDeleted = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((RCN == null) ? 0 : RCN.hashCode());
        result = prime * result
            + ((locked == null) ? 0 : locked.hashCode());
        result = prime * result
            + ((softDeleted == null) ? 0 : softDeleted.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
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
        if (RCN == null) {
            if (other.RCN != null) {
                return false;
            }
        } else if (!RCN.equals(other.RCN)) {
            return false;
        }
        if (locked == null) {
            if (other.locked != null) {
                return false;
            }
        } else if (!locked.equals(other.locked)) {
            return false;
        }
        if (softDeleted == null) {
            if (other.softDeleted != null) {
                return false;
            }
        } else if (!softDeleted.equals(other.softDeleted)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (passwordRotationEnabled != null) {
            if (other.passwordRotationEnabled != null) {
                if (passwordRotationEnabled != other.passwordRotationEnabled) {
                    return false;
                }
            }
            if (other.passwordRotationEnabled == null) {
                return false;
            }
        }
        if (passwordRotationDuration != other.passwordRotationDuration) {
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
        String format = "id=%s,RCN=%s";
        return String.format(format, getId(), RCN);
    }
}
