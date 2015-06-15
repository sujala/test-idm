package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

import java.util.List;
import java.util.regex.*;
import java.util.regex.Pattern;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKER)
public class Racker implements BaseUser, FederatedBaseUser {

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

    public static final java.util.regex.Pattern RACKER_ID_PATTERN = Pattern.compile("^(.+)@(.+)$");

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

    /**
     * Alternative name for setting the rackerId
     * @param id
     */
    @Override
    public void setId(String id) {
        rackerId = id;
    }

    public boolean isFederatedRacker() {
        return StringUtils.isNotBlank(getFederatedIdpUri());
    }

    /**
     * Return the identity provider URI (part of rackerId suffixed to end of '@' or null
     *
     * @return
     */
    public String getFederatedIdpUri() {
        if (StringUtils.isBlank(rackerId)) {
            return null;
        }

        Matcher matcher = RACKER_ID_PATTERN.matcher(rackerId);
        boolean matchFound = matcher.find();
        if (matchFound) {
            return matcher.group(2);
        }
        return null;
    }

    public String getFederatedUserName() {
        if (StringUtils.isBlank(rackerId)) {
            return null;
        }

        Matcher matcher = RACKER_ID_PATTERN.matcher(rackerId);
        boolean matchFound = matcher.find();
        if (matchFound) {
            return matcher.group(1);
        }
        return null;
    }

    public String getUsername() {
        if (StringUtils.isBlank(username) && StringUtils.isNotBlank(getFederatedIdpUri())) {
            return getFederatedUserName();
        }
        return username;
    }

}
