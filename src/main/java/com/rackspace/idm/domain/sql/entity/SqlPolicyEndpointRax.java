package com.rackspace.idm.domain.sql.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@IdClass(value = SqlPolicyEndpointRaxId.class)
@Table(name = "policy_endpoint_rax")
public class SqlPolicyEndpointRax {

    @Id
    @Column(name = "endpoint_id")
    private String endpointId;

    @Id
    @Column(name =  "policy_id")
    private String policyId;
}
