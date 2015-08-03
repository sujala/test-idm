package com.rackspace.idm.domain.dao;

import java.util.Date;

public interface APINodeSignoff {
    String getId();
    void setId(String id);
    String getNodeName();
    void setNodeName(String nodeName);
    Date getLoadedDate();
    void setLoadedDate(Date date);
    Date getCachedMetaCreatedDate();
    void setCachedMetaCreatedDate(Date date);
    String getKeyMetadataId();
    void setKeyMetadataId(String keyMetadataId );
}
