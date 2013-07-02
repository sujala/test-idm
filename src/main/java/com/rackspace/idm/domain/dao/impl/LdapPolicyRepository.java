package com.rackspace.idm.domain.dao.impl;

import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/6/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapPolicyRepository extends LdapGenericRepository<Policy> implements PolicyDao {

    public String getBaseDn() {
        return POLICY_BASE_DN;
    }

    public String getLdapEntityClass() {
        return OBJECTCLASS_POLICY;
    }

    public String getNextPolicyId() {
        return getNextId(NEXT_POLICY_ID);
    }

    public String getSortAttribute() {
        return ATTR_POLICY_ID;
    }

    @Override
    public void addPolicy(Policy policy) {
        addObject(policy);
    }

    @Override
    public Policy getPolicy(String policyId) {
        return getObject(searchFilterGetPolicyById(policyId));
    }

    @Override
    public Policy getPolicyByName(String name) {
        return getObject(searchFilterGetPolicyByName(name));
    }

    @Override
    public void updatePolicy(Policy policy) {
        updateObject(policy);
    }


    @Override
    public void deletePolicy(String policyId) {
        deleteObject(searchFilterGetPolicyById(policyId));
    }

    @Override
    public Policies getPolicies() {
        Policies policies = new Policies();
        policies.setPolicy(getObjects(searchFilterGetPolicies()));
        return policies;
    }

    @Override
    public void softDeletePolicy(Policy policy) {
          getLogger().info("SoftDeleting policy - {}", policy);
        try {
            String oldRdn = policy.getUniqueId();
            // Move the Policy
            String newRdn = ATTR_ID + "=" + policy.getPolicyId();
            getAppInterface().modifyDN(oldRdn, newRdn, true, SOFT_DELETED_POLICIES_BASE_DN);
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting user", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted policy - {}", policy);
    }

    Policy getSinglePolicy(Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(POLICY_BASE_DN, SearchScope.ONE, searchFilter, ATTR_POLICY_SEARCH_ATTRIBUTES);
        if (entry == null) {
            return null;
        }
        Policy policy = null;
        policy = LDAPPersister.getInstance(Policy.class).decode(entry);
        return policy;
    }

    private Filter searchFilterGetPolicyById(String policyId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(LdapPolicyRepository.ATTR_ID, policyId)
                .addEqualAttribute(LdapPolicyRepository.ATTR_OBJECT_CLASS, LdapPolicyRepository.OBJECTCLASS_POLICY).build();
    }

    private Filter searchFilterGetPolicyByName(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(LdapPolicyRepository.ATTR_NAME, name)
                .addEqualAttribute(LdapPolicyRepository.ATTR_OBJECT_CLASS, LdapPolicyRepository.OBJECTCLASS_POLICY).build();
    }

    private Filter searchFilterGetPolicies() {
        return new LdapSearchBuilder()
                .addEqualAttribute(LdapPolicyRepository.ATTR_OBJECT_CLASS, LdapPolicyRepository.OBJECTCLASS_POLICY).build();
    }
}
