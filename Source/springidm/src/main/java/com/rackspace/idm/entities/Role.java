package com.rackspace.idm.entities;

import java.io.Serializable;
import java.util.List;

public class Role implements Serializable {
    private static final long serialVersionUID = 875666210673489138L;
    
    private String uniqueId = null;
    private String name = null;
    private String customerId = null;
    private String country = null;
    private String inum = null;
    private String iname = null;
    private String orgInum = null;
    private String owner = null;
    private RoleStatus status = RoleStatus.ACTIVE;
    private String seeAlso = null;
    private String type = null;
    private List<Permission> permissions;

    public Role() {
    }

    public Role(String uniqueId, String name, String customerId,
        String country, String inum, String iname, String orgInum,
        String owner, RoleStatus status, String seeAlso, String type) {
        this.name = name;
        this.customerId = customerId;
        this.country = country;
        this.inum = inum;
        this.iname = iname;
        this.orgInum = orgInum;
        this.owner = owner;
        this.status = status;
        this.seeAlso = seeAlso;
        this.type = type;
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        if (country != null) {
            this.country = country;
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

    public String getOrgInum() {
        return orgInum;
    }

    public void setOrgInum(String orgInum) {
        if (orgInum != null) {
            this.orgInum = orgInum;
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

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null) {
            this.type = type;
        }
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((country == null) ? 0 : country.hashCode());
        result = prime * result
            + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((iname == null) ? 0 : iname.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((orgInum == null) ? 0 : orgInum.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
        result = prime * result
            + ((status == null) ? 0 : status.toString().hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Role other = (Role) obj;
        if (country == null) {
            if (other.country != null) {
                return false;
            }
        } else if (!country.equals(other.country)) {
            return false;
        }
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
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (orgInum == null) {
            if (other.orgInum != null) {
                return false;
            }
        } else if (!orgInum.equals(other.orgInum)) {
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
        if (status == null) {
            if (other.status != null) {
                return false;
            }
        } else if (!status.equals(other.status)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
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
        return "Role [country=" + country + ", customerId=" + customerId
            + ", iname=" + iname + ", inum=" + inum + ", name=" + name
            + ", orgInum=" + orgInum + ", owner=" + owner + ", seeAlso="
            + seeAlso + ", status=" + status + ", type=" + type + ", uniqueId="
            + uniqueId + "]";
    }
}
