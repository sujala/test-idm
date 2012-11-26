package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRsWeight(int weight) {
        this.rsWeight = weight;
    }

    public int getRsWeight() {
        return rsWeight;
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
