package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "endpoint")
@NamedEntityGraph(name = "SqlEndpoint.rax",
        attributeNodes = @NamedAttributeNode( value = "rax", subgraph = "sqlEndpointRax.sqlPolicyEndpointRax"),
        subgraphs = @NamedSubgraph(name = "sqlEndpointRax.sqlPolicyEndpointRax", attributeNodes = @NamedAttributeNode("sqlPolicyEndpointRax")))
public class SqlEndpoint {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "service_id", length = 255)
    private String clientId;

    @Column(name = "region_id")
    private String region;

    @Column(name = "legacy_endpoint_id")
    private String legacyEndpointId;

    @Column(name = "interface", length = 8)
    private String interface1;

    @Column(name = "url")
    @NotNull
    private String url;

    @Column(name = "extra")
    private String extra;

    @Column(name =  "enabled")
    private boolean enabled;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = true)
    private SqlEndpointRax rax;
}
