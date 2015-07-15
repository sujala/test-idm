package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "identity_provider_rax")
public class SqlIdentityProvider {

    @Id
    @Column(name = "id", length = 64)
    private String name;

    @Column(name = "uri")
    private String uri;

    @Column(name = "description")
    private String description;

    @Column(name = "public_certificate")
    private String publicCertificate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "id")
    private List<SqlUserCertificate> userCertificates;

}
