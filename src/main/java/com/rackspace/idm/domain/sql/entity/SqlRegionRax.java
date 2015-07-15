package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "region_rax")
public class SqlRegionRax {

    @Id
    @Column(name = "id", length = 64)
    private String name;

    @Column(name = "enabled")
    private Boolean isEnabled;

    @Column(name = "cloud", length = 64)
    private String cloud;

    @Column(name = "default_region")
    private Boolean isDefault;

}
