package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "service_rax")
public class SqlServiceRax {

    @Id
    @Column(name = "id", length = 255)
    private String clientId;

    @Column(name = "default_region", nullable = false)
    private boolean useForDefaultRegion;
}
