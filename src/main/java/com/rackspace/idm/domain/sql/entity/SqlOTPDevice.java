package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "otp_device_rax")
public class SqlOTPDevice {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "multifactor_device_verified")
    private boolean multiFactorDeviceVerified;

    @Column(name = "`name`")
    private String name;

    @Column(name = "`key`")
    private String key;

}
