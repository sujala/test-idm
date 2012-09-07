package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.exception.DuplicateException;
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
public class LdapPolicyRepository extends LdapRepository implements PolicyDao {

    private final Configuration config;
    public static final String NULL_OR_EMPTY_POLICY_ID_PARAMETER = "Null or Empty policyId parameter";
    public static final String ERROR_GETTING_POLICY_OBJECT = "Error getting policy object";
    public static final String PARENT_UNIQUE_ID_CANNOT_BE_BLANK = "ParentUniqueId cannot be blank";

    public LdapPolicyRepository(LdapConnectionPools connPools, Configuration config) {
        super(connPools, config);
        this.config = config;
    }

    @Override
    public void addPolicy(Policy policy) {
        if (policy == null) {
            getLogger().error("Null instance of policy was passed");
            throw new IllegalArgumentException("Null instance of policy was passed.");
        }

        String policyDN = new LdapDnBuilder(POLICY_BASE_DN).addAttribute(ATTR_ID, policy.getPolicyId().toString()).build();

        Audit audit = Audit.log(policy).add();

        Attribute[] attributes = getAddAttributes(policy);
        addEntry(policyDN, attributes, audit);
        audit.succeed();

        getLogger().debug("Added policy {}", policy);
    }

    Attribute[] getAddAttributes(Policy policy) {

        List<Attribute> atts = new ArrayList<Attribute>();

        atts.add(new Attribute(ATTR_OBJECT_CLASS, ATTR_POLICY_OBJECT_CLASS_VALUES));

        if (!StringUtils.isBlank(policy.getPolicyId().toString())) {
            atts.add(new Attribute(ATTR_ID, policy.getPolicyId().toString()));
        }

        if (!StringUtils.isBlank(policy.getName())) {
            atts.add(new Attribute(ATTR_NAME, policy.getName()));
        }

        if (!StringUtils.isBlank(policy.getDescription())) {
            atts.add(new Attribute(ATTR_DESCRIPTION, policy.getDescription()));
        }

        if (!StringUtils.isBlank(policy.getBlob())) {
            atts.add(new Attribute(ATTR_BLOB, policy.getBlob()));
        }

        if (!StringUtils.isBlank(policy.getPolicyType())) {
            atts.add(new Attribute(ATTR_POLICYTYPE, policy.getPolicyType()));
        }

        if (policy.isEnabled() != null) {
            atts.add(new Attribute(ATTR_ENABLED, String.valueOf(policy.isEnabled())));
        }

        if (policy.isGlobal() != null) {
            atts.add(new Attribute(ATTR_GLOBAL, String.valueOf(policy.isGlobal())));
        }

        Attribute[] attributes = atts.toArray(new Attribute[0]);
        getLogger().debug("Found {} attributes to add", attributes.length);
        return attributes;
    }

    @Override
    public Policy getPolicy(String policyId) {
        getLogger().debug("Doing search for policyId " + policyId);
        if (StringUtils.isBlank(policyId)) {
            getLogger().error(NULL_OR_EMPTY_POLICY_ID_PARAMETER);
            getLogger().info("Invalid policyId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, policyId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_POLICY).build();

        Policy policy = null;

        try {
            policy = getSinglePolicy(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_POLICY_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Policy - {}", policy);

        return policy;
    }

    @Override
    public void updatePolicy(Policy policy) {
        if (policy == null || StringUtils.isBlank(policy.getUniqueId())) {
            String errmsg = "Null instance of Policy was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating policy: {}", policy);
        Audit audit = Audit.log(policy);
        try {
            final LDAPPersister<Policy> persister = LDAPPersister.getInstance(Policy.class);
            List<Modification> modifications = persister.getModifications(policy, true);
            audit.modify(modifications);
            if (modifications.size() > 0) {
                persister.modify(policy, getAppInterface(), null, true);
            }
            getLogger().debug("Updated policy: {}", policy);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating policy", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void deletePolicy(String policyId) {
        if (StringUtils.isBlank(policyId)) {
            getLogger().error(NULL_OR_EMPTY_POLICY_ID_PARAMETER);
            throw new IllegalArgumentException(
                "Null or Empty policyId parameter.");
        }
        Policy policy = getPolicy(policyId);
        if (policy == null) {
            String errMsg = String.format("policy %s not found", policyId);
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        getLogger().debug("Deleting Policy: {}", policy);
        final String dn = policy.getUniqueId();
        final Audit audit = Audit.log(policy).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted Policy: {}", policy);
    }

    @Override
    public String getNextPolicyId() {
        return getNextId( NEXT_POLICY_ID);
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
}
