package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@Table(name = "user_certificate_rax")
@EqualsAndHashCode(exclude = "identityProvider")
@ToString(exclude = "identityProvider")
public class SqlUserCertificate {

    @ManyToOne(optional = false)
    @JoinColumn(name = "identity_provider_id")
    private SqlIdentityProvider identityProvider;

    @Id
    @Column(name = "id", length = 64)
    private String uuid;

    @Column(name = "certificate")
    private byte[] certificate;

}
