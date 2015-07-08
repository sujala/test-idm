package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "region")
@NamedEntityGraph(name = "SqlRegion.rax", attributeNodes = @NamedAttributeNode("rax"))
public class SqlRegion {

    @Id
    @Column(name = "id", length = 255)
    private String name;

    @Column(name = "description", length = 255)
    @NotNull
    private String description;

    @Column(name = "parent_region_id", length = 255)
    private String parentRegionId;

    @Column(name = "extra")
    private String extra;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlRegionRax rax;
}
