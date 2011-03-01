package com.rackspace.idm.domain.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class Customer implements Auditable {

    private String uniqueId = null;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String customerId = null;
    private String inum = null;
    private String iname = null;
    private CustomerStatus status = null;
    private String seeAlso = null;
    private String owner = null;

    private Boolean isLocked = null;
    private Boolean softDeleted = null;

    public Customer() {
    }

    public Customer(String customerId, String inum, String iname,
        CustomerStatus status, String seeAlso, String owner) {
        this.customerId = customerId;
        this.inum = inum;
        this.iname = iname;
        this.status = status;
        this.seeAlso = seeAlso;
        this.owner = owner;
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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        if (customerId != null) {
            this.customerId = customerId;
        }
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        if (inum != null) {
            this.inum = inum;
        }
    }

    public String getIname() {
        return iname;
    }

    public void setIname(String iname) {
        if (iname != null) {
            this.iname = iname;
        }
    }

    public Boolean getIsLocked() {
        return this.isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        if (seeAlso != null) {
            this.seeAlso = seeAlso;
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        if (owner != null) {
            this.owner = owner;
        }
    }

    public void setDefaults() {
        this.status = CustomerStatus.ACTIVE;
        this.isLocked = false;
        this.softDeleted = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((iname == null) ? 0 : iname.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result
            + ((isLocked == null) ? 0 : isLocked.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
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
        if (customerId == null) {
            if (other.customerId != null) {
                return false;
            }
        } else if (!customerId.equals(other.customerId)) {
            return false;
        }
        if (iname == null) {
            if (other.iname != null) {
                return false;
            }
        } else if (!iname.equals(other.iname)) {
            return false;
        }
        if (inum == null) {
            if (other.inum != null) {
                return false;
            }
        } else if (!inum.equals(other.inum)) {
            return false;
        }
        if (isLocked == null) {
            if (other.isLocked != null) {
                return false;
            }
        } else if (!isLocked.equals(other.isLocked)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        if (seeAlso == null) {
            if (other.seeAlso != null) {
                return false;
            }
        } else if (!seeAlso.equals(other.seeAlso)) {
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
        return true;
    }

    @Override
    public String toString() {
        return "Customer [customerId=" + customerId + ", iname=" + iname
            + ", inum=" + inum + ", owner=" + owner + ", seeAlso=" + seeAlso
            + ", softDeleted=" + softDeleted + ", status=" + status
            + ", uniqueId=" + uniqueId + "]";
    }

    @Override
    public String getAuditContext() {
        String format = "customerId=%s";
        return String.format(format, customerId);
    }
}
