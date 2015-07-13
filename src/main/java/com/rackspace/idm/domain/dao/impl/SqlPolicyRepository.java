package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.sql.dao.PolicyRepository;
import com.rackspace.idm.domain.sql.mapper.impl.PolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@SQLComponent
public class SqlPolicyRepository implements PolicyDao {

    @Autowired
    PolicyMapper mapper;

    @Autowired
    PolicyRepository policyRepository;

    @Override
    public void addPolicy(Policy policy) {
        policyRepository.save(mapper.toSQL(policy));
    }

    @Override
    public Policy getPolicy(String policyId) {
        return mapper.fromSQL(policyRepository.findOne(policyId));
    }

    @Override
    public Policy getPolicyByName(String name) {
        return mapper.fromSQL(policyRepository.findByRaxName(name));
    }

    @Override
    public void updatePolicy(Policy policy) {
        policyRepository.save(mapper.toSQL(policy));
    }

    @Override
    public void deletePolicy(String policyId) {
        policyRepository.delete(policyId);
    }

    @Override
    public String getNextPolicyId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public Policies getPolicies() {
        Policies policies = new Policies();
        for (Policy policy : mapper.fromSQL(policyRepository.findAll())) {
            policies.getPolicy().add(policy);
        }
        return policies;
    }

    @Override
    public void softDeletePolicy(Policy policy) {
        policyRepository.delete(mapper.toSQL(policy));
    }
}
