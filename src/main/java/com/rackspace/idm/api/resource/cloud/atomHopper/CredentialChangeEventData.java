package com.rackspace.idm.api.resource.cloud.atomHopper;


import com.rackspace.docs.event.identity.user.credential.CloudIdentityType;
import com.rackspace.docs.event.identity.user.credential.CredentialTypeEnum;
import com.rackspace.docs.event.identity.user.credential.ResourceTypes;
import lombok.Data;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@Data
public class CredentialChangeEventData {

    private String userId;
    private String username;
    private String domainId;
    private String email;
    private String requestId;
    private DateTime credentialUpdateDateTime;
    private CredentialTypeEnum credentialType;

    private static final Logger logger = LoggerFactory.getLogger(CredentialChangeEventData.class);

    public CloudIdentityType toCloudIdentityType () {
        CloudIdentityType cloudIdentityType = new CloudIdentityType();
        cloudIdentityType.setResourceType(ResourceTypes.USER);
        cloudIdentityType.setVersion(AtomHopperConstants.VERSION);
        cloudIdentityType.setServiceCode(AtomHopperConstants.CLOUD_IDENTITY);
        cloudIdentityType.setUserId(userId);
        cloudIdentityType.setUsername(username);
        cloudIdentityType.setDomainId(domainId);
        cloudIdentityType.setEmail(email);
        cloudIdentityType.setRequestId(requestId);
        cloudIdentityType.setCredentialType(credentialType);
        cloudIdentityType.setCredentialUpdateDateTime(toXmlGregorianCalendar(credentialUpdateDateTime));

        return cloudIdentityType;
    }

    private XMLGregorianCalendar toXmlGregorianCalendar(DateTime dateTime) {
        try {
            final GregorianCalendar c = new GregorianCalendar();
            c.setTime(dateTime.toDate());
            c.setTimeZone(TimeZone.getTimeZone("UTC"));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException e) {
            logger.error("Error creating XmlGregorianCalendar for cloud feed event.", e);
            throw new IllegalStateException("Error creating XmlGregorianCalendar.", e);
        }
    }

}
