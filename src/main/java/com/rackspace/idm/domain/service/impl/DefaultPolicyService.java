package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;

import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.service.PolicyService;
import com.rackspace.idm.exception.BadRequestException;
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addPolicy(Policy policy) {
        if(policy == null){
            throw new BadRequestException(POLICY_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(policy.getUniqueId())) {
            throw new BadRequestException(POLICY_ID_CANNOT_BE_NULL);
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
        logger.info("Adding Domain: {}", policy);
        policyDao.addPolicy(policy);
    }

    @Override
    public Policy getPolicy(String policyId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updatePolicy(Policy policy) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deletePolicy(String policyId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
