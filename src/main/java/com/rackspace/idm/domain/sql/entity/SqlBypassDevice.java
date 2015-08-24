package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "bypass_device_rax")
@NamedEntityGraph(name = "SqlBypassDevice.codes", attributeNodes = @NamedAttributeNode("codes"))
public class SqlBypassDevice {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "multifactor_device_pin_expiration")
    private Date multiFactorDevicePinExpiration;

    @Column(name = "salt")
    private String salt;

    @Column(name = "iterations")
    private Integer iterations;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "id")
    private Set<SqlBypassCode> codes = new HashSet<SqlBypassCode>();

}
