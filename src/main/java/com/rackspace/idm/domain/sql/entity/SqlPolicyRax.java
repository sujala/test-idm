package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "policy_rax")
public class SqlPolicyRax {

    @Id
    @Column(name = "id", length = 64)
    private String policyId;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "global")
    private Boolean global;

    @Column(name = "description")
    private String description;
}
