package com.rackspace.idm.domain.entity;

import java.util.List;

public class PermissionSet {
    private List<Permission> defineds;
    private List<Permission> granteds;

    public PermissionSet() {
    }

    public List<Permission> getDefineds() {
        return defineds;
    }

    public void setDefineds(List<Permission> defineds) {
        this.defineds = defineds;
    }

    public List<Permission> getGranteds() {
        return granteds;
    }

    public void setGranteds(List<Permission> granteds) {
        this.granteds = granteds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((defineds == null) ? 0 : defineds.hashCode());
        result = prime * result
            + ((granteds == null) ? 0 : granteds.hashCode());
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
        PermissionSet other = (PermissionSet) obj;
        if (defineds == null) {
            if (other.defineds != null) {
                return false;
            }
        } else if (!defineds.equals(other.defineds)) {
            return false;
        }
        if (granteds == null) {
            if (other.granteds != null) {
                return false;
            }
        } else if (!granteds.equals(other.granteds)) {
            return false;
        }
        return true;
    }
}
