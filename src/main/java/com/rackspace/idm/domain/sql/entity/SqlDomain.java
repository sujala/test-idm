package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

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

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinTable(name = "project_domain_rax",
            joinColumns={@JoinColumn(name="domain_id", referencedColumnName="id")},
            inverseJoinColumns={@JoinColumn(name="project_id", referencedColumnName="id")})
    private Set<SqlProject> sqlProject;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlDomainRax rax;

}
