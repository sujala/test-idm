package com.rackspace.idm.domain.entity;

import java.util.List;

public class Racker extends User implements Auditable {

    private String uniqueId;
    private String rackerId;
    private List<String> rackerRoles;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getRackerId() {
        return rackerId;
    }

    public void setRackerId(String rackerId) {
        this.rackerId = rackerId;
    }

    public List<String> getRackerRoles() {
        return rackerRoles;
    }

    public void setRackerRoles(List<String> rackerRoles) {
        this.rackerRoles = rackerRoles;
    }

    public boolean isDisabled() {
    	return this.enabled == null ? false : !this.enabled;
    }

    @Override
    public String getAuditContext() {
        return String.format("Racker(%s)", rackerId);
    }
}
