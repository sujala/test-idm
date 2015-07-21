package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by jorge on 7/16/15.
 */
@Data
public class SqlPolicyEndpointRaxId implements Serializable {
    private String endpointId;
    private String policyId;
}
