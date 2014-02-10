package com.rackspace.idm.api.resource.cloud.v20.multifactor;

import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

import java.util.List;

@Getter
@Setter
public class V1SessionId implements SessionId {
    private String version;
    private String userId;
    private DateTime createdDate;
    private DateTime expirationDate;
    private List<String> authenticatedBy;
}
