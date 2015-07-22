package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SqlAssignmentId implements Serializable {
    private String actorId;
    private String targetId;
    private String roleId;
}
