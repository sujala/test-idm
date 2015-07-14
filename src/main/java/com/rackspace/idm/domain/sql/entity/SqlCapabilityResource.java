package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "capability_resource_rax")
public class SqlCapabilityResource {

    @Id
    @Column(name = "capability_id", length = 64)
    private String id;

    @Column(name = "resource", length = 255)
    private String resource;
}
