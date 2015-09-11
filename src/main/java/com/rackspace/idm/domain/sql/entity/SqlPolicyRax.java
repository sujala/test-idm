package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "policy_rax")
public class SqlPolicyRax {

    /*
     * Foreign key: 'fk_por_policy'
     * Table: policy_rax
     * Key: id
     *
     * Reference table: policy
     * Key: id
     *
     * OnDelete: CASCADE
     */

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
