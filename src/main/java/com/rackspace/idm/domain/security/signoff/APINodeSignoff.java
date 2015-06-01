package com.rackspace.idm.domain.security.signoff;

import java.util.Date;

public interface APINodeSignoff {
    String getId();
    String getNodeName();
    Date getLoadedDate();
    Date getCachedMetaCreatedDate();
    String getKeyMetadataId();
}
