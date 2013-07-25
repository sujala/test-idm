package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.List;

@Data
public class Racker extends User implements Auditable {

    private String uniqueId;
    private String rackerId;
    private List<String> rackerRoles;

    public boolean isDisabled() {
    	return this.enabled == null ? false : !this.enabled;
    }

    @Override
    public String getAuditContext() {
        return String.format("Racker(%s)", rackerId);
    }
}
