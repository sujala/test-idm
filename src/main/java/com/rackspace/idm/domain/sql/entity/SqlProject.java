package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "project")
@EqualsAndHashCode(exclude = {"baseUrlIds", "v1Defaults"})
@NamedEntityGraph(name = "SqlProject.rax",
        attributeNodes = {
                @NamedAttributeNode(value = "baseUrlIds"),
                @NamedAttributeNode(value = "v1Defaults"),
                @NamedAttributeNode(value = "baseUrlIds", subgraph = "sqlProject.baseUrlIds.rax"),
                @NamedAttributeNode(value = "v1Defaults", subgraph = "sqlProject.v1Defaults.rax"),
                @NamedAttributeNode(value = "baseUrlIds", subgraph = "sqlProject.baseUrlIds.rax.policyList"),
                @NamedAttributeNode(value = "v1Defaults", subgraph = "sqlProject.v1Defaults.rax.policyList")
        },
        subgraphs = {
                @NamedSubgraph(name = "sqlProject.baseUrlIds.rax", attributeNodes = @NamedAttributeNode("rax")),
                @NamedSubgraph(name = "sqlProject.v1Defaults.rax", attributeNodes = @NamedAttributeNode("rax")),
                @NamedSubgraph(name = "sqlProject.baseUrlIds.rax.policyList", attributeNodes = @NamedAttributeNode(value = "rax", subgraph = "policyList")),
                @NamedSubgraph(name = "sqlProject.v1Defaults.rax.policyList", attributeNodes = @NamedAttributeNode(value = "rax", subgraph = "policyList")),
                @NamedSubgraph(name = "policyList", attributeNodes = @NamedAttributeNode("policyList"))
        })
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

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinTable(name="project_endpoint", joinColumns={@JoinColumn(name="project_id", referencedColumnName="id")}, inverseJoinColumns={@JoinColumn(name="endpoint_id", referencedColumnName="id")})
    private Set<SqlEndpoint> baseUrlIds = new HashSet<SqlEndpoint>();

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinTable(name="project_endpoint_rax", joinColumns={@JoinColumn(name="project_id", referencedColumnName="id")}, inverseJoinColumns={@JoinColumn(name="endpoint_id", referencedColumnName="id")})
    private Set<SqlEndpoint> v1Defaults = new HashSet<SqlEndpoint>();

}
