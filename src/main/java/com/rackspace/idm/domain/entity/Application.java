package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.dozer.Mapping;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION)
public class Application implements Auditable, UniqueId {
    private static final long serialVersionUID = -3160754818606772239L;

    // TODO: Remove those as soon as we remove the LDAP dependencies.
    @LDAPEntryField
    private ReadOnlyEntry readOnlyEntry;

    @LDAPDNField
    private String uniqueId;

    private ClientSecret clientSecret;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId = null;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @Mapping("type")
    @LDAPField(attribute = LdapRepository.ATTR_OPENSTACK_TYPE, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String openStackType;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_USE_FOR_DEFAULT_REGION, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEAPPLICATION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean useForDefaultRegion;

    private List<TenantRole> roles;

    public Application() {
    }

    public Application(String clientId, String name) {
        this.clientId = clientId;
        this.name = name;
        // In order to avoid a schema change, setting deprecated clientSecret to random uuid.
        this.clientSecret = ClientSecret.newInstance(UUID.randomUUID().toString().replaceAll("-", ""));
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

    	if (modifiedClient.getEnabled() != null) {
    		setEnabled(modifiedClient.getEnabled());
    	}

        if (modifiedClient.getDescription() != null) {
            setDescription(modifiedClient.getDescription());
        }
    }

	@Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "clientId=%s";
        return String.format(format, getClientId());
    }
}
