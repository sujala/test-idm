package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "policy")
public class SqlPolicy {

    @Id
    @Column(name = "id", length = 64)
    private String policyId;

    @Column(name = "type", length = 255)
    @NotNull
    private String policyType;

    @Column(name = "`blob`")
    @NotNull
    private String blob;

    @Column(name = "extra")
    private String extra;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = false)
    private SqlPolicyRax rax;

}
