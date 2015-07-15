package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "property_rax")
public class SqlProperty {

    @Id
    @Column(name = "id", length = 64)
    private String name;

    @Column(name = "value")
    private String value;

}
