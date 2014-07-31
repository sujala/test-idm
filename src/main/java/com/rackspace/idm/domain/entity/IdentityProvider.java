package com.rackspace.idm.domain.entity;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER)
public class IdentityProvider implements Auditable, UniqueId {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_OU, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, inRDN = true, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_URI, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = true)
    private String uri;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String description;

    /**
     * @deprecated Use userCertificates. This field will be removed in future commits
     */
    @Deprecated
    @LDAPField(attribute = LdapRepository.ATTR_PUBLIC_KEY, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String publicCertificate;

    @DeleteNullValues
    @LDAPField(attribute = LdapRepository.ATTR_USER_CERTIFICATES, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private List<byte[]> userCertificates;

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String getAuditContext() {
        String format = "identityProviderName=%s";
        return String.format(format, getName());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    public List<X509Certificate> getUserCertificatesAsX509() throws CertificateException {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        if (userCertificates == null) {
            return certs;
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (byte[] userCertificate : userCertificates) {
            certs.add((X509Certificate)cf.generateCertificate(new ByteArrayInputStream(userCertificate)));

        }
        return certs;
    }

    public void addUserCertificate(X509Certificate certificate) throws CertificateEncodingException {
        if (userCertificates == null) {
            userCertificates = new ArrayList<byte[]>();
        }

        byte[] toAdd = certificate.getEncoded();
        for (int i=0; i<userCertificates.size(); i++) {
            if (Arrays.equals(userCertificates.get(0), toAdd)) {
                return; //already there
            }
        }
        userCertificates.add(certificate.getEncoded());
    }

    public void removeUserCertificate(X509Certificate certificate)  throws CertificateEncodingException {
        if (userCertificates == null) {
            return;
        }

        byte[] toRemove = certificate.getEncoded();
        for (int i=0; i<userCertificates.size(); i++) {
            if (Arrays.equals(userCertificates.get(0), toRemove)) {
                userCertificates.remove(i);
            }
        }
    }

}
