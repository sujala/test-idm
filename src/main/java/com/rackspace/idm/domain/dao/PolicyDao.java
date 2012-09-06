package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.Policy;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/6/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PolicyDao {
    void addPolicy(Policy domain);
    Policy getPolicy(String domainId);
    void updatePolicy(Policy domain);
    void deletePolicy(String domainId);
    String getNextPolicyId();
}
