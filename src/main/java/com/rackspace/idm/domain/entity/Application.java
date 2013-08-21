package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION)
public class Application implements Auditable, UniqueId {
    private static final long serialVersionUID = -3160754818606772239L;

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    private ClientSecret clientSecret;

    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    @LDAPField(attribute = LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String rcn;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_OPENSTACK_TYPE, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String openStackType;

    @LDAPField(attribute = LdapRepository.ATTR_TOKEN_SCOPE, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String scope;

    @LDAPField(attribute = LdapRepository.ATTR_CALLBACK_URL, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String callBackUrl;

    @LDAPField(attribute = LdapRepository.ATTR_TITLE, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String title;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_USE_FOR_DEFAULT_REGION, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean useForDefaultRegion;

    @LDAPField(attribute = LdapRepository.ATTR_ENCRYPTION_VERSION_ID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String encryptionVersion;

    @LDAPField(attribute = LdapRepository.ATTR_ENCRYPTION_SALT, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String salt;

    @LDAPField(attribute = LdapRepository.ATTR_CLEAR_PASSWORD, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private byte[] clearPasswordBytes;

    private String clearPassword;

    private List<TenantRole> roles;

    public Application() {
    }

    public Application(String clientId, ClientSecret clientSecret, String name, String rcn) {
        this.clientId = clientId;
        this.rcn = rcn;
        this.clientSecret = clientSecret;
        this.name = name;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public void setClientSecretObj(ClientSecret clientSecret) {
        if (clientSecret != null) {
            this.clientSecret = clientSecret;
        }
    }

    public ClientSecret getClientSecretObj() {
        return clientSecret;
    }

    @LDAPSetter(attribute = LdapRepository.ATTR_CLIENT_SECRET)
    public void setClientSecret(String secret) {
        if (secret != null) {
            this.clientSecret = ClientSecret.newInstance(secret);
        }
    }

    @LDAPGetter(attribute = LdapRepository.ATTR_CLIENT_SECRET, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    public String getClientSecret() {
        if (clientSecret == null) {
            return null;
        }
        return this.clientSecret.getValue();
    }

    public void setDefaults() {
        this.setEnabled(true);
    }

    public void copyChanges(Application modifiedClient) {

    	if (modifiedClient.getRcn() != null) {
    		setRcn(modifiedClient.getRcn());
    	}

    	if (modifiedClient.getEnabled() != null) {
    		setEnabled(modifiedClient.getEnabled());
    	}

        if (modifiedClient.getCallBackUrl() != null) {
            setCallBackUrl(modifiedClient.getCallBackUrl());
        }

        if (modifiedClient.getDescription() != null) {
            setDescription(modifiedClient.getDescription());
        }

        if (modifiedClient.getScope() != null) {
            setScope(modifiedClient.getScope());
        }

        if (modifiedClient.getTitle() != null) {
            setTitle(modifiedClient.getTitle());
        }

        if (modifiedClient.getSalt() != null) {
            setSalt(modifiedClient.getSalt());
        }

        if (modifiedClient.getEncryptionVersion() != null) {
            setEncryptionVersion(modifiedClient.getEncryptionVersion());
        }
    }

	@Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "clientId=%s,customerId=%s";
        return String.format(format, getClientId(), rcn);
    }
}
