package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "role")
public class SqlRole {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", length = 255, unique = true)
    @NotNull
    private String name;

    @Column(name = "extra")
    private String extra;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlRoleRax rax;
}
