package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "bypass_device_rax")
public class SqlBypassDevice {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    /*
     * Foreign key: 'fk_bdr_user_id'
     * Table: bypass_device_rax
     * Key: user_id
     *
     * Reference table: user
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @Column(name = "user_id")
    private String userId;

    @Column(name = "multifactor_device_pin_expiration")
    private Date multiFactorDevicePinExpiration;

    @Column(name = "salt")
    private String salt;

    @Column(name = "iterations")
    private Integer iterations;

     /*
     * Foreign key: 'fk_bcr_bypass_device_rax_id'
     * Table: bypass_code_rax
     * Key: bypass_device_rax_id
     *
     * Reference table: bypass_device_rax
     * Key: id
     *
     * OnDelete: CASCADE
     */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bypass_code_rax",
            joinColumns = @JoinColumn(name = "bypass_device_rax_id"))
    @Column(name = "code")
    private Set<String> bypassCodes = new HashSet<String>();

}
