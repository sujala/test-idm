package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "domain")
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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlDomainRax rax;
}
