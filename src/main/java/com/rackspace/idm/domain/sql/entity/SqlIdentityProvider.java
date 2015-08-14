package com.rackspace.idm.domain.sql.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "identityProvider", orphanRemoval = true)
    private List<SqlUserCertificate> userCertificates = new ArrayList<SqlUserCertificate>();

    public void removeUserCertificate(X509Certificate certificate)  throws CertificateEncodingException {
        if (userCertificates == null) {
            return;
        }

        byte[] toRemove = certificate.getEncoded();
        for (int i=0; i<userCertificates.size(); i++) {
            if (Arrays.equals(userCertificates.get(i).getCertificate(), toRemove)) {
                userCertificates.remove(i);
            }
        }
    }

}
