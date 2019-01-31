package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.docs.core.event.DC;
import com.rackspace.docs.core.event.EventType;
import com.rackspace.docs.core.event.Region;
import com.rackspace.docs.core.event.V1Element;
import com.rackspace.idm.domain.config.IdentityConfig;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2005.atom.Title;
import org.w3._2005.atom.UsageContent;
import org.w3._2005.atom.UsageEntry;

import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

public class UsageEntryBuilder {

    private IdentityConfig identityConfig;

    private EventType eventType;
    private String eventId = UUID.randomUUID().toString();
    private String resourceId;
    private String resourceName;
    private String tenantId;
    private String eventTitle;
    private Object eventData;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private UsageEntryBuilder(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }

    public static UsageEntryBuilder builder(IdentityConfig identityConfig) {
        return new UsageEntryBuilder(identityConfig);
    }

    public UsageEntryBuilder eventType(EventType eventType) {
        this.eventType = eventType;
        return this;
    }

     public UsageEntryBuilder resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public UsageEntryBuilder resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public UsageEntryBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public UsageEntryBuilder eventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
        return this;
    }

    public UsageEntryBuilder eventData(Object eventData) {
        this.eventData = eventData;
        return this;
    }

    public UsageEntry buildUsageEntry() {
        Validate.notNull(eventType, "Event Type is required for cloud feed events.");
        Validate.notNull(resourceId, "Resource ID is required for cloud feed events.");
        Validate.notNull(eventData, "Event data is required for cloud feed events.");
        Validate.notNull(eventTitle, "Event title is required for cloud feed events.");

        final V1Element v1Element = new V1Element();
        v1Element.setType(eventType);
        v1Element.setResourceId(resourceId);
        v1Element.setResourceName(resourceName);
        v1Element.setTenantId(tenantId);
        v1Element.setRegion(Region.fromValue(identityConfig.getReloadableConfig().getFeedsRegion()));
        v1Element.setDataCenter(DC.fromValue(identityConfig.getReloadableConfig().getFeedsDataCenter()));
        v1Element.setVersion(AtomHopperConstants.VERSION);
        v1Element.getAny().add(eventData);

        final GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            final XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            v1Element.setEventTime(now);
        } catch (DatatypeConfigurationException e) {
            logger.error("Error creating calendar instance to set token creation date", e);
            throw new IllegalStateException("Error creating feed entry", e);
        }

        v1Element.setId(eventId);

        final UsageContent usageContent = new UsageContent();
        usageContent.setEvent(v1Element);
        usageContent.setType(MediaType.APPLICATION_XML);

        final UsageEntry usageEntry = new UsageEntry();
        usageEntry.setContent(usageContent);

        final Title entryTitle = new Title();
        entryTitle.setValue(eventTitle);
        usageEntry.setTitle(entryTitle);

        return usageEntry;
    }

}
