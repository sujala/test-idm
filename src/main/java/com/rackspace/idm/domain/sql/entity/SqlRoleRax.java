package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "role_rax")
public class SqlRoleRax {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "description")
    private String description;

    @Column(name = "weight")
    private Integer rsWeight;

    @Column(name = "propagate")
    private Boolean propagate;

    @Column(name = "client_id", length = 64)
    private String clientId;
}
