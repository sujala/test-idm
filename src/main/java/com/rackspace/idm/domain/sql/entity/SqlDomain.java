package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "domain")
@EqualsAndHashCode(exclude = "sqlProject")
@ToString(exclude = "sqlProject")
@NamedEntityGraph(name = "SqlDomain.rax",
        attributeNodes = {
                @NamedAttributeNode("rax"),
                @NamedAttributeNode("sqlProject"),
                @NamedAttributeNode(value = "sqlProject", subgraph = "sqlProject.baseUrlIds"),
                @NamedAttributeNode(value = "sqlProject", subgraph = "sqlProject.v1Defaults"),
                @NamedAttributeNode(value = "sqlProject", subgraph = "sqlProject.baseUrlIds.rax"),
                @NamedAttributeNode(value = "sqlProject", subgraph = "sqlProject.v1Defaults.rax")
        },
        subgraphs = {
                @NamedSubgraph(name = "sqlProject.baseUrlIds", attributeNodes = @NamedAttributeNode("baseUrlIds")),
                @NamedSubgraph(name = "sqlProject.v1Defaults", attributeNodes = @NamedAttributeNode("v1Defaults")),
                @NamedSubgraph(name = "sqlProject.baseUrlIds.rax", attributeNodes = @NamedAttributeNode(value = "baseUrlIds", subgraph = "sqlEndpoint.rax")),
                @NamedSubgraph(name = "sqlProject.v1Defaults.rax", attributeNodes = @NamedAttributeNode(value = "v1Defaults", subgraph = "sqlEndpoint.rax")),
                @NamedSubgraph(name = "sqlEndpoint.rax", attributeNodes = @NamedAttributeNode("rax"))
        })
public class SqlDomain {

    @Id
    @Column(name = "id", length = 64)
    private String domainId;

    @Column(name = "name", length = 64, unique = true)
    @NotNull
    private String name;

    @Column(name = "enabled")
    @NotNull
    private boolean enabled;

    @Column(name = "extra")
    private String extra;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "domainId")
    private Set<SqlProject> sqlProject = new HashSet<SqlProject>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlDomainRax rax;

}
