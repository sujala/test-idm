package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.sql.dao.PolicyRepository;
import com.rackspace.idm.domain.sql.entity.SqlPolicy;
import com.rackspace.idm.domain.sql.mapper.impl.PolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlPolicyRepository implements PolicyDao {

    @Autowired
    private PolicyMapper mapper;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private DeltaDao deltaDao;

    @Override
    @Transactional
    public void addPolicy(Policy policy) {
        final SqlPolicy sqlPolicy = policyRepository.save(mapper.toSQL(policy));

        final Policy newPolicy = mapper.fromSQL(sqlPolicy, policy);
        deltaDao.save(ChangeType.ADD, newPolicy.getUniqueId(), mapper.toLDIF(newPolicy));
    }

    @Override
    @Transactional
    public void updatePolicy(Policy policy) {
        final SqlPolicy sqlPolicy = policyRepository.save(mapper.toSQL(policy, policyRepository.findOne(policy.getPolicyId())));

        final Policy newPolicy = mapper.fromSQL(sqlPolicy, policy);
        deltaDao.save(ChangeType.MODIFY, newPolicy.getUniqueId(), mapper.toLDIF(newPolicy));
    }

    @Override
    @Transactional
    public void deletePolicy(String policyId) {
        final SqlPolicy sqlPolicy = policyRepository.findOne(policyId);
        policyRepository.delete(policyId);

        final Policy newPolicy = mapper.fromSQL(sqlPolicy);
        deltaDao.save(ChangeType.DELETE, newPolicy.getUniqueId(), null);
    }

    @Override
    @Transactional
    public void softDeletePolicy(Policy policy) {
        deletePolicy(policy.getPolicyId());
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

}
