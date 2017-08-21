package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

import java.io.ByteArrayInputStream;
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

    // TODO: Remove those as soon as we remove the LDAP dependencies.
    @LDAPEntryField
    private ReadOnlyEntry readOnlyEntry;

    @LDAPDNField
    private String uniqueId;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_OU, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, inRDN = true, requiredForEncode = true)
    private String providerId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = true)
    private String name;

    @Mapping("issuer")
    @LDAPField(attribute = LdapRepository.ATTR_URI, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = true)
    private String uri;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String description;

    @DeleteNullValues
    @LDAPField(attribute = LdapRepository.ATTR_USER_CERTIFICATES, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private List<byte[]> userCertificates;

    @Mapping("federationType")
    @LDAPField(attribute = LdapRepository.ATTR_TARGET_USER_SOURCE, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String federationType;

    @LDAPField(attribute = LdapRepository.ATTR_APPROVED_DOMAIN_GROUP, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String approvedDomainGroup;

    @LDAPField(attribute = LdapRepository.ATTR_APPROVED_DOMAIN_IDS, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private List<String> approvedDomainIds;

    @LDAPField(attribute = LdapRepository.ATTR_AUTHENTICATION_URL, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String authenticationUrl;

    @LDAPField(attribute = LdapRepository.ATTR_IDP_POLICY, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private byte[] policy;

    @LDAPField(attribute = LdapRepository.ATTR_IDP_POLICY_FORMAT, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false, defaultDecodeValue = "JSON")
    private String policyFormat;

    @LDAPField(attribute = LdapRepository.ATTR_IDP_METADATA, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private byte[] xmlMetadata;

    @Mapping("enabled")
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false, defaultDecodeValue = "TRUE")
    private Boolean enabled;

    @Override
    public String getAuditContext() {
        String format = "identityProviderId=%s";
        return String.format(format, getProviderId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    public boolean isApprovedForDomain(String domainId) {
        return getApprovedDomainGroupAsEnum() == ApprovedDomainGroupEnum.GLOBAL
                || (approvedDomainIds != null && approvedDomainIds.contains(domainId));
    }

    public ApprovedDomainGroupEnum getApprovedDomainGroupAsEnum() {
        return ApprovedDomainGroupEnum.lookupByStoredValue(approvedDomainGroup);
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
            if (Arrays.equals(userCertificates.get(i), toRemove)) {
                userCertificates.remove(i);
            }
        }
    }

    /**
     * The default source is provisioned
     *
     * @return
     */
    public IdentityProviderFederationTypeEnum getFederationTypeAsEnum() {
        if (StringUtils.isBlank(federationType)) {
            return IdentityProviderFederationTypeEnum.DOMAIN;
        }
        for (IdentityProviderFederationTypeEnum that : IdentityProviderFederationTypeEnum.values()) {
            if  (that.name().equals(federationType)) {
                return that;
            }
        }

        //default is DOMAIN (e.g. - if the value is anything else, like legacy "PROVISIONED"
        return IdentityProviderFederationTypeEnum.DOMAIN;
    }
}
