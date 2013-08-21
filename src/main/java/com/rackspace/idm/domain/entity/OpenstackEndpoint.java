package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.List;

@Data
public class OpenstackEndpoint {
    private String tenantId;
    private String tenantName;
    private List<CloudBaseUrl> baseUrls;
}
