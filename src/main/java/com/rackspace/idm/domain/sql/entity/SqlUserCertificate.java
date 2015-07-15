package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "user_certificate_rax")
public class SqlUserCertificate {

    @Id
    @Column(name = "identity_provider_id", length = 64)
    private String id;

    @Column(name = "id", length = 64)
    private String uuid;

    @Column(name = "certificate")
    private byte[] certificate;

}
