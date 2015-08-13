package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "identity_provider_rax")
@EqualsAndHashCode(exclude = "userCertificates")
@ToString(exclude = "userCertificates")
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

    @Column(name = "target_user_source")
    private String targetUserSource;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "identityProvider")
    private List<SqlUserCertificate> userCertificates;

}
