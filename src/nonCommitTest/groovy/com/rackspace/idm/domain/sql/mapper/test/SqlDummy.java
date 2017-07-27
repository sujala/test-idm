package com.rackspace.idm.domain.sql.mapper.test;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "dummy")
public class SqlDummy {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", length = 64, unique = true)
    @NotNull
    private String name;
}
