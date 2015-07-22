package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "assignment")
@EqualsAndHashCode(callSuper=false)
@DiscriminatorValue("UserProject")
@NamedEntityGraph(name = "SqlTenantRole.sqlRole",
        attributeNodes = @NamedAttributeNode(value = "sqlRole", subgraph = "sqlRole.rax"),
        subgraphs = @NamedSubgraph(name = "sqlRole.rax", attributeNodes = @NamedAttributeNode("rax")))
public class SqlTenantRole extends SqlAssignment{

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = true, updatable = false, insertable = false)
    private SqlRole sqlRole;
}