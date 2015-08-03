package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "project")
public class SqlProject {

    @Id
    @Column(name = "id", length = 64)
    private String tenantId;

    @Column(name = "name", length = 64, unique = true)
    @NotNull
    private String name;

    @Column(name = "extra")
    private String extra;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "domain_id", nullable = false)
    private String domainId;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinTable(name="project_endpoint", joinColumns={@JoinColumn(name="project_id", referencedColumnName="id")}, inverseJoinColumns={@JoinColumn(name="endpoint_id", referencedColumnName="id")})
    private Set<SqlEndpoint> baseUrlIds = new HashSet<SqlEndpoint>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinTable(name="project_endpoint_rax", joinColumns={@JoinColumn(name="project_id", referencedColumnName="id")}, inverseJoinColumns={@JoinColumn(name="endpoint_id", referencedColumnName="id")})
    private Set<SqlEndpoint> v1Defaults = new HashSet<SqlEndpoint>();
}
