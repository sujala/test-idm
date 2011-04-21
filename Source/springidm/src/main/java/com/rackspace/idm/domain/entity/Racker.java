package com.rackspace.idm.domain.entity;

public class Racker implements Auditable {

    private String uniqueId;
    private String rackerId;

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

    @Override
    public String getAuditContext() {
        return String.format("Racker(%s)", rackerId);
    }
}
