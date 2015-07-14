package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "capability_rax")
public class SqlCapability {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "url")
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "version")
    private String version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "id")
    private List<SqlCapabilityResource> resources;
}
