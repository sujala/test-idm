package com.rackspace.idm.domain.dao;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.idm.domain.entity.Policy;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/6/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PolicyDao {
    void addPolicy(Policy policy);
    Policy getPolicy(String policyId);
    Policy getPolicyByName(String name);
    void updatePolicy(Policy policy, String policyId);
    void deletePolicy(String policyId);
    String getNextPolicyId();
    com.rackspace.idm.domain.entity.Policies getPolicies();
    void softDeletePolicy(Policy policy);
}
