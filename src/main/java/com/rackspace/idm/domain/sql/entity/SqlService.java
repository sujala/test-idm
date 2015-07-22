package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "service")
@NamedEntityGraph(name = "SqlService.rax", attributeNodes = @NamedAttributeNode("rax"))
public class SqlService {

    @Id
    @Column(name = "id", length = 255)
    private String clientId;

    @Column(name = "type", length = 255)
    private String openStackType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "extra")
    private String extra;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = true)
    private SqlServiceRax rax;
}
