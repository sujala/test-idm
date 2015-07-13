package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.unboundid.ldap.sdk.Filter;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/6/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@LDAPComponent
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

    @Override
    public String getSoftDeletedBaseDn() {
        return SOFT_DELETED_POLICIES_BASE_DN;
    }

    public String getSortAttribute() {
        return ATTR_ID;
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
        for (Policy policy : getObjects(searchFilterGetPolicies())) {
            policies.getPolicy().add(policy);
        }
        return policies;
    }

    @Override
    public void softDeletePolicy(Policy policy) {
        softDeleteObject(policy);
    }

    private Filter searchFilterGetPolicyById(String policyId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, policyId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_POLICY).build();
    }

    private Filter searchFilterGetPolicyByName(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_POLICY).build();
    }

    private Filter searchFilterGetPolicies() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_POLICY).build();
    }
}
