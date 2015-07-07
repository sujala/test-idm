package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "bypass_code_rax")
public class SqlBypassCode {

    @Id
    @Column(name = "code", length = 255)
    private String code;

    @Column(name = "bypass_device_rax_id", length = 64)
    private String id;

}
