package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "`group`")
public class SqlGroup {

    @Id
    @Column(name = "id", length = 64)
    private String groupId;

    @Column(name = "domain_id", length = 64, unique = true)
    @NotNull
    private String domainId;

    @Column(name = "name", length = 64, unique = true)
    @NotNull
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "extra")
    private String extra;
}
