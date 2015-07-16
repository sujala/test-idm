package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "mobile_phone_rax")
public class SqlMobilePhone {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "number", length = 64)
    private String telephoneNumber;

    @Column(name = "name", length = 64)
    private String cn;

    @Column(name = "external_id", length = 64)
    private String externalMultiFactorPhoneId;

}
