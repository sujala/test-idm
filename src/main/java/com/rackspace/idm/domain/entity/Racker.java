package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.dozer.Mapping;

import java.util.List;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKER)
public class Racker implements BaseUser {

    //TODO: Not sure why this property is needed. Look into and remove if not necessary
    private String uniqueId;

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @Mapping("id")
    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKER,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String rackerId;

    private Boolean enabled;

    private String username;

    private List<String> rackerRoles;

    @Override
    public String getAuditContext() {
        return String.format("Racker(%s)", rackerId);
    }

    public boolean isDisabled() {
        return this.enabled == null ? false : !this.enabled;
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    /**
     * BaseUser requires this method, but it's irrelevant in context of Racker so return null
     *
     * @return
     */
    public String getDomainId(){
        return null;
    }

    @Override
    public String getId() {
        return getRackerId();
    }
}
