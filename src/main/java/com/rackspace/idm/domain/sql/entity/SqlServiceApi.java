package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "endpoint_rax")
public class SqlServiceApi {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name =  "version_id")
    private String version;

    @Column(name = "openstack_type")
    private String type;
}
