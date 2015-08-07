package com.rackspace.idm.util.migration.ldap;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.util.migration.ChangeType;
import com.rackspace.idm.util.migration.MigrationChangeEvent;
import com.rackspace.idm.util.migration.PersistenceTarget;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Format for persisting migration events to CA
 */
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_CHANGE_EVENT)
public class LdapMigrationChangeEvent implements UniqueId, Auditable, MigrationChangeEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapMigrationChangeEvent.class);

    @Getter
    @LDAPDNField
    private String uniqueId;

    @Getter
    @LDAPField(attribute= LdapRepository.ATTR_ID, objectClass=LdapRepository.OBJECTCLASS_CHANGE_EVENT, inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_CHANGE_OCCURRED_DATE, objectClass = LdapRepository.OBJECTCLASS_CHANGE_EVENT, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED, requiredForEncode=true)
    private Date changeOccurredDate;

    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_ENTITY_ID, objectClass = LdapRepository.OBJECTCLASS_CHANGE_EVENT, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED, requiredForEncode=true)
    private String entityId;

    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_HOST_NAME, objectClass = LdapRepository.OBJECTCLASS_CHANGE_EVENT, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED, requiredForEncode=true)
    private String hostName;

    @LDAPField(attribute = LdapRepository.ATTR_CHANGE_TYPE, objectClass = LdapRepository.OBJECTCLASS_CHANGE_EVENT, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED, requiredForEncode=true)
    private String internalChangeType;

    public LdapMigrationChangeEvent() {}

    public LdapMigrationChangeEvent(LDAPMigrationChangeApplicationEvent event, String id, String hostName) {
        this.id = id;
        this.changeOccurredDate = event.getChangeOccurredDate();
        this.entityId = event.getEntityUniqueIdentifier();
        this.setChangeType(event.getChangeType());
        this.hostName = hostName;
    }

    public ChangeType getChangeType() {
        for (ChangeType changeType : ChangeType.values()) {
            if (changeType.name().equals(internalChangeType)) {
                return changeType;
            }
        }
        return null;
    }

    public void setChangeType(ChangeType changeType) {
        internalChangeType = changeType.name();
    }

    @Override
    public String getEntityUniqueIdentifier() {
        return getEntityId();
    }

    @Override
    public PersistenceTarget getPersistenceTarget() {
        return PersistenceTarget.LDAP;
    }

    @Override
    public String getAuditContext() {
        return String.format("id=%s", id);
    }
}
