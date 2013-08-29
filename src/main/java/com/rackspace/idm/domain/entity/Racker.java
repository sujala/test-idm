package com.rackspace.idm.domain.entity;

import lombok.Data;
import org.dozer.Mapping;

import java.util.List;

@Data
public class Racker extends User implements Auditable {

    private String uniqueId;

    @Mapping("id")
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
