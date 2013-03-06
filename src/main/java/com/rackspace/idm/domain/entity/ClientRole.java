package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE)
public class ClientRole implements Auditable, UniqueId {

	public static final String SUPER_ADMIN_ROLE = "3";
	public static final String RACKER = "RackerVirtualRole";

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_ID, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String id;

    @LDAPField(attribute=LdapRepository.ATTR_NAME, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String name;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientId;

    @LDAPField(attribute=LdapRepository.ATTR_DESCRIPTION, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String description;

    @LDAPField(attribute=LdapRepository.ATTR_RS_WEIGHT, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private int rsWeight;

    @LDAPField(attribute=LdapRepository.ATTR_RS_PROPAGATE, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Boolean propagate;

    public Boolean getPropagate() {
        if (propagate == null) {
            return false;
        }
        return propagate;
    }

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    public void copyChanges(ClientRole modifiedClient) {

        if (StringUtils.isBlank(modifiedClient.getDescription())) {
            setDescription(null);
        }
        else {
            setDescription(modifiedClient.getDescription());
        }
    }

    @Override
    public String getAuditContext() {
        String format = "role=%s,clientId=%s";
        return String.format(format, getName(), getClientId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
