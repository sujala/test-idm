package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.service.PolicyService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/6/12
 * Time: 11:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultPolicyService implements PolicyService {

    @Autowired
    private Configuration config;

    public static final String POLICY_CANNOT_BE_NULL = "Policy cannot be null";
    public static final String POLICY_ID_CANNOT_BE_NULL = "Policy ID cannot be null";
    public static final String POLICY_NAME_CANNOT_BE_NULL = "Policy name cannot be null or empty";
    public static final String POLICY_BLOB_CANNOT_BE_NULL = "Policy Blob cannot be null";
    public static final String POLICY_TYPE_CANNOT_BE_NULL = "Policy type cannot be null";
    private final PolicyDao policyDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultPolicyService(PolicyDao policyDao) {
        this.policyDao = policyDao;
    }

    @Override
    public Policies getPolicies() {
        return policyDao.getPolicies();
    }

    @Override
    public void addPolicy(Policy policy) {
        if(policy == null){
            throw new BadRequestException(POLICY_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getName())) {
            throw new BadRequestException(POLICY_NAME_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getBlob())){
            throw new BadRequestException(POLICY_BLOB_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getDescription())) {
            policy.setDescription(null);
        }
        if(StringUtils.isBlank(policy.getPolicyType())) {
            throw new BadRequestException(POLICY_TYPE_CANNOT_BE_NULL);
        }
        if(policy.isEnabled() == null){
            policy.setEnabled(false);
        }
        if(policy.isGlobal() == null){
            policy.setGlobal(false);
        }
        policy.setPolicyId(this.policyDao.getNextPolicyId());
        validateUniqueNamePolicy(policy.getName());
        logger.info("Adding Policy: {}", policy);
        policyDao.addPolicy(policy);
    }

    private void validateUniqueNamePolicy(String name) {
        Policy policy = policyDao.getPolicyByName(name);
        if(policy !=  null){
            logger.info("Attempting to add existing Policy: {}", policy);
            String err = String.format("Policy with name %s already exist", name);
            throw new DuplicateException(err);
        }
    }

    @Override
    public Policy getPolicy(String policyId) {
        Policy policy = checkAndGetPolicy(policyId);
        return policy;
    }

    private Policy checkAndGetPolicy(String policyId) {
        Policy policy = policyDao.getPolicy(policyId);
        if(policy == null){
            String err = String.format("Policy with Id %s does not exist", policyId);
            throw new NotFoundException(err);
        }
        return policy;
    }

    @Override
    public void updatePolicy(Policy policy, String policyId) {
        if(policy == null){
            throw new BadRequestException(POLICY_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getName())) {
            throw new BadRequestException(POLICY_NAME_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getBlob())){
            policy.setBlob(null);
        }
        if(StringUtils.isBlank(policy.getDescription())) {
            policy.setDescription(null);
        }
        if(StringUtils.isBlank(policy.getPolicyType())) {
            policy.setPolicyType(null);
        }
        logger.info("Updating Policy: {}", policy);
        policyDao.updatePolicy(policy, policyId);
    }

    @Override
    public void deletePolicy(String policyId) {
        Policy policy = checkAndGetPolicy(policyId);
        policyDao.deletePolicy(policy.getPolicyId());
    }
}
