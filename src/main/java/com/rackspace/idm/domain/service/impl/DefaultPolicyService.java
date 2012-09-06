package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.service.PolicyService;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
        //To change body of implemented methods use File | Settings | File Templates.
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
