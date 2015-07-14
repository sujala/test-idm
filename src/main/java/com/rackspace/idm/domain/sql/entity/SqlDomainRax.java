package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "domain_rax")
public class SqlDomainRax {

    @Id
    @Column(name = "id")
    private String domainId;

    @Column(name = "mfa_enforcement_level", length = 64)
    private String domainMultiFactorEnforcementLevel;
}
