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

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "domain")
    private Set<SqlProject> sqlProject = new HashSet<SqlProject>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlDomainRax rax;
}
