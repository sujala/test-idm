package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.idm.domain.entity.Policy;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/6/12
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PolicyService {
    com.rackspace.idm.domain.entity.Policies getPolicies(List<String> policyIds);
    com.rackspace.idm.domain.entity.Policies getPolicies();
    void addPolicy(Policy policy);
    Policy getPolicy(String policyId);
    void updatePolicy(String policyId, Policy policy);
    void deletePolicy(String policyId);
    void softDeletePolicy(String policyId);
    Policy checkAndGetPolicy(String policyId);
}
