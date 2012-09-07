package com.rackspace.idm.domain.dao.impl;

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
public class LdapPolicyRepository extends LdapRepository implements PolicyDao {

    private final Configuration config;
    public static final String NULL_OR_EMPTY_POLICY_ID_PARAMETER = "Null or Empty policyId parameter";
    public static final String NULL_OR_EMPTY_POLICY_NAME_PARAMETER = "Null or Empty policy name parameter";
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
    public Policy getPolicyByName(String name) {
        getLogger().debug("Doing search for policy name: " + name);
        if (StringUtils.isBlank(name)) {
            getLogger().error(NULL_OR_EMPTY_POLICY_NAME_PARAMETER);
            getLogger().info("Invalid policy name parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, name)
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
        Policy oldPolicy = getPolicy(policy.getPolicyId());
        getLogger().debug("Found existing policy {}", oldPolicy);

        Audit audit = Audit.log(policy);

        try {
            List<Modification> mods = getModifications(oldPolicy, policy);
            audit.modify(mods);

            if (mods.size() < 1) {
                // No changes!
                return;
            }
            getAppInterface().modify(oldPolicy.getUniqueId(), mods);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating policy {} - {}", policy.getName(), ldapEx);
            audit.fail("Error updating policy");
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated policy - {}", policy);
    }

    private List<Modification> getModifications(Policy oldPolicy, Policy newPolicy) {
        List<Modification> mods = new ArrayList<Modification>();

        if (newPolicy.getName() != null && !newPolicy.getName().equals(oldPolicy.getName())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_NAME, String.valueOf(newPolicy.getName())));
        }

        if (newPolicy.getDescription()!= null && !newPolicy.getDescription().equals(oldPolicy.getDescription())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_DESCRIPTION, String.valueOf(newPolicy.getDescription())));
        }

        if (newPolicy.isEnabled() != null && !newPolicy.isEnabled().equals(oldPolicy.isEnabled())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_ENABLED, String.valueOf(newPolicy.isEnabled())));
        }

        if (newPolicy.isGlobal() != null && !newPolicy.isGlobal().equals(oldPolicy.isGlobal())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_GLOBAL, String.valueOf(newPolicy.isGlobal())));
        }

        if (newPolicy.getBlob() != null && !newPolicy.getBlob().equals(oldPolicy.getBlob())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_BLOB, String.valueOf(newPolicy.getBlob())));
        }

        if (newPolicy.getPolicyType() != null && !newPolicy.getPolicyType().equals(oldPolicy.getPolicyType())) {
            mods.add(new Modification(ModificationType.REPLACE, ATTR_POLICYTYPE, String.valueOf(newPolicy.getPolicyType())));
        }

        return mods;
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

    @Override
    public Policies getPolicies() {
        getLogger().debug("Getting Policies");

        Policies policies = new Policies();
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_POLICY).build();

        try {
            searchResult = getAppInterface().search(POLICY_BASE_DN, SearchScope.ONE, searchFilter);
            getLogger().info("Got Policies");
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for Polices - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                policies.getPolicy().add(getEntryPolicy(entry));
            }
        }

        return policies;
    }

    private Policy getEntryPolicy(SearchResultEntry entry) {
        getLogger().debug("Inside getEntryPolicy");
        Policy policy = new Policy();
        policy.setPolicyType(entry.getAttributeValue(ATTR_POLICYTYPE));
        policy.setBlob(entry.getAttributeValue(ATTR_BLOB));
        policy.setDescription(entry.getAttributeValue(ATTR_DESCRIPTION));
        policy.setEnabled(entry.getAttributeValueAsBoolean(ATTR_ENABLED));
        policy.setGlobal(entry.getAttributeValueAsBoolean(ATTR_GLOBAL));
        policy.setPolicyId(entry.getAttributeValue(ATTR_ID));
        policy.setName(entry.getAttributeValue(ATTR_NAME));
        return policy;
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
