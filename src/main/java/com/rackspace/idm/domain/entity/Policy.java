package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dozer.Mapping;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/6/12
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
@EqualsAndHashCode(exclude = "ldapEntry")
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_POLICY)
public class Policy implements Auditable, UniqueId {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_ID,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            inRDN = true,
            filterUsage = FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode = true)
    private String policyId;

    @Mapping("name")
    @LDAPField(attribute = LdapRepository.ATTR_NAME,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String name;

    @Mapping("enabled")
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean enabled;

    @Mapping("global")
    @LDAPField(attribute = LdapRepository.ATTR_GLOBAL,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean global;

    @Mapping("type")
    @LDAPField(attribute = LdapRepository.ATTR_POLICYTYPE,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String policyType;

    @LDAPField(attribute = LdapRepository.ATTR_BLOB,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String blob;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION,
            objectClass = LdapRepository.OBJECTCLASS_POLICY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String description;

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public void setUniqueId(String id){
        this.ldapEntry.setDN(id);
    }

    @Override
    public String getAuditContext() {
        String format = "policyId=%s";
        return String.format(format, getPolicyId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
