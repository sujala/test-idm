package com.rackspace.idm.domain.service.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.PolicyService;
import com.rackspace.idm.exception.BadRequestException;
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
@Component
public class DefaultPolicyService implements PolicyService {

    @Autowired
    private Configuration config;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private PolicyDao policyDao;

    public static final String POLICY_CANNOT_BE_NULL = "Policy cannot be null";
    public static final String POLICY_BLOB_CANNOT_BE_NULL = "Policy Blob cannot be null";
    public static final String POLICY_TYPE_CANNOT_BE_NULL = "Policy type cannot be null";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Policies getPolicies() {
        return policyDao.getPolicies();
    }

	@Override
	public Policies getPolicies(List<String> policyIds) {
        Policies policies = new Policies();

        for (String policyId : policyIds) {
            Policy policy = checkAndGetPolicy(policyId);
            policies.getPolicy().add(policy);
        }

        return policies;
	}

    @Override
    public void addPolicy(Policy policy) {
        if(policy == null){
            throw new BadRequestException(POLICY_CANNOT_BE_NULL);
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
        policy.setPolicyId(this.policyDao.getNextPolicyId());
        //Trim the name
        String name = policy.getName().trim();
        policy.setName(name);
        //Not doing unique name right now, maybe later.
        //validateUniqueNamePolicy(policy.getName());
        logger.info("Adding Policy: {}", policy);
        policyDao.addPolicy(policy);
    }

    @Override
    public Policy getPolicy(String policyId) {
        if(policyId == null) {
            throw new NotFoundException("Policy Id cannot be null");
        }
        return policyDao.getPolicy(policyId);
    }

    @Override
    public Policy checkAndGetPolicy(String policyId) {
        Policy policy = policyDao.getPolicy(policyId);
        if(policy == null){
            String err = String.format("Policy with Id %s does not exist", policyId);
            throw new NotFoundException(err);
        }
        return policy;
    }

    @Override
    public void updatePolicy(String policyId, Policy policy) {
        if(policy == null){
            throw new BadRequestException(POLICY_CANNOT_BE_NULL);
        }
        if(policyId == null) {
            throw new BadRequestException("Policy ID must not be null");
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
        policy.setLdapEntry(policyDao.getPolicy(policyId).getLdapEntry());
        logger.info("Updating Policy: {}", policy);
        policyDao.updatePolicy(policy);
    }

    @Override
    public void deletePolicy(String policyId) {
        Policy policy = checkAndGetPolicy(policyId);
        List<CloudBaseUrl> cloudBaseUrlList = this.endpointService.getBaseUrlsWithPolicyId(policy.getPolicyId());
        if (cloudBaseUrlList.isEmpty()) {
            policyDao.deletePolicy(policy.getPolicyId());
        }else{
            throw new BadRequestException("Cannot delete policy that belongs to endpoint");
        }
    }

    @Override
    public void softDeletePolicy(String policyId) {
        logger.debug("SoftDeleting Policy: {}", policyId);
        Policy policy = checkAndGetPolicy(policyId);
        List<CloudBaseUrl> cloudBaseUrlList = this.endpointService.getBaseUrlsWithPolicyId(policy.getPolicyId());
        if (cloudBaseUrlList.isEmpty()) {
            policyDao.softDeletePolicy(policy);
        }else{
            throw new BadRequestException("Cannot delete policy that belongs to endpoint");
        }
        logger.debug("SoftDeleted User: {}", policyId);
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }
}
